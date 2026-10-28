# Development notes

Use the existing `/workspace/Weight-Loss` checkout. Each cloud task is isolated; do not create a worktree unless explicitly requested.

## Layers

- `Domain.kt`: immutable models, local date/number calculations, monotonic timer, illustrative catalogue.
- `Storage.kt`: Room DAO, versioned schema, encrypted JSON serialization; the UUID is authenticated as AES-GCM associated data. Payloads are encrypted; opaque IDs, revisions and queue sequencing metadata remain visible in Room. Backups are disabled because keys must not be restored separately from data.
- `WellbeingViewModel.kt`: StateFlow, explicit mutations, private repositories, atomic timer completion, profile and DataStore appearance preference.
- `MainActivity.kt`: Compose UI, responsive navigation, validated forms, document export and confirmation flows.

No credentials, server URLs or network permission are needed. Record decryption errors are surfaced; data is never silently reset. Existing UUIDs are upserted, so repeated writes replace rather than duplicate a record. An encrypted transactional outbox and gateway interfaces support a future sync provider; no server is connected. See SYNC_READINESS.md.

## Cloud tools

Run `scripts/bootstrap-android.sh` once, then `source /workspace/toolchains/android-env.sh` in each shell. Installed files and caches persist; emulator processes do not.

Optional software-emulator setup (no KVM is available in the tested cloud instance):

```sh
source /workspace/toolchains/android-env.sh
mapfile -t SDK_PROXY < /workspace/toolchains/sdk-proxy.args
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "${SDK_PROXY[@]}" 'emulator' 'system-images;android-28;default;x86'
printf 'no\n' | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd --name wellbeing-api28 --package 'system-images;android-28;default;x86' --device pixel_3a
"$ANDROID_HOME/emulator/emulator" -avd wellbeing-api28 -no-window -no-audio -no-boot-anim -no-snapshot -accel off -gpu swiftshader_indirect -feature -Vulkan -cores 2 -memory 2048 -skin 480x960
```

Run the last command in a persistent terminal. In another shell, `adb wait-for-device` and check `adb shell getprop sys.boot_completed` returns `1`. Configure the AVD density before boot; ensure package services stay responsive before installation. Build and run `./gradlew :app:connectedDebugAndroidTest`. Tests write synthetic entries and the journal UI test deletes them at the end. Use a dedicated test device/profile; do not run against a user's journal. Console-auth warnings do not establish boot readiness.

For local Android Studio use a hardware-accelerated emulator or physical phone. Verify Android 16, TalkBack, larger fonts, Sinhala/Tamil fonts, low-memory devices and wide layouts before a public beta. Do not treat API 28 emulator coverage as API 36 validation.

The API 30 x86_64 software image failed during Android system startup in this cloud instance, before any app tests ran. The lighter API 28 x86 image with Vulkan disabled reached boot completion but also failed to maintain stable system services; app device validation remains blocked. Hardware acceleration is preferable.

For complete release checks run `./scripts/build-release.sh --no-daemon`. Final device-test evidence and native 16 KB alignment checks are in VALIDATION.md.

## Reproducible lint policy

Release dependencies and the Gradle wrapper are pinned. Lint online upgrade notices (`AndroidGradlePluginVersion`, `GradleDependency`, `NewerVersionAvailable`) are informational, so availability of a newer version does not change CI success. This does not suppress correctness, security, missing/extra translation or other lint warnings; those remain blocking. Review dependency updates separately before changing the release toolchain.
