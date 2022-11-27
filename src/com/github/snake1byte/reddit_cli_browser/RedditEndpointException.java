package com.github.snake1byte.reddit_cli_browser;

public class RedditEndpointException extends Exception {
    public RedditEndpointException() {
    }

    public RedditEndpointException(String message) {
        super(message);
    }

    public RedditEndpointException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedditEndpointException(Throwable cause) {
        super(cause);
    }

    public RedditEndpointException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
