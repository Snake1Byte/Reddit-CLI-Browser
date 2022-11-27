// INFO: bei kommentaren ist das letzte im array was man kriegt ein objekt mit dem "kind" attribut auf "more" gesetzt. diese objekte
// beinhalten wiederum arrays voller IDs von weitern kommentaren auf dieser reply-tree ebene (quasi, siblings, nicht children)
// wenn man immer tiefer im reply-tree gehen will, dann wird einem als reply objekt mancher kommentare auch ein "more" angeboten statt
// eines konkreten kommentares. dieses kann man dann benutzen um weiter im kommentar baum zu graben

package com.github.snake1byte.reddit_cli_browser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.|URISyntaxException;
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

    public List<RedditPost> getPosts(Listings listings, String subreddit, Listings.Timespan timespan, String after, int limit) throws RedditEndpointException {
        try {
            URIBuilder builder = new URIBuilder("https://oauth.reddit.com");
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

            HttpRequest req = HttpRequest.newBuilder().GET().uri(builder.build())
                    .header("Authorization", String.format("Bearer %s", constants.getAccessToken()))
                    .header("User-Agent", constants.getUserAgent()).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString()); //TODO async
            List<RedditPost> posts = new ArrayList<>();
            JsonNode node = mapper.readTree(res.body());
            //                logger.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
            for (JsonNode post : node.get("data").get("children")) {
                post = post.get("data");
                RedditPost redditPost = new RedditPost(post.get("subreddit_id")
                        .asText(), post.get("author_fullname") == null ? null : post.get("author_fullname")
                        .asText(), post.get("subreddit").asText(), post.get("author").asText(), post.get("num_comments")
                        .asInt(), post.get("saved").asBoolean(), post.get("title").asText(), post.get("ups")
                        .asInt(), post.get("downs").asInt(), post.get("name").asText(), post.get("id")
                        .asText(), post.get("score")
                        .asInt(), LocalDateTime.ofInstant(Instant.ofEpochMilli((long) post.get("created")
                        .asDouble()), ZoneId.of("UTC+1")), post.get("url_overridden_by_dest") == null ? null : new URI(post.get("url_overridden_by_dest")
                        .asText()), post.get("permalink").asText(), post.get("is_video").asBoolean());
                posts.add(redditPost);
                getComments(redditPost.id(), -1, null);
            }
            //                posts.forEach(logger::info);
            return posts;
        } catch (IOException | URISyntaxException e) {
            throw new RedditEndpointException("Error trying to fetch posts.", e);
        }
    }

    public List<RedditComment> getComments(String postId, int limit, String highlightedCommentId) throws RedditEndpointException {
        try {
            URIBuilder builder = new URIBuilder("https://oauth.reddit.com");
            builder.setPathSegments("comments", postId);
            if (limit > -1) {
                builder.addParameter("limit", String.valueOf(limit));
            }

            if (highlightedCommentId != null) {
                builder.addParameter("comment", highlightedCommentId);
            }

            HttpRequest req = HttpRequest.newBuilder().GET().uri(builder.build())
                    .header("Authorization", String.format("Bearer %s", constants.getAccessToken()))
                    .header("User-Agent", constants.getUserAgent()).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());  //TODO async
            List<RedditComment> comments = new ArrayList<>();
            JsonNode node = mapper.readTree(res.body());
            //                logger.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
            for (JsonNode comment : node.get(1).get("data").get("children")) {
                comment = comment.get("data");
                comments.add(new RedditComment(comment.get("name").asText(), comment.get("author_fullname")
                        .asText(), comment.get("author").asText(), comment.get("saved")
                        .asBoolean(), LocalDateTime.ofInstant(Instant.ofEpochMilli((long) comment.get("created")
                        .asDouble()), ZoneId.of("UTC+1")), comment.get("score").asInt(), comment.get("body")
                        .asText(), comment.get("is_submitter").asBoolean(), comment.get("downs")
                        .asInt(), comment.get("stickied").asBoolean(), new URI(comment.get("permalink")
                        .asText()), comment.get("ups").asInt(), null, null));
            }
            //                comment.forEach(logger::info);
            return comments;
        } catch (IOException | URISyntaxException e) {
            throw new RedditEndpointException("Error trying to fetch comments.", e);
        }
    }
}