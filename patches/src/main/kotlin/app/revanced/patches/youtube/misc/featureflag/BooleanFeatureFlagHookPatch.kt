package app.revanced.patches.youtube.misc.featureflag

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.cloneMutable
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Methods that dispatch the overrides. The flag value is held in register v2.
 */
private val dispatchMethods = mutableListOf<MutableMethod>()

/**
 * Overrides the value of a boolean feature flag.
 *
 * Unlike overriding the flag at a specific call site, this works even if the
 * flag is read in multiple places, as is common when the app inlines the flag accessor.
 *
 * @param flag The feature flag literal.
 * @param extensionMethodDescriptor A static `(Z)Z` method that receives the original value
 * and returns the value to use.
 */
fun overrideBooleanFeatureFlag(flag: Long, extensionMethodDescriptor: String) = addOverride(
    flag,
    """
        invoke-static { v2 }, $extensionMethodDescriptor
        move-result v2
    """,
)

/**
 * Overrides the value of a boolean feature flag with a constant.
 */
fun overrideBooleanFeatureFlag(flag: Long, value: Boolean) =
    addOverride(flag, "const/4 v2, ${if (value) "0x1" else "0x0"}")

/**
 * Passes the value of every boolean feature flag, after any overrides are applied,
 * to a static `(ZJ)Z` extension method that receives the value and the flag
 * and returns the value to use.
 */
fun hookBooleanFeatureFlags(extensionMethodDescriptor: String) = dispatchMethods.forEach { method ->
    // Insert after the nop that marks the end of the overrides.
    method.addInstructions(
        method.implementation!!.instructions.size - 1,
        """
            invoke-static { v2, p1, p2 }, $extensionMethodDescriptor
            move-result v2
        """,
    )
}

private fun addOverride(flag: Long, overrideSmali: String) = dispatchMethods.forEach { method ->
    method.addInstructionsWithLabels(
        2,
        """
            const-wide v0, $flag
            cmp-long v0, p1, v0
            if-nez v0, :next
            $overrideSmali
        """,
        ExternalLabel("next", method.getInstruction(2)),
    )
}

/**
 * Moves the original code of [originalMethod] to a helper method,
 * and replaces the original method with one that dispatches the overrides.
 */
context(_: BytecodePatchContext)
private fun addDispatchMethod(originalMethod: MutableMethod) {
    val helperMethod = originalMethod.cloneMutable(name = "patch_" + originalMethod.name)

    // p0 this, p1-p2 flag and optionally p3 the default value.
    val parameterRegisters = 1 + originalMethod.parameterTypes.sumOf { type -> if (type == "J") 2 else 1 }

    val dispatchMethod = ImmutableMethod(
        originalMethod.definingClass,
        originalMethod.name,
        originalMethod.parameters,
        originalMethod.returnType,
        originalMethod.accessFlags,
        null,
        null,
        // v0-v1 wide flag constant, v2 flag value.
        MutableMethodImplementation(3 + parameterRegisters),
    ).toMutable().apply {
        addInstructions(
            """
                invoke-virtual/range { p0 .. p${parameterRegisters - 1} }, $helperMethod
                move-result v2
                # Overrides are inserted here.
                nop
                # Hooks of all flags are inserted here.
                return v2
            """,
        )
    }

    originalMethod.classDef.methods.apply {
        remove(originalMethod)
        add(helperMethod)
        add(dispatchMethod)
    }

    dispatchMethods += dispatchMethod
}

val booleanFeatureFlagHookPatch = bytecodePatch(
    description = "Hooks the boolean feature flag accessors.",
) {
    apply {
        addDispatchMethod(booleanFeatureFlagMethod)
        addDispatchMethod(booleanFeatureFlagNoDefaultMethod)
    }
}
