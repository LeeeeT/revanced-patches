package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import com.android.tools.smali.dexlib2.Opcode

internal const val GRADIENT_LOADING_SCREEN_FEATURE_FLAG = 45412406L

internal val BytecodePatchContext.splashScreenStyleMethodMatch by composingFirstMethod {
    definingClass(YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE)
    name("onCreate")
    returnType("V")
    parameterTypes("Landroid/os/Bundle;")
    instructions(1074339245L())
}


/**
 * Matches to the same method as [splashScreenStyleMethodMatch].
 */
internal val BytecodePatchContext.showSplashScreen1MethodMatch by composingFirstMethod {
    name("onCreate")
    definingClass(YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE)
    returnType("V")
    parameterTypes("Landroid/os/Bundle;")
    instructions(
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method { returnType == "I" && parameterTypes.size == 1 && parameterTypes[0] == "I" },
        ),
        after(Opcode.MOVE_RESULT()), // Splash screen state.
        after(Opcode.IF_NEZ()),
        afterAtMost(
            15,
            method {
                returnType == "I" &&
                    parameterTypes.map { it.toString() } == listOf("Landroid/content/Intent;", "Z", "I")
            },
        ),
    )
}

/**
 * Matches to the same method as [splashScreenStyleMethodMatch].
 */
internal val BytecodePatchContext.showSplashScreen2MethodMatch by composingFirstMethod {
    name("onCreate")
    definingClass(YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE)
    returnType("V")
    parameterTypes("Landroid/os/Bundle;")
    instructions(
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method { returnType == "V" && parameterTypes.size == 1 && parameterTypes[0].startsWith("[L") }
        ),
        Opcode.IF_NE(),
        method { toString() == "Landroid/graphics/drawable/AnimatedVectorDrawable;->start()V" }
    )
}
