package app.revanced.patches.youtube.misc.backgroundplayback

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.util.literal
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.backgroundPlaybackManagerMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes("L")
    opcodes(
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET,
        Opcode.CONST,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.GOTO,
        Opcode.SGET_OBJECT,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
    )
}

/**
 * Lambda that chooses the title of the background and offline settings category.
 */
internal val BytecodePatchContext.backgroundPlaybackSettingsMethodMatch by composingFirstMethod {
    returnType("Ljava/lang/Object;")
    parameterTypes("Ljava/lang/Object;")
    instructions(
        // Is background playback available.
        allOf(Opcode.INVOKE_VIRTUAL(), method { returnType == "Z" && parameterTypes.isEmpty() }),
        after(Opcode.MOVE_RESULT()),
        // Is offline available.
        after(method { toString() == "Ljava/lang/Boolean;->booleanValue()Z" }),
        afterAtMost(10, ResourceType.STRING("pref_background_and_offline_category")),
    )
}

internal val BytecodePatchContext.kidsBackgroundPlaybackPolicyControllerMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("I", "L", "L")
    opcodes(
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
    )
    literal { 5 }
}

internal const val SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG = 45415425L

internal const val NEW_PLAYER_TYPE_ENUM_FEATURE_FLAG = 45698813L
