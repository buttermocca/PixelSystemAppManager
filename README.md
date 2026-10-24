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


## v1.5.0 visual design

- A full-height amethyst-to-magenta background with pearlescent glows and sparkle flecks.
- A rounded galaxy-gradient hero header featuring the waving mascot.
- Separate mascot artwork roles: a tilted wink for the launcher icon, a waving pose in the header, and a crystal-origami pose on a crescent moon in the splash screen.
- Purple glass-style profile, search, package summary, and list cards with high-contrast labels and luminous gradient action buttons.
- App icons alongside package names in the system package list.


## v1.5.1 concept-style visual refresh

- Uses the high-detail crystal mascot bitmap without a square tile for the launcher icon.
- Adds a crescent-moon mascot composition to the splash scene and a dedicated galaxy hero banner.
- Reworks the main screen into violet glass cards with luminous magenta buttons, package icons, and pearl-like sparkle highlights.


## v1.5.2 image-backed hero banner

- The rounded main header now uses the full wide crystal-mascot scene artwork instead of a flat vector-only composition.
- The build script reconstructs the artwork from small text chunks into WebP resources before Android resource compilation.


## v1.5.3 — concept artwork integration

- The header banner and portrait splash now use full image-backed crystal-mascot artwork rather than enlarging the simplified vector mascot.
- The launcher foreground uses a separate transparent, winking crystal-mascot image.
- Profile, search, status, summary, and package-list panels use filled translucent violet glass surfaces with luminous pale-pink outlines. The hero title sits directly over the galaxy artwork without a nested card.
- The build reassembles the reviewable WebP source chunks before resource compilation and validates that each image is a genuine WebP file.


## v1.5.4 — startup and artwork layout fixes

- Draw the splash and banner from their image resources directly, avoiding the prior layer-list sizing/cropping behavior.
- Give the title banner a fixed height so its text and artwork cannot unexpectedly stretch the top panel.
- Show a diagnostic retry screen instead of silently closing if UI construction fails.
- Display the actual `su` check output when root access is not granted, and declare package visibility for Android's package-manager icon lookup.
- Adaptive launcher icons use a violet/magenta background and an inset mascot foreground to avoid the old checkerboard/cropped icon appearance.
