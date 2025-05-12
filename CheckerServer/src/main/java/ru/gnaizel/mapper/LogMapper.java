package ru.gnaizel.mapper;

import org.springframework.stereotype.Component;
import ru.gnaizel.dto.log.LogUploadDto;
import ru.gnaizel.model.FileUpload;

@Component
public class LogMapper {
    public static LogUploadDto toDto(FileUpload log) {
        return LogUploadDto.builder()
                .fileName(log.getFileName())
                .fileSizeInMegabyte(log.getFileSizeInMegabyte())
                .build();
    }
}
