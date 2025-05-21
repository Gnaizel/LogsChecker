package ru.gnaizel.service.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogUploadDto;
import ru.gnaizel.exceptions.FileUploadError;
import ru.gnaizel.exceptions.FileValidationException;
import ru.gnaizel.mapper.LogMapper;
import ru.gnaizel.model.FileInExpect;
import ru.gnaizel.model.FileUpload;
import ru.gnaizel.repository.LogsRepository;
import ru.gnaizel.repository.QueueRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogsServiceImpl implements LogsService {
    private final LogsRepository logsRepository;
    private final QueueRepository queueRepository;

    @Value("${upload_dir}")
    private String UPLOAD_DIR;

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors(); // Количество потоков равно количеству ядер
    private static final int BLOCK_SIZE = 1024 * 1024 * 10;
    @Value("${result_dir}")
    private String RESULT_DIR;

    @Override
    @Transactional
    public LogUploadDto uploadLog(MultipartFile file, long telegramId) {
        if (file.isEmpty()) throw new FileValidationException("File is empty");
        FileUpload fileLog;

        try {
            Path uploadDir = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = Objects.requireNonNull(file.getOriginalFilename()).replace(".txt", "-")
                    + Timestamp.valueOf(LocalDateTime.now()).getTime() + ".txt";
            Path filePath = uploadDir.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File: {}, ({}) is upload in server", fileName, file.getSize() / 1048576);

            fileLog = FileUpload.builder()
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSizeInMegabyte(file.getSize() / 1048576)
                    .ownerId(telegramId)
                    .build();
        } catch (IOException e) {
            throw new FileUploadError(e.getMessage());
        }

        FileUpload fileUpload = logsRepository.save(fileLog);
        queueRepository.save(FileInExpect.builder()
                .ownerTelegramId(fileUpload.getOwnerId())
                .file(fileUpload)
                .build());

        return LogMapper.toDto(fileUpload);
    }

    @Override
    @Transactional
    public long check(String url, long telegramId) {
        AtomicLong countWriteLine = new AtomicLong();
        countWriteLine.set(0);
        Path resultDir = Paths.get(RESULT_DIR);

        try {
            if (!Files.exists(resultDir)) Files.createDirectories(resultDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Path> filePaths = queueRepository.findByOwnerTelegramId(telegramId).stream()
                .map(FileInExpect::getFile)
                .map(fileUpload -> Paths.get(UPLOAD_DIR).resolve(fileUpload.getFileName()))
                .toList();

        for (Path file : filePaths) {
            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            BlockingQueue<List<String>> resultsQueue = new LinkedBlockingQueue<>();
            File resultFile = resultDir.resolve(file.getFileName()).toFile();

            List<Future<?>> futures = new ArrayList<>();

            // Поток записи
            Future<?> writerFuture = executor.submit(() -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
                    while (true) {
                        List<String> lines = resultsQueue.poll(1, TimeUnit.SECONDS);
                        if (lines == null) continue;
                        if (lines.isEmpty()) break; // сигнал завершения

                        for (String line : lines) {
                            countWriteLine.incrementAndGet();
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    log.error("Writer thread error: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            });

            // Чтение и разбиение файла на блоки
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file.toFile())))) {

                char[] buffer = new char[BLOCK_SIZE];
                int bytesRead;

                while ((bytesRead = reader.read(buffer)) != -1) {
                    String block = new String(buffer, 0, bytesRead);

                    String finalUrl = url;
                    Future<?> future = executor.submit(() -> {
                        List<String> localResults = new ArrayList<>();
                        String[] lines = block.split("\\r?\\n");

                        for (String line : lines) {
                            if (line.contains(finalUrl)) {
                                localResults.add(processLine(finalUrl, line));
                            }
                        }

                        try {
                            resultsQueue.put(localResults);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });

                    futures.add(future);
                }

            } catch (IOException e) {
                log.error("Error reading file {}: {}", file.getFileName(), e.getMessage());
            }

            // Дождаться завершения всех задач обработки блоков
            for (Future<?> f : futures) {
                try {
                    f.get(); // ждем каждую
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error in processing task: {}", e.getMessage());
                }
            }

            try {
                // Отправляем сигнал завершения писателю
                resultsQueue.put(new ArrayList<>());

                // Ждём завершения записи
                writerFuture.get();

                // Очищаем очередь ожидания (done)
//                queueRepository.deleteByFile(logsRepository.findByFileName(file.toFile().getName()));
            } catch (InterruptedException | ExecutionException e) {
                log.error("Finalization error: {}", e.getMessage());
            } finally {
                executor.shutdownNow(); // жёсткое завершение
            }
        }

        return countWriteLine.get();
    }

    private String processLine(String url, String line) {
        String cleanUrl = url.replaceAll("^(?i)https?://", "");

        String regex = "(?i)^(https?://)?" + Pattern.quote(cleanUrl) + ".*?:";

        String result = line.replaceFirst(regex, "");

        result = result.replaceAll("^:|:$", "");

        return result;
    }
}
