package io.temporal.docs.bestpractices.errorhandling;

public class Result {
    private String status;
    private String message;

    public Result() {}

    private Result(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static Result approved(String message) {
        return new Result("approved", message);
    }

    public static Result rejected(String message) {
        return new Result("rejected", message);
    }

    public static Result declined(String message) {
        return new Result("declined", message);
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
