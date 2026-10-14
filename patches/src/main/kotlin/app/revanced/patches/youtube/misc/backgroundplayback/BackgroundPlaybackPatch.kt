package app.revanced.patches.youtube.misc.backgroundplayback

import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagHookPatch
import app.revanced.patches.youtube.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.youtube.misc.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.backgroundPlaybackManagerShortsMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/BackgroundPlaybackPatch;"

@Suppress("unused")
val removeBackgroundPlaybackRestrictionsPatch = bytecodePatch(
    name = "Remove background playback restrictions",
    description = "Removes restrictions on background playback, including playing kids videos in the background.",
) {
    dependsOn(
        resourceMappingPatch,
        addResourcesPatch,
        sharedExtensionPatch,
        booleanFeatureFlagHookPatch,
        playerTypeHookPatch,
        videoInformationPatch,
        settingsPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
    )

    apply {
        addResources("youtube", "misc.backgroundplayback.backgroundPlaybackPatch")

        PreferenceScreen.SHORTS.addPreferences(
            SwitchPreference("revanced_shorts_disable_background_playback"),
        )

        arrayOf(
            backgroundPlaybackManagerMethod to "isBackgroundPlaybackAllowed",
            backgroundPlaybackManagerShortsMethod to "isBackgroundShortsPlaybackAllowed",
        ).forEach { (method, integrationsMethod) ->
            method.apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN).forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsAtControlFlowLabel(
                        index,
                        """
                            invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->$integrationsMethod(Z)Z
                            move-result v$register 
                        """
                    )
                }
            }
        }

        // Enable background playback option in YouTube settings
        backgroundPlaybackSettingsMethodMatch.let {
            navigate(it.immutableMethod).to(it[0]).stop().returnEarly(true)
        }

        // Force allowing background play for Shorts.
        overrideBooleanFeatureFlag(SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG, true)

        // Force allowing background play for videos labeled for kids.
        kidsBackgroundPlaybackPolicyControllerMethod.returnEarly()

        // Client flag that interferes with background playback of some video types.
        // Exact purpose is not clear and it's used in ~ 100 locations.
        overrideBooleanFeatureFlag(NEW_PLAYER_TYPE_ENUM_FEATURE_FLAG, false)
    }
}
