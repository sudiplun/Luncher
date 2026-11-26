# CI/CD Setup

This repository uses GitHub Actions for continuous integration (CI) and release delivery (CD).

## Workflows

### 1. Continuous Integration ([`build.yml`](../.github/workflows/build.yml) & [`validate-gradle-wrapper.yaml`](../.github/workflows/validate-gradle-wrapper.yaml))
- **Triggers:** `push` and `pull_request` targeting `main` (ignoring `**/*.md` documentation-only changes).
- **Environment:** `ubuntu-latest`, Temurin JDK `17`, Gradle `9.9.2` via `gradle/actions/setup-gradle@v4`.
- **Checks executed:**
  1. Gradle wrapper checksum validation (`gradle/actions/wrapper-validation@v4`)
  2. Unit tests: `./gradlew testDebugUnitTest`
  3. Instrumented test compilation: `./gradlew compileDebugAndroidTestKotlin`
  4. Android Lint: `./gradlew lintDebug`
  5. Debug & Release builds: `./gradlew assembleDebug assembleRelease`
- Built APKs are uploaded as workflow artifacts (retained for 14 days).

### 2. Android Integration Tests ([`integration_test.yml`](../.github/workflows/integration_test.yml))
- **Trigger:** `workflow_dispatch` (manual run from the Actions tab).
- Boots an Android API 28 emulator and runs `./gradlew connectedAndroidTest --stacktrace`.

### 3. Release Delivery ([`release.yml`](../.github/workflows/release.yml))
- **Triggers:** Pushing a version tag matching `v*` (e.g., `v1.6.7`), or manual `workflow_dispatch`.
- **Steps:**
  1. Verifies all required signing secrets are present.
  2. Decodes the release keystore to `app/release.jks` and writes `release.properties` (both gitignored and removed after the build).
  3. Runs `./gradlew testDebugUnitTest assembleRelease` to produce signed per-ABI split APKs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`).
  4. Stages the APKs as `Luncher-<tag>-<abi>.apk` (rejecting any `-unsigned.apk`).
  5. Creates a GitHub Release for the pushed `v*` tag with auto-generated release notes and uploads all split APKs as release assets.

## Required GitHub Actions Secrets

Configure these repository secrets under **Settings → Secrets and variables → Actions** (or via the GitHub CLI) before pushing a `v*` release tag:

| Secret Name | Description |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `.jks` signing keystore file |
| `RELEASE_STORE_PASSWORD` | Keystore password (`storePassword` in `release.properties`) |
| `RELEASE_KEY_ALIAS` | Signing key alias (`keyAlias` in `release.properties`) |
| `RELEASE_KEY_PASSWORD` | Signing key password (`keyPassword` in `release.properties`) |

### Setting Secrets with GitHub CLI

```bash
base64 -w 0 app/release.jks | gh secret set RELEASE_KEYSTORE_BASE64
gh secret set RELEASE_STORE_PASSWORD
gh secret set RELEASE_KEY_ALIAS
gh secret set RELEASE_KEY_PASSWORD
```

## Cutting a Release

1. Update `versionCode` and `versionName` in `app/build.gradle` on a PR and merge to `main`.
2. Ensure all four `RELEASE_*` secrets are configured in the repository.
3. Create and push a matching `v*` tag on `main`:
   ```bash
   git checkout main
   git pull origin main
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```
