# GitHub Actions — Required Secrets

The `release` job in `ci.yml` requires four repository secrets.
Configure them at: **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Description |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `.keystore` / `.jks` file |
| `RELEASE_STORE_PASSWORD` | Password for the keystore itself |
| `RELEASE_KEY_ALIAS` | Alias of the signing key inside the keystore (default: `doit`) |
| `RELEASE_KEY_PASSWORD` | Password for the signing key |

## Generate the keystore (one-time)

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias doit \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

## Encode it for the secret

```bash
base64 -w 0 release.keystore
```

Paste the output as the value of `RELEASE_KEYSTORE_BASE64`.

## Notes

- The `release` job only runs on pushes to `main`/`master`, never on PRs.
- Unit tests and lint run on every push and every PR (the `test` job).
- The signed AAB is uploaded as a GitHub Actions artifact (retained 30 days).
  Download it from the Actions tab to upload manually to Google Play, or wire
  up the `google-github-actions/upload-to-play` action when you're ready for
  automated publishing.
- **Never commit** `release.keystore` or `keystore.properties` — both are
  already listed in `.gitignore`.
