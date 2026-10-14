package app.revanced.patches.youtube.layout.shortsautoplay

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.reelEnumConstructorMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR)
    instructions(
        "REEL_LOOP_BEHAVIOR_UNKNOWN"(),
        "REEL_LOOP_BEHAVIOR_SINGLE_PLAY"(),
        "REEL_LOOP_BEHAVIOR_REPEAT"(),
        "REEL_LOOP_BEHAVIOR_END_SCREEN"(),
        "REEL_LOOP_BEHAVIOR_AUTO_ADVANCE"(),
        Opcode.RETURN_VOID(),
    )
}

/**
 * Resolves the loop behavior of a Short from the player response and the reel watch endpoint.
 * All Shorts player code uses this method to decide what happens when a Short ends.
 *
 * @param reelEnumClass The loop behavior enum class found with [reelEnumConstructorMethodMatch].
 */
internal fun BytecodePatchContext.getReelLoopBehaviorMethod(reelEnumClass: String) = firstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType(reelEnumClass)
    parameterTypes("L", "L")
    instructions(
        // Loop behavior is looked up from the ordinal in the proto.
        method { definingClass == reelEnumClass && returnType == reelEnumClass && parameterTypes == listOf("I") },
    )
}

/**
 * Enables the auto advance loop behavior of the Shorts player.
 */
internal const val SHORTS_AUTO_ADVANCE_FEATURE_FLAG = 45831829L

/**
 * Enables the auto advance loop behavior of individual Shorts in the Shorts sequence.
 */
internal const val SHORTS_SEQUENCE_AUTO_ADVANCE_FEATURE_FLAG = 45813015L
