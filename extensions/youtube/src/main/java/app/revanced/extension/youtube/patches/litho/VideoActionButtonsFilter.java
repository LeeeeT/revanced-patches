package app.revanced.extension.youtube.patches.litho;

import app.revanced.extension.shared.patches.litho.Filter;
import app.revanced.extension.shared.patches.litho.FilterGroup.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.litho.FilterGroup.StringFilterGroup;
import app.revanced.extension.shared.patches.litho.FilterGroupList.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.litho.FilterGroupList.StringFilterGroupList;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class VideoActionButtonsFilter extends Filter {
    private static final String COMPACT_CHANNEL_BAR_PATH_PREFIX = "compact_channel_bar.e";
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.e";
    private static final String VIDEO_ACTION_BAR_PATH = "video_action_bar.e";
    /**
     * Video bar path when the video information is collapsed. Seems to shown only with 20.14+
     */
    private static final String COMPACTIFY_VIDEO_ACTION_BAR_PATH = "compactify_video_action_bar.e";
    private static final String ANIMATED_VECTOR_TYPE_PATH = "AnimatedVectorType";

    private final StringFilterGroup likeSubscribeGlow;
    private final StringFilterGroup actionBarGroup;
    private final StringFilterGroup buttonFilterPathGroup;
    private final StringFilterGroupList accessibilityButtonsGroupList = new StringFilterGroupList();

    public VideoActionButtonsFilter() {
        actionBarGroup = new StringFilterGroup(
                null,
                VIDEO_ACTION_BAR_PATH
        );
        addIdentifierCallbacks(actionBarGroup);

        likeSubscribeGlow = new StringFilterGroup(
                Settings.DISABLE_LIKE_SUBSCRIBE_GLOW,
                "animated_button_border.e"
        );

        buttonFilterPathGroup = new StringFilterGroup(
                null,
                "|ContainerType|button.e"
        );

        addPathCallbacks(
                likeSubscribeGlow,
                new StringFilterGroup(
                        Settings.HIDE_LIKE_DISLIKE_BUTTON,
                        "|segmented_like_dislike_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_DOWNLOAD_BUTTON,
                        "|download_button.e"
                ),
                new StringFilterGroup(
                        Settings.HIDE_SAVE_BUTTON,
                        "|save_to_playlist_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_CLIP_BUTTON,
                        "|clip_button.e"
                )
        );

        addPathCallbacks(buttonFilterPathGroup);

        // FIXME: Most buttons do not have an accessibilityId.
        //        Instead, they have an accessibilityText, so hiding functionality must be implemented using this
        //        (e.g. custom filter - 'video_action_bar#Hype')
        accessibilityButtonsGroupList.addAll(
                new StringFilterGroup(
                        Settings.HIDE_SHARE_BUTTON,
                        "id.video.share.button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_REMIX_BUTTON,
                        "id.video.remix.button"
                )
        );
    }

    private boolean isEveryFilterGroupEnabled() {
        for (var group : pathCallbacks) {
            if (!group.isEnabled()) return false;
        }

        for (var group : accessibilityButtonsGroupList) {
            if (!group.isEnabled()) return false;
        }

        return true;
    }

    private boolean hideButtons(String path, String accessibility, byte[] buffer) {
        // Make sure the current path is the right one to avoid false positives.
        if (!path.startsWith(VIDEO_ACTION_BAR_PATH) && !path.startsWith(COMPACTIFY_VIDEO_ACTION_BAR_PATH)) {
            return false;
        }

        return accessibilityButtonsGroupList.check(accessibility).isFiltered();
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                       StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == likeSubscribeGlow) {
            return path.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX) || path.startsWith(COMPACT_CHANNEL_BAR_PATH_PREFIX)
                    || path.startsWith(COMPACTIFY_VIDEO_ACTION_BAR_PATH);
        }

        // If the current matched group is the action bar group,
        // in case every filter group is enabled, hide the action bar.
        if (matchedGroup == actionBarGroup) {
            return isEveryFilterGroupEnabled();
        }

        if (matchedGroup == buttonFilterPathGroup) {
            return hideButtons(path, accessibility, buffer);
        }

        return true;
    }
}
