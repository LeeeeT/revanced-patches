package app.revanced.patches.youtube.misc.dns

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.dns.checkWatchHistoryDomainNameResolutionPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.shared.mainActivityOnCreateMethod

val checkWatchHistoryDomainNameResolutionPatch = checkWatchHistoryDomainNameResolutionPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
        )
    },
    getMainActivityMethod = BytecodePatchContext::mainActivityOnCreateMethod::get,
)
