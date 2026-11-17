// Huawei Connect API helper. Requires Node.js 22+; credentials never leave memory.
// Official auth: https://developer.huawei.com/consumer/en/doc/appgallery-connect-guides/agcapi-getstarted-0000001111845114/
import { readFile, writeFile } from 'node:fs/promises';
import { constants, sign, createHash } from 'node:crypto';
import { resolve, basename } from 'node:path';
import { fileURLToPath } from 'node:url';

const apiOrigin = 'https://connect-api.cloud.huawei.com';
const credentialPath = resolve(process.env.HUAWEI_SERVICE_ACCOUNT || 'svc/svc-acc.json');

function encode(value) {
  return Buffer.from(JSON.stringify(value)).toString('base64url');
}

export async function authorization() {
  const credential = JSON.parse(await readFile(credentialPath, 'utf8'));
  if (!credential.key_id || !credential.private_key || !credential.sub_account) {
    throw new Error('Service account credential fields are missing.');
  }
  const now = Math.floor(Date.now() / 1000);
  const content = `${encode({ kid: credential.key_id, typ: 'JWT', alg: 'PS256' })}.${encode({
    aud: 'https://oauth-login.cloud.huawei.com/oauth2/v3/token',
    iss: credential.sub_account, iat: now, exp: now + 3600,
  })}`;
  const signature = sign('sha256', Buffer.from(content), {
    key: credential.private_key,
    padding: constants.RSA_PKCS1_PSS_PADDING,
    saltLength: constants.RSA_PSS_SALTLEN_DIGEST,
  });
  return `Bearer ${content}.${signature.toString('base64url')}`;
}

