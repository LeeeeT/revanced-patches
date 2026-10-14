package app.revanced.patches.youtube.misc.playertype

import app.revanced.patcher.*
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.util.forEachInstructionAsSequence
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/youtube/patches/PlayerTypeHookPatch;"

val playerTypeHookPatch = bytecodePatch(
    description = "Hook to get the current player type and video playback state.",
) {
    dependsOn(sharedExtensionPatch, resourceMappingPatch)

    apply {
        // The player type is computed reactively and stored in a volatile field of the player type
        // provider. Hook every write of that field to get notified of all player type changes.
        val playerType = playerTypeEnumMethod.immutableClassDef.type
        val playerTypeHolderType = classDefs.single { classDef ->
            classDef.fields.any { field ->
                field.type == playerType && AccessFlags.VOLATILE.isSet(field.accessFlags)
            }
        }.type

        forEachInstructionAsSequence(
            match = { _, method, instruction, index ->
                if (method.name == "<init>" || instruction.opcode != Opcode.IPUT_OBJECT) return@forEachInstructionAsSequence null
                val field = instruction.getReference<FieldReference>() ?: return@forEachInstructionAsSequence null
                if (field.definingClass != playerTypeHolderType || field.type != playerType) return@forEachInstructionAsSequence null

                index
            },
            transform = { method, index ->
                val register = method.getInstruction<TwoRegisterInstruction>(index).registerA

                method.addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V",
                )
            },
        )

        reelWatchPagerMethodMatch.method.apply {
            val index = reelWatchPagerMethodMatch[-1]
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index + 1,
                "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->onShortsCreate(Landroid/view/View;)V",
            )
        }

        val controlStateType = controlsStateToStringMethod.immutableClassDef.type

        val videoStateEnumMethod = videoStateEnumMethod
        firstMethodComposite {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returnType("V")
            parameterTypes(controlStateType)
            instructions(
                field {
                    definingClass == controlStateType && type == videoStateEnumMethod.immutableClassDef.type
                },
                // Obfuscated parameter field name.
                ResourceType.STRING("accessibility_play"),
                ResourceType.STRING("accessibility_pause"),
            )
        }.let {
            it.method.apply {
                val videoStateFieldName = getInstruction<ReferenceInstruction>(it[0]).reference

                addInstructions(
                    0,
                    """
                        iget-object v0, p1, $videoStateFieldName  # copy VideoState parameter field
                        invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->setVideoState(Ljava/lang/Enum;)V
                    """,
                )
            }
        }
    }
}
