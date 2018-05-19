package me.deprilula28.jdacmdframework.discordbotsorgapi;

public class APIError extends RuntimeException {
    private String message;

    public APIError(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
