package com.axiom.hermes.common.exceptions;

/**
 * Исключение для сообщения клиенту API ошибок уровня бизнес-логики
 */
public class HermesException extends Exception {

    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_REQUEST_TOO_LARGE =413;
    public static final int HTTP_UNSUPPORTED_MEDIA = 415;
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;

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
