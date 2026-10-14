package app.revanced.patches.youtube.layout.hide.shorts

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.componentContextParserMethod by gettingFirstImmutableMethodDeclaratively {
    returnType("L")
    instructions(
        "Failed to parse Element proto."(),
        "Cannot read theme key from model."()
    )
}

context(_: BytecodePatchContext)
internal fun ClassDef.getTreeNodeResultListMethod() = firstMethodDeclaratively  {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("Ljava/util/List;")
    instructions(allOf(Opcode.INVOKE_STATIC(), method("nCopies")))
}

internal val BytecodePatchContext.shortsBottomBarContainerMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/View;", "Landroid/os/Bundle;")
    instructions(
        "r_pfvc"(),
        ResourceType.ID("bottom_bar_container"),
        method("getHeight"),
        Opcode.MOVE_RESULT(),
    )
}

context(_: BytecodePatchContext)
internal fun ClassDef.getRenderBottomNavigationBarMethodMatch() = firstMethodDeclaratively {
    returnType("V")
    parameterTypes("Ljava/lang/String;")
    instructions(
        Opcode.IGET_OBJECT(),
        after(Opcode.MONITOR_ENTER()),
        after(Opcode.IGET_OBJECT()),
        after(Opcode.IF_EQZ()),
        after(Opcode.INVOKE_INTERFACE()),
        Opcode.MONITOR_EXIT(),
        Opcode.RETURN_VOID(),
        Opcode.MOVE_EXCEPTION(),
        Opcode.MONITOR_EXIT(),
        Opcode.THROW()
    )
}

/**
 * 20.45+
 */
internal val BytecodePatchContext.renderBottomNavigationBarParentMethod by gettingFirstImmutableMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("[Ljava/lang/Class;")
    parameterTypes(
        "Ljava/lang/Class;",
        "Ljava/lang/Object;",
        "I",
    )
    instructions(
        "RPCAC"(),
    )
}

internal val ClassDef.setPivotBarVisibilityMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Z")
    instructions(
        allOf(
            Opcode.CHECK_CAST(),
            type("Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"),
        ),
        after(Opcode.IF_EQZ()),
    )
}

internal val BytecodePatchContext.setPivotBarVisibilityParentMethod by gettingFirstImmutableMethodDeclaratively {
    parameterTypes("Z")
    instructions(
        "FEnotifications_inbox"(),
    )
}

internal const val SHORTS_EXPERIMENTAL_PLAYER_FEATURE_FLAG = 45677719L

internal const val RENDER_NEXT_UI_FEATURE_FLAG = 45649743L
