package com.ankush.shortener.exception;

public class UnsafeUrlException extends RuntimeException {
    public UnsafeUrlException(String message) { super(message); }
}
