<p align="center">
  <picture>
    <source
      width="256px"
      media="(prefers-color-scheme: dark)"
      srcset="assets/revanced-headline/revanced-headline-vertical-dark.svg"
    >
    <img 
      width="256px"
      src="assets/revanced-headline/revanced-headline-vertical-light.svg"
    >
  </picture>
  <br>
  <a href="https://revanced.app/">
     <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="assets/revanced-logo/revanced-logo.svg" />
         <img height="24px" src="assets/revanced-logo/revanced-logo.svg" />
     </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://github.com/ReVanced">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://i.ibb.co/dMMmCrW/Git-Hub-Mark.png" />
           <img height="24px" src="https://i.ibb.co/9wV3HGF/Git-Hub-Mark-Light.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="http://revanced.app/discord">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://reddit.com/r/revancedapp">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://t.me/app_revanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://x.com/revancedapp">
      <picture>
         <source media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/93124920/270180600-7c1b38bf-889b-4d68-bd5e-b9d86f91421a.png">
         <img height="24px" src="https://user-images.githubusercontent.com/93124920/270108715-d80743fa-b330-4809-b1e6-79fbdc60d09c.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://www.youtube.com/@ReVanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
     </picture>
   </a>
   <br>
   <br>
   Continuing the legacy of Vanced
</p>

# 🧩 ReVanced Patches

![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)

This repository contains a collection of ReVanced Patches.

This fork ports the YouTube patches to YouTube **21.41.525** and drops support for all other YouTube versions.
Patches for other apps are unchanged.

## ❓ About

Patches are small modifications to Android apps that allow you to change the behavior of or add new features,
block ads, customize the appearance, and much more.

## 💪 Features

Some of the features the patches provide are:

* 🚫 **Block ads**: Say goodbye to ads
* ⭐ **Customize your app**: Personalize the appearance of apps with various layouts and themes
* 🪄 **Add new features**: Extend the functionality of apps with lots of new features
* ⚙️ **Miscellaneous and general purpose**: Rename packages, enable debugging, disable screen capture restrictions,
  export activities, etc.
* ✨ **And much more!**

For a complete list of all available patches, visit [revanced.app/patches](https://revanced.app/patches).

## 🚀 How to get started

You can use [ReVanced CLI](https://github.com/ReVanced/revanced-cli) or [ReVanced Manager](https://github.com/ReVanced/revanced-manager) to use ReVanced Patches.

### ReVanced Manager

Add the bundle to ReVanced Manager by URL. Manager tracks this repository's releases and offers updates
whenever a new release is published.

1. Open ReVanced Manager and go to the **Patches** tab.
2. Tap the ✏️ button in the bottom-right, then the **+** button.
3. Choose **Enter URL** and paste:

   ```
   https://github.com/LeeeeT/revanced-patches/releases/latest/download/revanced-asset.json
   ```

4. Save. The bundle now appears as a patch source in Manager.
   Patch YouTube **21.41.525** with it.

On non-root devices, keep the default **GmsCore support** patch enabled and install
[GmsCore](https://github.com/ReVanced/GmsCore).
On rooted devices, the patched app can instead be mounted over the stock YouTube 21.41.525
(disable **GmsCore support** and choose the mount/root install).

### ReVanced CLI

Download `patches.rvp` from the [latest release](https://github.com/LeeeeT/revanced-patches/releases/latest) and run:

```bash
revanced-cli patch -p patches.rvp -b com.google.android.youtube_21.41.525.apk
```

## 📚 Everything else

### 📙 Contributing

Thank you for considering contributing to ReVanced Patches. You can find the contribution guidelines [here](CONTRIBUTING.md).

### 🛠️ Building

To build ReVanced Patches, you can follow the [ReVanced documentation](https://github.com/ReVanced/revanced-documentation).

#### Nix development environment

The YouTube patches target YouTube `21.41.525` only.
`flake.nix` provides a development shell with JDK 17, the Android SDK (build tools, platform tools, emulator and an
x86_64 Google APIs system image), ReVanced CLI, apktool, jadx and scrcpy:

```bash
nix develop

# Build the patches bundle (GitHub Packages credentials are required, see the ReVanced documentation).
./gradlew :patches:build -x test

# Patch YouTube with the locally built bundle.
# Defaults to ../com.google.android.youtube_21.41.525.apk and build/youtube-patched.apk.
rv-patch [input.apk] [output.apk] [extra revanced-cli patch arguments]

# Create (once) and boot the "revanced" emulator. The AVD is stored in .avd/.
rv-emulator -no-window
```

The patched app requires [GmsCore](https://github.com/ReVanced/GmsCore) to sign in and play videos, so install it on
the emulator before the patched APK.
The emulator image ships with a preinstalled YouTube that captures `youtube.com` links.
To remove it, boot with `rv-emulator -writable-system`, run `adb root && adb remount`,
delete the YouTube app directory from `/product/app` and reboot.

## 📜 License

ReVanced Patches is licensed under the GPLv3 license. Please see the [license file](LICENSE) for more information.
[tl;dr](https://www.tldrlegal.com/license/gnu-general-public-license-v3-gpl-3) you may copy, distribute and modify ReVanced Patches as long as you track changes/dates in source files.
Any modifications to ReVanced Patches must also be made available under the GPL,
along with build & install instructions.
