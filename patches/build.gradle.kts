group = "app.revanced"

patches {
    about {
        name = "ReVanced Patches for YouTube 21.41.525"
        description = "ReVanced Patches ported to YouTube 21.41.525"
        source = "git@github.com:LeeeeT/revanced-patches.git"
        author = "LeeeeT"
        contact = "me@leeeet.dev"
        website = "https://github.com/LeeeeT/revanced-patches"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.apksig)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-patches")
            credentials(PasswordCredentials::class)
        }
    }
}

apply(from = "strings-processing.gradle.kts")
