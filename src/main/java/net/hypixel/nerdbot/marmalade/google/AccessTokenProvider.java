package net.hypixel.nerdbot.marmalade.google;

/**
 * Supplies a currently-valid Google OAuth2 access token. Implementations are
 * expected to cache and refresh internally so callers can request a token per
 * API call without cost.
 */
public interface AccessTokenProvider {

    String getAccessToken() throws GoogleAuthException;
}
