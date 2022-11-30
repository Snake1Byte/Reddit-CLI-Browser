// INFO: bei kommentaren ist das letzte im array was man kriegt ein objekt mit dem "kind" attribut auf "more" gesetzt. diese objekte
// beinhalten wiederum arrays voller IDs von weitern kommentaren auf dieser reply-tree ebene (quasi, siblings, nicht children)
// wenn man immer tiefer im reply-tree gehen will, dann wird einem als reply objekt mancher kommentare auch ein "more" angeboten statt
// eines konkreten kommentares. dieses kann man dann benutzen um weiter im kommentar baum zu graben

package com.github.snake1byte.reddit_cli_browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RedditClient {
    private HttpClient client;
    private ObjectMapper mapper;
    private Constants constants;

    private Logger logger = LogManager.getLogger("file");

    public static void main(String[] args) {
        RedditClient client = new RedditClient();
        client.getPosts(Listings.HOT, "askreddit", null, null, 2);
    }

    public RedditClient() {
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        constants = Constants.instance();
        mapper = new ObjectMapper();
    }

    public CompletableFuture<List<RedditPost>> getPosts(Listings listings, String subreddit, Listings.Timespan timespan, String after, int limit) {
        URIBuilder builder = null;
        try {
            builder = new URIBuilder("https://oauth.reddit.com");
        } catch (URISyntaxException ignored) {
        }
        if (subreddit != null) {
            builder.setPathSegments("r", subreddit);
        }

        builder.appendPath(listings.name().toLowerCase());

        if (timespan != null) {
            builder.addParameter("t", timespan.name().toLowerCase());
        }

        if (after != null) {
            builder.addParameter("after", after);
        }

        if (limit > -1) {
            builder.addParameter("limit", String.valueOf(limit));
        }

        HttpRequest req = null;
        try {
            req = HttpRequest.newBuilder().GET().uri(builder.build())
                    .header("Authorization", String.format("Bearer %s", constants.getAccessToken()))
                    .header("User-Agent", constants.getUserAgent()).build();
        } catch (URISyntaxException ignored) {
        }

        CompletableFuture<List<RedditPost>> completableFuture = new CompletableFuture<>();
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).handle((res, throwable) -> {
            if (throwable == null) {
                try {
                    List<RedditPost> posts = new ArrayList<>();
                    JsonNode node = mapper.readTree(res.body());
                    for (JsonNode post : node.get("data").get("children")) {
                        post = post.get("data");
                        RedditPost redditPost = new RedditPost(post.get("subreddit_id")
                                .asText(), post.get("author_fullname") == null ? null : post.get("author_fullname")
                                .asText(), post.get("subreddit").asText(), post.get("author")
                                .asText(), post.get("num_comments").asInt(), post.get("saved")
                                .asBoolean(), post.get("title").asText(), post.get("ups").asInt(), post.get("downs")
                                .asInt(), post.get("name").asText(), post.get("id").asText(), post.get("score")
                                .asInt(), LocalDateTime.ofInstant(Instant.ofEpochMilli(((long) post.get("created")
                                .asDouble()) * 1000), ZoneId.of("UTC+1")), post.get("url_overridden_by_dest") == null ? null : new URI(post.get("url_overridden_by_dest")
                                .asText()), post.get("permalink").asText(), post.get("is_video").asBoolean());
                        posts.add(redditPost);
                    }
                    completableFuture.complete(posts);
                } catch (IOException | URISyntaxException e) {
                    completableFuture.completeExceptionally(new RedditEndpointException("Error trying to fetch posts.", e));
                }
            } else {
                completableFuture.completeExceptionally(new RedditEndpointException("An exception occurred while querying the Reddit API.", throwable));
            }
            return null;
        });

        return completableFuture;
    }

    public CompletableFuture<List<RedditComment>> getComments(String postId, int limit, String highlightedCommentId) {
        URIBuilder builder = null;
        try {
            builder = new URIBuilder("https://oauth.reddit.com");
        } catch (URISyntaxException ignored) {
        }

        builder.setPathSegments("comments", postId);
        if (limit > -1) {
            builder.addParameter("limit", String.valueOf(limit));
        }

        if (highlightedCommentId != null) {
            builder.addParameter("comment", highlightedCommentId);
        }

        HttpRequest req = null;
        try {
            req = HttpRequest.newBuilder().GET().uri(builder.build())
                    .header("Authorization", String.format("Bearer %s", constants.getAccessToken()))
                    .header("User-Agent", constants.getUserAgent()).build();
        } catch (URISyntaxException ignored) {
        }

        CompletableFuture<List<RedditComment>> completableFuture = new CompletableFuture<>();
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).handle((res, throwable) -> {
            if (throwable == null) {
                try {
                    List<RedditComment> comments = new ArrayList<>();
                    JsonNode node = mapper.readTree(res.body());
                    for (JsonNode comment : node.get(1).get("data").get("children")) {
                        String kind = comment.get("kind").asText();
                        comment = comment.get("data");
                        if (kind.equals("t1")) {
                            comments.add(new RedditComment(
                                    comment.get("name").asText(),
                                    comment.get("author_fullname").asText(),
                                    comment.get("author").asText(),
                                    comment.get("saved").asBoolean(),
                                    LocalDateTime.ofInstant(Instant.ofEpochMilli((long) comment.get("created").asDouble()), ZoneId.of("UTC+1")),
                                    comment.get("score").asInt(),
                                    comment.get("body").asText(),
                                    comment.get("is_submitter").asBoolean(),
                                    comment.get("downs").asInt(),
                                    comment.get("stickied").asBoolean(),
                                    new URI(comment.get("permalink").asText()),
                                    comment.get("ups").asInt(), null, null));
                        } else if (kind.equals("more")) {
                            // TODO
                        }
                    }
                    completableFuture.complete(comments);
                } catch (IOException | URISyntaxException e) {
                    completableFuture.completeExceptionally(new RedditEndpointException("Error trying to fetch comments.", e));
                }
            } else {
                completableFuture.completeExceptionally(new RedditEndpointException("An exception occurred while querying the Reddit API.", throwable));
            }
            return null;
        });

        return completableFuture;
    }
}