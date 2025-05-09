package ru.gnaizel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogDto;
import ru.gnaizel.exceptions.FileUploadError;
import ru.gnaizel.exceptions.FileValidationException;
import ru.gnaizel.mapper.LogMapper;
import ru.gnaizel.model.Log;
import ru.gnaizel.repository.LogsRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogsServiceImpl implements LogsService {
    private final LogsRepository logsRepository;

    @Value("${upload_dir}")
    private String UPLOAD_DIR;


    @Override
    public LogDto uploadLog(MultipartFile file, long telegramId) {
        if (file.isEmpty()) throw new FileValidationException("File is empty");
        Log fileLog;

        try {
            Path uploadDir = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null) throw new FileValidationException("File name can't be null");
            Path filePath = uploadDir.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File: {}, ({}) is upload in server", fileName, file.getSize() / 1048576);

            fileLog = Log.builder()
                    .fileName(fileName)
                    .fileSizeInMegabyte(file.getSize() / 1048576)
                    .ownerId(telegramId)
                    .build();
        } catch (IOException e) {
            throw new FileUploadError(e.getMessage());
        }

        return LogMapper.toDto(logsRepository.save(fileLog));
    }
}
