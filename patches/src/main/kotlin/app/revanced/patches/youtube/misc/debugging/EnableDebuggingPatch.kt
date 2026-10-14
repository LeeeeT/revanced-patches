package app.revanced.patches.youtube.misc.debugging

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.firstClassDef
import app.revanced.patches.shared.misc.debugging.enableDebuggingPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagHookPatch
import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagMethod
import app.revanced.patches.youtube.misc.featureflag.hookBooleanFeatureFlags
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.cloneMutable
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/shared/patches/EnableDebuggingPatch;"

@Suppress("unused")
val enableDebuggingPatch = enableDebuggingPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            booleanFeatureFlagHookPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
                "21.41.525-SECONDARY",
            ),
        )
    },
    hookStringFeatureFlag = { true },
    hookLongFeatureFlag = { true },
    hookDoubleFeatureFlag = { true },
    hookFeatureFlags = {
        hookBooleanFeatureFlags("$EXTENSION_CLASS_DESCRIPTOR->isBooleanFeatureFlagEnabled(ZJ)Z")

        // The other accessors are in the same class as the boolean accessor.
        val flagsClass = firstClassDef(booleanFeatureFlagMethod.definingClass)

        // Accessor parameters are the flag and the default value.
        // The original accessor code is moved to a helper method,
        // and the accessor is replaced with a method that passes the value to the extension.
        fun hookAccessor(valueType: String, wrapperRegisters: Int, wrapperSmali: (String) -> String) {
            val accessor = flagsClass.methods.single { method ->
                method.returnType == valueType && method.parameterTypes.map { it.toString() } == listOf("J", valueType)
            }
            val helperMethod = accessor.cloneMutable(name = "patch_" + accessor.name)

            val wrapperMethod = ImmutableMethod(
                accessor.definingClass,
                accessor.name,
                accessor.parameters,
                accessor.returnType,
                accessor.accessFlags,
                null,
                null,
                MutableMethodImplementation(wrapperRegisters),
            ).toMutable().apply {
                addInstructions(wrapperSmali(helperMethod.toString()))
            }

            flagsClass.methods.apply {
                remove(accessor)
                add(helperMethod)
                add(wrapperMethod)
            }
        }

        // v0-v1 value, v2-v3 flag, v4-v5 default value, p0 this, p1-p2 flag, p3-p4 default value.
        listOf(
            "J" to "isLongFeatureFlagEnabled(JJJ)J",
            "D" to "isDoubleFeatureFlagEnabled(DJD)D",
        ).forEach { (type, extensionMethod) ->
            hookAccessor(type, 11) { helperMethod ->
                """
                    invoke-virtual/range { p0 .. p4 }, $helperMethod
                    move-result-wide v0
                    move-wide v2, p1
                    move-wide v4, p3
                    invoke-static/range { v0 .. v5 }, $EXTENSION_CLASS_DESCRIPTOR->$extensionMethod
                    move-result-wide v0
                    return-wide v0
                """
            }
        }

        // v0 value, p0 this, p1-p2 flag, p3 default value.
        hookAccessor("Ljava/lang/String;", 5) { helperMethod ->
            """
                invoke-virtual { p0, p1, p2, p3 }, $helperMethod
                move-result-object v0
                invoke-static { v0, p1, p2, p3 }, $EXTENSION_CLASS_DESCRIPTOR->isStringFeatureFlagEnabled(Ljava/lang/String;JLjava/lang/String;)Ljava/lang/String;
                move-result-object v0
                return-object v0
            """
        }
    },
    preferenceScreen = PreferenceScreen.MISC,
)
