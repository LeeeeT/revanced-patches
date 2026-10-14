package app.revanced.patches.youtube.misc.spoof

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.shared.misc.settings.preference.NonInteractivePreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.spoof.spoofVideoStreamsPatch
import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagHookPatch
import app.revanced.patches.youtube.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod

/**
 * Feature flag that enables different code for parsing and starting video playback.
 * If this flag is enabled while stream spoofing then videos never start playback.
 */
private const val PLAYBACK_START_DESCRIPTOR_FEATURE_FLAG = 45665455L

val spoofVideoStreamsPatch = spoofVideoStreamsPatch(
    extensionClassDescriptor = "Lapp/revanced/extension/youtube/patches/spoof/SpoofVideoStreamsPatch;",
    getMainActivityOnCreateMethod = BytecodePatchContext::mainActivityOnCreateMethod::get,
    // The media fetch hot config flags are not present in the supported version.
    fixMediaFetchHotConfig = { false },
    fixMediaFetchHotConfigAlternative = { false },
    // The flag accessor is inlined, so the flag is overridden using the feature flag hook instead.
    fixParsePlaybackResponseFeatureFlag = { false },

    block = {
        compatibleWith(
            "com.google.android.youtube"(
                "21.41.525-SECONDARY",
            ),
        )

        dependsOn(
            userAgentClientSpoofPatch,
            settingsPatch,
            booleanFeatureFlagHookPatch,
        )
    },

    executeBlock = {
        addResources("youtube", "misc.fix.playback.spoofVideoStreamsPatch")

        overrideBooleanFeatureFlag(
            PLAYBACK_START_DESCRIPTOR_FEATURE_FLAG,
            "Lapp/revanced/extension/shared/spoof/SpoofVideoStreamsPatch;->usePlaybackStartFeatureFlag(Z)Z",
        )

        PreferenceScreen.MISC.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_spoof_video_streams_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("revanced_spoof_video_streams"),
                    ListPreference("revanced_spoof_video_streams_client_type"),
                    NonInteractivePreference(
                        // Requires a key and title but the actual text is chosen at runtime.
                        key = "revanced_spoof_video_streams_about",
                        summaryKey = null,
                        tag = "app.revanced.extension.youtube.settings.preference.SpoofVideoStreamsSideEffectsPreference",
                    ),
                    SwitchPreference("revanced_spoof_video_streams_av1"),
                    SwitchPreference("revanced_spoof_video_streams_stats_for_nerds"),
                ),
            ),
        )
    },
)
