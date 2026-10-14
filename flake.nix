{
  description = "ReVanced Patches development environment (YouTube 21.41.525)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachSystem [ "x86_64-linux" ] (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        androidSdkVersion = "35";

        android = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [
            "34"
            "35"
            "36"
          ];
          buildToolsVersions = [
            "34.0.0"
            "35.0.0"
            "36.0.0"
          ];
          includeEmulator = true;
          includeSystemImages = true;
          systemImageTypes = [ "google_apis" ];
          abiVersions = [ "x86_64" ];
          includeNDK = false;
        };

        androidSdk = android.androidsdk;
        androidHome = "${androidSdk}/libexec/android-sdk";

        avdName = "revanced";

        # Creates (once) and boots an x86_64 Google APIs emulator suitable for testing patched YouTube.
        emulatorScript = pkgs.writeShellScriptBin "rv-emulator" ''
          set -euo pipefail
          export ANDROID_AVD_HOME="''${ANDROID_AVD_HOME:-$PWD/.avd}"
          mkdir -p "$ANDROID_AVD_HOME"
          if [ ! -d "$ANDROID_AVD_HOME/${avdName}.avd" ]; then
            echo no | ${androidSdk}/bin/avdmanager create avd \
              --force --name ${avdName} \
              --package "system-images;android-${androidSdkVersion};google_apis;x86_64" \
              --device pixel_6
            cat >> "$ANDROID_AVD_HOME/${avdName}.avd/config.ini" <<EOF
          hw.ramSize=4096
          disk.dataPartition.size=8G
          hw.keyboard=yes
          hw.gpu.enabled=yes
          hw.gpu.mode=swiftshader_indirect
          EOF
          fi
          exec ${androidHome}/emulator/emulator -avd ${avdName} \
            -no-snapshot -no-audio -no-boot-anim -gpu swiftshader_indirect "$@"
        '';

        # Patches a YouTube APK with the locally built patches.
        patchScript = pkgs.writeShellScriptBin "rv-patch" ''
          set -euo pipefail
          apk="''${1:-../com.google.android.youtube_21.41.525.apk}"
          out="''${2:-build/youtube-patched.apk}"
          if [ $# -ge 2 ]; then shift 2; else shift $#; fi
          mkdir -p "$(dirname "$out")"
          bundle=$(ls -t patches/build/libs/patches-*.rvp | grep -v -- -sources | head -1)
          exec ${pkgs.revanced-cli}/bin/revanced-cli patch \
            --patches "$bundle" --bypass-verification \
            --temporary-files-path build/revanced-tmp \
            --out "$out" \
            --keystore build/test.keystore \
            --purge \
            "$@" "$apk"
        '';
      in
      {
        devShells.default = pkgs.mkShell {
          packages = [
            pkgs.jdk17
            androidSdk
            pkgs.revanced-cli
            pkgs.apktool
            pkgs.jadx
            pkgs.dex2jar
            pkgs.unzip
            pkgs.zip
            pkgs.jq
            pkgs.python3
            pkgs.scrcpy
            emulatorScript
            patchScript
          ];

          JAVA_HOME = pkgs.jdk17.home;
          ANDROID_HOME = androidHome;
          ANDROID_SDK_ROOT = androidHome;
          # Use the SDK's aapt2 instead of the Maven one, which is dynamically linked and fails on NixOS.
          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidHome}/build-tools/36.0.0/aapt2";

          shellHook = ''
            export ANDROID_AVD_HOME="$PWD/.avd"
            export PATH="${androidHome}/platform-tools:${androidHome}/emulator:$PATH"
          '';
        };
      }
    );
}
