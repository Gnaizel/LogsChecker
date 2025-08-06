package ru.gnaizel.exception.file;

public class FileUploadError extends RuntimeException {
    public FileUploadError(String message) {
        super(message);
    }
}
