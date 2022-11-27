package com.github.snake1byte.reddit_cli_browser;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;


public record RedditComment(String fullname, String authorFullname, String author, boolean saved, LocalDateTime created,
                            int score, String text, boolean isOP, int downvotes, boolean pinned, URI redditLink,
                            int upvotes, List<RedditComment> children, List<String> more) {
}
