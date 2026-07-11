package com.oglimmer.wiki.exception;

import org.springframework.http.HttpStatus;

public sealed class ApiException extends RuntimeException
        permits NotFoundException, ConflictException, ValidationException, ForbiddenException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
