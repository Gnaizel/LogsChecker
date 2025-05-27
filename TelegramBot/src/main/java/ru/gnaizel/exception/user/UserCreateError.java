package ru.gnaizel.exception.user;

public class UserCreateError extends RuntimeException {
    public UserCreateError(String message) {
        super(message);
    }
}
