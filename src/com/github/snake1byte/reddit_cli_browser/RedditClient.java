// INFO: bei kommentaren ist das letzte im array was man kriegt ein objekt mit dem "kind" attribut auf "more" gesetzt. diese objekte
// beinhalten wiederum arrays voller IDs von weitern kommentaren auf dieser reply-tree ebene (quasi, siblings, nicht children)
// wenn man immer tiefer im reply-tree gehen will, dann wird einem als reply objekt mancher kommentare auch ein "more" angeboten statt
// eines konkreten kommentares. dieses kann man dann benutzen um weiter im kommentar baum zu graben

package com.github.snake1byte.reddit_cli_browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class RedditClient {
    private HttpClient client;
    private ObjectMapper mapper;
    private Constants constants;

    public static void main(String[] args) {
        RedditClient client = new RedditClient();
        client.getPosts(Listings.TOP, null, null, null, 1000);
    }

    public RedditClient() {
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        constants = Constants.instance();
        mapper = new ObjectMapper();
    }

    public List<JsonNode> getPosts(Listings listings, String subreddit, Listings.Sort sort, String after, int limit) {
        try {
            URIBuilder builder = new URIBuilder("https://oauth.reddit.com");
            if (subreddit != null) {
                builder.setPathSegments("r", subreddit);
            }

            builder.appendPath(listings.name().toLowerCase());

            if (sort != null) {
                builder.addParameter("t", sort.name().toLowerCase());
            }

            if (after != null) {
                builder.addParameter("after", after);
            }

            if (limit > -1) {
                builder.addParameter("limit", String.valueOf(limit));
            }

            try {
                HttpRequest req = HttpRequest.newBuilder().GET().uri(builder.build())
                        .header("Authorization", String.format("Bearer %s", constants.getAccessToken()))
                        .header("User-Agent", constants.getUserAgent()).build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println(mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(mapper.readTree(res.body())));
                List<RedditPost> posts = new ArrayList<>();
                JsonNode node = mapper.readTree(res.body());
                for (JsonNode post : node.get("data").get("children")) {
                    post = post.get("data");
                    System.out.println(post.toString());
                    System.out.println("\n\n\n\n");
                    posts.add(new RedditPost(
                            post.get("subreddit_id").asText(),
                            post.get("author_fullname") == null ? null : post.get("author_fullname").asText(),
                            post.get("subreddit").asText(),
                            post.get("author").asText(),
                            post.get("num_comments").asInt(),
                            post.get("saved").asBoolean(),
                            post.get("title").asText(),
                            post.get("ups").asInt(),
                            post.get("downs").asInt(),
                            post.get("name").asText(),
                            post.get("score").asInt(),
                            LocalDateTime.ofInstant(Instant.ofEpochMilli((long) post.get("created").asDouble()), ZoneId.of("UTC+1")),
                            post.get("url_overridden_by_dest") == null ? null : new URI(post.get("url_overridden_by_dest").asText()),
                            post.get("permalink").asText(),
                            post.get("is_video").asBoolean()));
                }
                System.out.println();
            } catch (URISyntaxException ignored) {
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e); //TODO exceptions
            }
        } catch (URISyntaxException ignored) {
        }

        return null;
    }

}
