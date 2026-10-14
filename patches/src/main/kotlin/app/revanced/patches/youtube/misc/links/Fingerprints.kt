package app.revanced.patches.youtube.misc.links

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Opens a link in the default browser.
 */
internal val BytecodePatchContext.openLinkInBrowserMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes("Landroid/content/Context;", "Landroid/net/Uri;")
    instructions(
        "android.intent.action.VIEW"(),
        "text/html"(),
        method { toString() == "Landroid/content/Intent;->setDataAndType(Landroid/net/Uri;Ljava/lang/String;)Landroid/content/Intent;" },
    )
}
