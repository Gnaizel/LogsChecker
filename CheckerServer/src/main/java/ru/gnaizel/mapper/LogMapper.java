package ru.gnaizel.mapper;

import org.springframework.stereotype.Component;
import ru.gnaizel.dto.log.LogDto;
import ru.gnaizel.model.Log;

@Component
public class LogMapper {
    public static LogDto toDto(Log log) {
        return LogDto.builder()
                .fileName(log.getFileName())
                .fileSizeInMegabyte(log.getFileSizeInMegabyte())
                .build();
    }
}
