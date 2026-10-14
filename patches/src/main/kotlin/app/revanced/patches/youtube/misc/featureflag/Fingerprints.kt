package app.revanced.patches.youtube.misc.featureflag

import app.revanced.patcher.accessFlags
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.method
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * The accessor used for nearly all boolean feature flags.
 */
internal val BytecodePatchContext.booleanFeatureFlagMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes("J", "Z")
    instructions(
        method { toString() == "Lj\$/util/DesugarCollections;->unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;" },
        method { toString() == "Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;" },
        method { toString() == "Ljava/lang/Boolean;->booleanValue()Z" },
    )
}

/**
 * Same as [booleanFeatureFlagMethod] but without a default value parameter.
 */
internal val BytecodePatchContext.booleanFeatureFlagNoDefaultMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes("J")
    instructions(
        method { toString() == "Lj\$/util/DesugarCollections;->unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;" },
        method { toString() == "Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;" },
        method { toString() == "Ljava/lang/Boolean;->booleanValue()Z" },
    )
}
