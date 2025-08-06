package ru.gnaizel.mapper;

import org.springframework.stereotype.Component;
import ru.gnaizel.dto.log.LogFileShortDto;
import ru.gnaizel.dto.log.LogFileUploadInfoDto;
import ru.gnaizel.model.LogFile;

@Component
public class LogMapper {
    public LogFileUploadInfoDto toUploadInfoDto(LogFile logFile) {
        return LogFileUploadInfoDto.builder()
                .fileName(logFile.getFileName())
                .fileSize(logFile.getFileSize())
                .fileSizeUnit(logFile.getFileSizeUnit())
                .build();
    }

    public LogFileShortDto toShortDto(LogFile logFile) {
        return LogFileShortDto.builder()
                .fileName(logFile.getOriginalFileName())
                .cleanLineCount(logFile.getCleanLineCount())
                .allLineCount(logFile.getAllLineCount())
                .build();
    }
}
