package ru.gnaizel.service.log;

import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogFileShortDto;
import ru.gnaizel.dto.log.LogFileUploadInfoDto;

import java.util.List;

public interface LogsService {

    LogFileUploadInfoDto uploadLog(MultipartFile file, long telegramId);

    List<LogFileShortDto> check(String url, long telegramId);
}
