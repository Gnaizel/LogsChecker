package ru.gnaizel.dto.log;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LogUploadDto {
    private String fileName;
    private long fileSizeInMegabyte;
}
