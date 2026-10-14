package app.revanced.patches.youtube.layout.buttons.navigation

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.createPivotBarMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes(
        "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
        "Landroid/widget/TextView;",
        "Ljava/lang/CharSequence;",
    )
    instructions(
        method { name == "setText" && definingClass == "Landroid/widget/TextView;" },
        Opcode.RETURN_VOID(),
    )
}

internal const val ANIMATED_NAVIGATION_TABS_FEATURE_FLAG = 45680008L

internal val BytecodePatchContext.pivotBarStyleMethodMatch by composingFirstMethod {
    definingClass("/PivotBar;")
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.XOR_INT_2ADDR
    )
}

internal val BytecodePatchContext.pivotBarChangedMethodMatch by composingFirstMethod {
    name("onConfigurationChanged")
    definingClass("/PivotBar;")
    returnType("V")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    )
}

internal const val TRANSLUCENT_STATUS_BAR_FEATURE_FLAG = 45400535L

/**
 * YouTube nav buttons.
 */
internal const val TRANSLUCENT_NAVIGATION_BUTTONS_FEATURE_FLAG = 45630927L

/**
 * Device on screen back/home/recent buttons.
 */
internal const val TRANSLUCENT_NAVIGATION_BUTTONS_SYSTEM_FEATURE_FLAG = 45632194L

internal val BytecodePatchContext.wideSearchbarLayoutMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Landroid/view/View;")
    parameterTypes("L", "L")
    instructions(ResourceType.LAYOUT("action_bar_ringo"))
}
