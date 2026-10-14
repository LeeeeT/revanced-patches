@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.layout.miniplayer

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreference
import app.revanced.patches.shared.misc.settings.preference.InputType
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.TextPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/MiniplayerPatch;"

@Suppress("unused")
val miniplayerPatch = bytecodePatch(
    name = "Miniplayer",
    description = "Adds options to change the in-app minimized player.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        resourceMappingPatch,
        playerTypeHookPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
    )

    apply {
        addResources("youtube", "layout.miniplayer.miniplayerPatch")

        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_miniplayer_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf<BasePreference>(
                    ListPreference("revanced_miniplayer_type"),
                    SwitchPreference("revanced_miniplayer_hide_subtext"),
                    SwitchPreference("revanced_miniplayer_hide_overlay_buttons"),
                    TextPreference("revanced_miniplayer_opacity", inputType = InputType.NUMBER),
                ),
            ),
        )

        // region Add hooks to hide miniplayer buttons and change the overlay opacity.

        listOf(
            ClassDef::miniplayerModernExpandButtonMethodMatch to "hideMiniplayerExpandClose",
            ClassDef::miniplayerModernCloseButtonMethodMatch to "hideMiniplayerExpandClose",
            ClassDef::miniplayerModernActionButtonMethodMatch to "hideMiniplayerActionButton",
            ClassDef::miniplayerModernRewindButtonMethodMatch to "hideMiniplayerRewindForward",
            ClassDef::miniplayerModernForwardButtonMethodMatch to "hideMiniplayerRewindForward",
            ClassDef::miniplayerModernOverlayViewMethodMatch to "adjustMiniplayerOpacity",
        ).forEach { (matching, methodName) ->
            val match = matching.get(miniplayerModernViewParentMethod.immutableClassDef)

            match.method.apply {
                val index = match[-1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V",
                )
            }
        }

        miniplayerModernViewParentMethod.immutableClassDef.getMiniplayerModernAddViewListenerMethod()
            .addInstruction(
                0,
                "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "hideMiniplayerSubTexts(Landroid/view/View;)V",
            )

        // endregion
    }
}
