package me.deprilula28.jdacmdframework.exceptions;

public class CommandArgsException extends RuntimeException {
    private String message;

    public CommandArgsException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
