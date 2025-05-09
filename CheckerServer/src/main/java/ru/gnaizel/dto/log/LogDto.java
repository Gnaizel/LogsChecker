package ru.gnaizel.dto.log;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LogDto {
    private String fileName;
    private long fileSizeInMegabyte;
}
