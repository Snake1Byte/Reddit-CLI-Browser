package com.github.snake1byte.reddit_cli_browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class Constants {
    private static Constants unique = null;

    private ObjectMapper mapper;
    private HttpClient client;

    private String refreshToken;
    private String accessToken;
    private String userAgent;
    private String clientId;
    private String clientSecret;

    public static Constants instance() {
        if (unique == null) {
            try {
                unique = new Constants();
            } catch (IOException e) {
                //TODO log it or throw it
            }
        }
        return unique;
    }

    private Constants() throws IOException {
        mapper = new ObjectMapper();
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        InputStream constantsStream = getClass().getResourceAsStream("/com/github/snake1byte/reddit_cli_browser/resources/constants.json");
        JsonNode node = mapper.readTree(constantsStream);
        refreshToken = node.get("refresh_token").asText();
        userAgent = node.get("user_agent").asText();
        clientId = node.get("client_id").asText();
        clientSecret = node.get("client_secret").asText();
        accessToken = refreshAccessToken();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String refreshAccessToken() {
        try {
            String authorization = Base64.getEncoder()
                    .encodeToString(String.format("%s:%s", clientId, clientSecret).getBytes());
            HttpRequest req = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(String.format("grant_type=refresh_token&refresh_token=%s", refreshToken)))
                    .uri(new URI("https://www.reddit.com/api/v1/access_token"))
                    .header("Authorization", String.format("Basic %s", authorization)).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(res.body());
            accessToken = node.get("access_token").asText();
            return accessToken;
        } catch (URISyntaxException ignored) {
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e); //TODO exception handling
        }
        return null;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
