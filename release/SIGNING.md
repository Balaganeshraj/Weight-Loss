# Release signing and placeholder configuration

The AAB is built with R8 and resource shrinking, target/compile SDK 36 and min SDK 26. All three languages are bundled together. Default application ID `com.example.wellbeing.offline` and publisher details are provisional at the owner's request.

An **unsigned** AAB is a real build artifact but is not ready for Play upload. Do not use a debug certificate as a production upload identity. The accompanying `test-signed.apk` uses the local development certificate only, to allow installation and testing of the optimized bundle.

Set these variables securely in your local build/CI environment, without committing values or sending passwords in chat:

- `WELLBEING_KEYSTORE`: absolute path to your upload keystore.
- `WELLBEING_STORE_PASSWORD`: keystore password.
- `WELLBEING_KEY_ALIAS`: upload key alias.
- `WELLBEING_KEY_PASSWORD`: private-key password.

The Gradle build accepts either all four variables or none. Partial configuration fails; it never silently falls back to debug signing for release.

```sh
# With JDK 17 and Android SDK 36 installed (and signing variables securely set):
./gradlew -Pwellbeing.applicationId=YOUR.PRODUCTION.ID \
  :app:testDebugUnitTest :app:lintRelease :app:bundleRelease :app:assembleRelease
```

Replace `YOUR.PRODUCTION.ID` with the final owned application ID before first publication; Play application IDs cannot be renamed later. The Kotlin namespace remains stable and need not equal the application ID.

Verify the signed AAB with `jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab`; verify the actual upload certificate fingerprint against Play Console. Validate the bundle with Google's bundletool. Release APKs require `apksigner verify`. Keep the keystore and backups outside the source archive. Enroll in Play App Signing and use the upload key as directed by your account.

The generated release bundle includes R8 mapping metadata. Keep mapping files for each released version. Replace/support/host the privacy-policy template and complete the checklist in PLAY_STORE_SUBMISSION.md before upload. No live listing, policy hosting, signing identity, store screenshots or production approval is supplied by placeholders.
