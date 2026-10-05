package org.ecom.productcatalog.exception;

import java.time.Instant;

public class ApiError {
    private final Instant timestamp;
    private final int status;
    private final String message;
    private final String path;

    public ApiError(int status, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}