export async function request(method, path, body) {
  if (!path.startsWith('/api/')) throw new Error('Expected a Huawei API path.');
  const response = await fetch(new URL(path, apiOrigin), {
    method, redirect: 'error', signal: AbortSignal.timeout(60000),
    headers: { Authorization: await authorization(), 'Content-Type': 'application/json' },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const text = await response.text();
  let data;
  try { data = JSON.parse(text); } catch { throw new Error(`Huawei returned non-JSON (HTTP ${response.status}).`); }
  const result = typeof data.ret === 'string' ? JSON.parse(data.ret) : data.ret;
  if (!response.ok || (result && Number(result.code) !== 0)) {
    throw new Error(`Huawei HTTP ${response.status}: ${JSON.stringify(result || data)}`);
  }
  return data;
}

export async function upload(appId, filePath) {
  const current = await request('GET', `/api/publish/v2/app-info?appId=${appId}`);
  if (current.appInfo?.releaseState !== 7) throw new Error('Uploads are restricted to an existing release draft.');
  if (!current.appInfo.publishCountry) throw new Error('Save the intended release countries before uploading files.');
  const content = await readFile(filePath);
  const sha256 = createHash('sha256').update(content).digest('hex');
  const query = new URLSearchParams({ appId, fileName: basename(filePath),
    sha256, contentLength: String(content.length), releaseType: '1',
    chineseMainlandFlag: current.appInfo.publishCountry.split(',').includes('CN') ? '1' : '0' });
  const prepared = await request('GET', `/api/publish/v2/upload-url/for-obs?${query}`);
  const info = prepared.urlInfo;
  if (!info?.url || !info.objectId || info.method !== 'PUT') {
    throw new Error(`Unexpected upload response fields: ${Object.keys(prepared).join(', ')}`);
  }
  const destination = new URL(info.url);
  const trustedDomains = ['huawei.com', 'hicloud.com', 'dbankcloud.com', 'dbankcdn.com', 'huaweicloud.com', 'myhuaweicloud.com'];
  if (destination.protocol !== 'https:' || !trustedDomains.some(d => destination.hostname === d || destination.hostname.endsWith(`.${d}`))) {
    throw new Error(`Unexpected Huawei upload host: ${destination.hostname}`);
  }
  const headers = typeof info.headers === 'string' ? JSON.parse(info.headers) : { ...info.headers };
  const response = await fetch(destination, { method: 'PUT', headers, body: content,
    redirect: 'error', signal: AbortSignal.timeout(120000) });
  if (!response.ok) throw new Error(`File upload failed (HTTP ${response.status}).`);
  console.log(`Uploaded ${basename(filePath)} (${content.length} bytes), SHA-256 ${sha256}`);
  return { fileName: basename(filePath), fileDestUrl: info.objectId, sha256, bytes: content.length };
}

async function main() {
  const [command, argument, output, ...extra] = process.argv.slice(2);
  let data;
  if (command === 'lookup') {
    if (!argument) throw new Error('Usage: node scripts/huawei-connect.mjs lookup <package-name>');
    data = await request('GET', `/api/publish/v2/appid-list?packageName=${encodeURIComponent(argument)}`);
  } else if (command === 'info') {
    if (!/^\d+$/.test(argument || '')) throw new Error('An AppGallery numeric app ID is required.');
    data = await request('GET', `/api/publish/v2/app-info?appId=${argument}`);
  } else if (command === 'update-info') {
    if (!/^\d+$/.test(argument || '') || !output) throw new Error('Usage: update-info <app-id> <body-json>');
    data = await request('PUT', `/api/publish/v2/app-info?appId=${argument}`, JSON.parse(await readFile(output, 'utf8')));
    console.log(JSON.stringify(data));
    return;
  } else if (command === 'update-language') {
    if (!/^\d+$/.test(argument || '') || !output) throw new Error('Usage: update-language <app-id> <body-json>');
    data = await request('PUT', `/api/publish/v2/app-language-info?appId=${argument}`, JSON.parse(await readFile(output, 'utf8')));
    console.log(JSON.stringify(data));
    return;
  } else if (command === 'packages') {
    if (!/^\d+$/.test(argument || '')) throw new Error('An AppGallery numeric app ID is required.');
    data = await request('GET', `/api/publish/v2/package-list?appId=${argument}&fromRecCount=1&maxReqCount=100`);
    // Do not print short-lived signed download URLs.
    if (!output) {
      console.log(JSON.stringify({ result: data.ret, totalCount: data.totalCount, packages: data.pkgList?.map(p => ({
        pkgVersion: p.pkgVersion, fileName: p.fileName, versionName: p.versionName,
        versionCode: p.versionCode, packageSize: p.packageSize, shaCode: p.shaCode,
      })) }, null, 2));
      return;
    }
  } else if (command === 'upload') {
    if (!/^\d+$/.test(argument || '') || !['0', '2', '5'].includes(output) || extra.length < 2) {
      throw new Error('Usage: upload <app-id> <file-type: 0 icon, 2 screenshots, 5 APK> <language> <files...>');
    }
    const fileType = Number(output);
    const [lang, ...paths] = extra;
    const files = [];
    for (const path of paths) files.push(await upload(argument, path));
    const body = { fileType, files: files.map(({ fileName, fileDestUrl }) => ({ fileName, fileDestUrl })),
      ...(fileType === 5 ? {} : { lang, deviceType: 4 }), ...(fileType === 2 ? { imgShowType: 0 } : {}) };
    // Record uploaded object IDs before binding, allowing recovery without reuploading.
    const receipt = `svc/huawei-upload-${argument}-${fileType}-${Date.now()}.json`;
    await writeFile(receipt, JSON.stringify({ appId: argument, files, body }, null, 2) + '\n');
    console.log(`Upload receipt saved to ${receipt}`);
    data = await request('PUT', `/api/publish/v2/app-file-info?appId=${argument}`, body);
    console.log(JSON.stringify(data));
    return;
  } else {
    throw new Error('Supported commands: lookup, info, packages, update-info, update-language, upload');
  }
  if (output) {
    await writeFile(output, JSON.stringify(data, null, 2) + '\n');
    console.log(`Response saved to ${output}`);
    console.log(JSON.stringify({ result: data.ret, appInfoFields: Object.keys(data.appInfo || {}), languages: data.languages?.map(l => l.lang) }));
  } else console.log(JSON.stringify(data, null, 2));
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
