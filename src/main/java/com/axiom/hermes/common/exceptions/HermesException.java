package com.axiom.hermes.common.exceptions;

/**
 * Исключение для сообщения клиенту API ошибок уровня бизнес-логики
 */
public class HermesException extends Exception {

    public static final int BAD_REQUEST = 400;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int REQUEST_TOO_LARGE =413;
    public static final int UNSUPPORTED_MEDIA = 415;
    public static final int INTERNAL_SERVER_ERROR = 500;

    private final int status;
    private final String error;

    public HermesException(int statusCode, String error, String message) {
        super(message);
        this.status = statusCode;
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getPrettyJSON() {
        return
                "{\n" +
                "    \"status\": " + getStatus() + ",\n" +
                "    \"error\": \"" + getError() + "\",\n" +
                "    \"message\": \"" + getMessage() + "\",\n" +
                "}";
    }
}
