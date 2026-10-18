# `OpenRuStore`

oss android client for the [rustore](https://www.rustore.ru/)

## notes on the API

goes against `https://backapi.rustore.ru` with a `deviceType` header and a `ruStoreVerCode` header claiming a recent store build,
which some answers (download links above all) depend on.

- `GET applicationData/apps` search, `GET applicationData/overallInfo/{package}` app details
- `GET applicationData/allAppVersionWhatsNew/{appId}` version history, `GET comment/comment` reviews
- `POST v3/showcase/apps/download-link` returns the universal package; apps published as an app
  bundle stay silent there and need `POST applicationData/v2/download-link` with a device profile,
  which answers with one url per split

downloads are `.zip` archives holding the apk plus baseline profiles; the same object under an
`.apk` extension is the bare package, which is what the links sheet offers for manual downloads

da cdn (`static-m.rustore.ru`) is signed by the Russian Trusted Root CA, which Android does not
trust out of the box, so the certificate is bundled in `res/raw` and wired up for `rustore.ru` only
through `res/xml/network_security_config.xml`