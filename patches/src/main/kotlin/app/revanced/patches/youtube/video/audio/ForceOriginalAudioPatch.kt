package app.revanced.patches.youtube.video.audio

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.audio.forceOriginalAudioPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod

@Suppress("unused")
val forceOriginalAudioPatch = forceOriginalAudioPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
        )
    },
    fixUseLocalizedAudioTrackFlag = { false },
    getMainActivityOnCreateMethod = BytecodePatchContext::mainActivityOnCreateMethod::get,
    subclassExtensionClassDescriptor = "Lapp/revanced/extension/youtube/patches/ForceOriginalAudioPatch;",
    preferenceScreen = PreferenceScreen.VIDEO,
)
