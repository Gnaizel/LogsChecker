package ru.gnaizel.dto.log;

import lombok.Builder;
import lombok.Data;
import ru.gnaizel.enums.log.FileSizeUnit;

@Builder
@Data
public class LogFileUploadInfoDto {
    private String fileName;
    private long fileSize;
    private FileSizeUnit fileSizeUnit;
}
