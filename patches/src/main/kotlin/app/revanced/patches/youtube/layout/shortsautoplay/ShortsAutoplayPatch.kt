package app.revanced.patches.youtube.layout.shortsautoplay

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagHookPatch
import app.revanced.patches.youtube.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.patcher.extensions.getInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/ShortsAutoplayPatch;"

@Suppress("ObjectPropertyName")
val shortsAutoplayPatch = bytecodePatch(
    name = "Shorts autoplay",
    description = "Adds options to automatically play the next Short.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        resourceMappingPatch,
        booleanFeatureFlagHookPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
    )

    apply {
        addResources("youtube", "layout.shortsautoplay.shortsAutoplayPatch")

        PreferenceScreen.SHORTS.addPreferences(
            SwitchPreference("revanced_shorts_autoplay"),
        )

        PreferenceScreen.SHORTS.addPreferences(
            SwitchPreference("revanced_shorts_autoplay_background"),
        )

        // Main activity is used to check if app is in pip mode.
        mainActivityOnCreateMethod.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->setMainActivity(Landroid/app/Activity;)V",
        )

        val reelEnumClass = reelEnumConstructorMethodMatch.immutableClassDef.type

        reelEnumConstructorMethodMatch.method.addInstructions(
            reelEnumConstructorMethodMatch[-1],
            """
                # Pass the first enum value to extension.
                # Any enum value of this type will work.
                sget-object v0, $reelEnumClass->a:$reelEnumClass
                invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->setYTShortsRepeatEnum(Ljava/lang/Enum;)V
            """,
        )

        // Override the loop behavior used when a Short ends.
        getReelLoopBehaviorMethod(reelEnumClass).apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructionsAtControlFlowLabel(
                    index,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->changeShortsRepeatBehavior(Ljava/lang/Enum;)Ljava/lang/Enum;
                        move-result-object v$register
                        check-cast v$register, $reelEnumClass
                    """,
                )
            }
        }

        // Auto advance is only used by the player if these flags are enabled.
        listOf(
            SHORTS_AUTO_ADVANCE_FEATURE_FLAG,
            SHORTS_SEQUENCE_AUTO_ADVANCE_FEATURE_FLAG,
        ).forEach { flag ->
            overrideBooleanFeatureFlag(flag, "$EXTENSION_CLASS_DESCRIPTOR->useAutoAdvance(Z)Z")
        }
    }
}
