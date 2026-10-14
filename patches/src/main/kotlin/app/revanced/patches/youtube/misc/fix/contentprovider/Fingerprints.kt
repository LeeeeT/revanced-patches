package app.revanced.patches.youtube.misc.fix.contentprovider

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.unstableContentProviderMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/content/ContentResolver;", "[Ljava/lang/String;")
    instructions(
        "ContentProvider query returned null cursor"(),
        method { name == "putAll" && parameterTypes.count() == 1 && parameterTypes.first() == "Ljava/util/Map;" },
    )
}
