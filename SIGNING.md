# Signing configuration

Private signing credentials are managed outside this repository in
`~/.signing/simple-dialer/key.properties` (`%USERPROFILE%\.signing\simple-dialer\key.properties` on Windows).
The build resolves the current user home directory; no username is embedded in Gradle.
Set `SIGNING_ROOT` to use a different base directory containing the same project subfolders.

The properties file contains `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.
`storeFile` is relative to the properties file, or an absolute path. The current keystore
is `simple-dialer-release.jks` and the alias is `simple-dialer`.

Certificate SHA-256:

```text
D3:41:2C:61:19:93:EA:BF:5E:41:BF:85:A8:B6:2A:B5:17:E9:78:00:1F:0E:58:73:78:10:52:93:41:83:D9:27
```

Public certificate exports are stored beside the keystore as `upload_cert.der` and
`upload_certificate.pem`.
The public files alone cannot sign an application.

Verify Gradle's selected release certificate with:

```powershell
.\gradlew.bat :app:signingReport :app:validateSigningRelease
```

Back up the entire private `.signing` directory to encrypted separate storage, and store
its passwords and recovery information in a password manager. The central directory is
a management location, not an off-device backup. Keep all private signing files out of Git.
