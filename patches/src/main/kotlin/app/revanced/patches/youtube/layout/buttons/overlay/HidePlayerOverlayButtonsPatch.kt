package app.revanced.patches.youtube.layout.buttons.overlay

import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagHookPatch
import app.revanced.patches.youtube.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.getLayoutConstructorMethodMatch
import app.revanced.patches.youtube.shared.subtitleButtonControllerMethod
import app.revanced.util.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/HidePlayerOverlayButtonsPatch;"

@Suppress("ObjectPropertyName")
val hidePlayerOverlayButtonsPatch = bytecodePatch(
    name = "Hide player overlay buttons",
    description = "Adds options to hide the player Cast, Autoplay, Captions, Previous & Next buttons, and the player " +
            "control buttons background.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        resourceMappingPatch, // Used for finding methods.
        booleanFeatureFlagHookPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
    )

    apply {
        addResources("youtube", "layout.buttons.overlay.hidePlayerOverlayButtonsPatch")

        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("revanced_hide_autoplay_button"),
            SwitchPreference("revanced_hide_captions_button"),
            SwitchPreference("revanced_hide_cast_button"),
            SwitchPreference("revanced_hide_collapse_button"),
            SwitchPreference("revanced_hide_fullscreen_button"),
            SwitchPreference("revanced_hide_player_control_buttons_background"),
            SwitchPreference("revanced_hide_player_previous_next_buttons"),
        )

        // region Hide player next/previous button.

        getLayoutConstructorMethodMatch().let {
            val insertIndex = it[-1]
            val viewRegister =
                it.method.getInstruction<FiveRegisterInstruction>(insertIndex).registerC

            it.method.addInstruction(
                insertIndex,
                "invoke-static { v$viewRegister }, $EXTENSION_CLASS_DESCRIPTOR" +
                        "->hidePreviousNextButtons(Landroid/view/View;)V",
            )
        }

        // endregion

        // region Hide cast button.

        mediaRouteButtonMethod.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->getCastButtonOverrideV2(I)I
                move-result p1
            """,
        )

        arrayOf(
            CAST_BUTTON_PLAYER_FEATURE_FLAG,
            CAST_BUTTON_ACTION_FEATURE_FLAG,
        ).forEach { flag ->
            overrideBooleanFeatureFlag(flag, "$EXTENSION_CLASS_DESCRIPTOR->getCastButtonOverrideV2(Z)Z")
        }

        // endregion

        // region Hide captions button.

        subtitleButtonControllerMethod.apply {
            val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IGET_BOOLEAN) + 1

            addInstruction(
                insertIndex,
                "invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->hideCaptionsButton(Landroid/widget/ImageView;)V",
            )
        }

        // endregion

        // region Hide autoplay button.

        getLayoutConstructorMethodMatch().method.apply {
            val constIndex = indexOfFirstResourceIdOrThrow("autonav_toggle")
            val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA

            // Add a conditional branch around the code that inflates and adds the auto-repeat button.
            val gotoIndex = indexOfFirstInstructionOrThrow(constIndex) {
                val parameterTypes = getReference<MethodReference>()?.parameterTypes
                opcode == Opcode.INVOKE_VIRTUAL &&
                        parameterTypes?.size == 2 &&
                        parameterTypes.first() == "Landroid/view/ViewStub;"
            } + 1

            addInstructionsWithLabels(
                constIndex,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideAutoplayButton()Z
                    move-result v$constRegister
                    if-nez v$constRegister, :hidden
                """,
                ExternalLabel("hidden", getInstruction(gotoIndex)),
            )
        }

        // endregion

        // region Hide collapse button.

        titleAnchorMethodMatch.let {
            it.method.apply {
                val titleAnchorIndex = it[-1]
                val titleAnchorRegister = getInstruction<OneRegisterInstruction>(titleAnchorIndex).registerA

                addInstruction(
                    titleAnchorIndex + 1,
                    "invoke-static { v$titleAnchorRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->setTitleAnchorStartMargin(Landroid/view/View;)V"
                )

                val playerCollapseButtonIndex = it[1]
                val playerCollapseButtonRegister = getInstruction<OneRegisterInstruction>(playerCollapseButtonIndex).registerA

                addInstruction(
                    playerCollapseButtonIndex + 1,
                    "invoke-static { v$playerCollapseButtonRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->hideCollapseButton(Landroid/widget/ImageView;)V"
                )
            }
        }

        // endregion

        // region Hide fullscreen button.

        fullscreenButtonMethodMatch.let {
            it.method.apply {
                // Youtube 21.x doesn't cast the fullscreen button to ImageView anymore,
                // so match on move-result-object after findViewById instead of check-cast.
                val moveResultIndex = it[2]
                val insertIndex = moveResultIndex + 1
                val insertRegister = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static { v$insertRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->hideFullscreenButton(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        if-nez v$insertRegister, :show
                        return-void
                    """,
                    ExternalLabel("show", getInstruction(insertIndex))
                )
            }
        }

        // endregion

        // region Hide player control buttons background.

        inflateControlsGroupLayoutStubMethodMatch.let {
            it.method.apply {
                val moveResultIndex = it[-1]
                val layoutRegister = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                addInstruction(
                    moveResultIndex + 1,
                    "invoke-static { v$layoutRegister }, $EXTENSION_CLASS_DESCRIPTOR->hidePlayerControlButtonsBackground(Landroid/view/View;)V",
                )
            }
        }

        // endregion
    }
}
