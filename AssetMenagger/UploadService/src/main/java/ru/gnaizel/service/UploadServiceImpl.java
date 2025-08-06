package ru.gnaizel.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.gnaizel.client.Client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final Client client;

    @Value("${upload-value.file-path}")
    private String filePath;

    @PostConstruct
    @Scheduled(fixedDelay = 10800000)
    @Override
    public void uploadFiles() {
        Path path = Paths.get(filePath);
        List<Path> files;
        try  {
            files = Files.list(path).toList();


            if (files.isEmpty()) {
                return;
            }

            for (Path pathFile : files) {
                File file = pathFile.toFile();
                if (!file.getName().endsWith(".txt")) {
                    file.delete();
                } else if (client.uploadLog(file).equals(HttpStatusCode.valueOf(200))) {
                    file.delete();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
