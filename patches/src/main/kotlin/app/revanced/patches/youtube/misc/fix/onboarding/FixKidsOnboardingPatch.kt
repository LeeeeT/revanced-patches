package app.revanced.patches.youtube.misc.fix.onboarding

import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.forEachInstructionAsSequence
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val KIDS_ONBOARDING_FAILURE_MESSAGE = "Failed to fetch kids onboarding status, finishing the App."

/**
 * The signed out onboarding flow fetches the kids onboarding status.
 * With a patched app the status is not provided, and the app closes itself on every launch.
 * Fix this by not closing the app, which is the same as the status of a regular account.
 */
internal val fixKidsOnboardingPatch = bytecodePatch {
    apply {
        var fixed = 0

        forEachInstructionAsSequence(
            match = { _, method, instruction, index ->
                if (instruction.getReference<StringReference>()?.string != KIDS_ONBOARDING_FAILURE_MESSAGE) {
                    return@forEachInstructionAsSequence null
                }

                // The app is closed shortly after the failure is logged.
                val instructions = method.implementation!!.instructions.toList()
                (index until minOf(index + 20, instructions.size)).firstOrNull { finishIndex ->
                    val finishInstruction = instructions[finishIndex]
                    finishInstruction.opcode == Opcode.INVOKE_VIRTUAL &&
                        finishInstruction.getReference<MethodReference>()?.name == "finishAffinity"
                }
            },
            transform = { method, finishIndex ->
                method.replaceInstruction(finishIndex, "nop")
                fixed++
            },
        )

        if (fixed == 0) throw PatchException("Could not find the kids onboarding failure handling")
    }
}
