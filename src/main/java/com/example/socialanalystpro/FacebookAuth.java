package com.example.socialanalystpro;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FacebookAuth {
    private static final String APP_ID = "883581437450898";
    // Using a standard redirect URI that Facebook allows for desktop apps or localhost
    // Ensure "https://localhost:8080/" is added to your App's "Valid OAuth Redirect URIs" in Facebook Developer Console
    private static final String REDIRECT_URI = "https://localhost:8080/";
    
    public String getAuthUrl() {
        return "https://www.facebook.com/v18.0/dialog/oauth?" +
               "client_id=" + APP_ID +
               "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
               "&scope=pages_read_engagement,pages_read_user_content,pages_show_list" +
               "&response_type=token";
    }
    
    public String getRedirectUri() {
        return REDIRECT_URI;
    }
}
