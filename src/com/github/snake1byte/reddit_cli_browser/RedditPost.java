package com.github.snake1byte.reddit_cli_browser;

import java.net.URI;
import java.time.LocalDateTime;

public record RedditPost(String subredditID, String authorID, String subredditName, String authorName, int numberComments, boolean saved, String title, int upvotes, int downvotes, String id, int score, LocalDateTime created, URI media, String redditLink, boolean isVideo) {}
