package app.revanced.patches.youtube.layout.buttons.navigation

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.layout.toolbar.hookToolbar
import app.revanced.patches.youtube.layout.toolbar.toolbarHookPatch
import app.revanced.patches.youtube.misc.contexthook.Endpoint
import app.revanced.patches.youtube.misc.contexthook.addOSNameHook
import app.revanced.patches.youtube.misc.contexthook.hookClientContextPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagHookPatch
import app.revanced.patches.youtube.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.youtube.misc.navigation.hookNavigationButtonCreated
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import kotlin.collections.plusAssign

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/NavigationBarPatch;"

val navigationBarPatch = bytecodePatch(
    name = "Navigation bar",
    description = "Adds options to hide and change the bottom navigation bar (such as the Shorts button) "
            + " and the upper navigation toolbar."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        navigationBarHookPatch,
        booleanFeatureFlagHookPatch,
        hookClientContextPatch,
        toolbarHookPatch
    )

    compatibleWith(
        "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
    )

    apply {
        addResources("youtube", "layout.buttons.navigation.navigationBarPatch")

        val preferences = mutableSetOf(
            SwitchPreference("revanced_hide_home_button"),
            SwitchPreference("revanced_hide_shorts_button"),
            SwitchPreference("revanced_hide_create_button"),
            SwitchPreference("revanced_hide_subscriptions_button"),
            SwitchPreference("revanced_hide_notifications_button"),
            SwitchPreference("revanced_switch_create_with_notifications_button"),
            SwitchPreference("revanced_hide_navigation_button_labels"),
            SwitchPreference("revanced_narrow_navigation_buttons"),
        )

        preferences += SwitchPreference("revanced_disable_translucent_navigation_bar_light")
        preferences += SwitchPreference("revanced_disable_translucent_navigation_bar_dark")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_disable_translucent_status_bar")
        )

        preferences += SwitchPreference("revanced_navigation_bar_animations")

        PreferenceScreen.GENERAL.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_navigation_buttons_screen",
                sorting = Sorting.UNSORTED,
                preferences = preferences
            )
        )

        // Switch create with notifications button.
        addOSNameHook(
            Endpoint.GUIDE,
            "${EXTENSION_CLASS_DESCRIPTOR}->switchCreateWithNotificationButton(Ljava/lang/String;)Ljava/lang/String;",
        )

        // Hide navigation button labels.
        createPivotBarMethodMatch.let {
            it.method.apply {
                val setTextIndex = it[0]
                val targetRegister = getInstruction<FiveRegisterInstruction>(setTextIndex).registerC

                addInstruction(
                    setTextIndex,
                    "invoke-static { v$targetRegister }, " +
                            "$EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtonLabels(Landroid/widget/TextView;)V",
                )
            }
        }

        // Hook navigation button created, in order to hide them.
        hookNavigationButtonCreated(EXTENSION_CLASS_DESCRIPTOR)

        // Force on/off translucent effect on status bar and navigation buttons.
        overrideBooleanFeatureFlag(
            TRANSLUCENT_STATUS_BAR_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationStatusBar(Z)Z",
        )

        overrideBooleanFeatureFlag(
            TRANSLUCENT_NAVIGATION_BUTTONS_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationButtons(Z)Z",
        )

        overrideBooleanFeatureFlag(
            TRANSLUCENT_NAVIGATION_BUTTONS_SYSTEM_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationButtons(Z)Z",
        )

        overrideBooleanFeatureFlag(
            ANIMATED_NAVIGATION_TABS_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->useAnimatedNavigationButtons(Z)Z",
        )

        arrayOf(
            pivotBarChangedMethodMatch,
            pivotBarStyleMethodMatch
        ).forEach { match ->
            match.method.apply {
                val targetIndex = match[1] + 1
                val register = getInstruction<OneRegisterInstruction>(targetIndex - 1).registerA

                addInstructions(
                    targetIndex,
                    """
                        invoke-static { v$register }, ${EXTENSION_CLASS_DESCRIPTOR}->enableNarrowNavigationButton(Z)Z
                        move-result v$register
                    """
                )
            }

        }

        //
        // Toolbar.
        //

        val toolbarPreferences = mutableSetOf(
            SwitchPreference("revanced_hide_toolbar_create_button"),
            SwitchPreference("revanced_hide_toolbar_notification_button"),
            SwitchPreference("revanced_hide_toolbar_search_button")
        )

        PreferenceScreen.GENERAL.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_toolbar_screen",
                sorting = Sorting.UNSORTED,
                preferences = toolbarPreferences
            )
        )

        hookToolbar("${EXTENSION_CLASS_DESCRIPTOR}->hideCreateButton")
        hookToolbar("${EXTENSION_CLASS_DESCRIPTOR}->hideNotificationButton")
        hookToolbar("${EXTENSION_CLASS_DESCRIPTOR}->hideSearchButton")

        //
        // Wide searchbar.
        //

        // YT removed the legacy text search text field all code required to use it.
        // This functionality could be restored by adding a search text field to the toolbar
        // with a listener that artificially clicks the toolbar search button.
    }
}
