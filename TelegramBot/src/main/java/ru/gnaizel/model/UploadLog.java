package ru.gnaizel.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UploadLog {
    private String fileName;
    private long fileSizeInMegabyte;
}
