package app.revanced.patches.youtube.misc.settings

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.licenseActivityOnCreateMethod by gettingFirstMethodDeclaratively {
    name("onCreate")
    definingClass("/LicenseActivity;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/os/Bundle;")
}

internal val BytecodePatchContext.setThemeMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    instructions(ResourceType.STRING("app_theme_appearance_dark"))
}

internal const val CAIRO_SETTINGS_FRAGMENT_FEATURE_FLAG = 45532100L

internal const val BOLD_ICONS_FEATURE_FLAG = 45685201L
