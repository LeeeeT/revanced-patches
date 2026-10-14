package app.revanced.patches.youtube.misc.playercontrols

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.playerControlsVisibilityEntityModelMethodMatch by composingFirstMethod {
    name("getPlayerControlsVisibility")
    accessFlags(AccessFlags.PUBLIC)
    returnType("L")
    parameterTypes()
    opcodes(
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
    )
}

internal val BytecodePatchContext.youtubeControlsOverlayMethod by gettingFirstImmutableMethodDeclaratively {
    returnType("V")
    parameterTypes()
    instructions(
        method("setFocusableInTouchMode"),
        ResourceType.ID("inset_overlay_view_layout"),
        ResourceType.ID("scrim_overlay"),
    )
}

internal val ClassDef.motionEventMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("V")
    parameterTypes("Landroid/view/MotionEvent;")
    instructions(
        // Hides the overlay controls.
        allOf(
            Opcode.INVOKE_INTERFACE(),
            method { returnType == "V" && parameterTypes.map { it.toString() } == listOf("I", "Z") },
        ),
    )
}

internal val BytecodePatchContext.playerControlsExtensionHookListenersExistMethod by gettingFirstMethodDeclaratively {
    name("fullscreenButtonVisibilityCallbacksExist")
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes()
}

internal val BytecodePatchContext.playerControlsExtensionHookMethod by gettingFirstMethodDeclaratively {
    name("fullscreenButtonVisibilityChanged")
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("V")
    parameterTypes("Z")
}

internal val BytecodePatchContext.playerTopControlsInflateMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
    instructions(
        ResourceType.ID("controls_layout_stub"),
        method { name == "inflate" && definingClass == "Landroid/view/ViewStub;" },
        after(Opcode.MOVE_RESULT_OBJECT()),
    )
}

internal val BytecodePatchContext.playerBottomControlsInflateMethodMatch by composingFirstMethod {
    returnType("Ljava/lang/Object;")
    parameterTypes()
    instructions(
        ResourceType.ID("bottom_ui_container_stub"),
        method { name == "inflate" && definingClass == "Landroid/view/ViewStub;" },
        after(Opcode.MOVE_RESULT_OBJECT()),
    )
}

internal val BytecodePatchContext.overlayViewInflateMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/View;")
    instructions(
        ResourceType.ID("fullscreen_button"),
        afterAtMost(3, Opcode.CHECK_CAST()),
        ResourceType.ID("heatseeker_viewstub"),
    )
}

/**
 * Resolves to the class found in [playerTopControlsInflateMethodMatch].
 */
context(_: BytecodePatchContext)
internal fun ClassDef.getControlsOverlayVisibilityMethod() = firstMethodDeclaratively {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Z", "Z")
}

internal const val PLAYER_BOTTOM_CONTROLS_EXPLODER_FEATURE_FLAG = 45643739L

internal const val PLAYER_CONTROLS_LARGE_OVERLAY_BUTTONS_FEATURE_FLAG = 45709810L

internal const val PLAYER_CONTROLS_BUTTON_STROKE_FEATURE_FLAG = 45713296L
