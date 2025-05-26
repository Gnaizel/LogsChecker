package ru.gnaizel.exception.file;

public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}
