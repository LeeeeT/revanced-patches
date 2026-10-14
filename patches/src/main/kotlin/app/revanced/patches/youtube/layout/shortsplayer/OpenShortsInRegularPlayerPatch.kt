package app.revanced.patches.youtube.layout.shortsplayer

import app.revanced.util.findFreeRegister
import app.revanced.util.registersUsed
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.youtube.layout.player.fullscreen.openVideosFullscreenHookPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import app.revanced.patches.youtube.video.information.playbackStartDescriptorToStringMethodMatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/OpenShortsInRegularPlayerPatch;"

@Suppress("unused")
val openShortsInRegularPlayerPatch = bytecodePatch(
    name = "Open Shorts in regular player",
    description = "Adds options to open Shorts in the regular video player.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        openVideosFullscreenHookPatch,
        navigationBarHookPatch,
        resourceMappingPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
    )

    apply {
        addResources("youtube", "layout.shortsplayer.shortsPlayerTypePatch")

        PreferenceScreen.SHORTS.addPreferences(
            ListPreference("revanced_shorts_player_type"),
        )

        // Activity is used as the context to launch an Intent.
        mainActivityOnCreateMethod.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "setMainActivity(Landroid/app/Activity;)V",
        )

        val playbackStartVideoIdMethod = playbackStartDescriptorToStringMethodMatch.let {
            navigate(it.method).to(it[1]).original()
        }

        shortsPlaybackIntentMethod.addInstructionsWithLabels(
            0,
            """
                move-object/from16 v0, p1
                
                invoke-virtual { v0 }, $playbackStartVideoIdMethod
                move-result-object v1
                invoke-static { v1 }, ${EXTENSION_CLASS_DESCRIPTOR}->openShort(Ljava/lang/String;)Z
                move-result v1
                if-eqz v1, :disabled
                return-void
                
                :disabled
                nop
            """
        )

        // Fix issue with back button exiting the app instead of minimizing the player.
        // Without this change this issue can be difficult to reproduce, but seems to occur
        // most often with 'open video in regular player' and not open in fullscreen player.
        exitVideoPlayerMethod.apply {
            findInstructionIndicesReversedOrThrow {
                val methodReference = methodReference
                methodReference?.name == "finish" && methodReference.parameterTypes.isEmpty()
            }.forEach { index ->
                // Exclude the registers of the finish call that follows the inserted code.
                val freeRegister = findFreeRegister(index, getInstruction(index).registersUsed)

                addInstructionsAtControlFlowLabel(
                    index,
                    """
                        invoke-static { }, ${EXTENSION_CLASS_DESCRIPTOR}->overrideBackPressToExit()Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :finish
                        return-void
                        :finish
                        nop
                    """,
                )
            }
        }
    }
}
