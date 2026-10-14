package app.revanced.extension.youtube.patches;

import static app.revanced.extension.shared.StringRef.str;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import kotlin.Unit;

@SuppressWarnings("unused")
public final class MiniplayerPatch {

    /**
     * Miniplayer type.
     */
    public enum MiniplayerType {
        /**
         * Disabled. When minimized the miniplayer is immediately closed.
         */
        DISABLED,
        /**
         * Unmodified type, and same as un-patched.
         */
        DEFAULT
    }

    private static final MiniplayerType CURRENT_TYPE = Settings.MINIPLAYER_TYPE.get();

    private static final boolean HIDE_OVERLAY_BUTTONS_ENABLED = CURRENT_TYPE == MiniplayerType.DEFAULT
            && Settings.MINIPLAYER_HIDE_OVERLAY_BUTTONS.get();

    private static final boolean HIDE_SUBTEXT_ENABLED = CURRENT_TYPE == MiniplayerType.DEFAULT
            && Settings.MINIPLAYER_HIDE_SUBTEXT.get();

    private static final int CLOSE_BUTTON_ID = Utils.getResourceIdentifier(ResourceType.ID, "modern_miniplayer_close");

    private static final int SCRIM_OVERLAY_ID = Utils.getResourceIdentifier(ResourceType.ID, "scrim_overlay");

    private static final int OPACITY_LEVEL;

    static {
        int opacity = Settings.MINIPLAYER_OPACITY.get();

        if (opacity < 0 || opacity > 100) {
            Utils.showToastLong(str("revanced_miniplayer_opacity_invalid_toast"));
            opacity = Settings.MINIPLAYER_OPACITY.resetToDefault();
        }

        OPACITY_LEVEL = (opacity * 255) / 100;
    }

    public static final class MiniplayerEnabledAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.MINIPLAYER_TYPE.get() == MiniplayerType.DEFAULT;
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.MINIPLAYER_TYPE);
        }
    }

    /**
     * Close button of the miniplayer, used to close the miniplayer when it's disabled.
     */
    private static WeakReference<View> closeButtonRef = new WeakReference<>(null);

    static {
        if (CURRENT_TYPE == MiniplayerType.DISABLED) {
            PlayerType.getOnChange().addObserver((PlayerType type) -> {
                if (type == PlayerType.WATCH_WHILE_MINIMIZED) {
                    closeMiniplayer();
                }
                return Unit.INSTANCE;
            });
        }
    }

    /**
     * Closes the miniplayer by clicking the close button.
     * The close button is created when the miniplayer is first shown,
     * so this can be called both when the player is minimized and when the button is created.
     */
    private static void closeMiniplayer() {
        View closeButton = closeButtonRef.get();
        if (closeButton == null || PlayerType.getCurrent() != PlayerType.WATCH_WHILE_MINIMIZED) return;

        // Post to allow the minimize animation and click handlers to be set up.
        closeButton.post(() -> {
            if (PlayerType.getCurrent() == PlayerType.WATCH_WHILE_MINIMIZED) {
                Logger.printDebug(() -> "Closing disabled miniplayer");
                closeButton.performClick();
            }
        });
    }

    /**
     * Injection point.
     *
     * @param overlay The inflated miniplayer controls overlay.
     */
    public static void adjustMiniplayerOpacity(View overlay) {
        if (CURRENT_TYPE != MiniplayerType.DEFAULT || OPACITY_LEVEL == 255) return;

        View scrim = overlay.findViewById(SCRIM_OVERLAY_ID);
        if (scrim instanceof ImageView imageView) {
            imageView.setImageAlpha(OPACITY_LEVEL);
        } else {
            Logger.printException(() -> "Unknown miniplayer overlay view: " + scrim);
        }
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerExpandClose(View view) {
        if (CURRENT_TYPE == MiniplayerType.DISABLED) {
            if (view.getId() == CLOSE_BUTTON_ID) {
                closeButtonRef = new WeakReference<>(view);
                closeMiniplayer();
            }
            return;
        }

        Utils.hideViewByRemovingFromParentUnderCondition(HIDE_OVERLAY_BUTTONS_ENABLED, view);
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerActionButton(View view) {
        Utils.hideViewByRemovingFromParentUnderCondition(HIDE_OVERLAY_BUTTONS_ENABLED, view);
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerRewindForward(View view) {
        Utils.hideViewByRemovingFromParentUnderCondition(HIDE_OVERLAY_BUTTONS_ENABLED, view);
    }

    /**
     * Injection point.
     */
    public static void hideMiniplayerSubTexts(View view) {
        try {
            // Different subviews are passed in, but only TextView is of interest here.
            if (HIDE_SUBTEXT_ENABLED && view instanceof TextView) {
                Logger.printDebug(() -> "Hiding subtext view");
                Utils.hideViewByRemovingFromParentUnderCondition(true, view);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideMiniplayerSubTexts failure", ex);
        }
    }
}
