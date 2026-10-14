@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.misc.litho.filter

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.shared.conversionContextToStringMethod
import app.revanced.patches.shared.misc.litho.filter.EXTENSION_CLASS_DESCRIPTOR
import app.revanced.patches.shared.misc.litho.filter.lithoFilterPatch
import app.revanced.patches.shared.misc.litho.filter.protobufBufferReferenceLegacyMethod
import app.revanced.patches.shared.misc.litho.filter.protobufBufferReferenceMethodMatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.insertLiteralOverride
import app.revanced.util.returnLate
import com.android.tools.smali.dexlib2.Opcode

val lithoFilterPatch = lithoFilterPatch(
    componentCreateInsertionIndex = {
        indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
    },
    insertProtobufHook = {
        // Hook method that bridges between UPB buffer native code and FB Litho.
        // Method is found in 19.25+, but is forcefully turned off for 20.21 and lower.
        protobufBufferReferenceMethodMatch.let {
            // Hook the buffer after the call to jniDecode().
            it.method.addInstruction(
                it[-1] + 1,
                "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->setProtoBuffer([B)V",
            )
        }

        // Legacy non-native buffer.
        protobufBufferReferenceLegacyMethod.addInstruction(
            0,
            "invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V",
        )
    },
    getConversionContextToStringMethod = BytecodePatchContext::conversionContextToStringMethod::get,
    getExtractIdentifierFromBuffer = { true },
) {
    dependsOn(sharedExtensionPatch)
}
