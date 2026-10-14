package app.revanced.extension.youtube.patches;

import android.net.Uri;

import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.shared.Logger;

@SuppressWarnings("unused")
public class BypassURLRedirectsPatch {
    private static final String YOUTUBE_REDIRECT_PATH = "/redirect";

    /**
     * Injection point.
     * <p>
     * Convert the YouTube redirect URI to the redirect query URI.
     *
     * @param uri The YouTube redirect URI.
     * @return The redirect query URI, or the original URI if it's not a redirect.
     */
    public static Uri parseRedirectUri(Uri uri) {
        try {
            if (Settings.BYPASS_URL_REDIRECTS.get() && uri != null
                    && YOUTUBE_REDIRECT_PATH.equals(uri.getPath())) {
                String query = uri.getQueryParameter("q");
                if (query != null) {
                    Uri redirect = Uri.parse(query);
                    Logger.printDebug(() -> "Bypassing YouTube redirect URI: " + redirect);
                    return redirect;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "parseRedirectUri failure", ex);
        }

        return uri;
    }
}
