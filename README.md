# Pixel System App Manager

A small, offline-first Android app for rooted Google Pixel devices. It targets Android 13+ (API 33+) and uses `su` to run Package Manager commands for a selected Android user/profile.

## Features

- Detects available Android user/profile IDs using `pm list users`.
- Lists system package names, including apps currently uninstalled for the selected user.
- Shows whether each package is installed for the selected user.
- Removes selected packages only for the selected user with `pm uninstall --user <ID> <PACKAGE>`.
- Restores packages with `cmd package install-existing --user <ID> <PACKAGE>`.
- Searches package names and supports multi-select.
- Blocks removal of a short list of critical system packages.
- Does not contact the network or delete APK files from system partitions.

## Root requirements

Magisk, KernelSU/KernelSU Next, APatch, or another root solution must provide `su`. Approve the superuser prompt when requested.

## Build the APK

1. Open the **Actions** tab.
2. Select **Build Android APK** and run the workflow, or push a commit to `main`.
3. Open the completed run and download the `PixelSystemAppManager-debug-apk` artifact.
4. Extract the artifact ZIP to get `app-debug.apk`.

## Important

`pm uninstall --user` removes availability for the selected user; it does not delete the underlying system APK. Removing system packages can still break features. Make a backup and verify each package's purpose before removal. The app includes confirmation prompts and blocks a small list of core packages, but cannot anticipate every device-specific dependency.
