# Huawei AppGallery upload

The Android app is **Simple Dialer**, package `info.pakkudev.simple_dialer`, Huawei app ID `6917621412793613771`. Publishing commands target this existing AppGallery entry.

The service account credential is read from `svc/svc-acc.json` by default. `svc/` is excluded by `.gitignore`. Override the location with the `HUAWEI_SERVICE_ACCOUNT` environment variable. The helper signs a short-lived PS256 JWT in memory using Node's built-in crypto module; it does not print or persist the JWT or private key. No third-party package installation is required. Node.js 22 or newer is required.

## Commands

Run from the project root:

```powershell
node scripts/huawei-connect.mjs lookup info.pakkudev.simple_dialer
node scripts/huawei-connect.mjs info 6917621412793613771 svc/app-info.json
node scripts/huawei-connect.mjs packages 6917621412793613771
```

Commands below modify the Huawei release draft:

```powershell
node scripts/huawei-connect.mjs update-info 6917621412793613771 artwork/store/huawei/category.json
node scripts/huawei-connect.mjs update-language 6917621412793613771 artwork/store/huawei/en-US/listing.json
node scripts/huawei-connect.mjs upload 6917621412793613771 5 en-US app/build/outputs/apk/release/simple-dialer-1.0.3-28.apk
node scripts/huawei-connect.mjs upload 6917621412793613771 0 en-US artwork/store/huawei/en-US/app-icon-1024.png
$huaweiScreenshots = (Get-ChildItem artwork/store/huawei/en-US/screenshots/*.png | Sort-Object Name).FullName
node scripts/huawei-connect.mjs upload 6917621412793613771 2 en-US @huaweiScreenshots
```

`upload` first checks that the app remains a draft and that release countries have been saved. It computes the file's SHA-256, obtains a signed Huawei upload URL, transfers the binary, then binds its object ID to the app. Only the provided files are transmitted. After transfer, a receipt is saved under ignored `svc/` before binding, allowing investigation of a binding failure without exposing temporary upload authorization headers. Do not assume success based on the transfer message alone: the binding response must also succeed, and app/package information should then be queried.

An upload does not submit the app for review or publish it. This helper deliberately has no release-submission command. Store readiness fields such as a public privacy policy, support details and content rating must be completed before submission.

## Category

The legacy Android IDs **Social (26) / Communication (10041)** were rejected for this entry with code `204144757`, asking for the newer category system. The newer **Tools (10000000)** category with **Communications (15000018)** as its main tag was accepted and read back successfully. Its reusable request is `artwork/store/huawei/category.json`.

## Worldwide distribution and current upload status

The distribution configuration saved on November 10, 2026 covers 200 countries/regions, including mainland China. Huawei rejected `US` (United States) and `YK` (Kosovo) as unsupported. The reusable request is `artwork/store/huawei/worldwide.json`. It includes `OTHER` to request future regions; the server returned only the 200 current country codes, so future automatic expansion has not been verified.

The English listing and category are saved. The app remains a release draft. No APK or image transfer has succeeded yet: upload URL preparation returns code `204144694`, reporting `distContryList is empty and usage route site is not China`, even though both API versions return the saved country list. Saving the country list through v2 and v3 did not resolve this mismatch. No submission or publication was attempted.

To retry, open **Distribute > Version information > Draft**, select **All countries/regions**, and click **Save Draft** in AppGallery Connect. Then retry the upload command above and verify both the binding response and the app package list.

Country reference: https://developer.huawei.com/consumer/en/doc/appgallery-connect-references/agcapi-reference-countrycode-0000001111845110/

## References

- Authentication: https://developer.huawei.com/consumer/en/doc/appgallery-connect-guides/agcapi-getstarted-0000001111845114/
- Category IDs: https://developer.huawei.com/consumer/en/doc/AppGallery-connect-References/agcapi-reference-apptype-0000001158365069/
- Upload URL: https://developer.huawei.com/consumer/en/doc/AppGallery-connect-References/agcapi-upload-url-new-0000001111685200/
- Bind uploaded files: https://developer.huawei.com/consumer/en/doc/AppGallery-connect-References/agcapi-app-file-info-0000001111685202/
