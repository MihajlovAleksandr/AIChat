package com.example.aichat.model.entities;

public enum CommandOperation {

    OK(200),
    CREATED(201),
    NO_CONTENT(204),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    UNPROCESSABLE_ENTITY(422),
    INTERNAL_SERVER_ERROR(500),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);

    private final int code;

    CommandOperation(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CommandOperation valueOfCode(int code) {

        for (CommandOperation op : values()) {
            if (op.code == code) {
                return op;
            }
        }

        if (code >= 200 && code < 300) {
            return OK;
        }

        if (code >= 400 && code < 500) {
            return BAD_REQUEST;
        }

        if (code >= 500 && code < 600) {
            return INTERNAL_SERVER_ERROR;
        }

        return INTERNAL_SERVER_ERROR;
    }
}
