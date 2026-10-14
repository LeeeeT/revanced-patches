package app.revanced.patches.youtube.video.videoid

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

/**
 * Matches using the class found in [videoIdParentMethodMatch].
 */
context(_: BytecodePatchContext)
internal fun ClassDef.getVideoIdMethodMatch() = firstMethodComposite {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    instructions(
        allOf(Opcode.INVOKE_INTERFACE(), method { returnType == "Ljava/lang/String;" && parameterTypes.isEmpty() }),
        after(Opcode.MOVE_RESULT_OBJECT()), // videoId
        after(Opcode.INVOKE_INTERFACE()),
        after(Opcode.MOVE_RESULT_OBJECT()),
        // Method that saves the video ID to a map.
        after(
            allOf(
                Opcode.INVOKE_VIRTUAL(),
                method { returnType == "V" && parameterTypes.firstOrNull() == "Ljava/lang/String;" },
            ),
        ),
        after(Opcode.RETURN_VOID()),
    )
}

internal val BytecodePatchContext.videoIdBackgroundPlayMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.DECLARED_SYNCHRONIZED, AccessFlags.FINAL, AccessFlags.PUBLIC)
    returnType("V")
    parameterTypes("L")
    instructions(
        method { returnType == "Ljava/lang/String;" },
        Opcode.MOVE_RESULT_OBJECT(),
        Opcode.IPUT_OBJECT(),
        Opcode.MONITOR_EXIT(),
        Opcode.RETURN_VOID(),
        Opcode.MONITOR_EXIT(),
        Opcode.RETURN_VOID(),
    )
    custom { immutableClassDef.methods.count() == 17 }
}

internal val BytecodePatchContext.videoIdParentMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("[L")
    parameterTypes("L")
    instructions(
        524288L(),
    )
}
