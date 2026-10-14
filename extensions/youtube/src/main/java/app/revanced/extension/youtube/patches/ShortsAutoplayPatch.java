package app.revanced.extension.youtube.patches;

import android.app.Activity;

import java.lang.ref.WeakReference;
import java.util.Objects;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ShortsAutoplayPatch {

    private enum ShortsLoopBehavior {
        /**
         * Repeat the same Short forever!
         */
        REPEAT,
        /**
         * Play once, then advance to the next Short.
         */
        AUTO_ADVANCE;

        static void setYTEnumValue(Enum<?> ytBehavior) {
            for (ShortsLoopBehavior rvBehavior : values()) {
                if (ytBehavior.name().endsWith("_" + rvBehavior.name())) {
                    rvBehavior.ytEnumValue = ytBehavior;

                    Logger.printDebug(() -> rvBehavior + " set to YT enum: " + ytBehavior.name());
                    return;
                }
            }
        }

        /**
         * YouTube enum value of the obfuscated enum type.
         */
        private Enum<?> ytEnumValue;
    }

    private static WeakReference<Activity> mainActivityRef = new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static void setMainActivity(Activity activity) {
        mainActivityRef = new WeakReference<>(activity);
    }

    /**
     * @return If the app is currently in background PiP mode.
     */
    private static boolean isAppInBackgroundPiPMode() {
        Activity activity = mainActivityRef.get();
        return activity != null && activity.isInPictureInPictureMode();
    }

    private static boolean isAutoplayEnabled() {
        return isAppInBackgroundPiPMode()
                ? Settings.SHORTS_AUTOPLAY_BACKGROUND.get()
                : Settings.SHORTS_AUTOPLAY.get();
    }

    /**
     * Injection point.
     */
    public static void setYTShortsRepeatEnum(Enum<?> ytEnum) {
        try {
            for (Enum<?> ytBehavior : Objects.requireNonNull(ytEnum.getClass().getEnumConstants())) {
                ShortsLoopBehavior.setYTEnumValue(ytBehavior);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setYTShortsRepeatEnum failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static Enum<?> changeShortsRepeatBehavior(Enum<?> original) {
        try {
            Enum<?> overrideBehavior = (isAutoplayEnabled()
                    ? ShortsLoopBehavior.AUTO_ADVANCE
                    : ShortsLoopBehavior.REPEAT).ytEnumValue;

            if (overrideBehavior != null) {
                if (overrideBehavior != original) {
                    Logger.printDebug(() -> "Changing Shorts repeat behavior from: "
                            + (original == null ? "null" : original.name()) + " to: " + overrideBehavior.name());
                }

                return overrideBehavior;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "changeShortsRepeatBehavior failure", ex);
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static boolean useAutoAdvance(boolean original) {
        return Settings.SHORTS_AUTOPLAY.get() || Settings.SHORTS_AUTOPLAY_BACKGROUND.get();
    }
}
