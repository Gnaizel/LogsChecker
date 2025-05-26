package ru.gnaizel.service.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.gnaizel.dto.log.LogFileShortDto;
import ru.gnaizel.dto.log.LogFileUploadInfoDto;
import ru.gnaizel.enums.log.FileSizeUnit;
import ru.gnaizel.exception.file.FileUploadError;
import ru.gnaizel.exception.file.FileValidationException;
import ru.gnaizel.mapper.LogMapper;
import ru.gnaizel.model.LogFileInExpect;
import ru.gnaizel.model.LogFile;
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
    private final LogMapper logMapper;

    @Value("${upload_dir}")
    private String UPLOAD_DIR;

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors(); // Количество потоков равно количеству ядер
    private static final int BLOCK_SIZE = 1024 * 1024 * 10;
    @Value("${result_dir}")
    private String RESULT_DIR;

    @Override
    @Transactional
    public LogFileUploadInfoDto uploadLog(MultipartFile file, long telegramId) {
        if (file.isEmpty()) throw new FileValidationException("File is empty");
        LogFile fileLog;

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

            fileLog = LogFile.builder()
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .ownerId(telegramId)
                    .cleanLineCount(0L)
                    .allLineCount(0L)
                    .build();

            long fileSize = file.getSize();
            if (fileSize < 1024) {
                fileLog.setFileSize(fileSize);
                fileLog.setFileSizeUnit(FileSizeUnit.BYTE);
            } else if (fileSize < 1024 * 1024) {
                // Килобайты
                long sizeInKB = fileSize / 1024;
                fileLog.setFileSize(sizeInKB);
                fileLog.setFileSizeUnit(FileSizeUnit.KILOBYTE);
            } else if (fileSize < 1024 * 1024 * 1024) {
                // Мегабайты
                long sizeInMB = fileSize / (1024 * 1024);
                fileLog.setFileSize(sizeInMB);
                fileLog.setFileSizeUnit(FileSizeUnit.MEGABYTE);
            } else {
                // Гигабайты
                long sizeInGB = fileSize / (1024 * 1024 * 1024);
                fileLog.setFileSize(sizeInGB);
                fileLog.setFileSizeUnit(FileSizeUnit.GIGABYTE);
            }
        } catch (IOException e) {
            throw new FileUploadError(e.getMessage());
        }

        LogFile fileUpload = logsRepository.save(fileLog);
        queueRepository.save(LogFileInExpect.builder()
                .ownerTelegramId(fileUpload.getOwnerId())
                .file(fileUpload)
                .build());

        return logMapper.toUploadInfoDto(fileUpload);
    }

    @Override
    @Transactional
    public List<LogFileShortDto> check(String url, long telegramId) {
        AtomicLong countAllLine = new AtomicLong();
        AtomicLong countWriteLine = new AtomicLong();
        countWriteLine.set(0);

        createResultDirectory();
        List<LogFile> logFiles = getLogFiles(telegramId);
        List<Path> filePaths = getFilePaths(logFiles);

        for (Path file : filePaths) {
            processFile(url, file, countAllLine, countWriteLine, logFiles);
        }

        return mapToShortDto(logFiles);
    }

    // Основные вспомогательные методы
    private void createResultDirectory() {
        Path resultDir = Paths.get(RESULT_DIR);
        try {
            if (!Files.exists(resultDir)) Files.createDirectories(resultDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create result directory", e);
        }
    }

    private List<LogFile> getLogFiles(long telegramId) {
        return queueRepository.findByOwnerTelegramId(telegramId).stream()
                .map(LogFileInExpect::getFile)
                .toList();
    }

    private List<Path> getFilePaths(List<LogFile> logFiles) {
        return logFiles.stream()
                .map(fileUpload -> Paths.get(UPLOAD_DIR).resolve(fileUpload.getFileName()))
                .toList();
    }

    private List<LogFileShortDto> mapToShortDto(List<LogFile> logFiles) {
        return logFiles.stream()
                .map(logMapper::toShortDto)
                .toList();
    }

    // Методы обработки файлов
    private void processFile(String url, Path file, AtomicLong countAllLine,
                             AtomicLong countWriteLine, List<LogFile> logFiles) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        BlockingQueue<List<String>> resultsQueue = new LinkedBlockingQueue<>();
        File resultFile = getResultFile(file);

        Future<?> writerFuture = startWriterThread(executor, resultsQueue, resultFile, countWriteLine);
        processFileContent(url, file, executor, resultsQueue, countAllLine);
        waitForProcessingTasks(executor, resultsQueue, writerFuture);

        LogFile fileFromDataBase = updateFileStatistics(file, countAllLine, countWriteLine);
        cleanUpResources(executor, fileFromDataBase);
    }

    private File getResultFile(Path file) {
        return Paths.get(RESULT_DIR).resolve(file.getFileName()).toFile();
    }

    // Методы работы с потоками
    private Future<?> startWriterThread(ExecutorService executor,
                                        BlockingQueue<List<String>> resultsQueue,
                                        File resultFile,
                                        AtomicLong countWriteLine) {
        return executor.submit(() -> runWriter(resultsQueue, resultFile, countWriteLine));
    }

    private void runWriter(BlockingQueue<List<String>> resultsQueue,
                           File resultFile,
                           AtomicLong countWriteLine) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
            while (!Thread.currentThread().isInterrupted()) {
                List<String> lines = resultsQueue.poll(1, TimeUnit.SECONDS);
                if (lines == null) continue;
                if (lines.isEmpty()) break;

                writeLines(writer, lines, countWriteLine);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Writer thread error: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void writeLines(BufferedWriter writer, List<String> lines, AtomicLong countWriteLine) throws IOException {
        for (String line : lines) {
            countWriteLine.incrementAndGet();
            writer.write(line);
            writer.newLine();
        }
    }

    // Методы обработки содержимого файла
    private void processFileContent(String url, Path file,
                                    ExecutorService executor,
                                    BlockingQueue<List<String>> resultsQueue,
                                    AtomicLong countAllLine) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            processFileBlocks(url, executor, resultsQueue, reader, countAllLine);
        } catch (IOException e) {
            log.error("Error reading file {}: {}", file.getFileName(), e.getMessage());
        }
    }

    private void processFileBlocks(String url,
                                   ExecutorService executor,
                                   BlockingQueue<List<String>> resultsQueue,
                                   BufferedReader reader,
                                   AtomicLong countAllLine) throws IOException {
        char[] buffer = new char[BLOCK_SIZE];
        int bytesRead;

        while ((bytesRead = reader.read(buffer)) != -1) {
            submitProcessingTask(url, buffer, bytesRead, executor, resultsQueue, countAllLine);
        }
    }

    private void submitProcessingTask(String url,
                                      char[] buffer,
                                      int bytesRead,
                                      ExecutorService executor,
                                      BlockingQueue<List<String>> resultsQueue,
                                      AtomicLong countAllLine) {
        String block = new String(buffer, 0, bytesRead);
        executor.submit(() -> processBlock(url, block, resultsQueue, countAllLine));
    }

    private void processBlock(String url,
                              String block,
                              BlockingQueue<List<String>> resultsQueue,
                              AtomicLong countAllLine) {
        List<String> localResults = new ArrayList<>();
        String[] lines = block.split("\\r?\\n");

        for (String line : lines) {
            countAllLine.incrementAndGet();
            if (line.contains(url)) {
                localResults.add(processLine(url, line));
            }
        }

        try {
            resultsQueue.put(localResults);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Методы завершения обработки
    private void waitForProcessingTasks(ExecutorService executor,
                                        BlockingQueue<List<String>> resultsQueue,
                                        Future<?> writerFuture) {
        try {
            resultsQueue.put(new ArrayList<>());
            writerFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Finalization error: {}", e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private LogFile updateFileStatistics(Path file,
                                         AtomicLong countAllLine,
                                         AtomicLong countWriteLine) {
        LogFile fileFromDataBase = logsRepository.findByFileName(file.getFileName().toString());
        fileFromDataBase.setAllLineCount(countAllLine.get());
        fileFromDataBase.setCleanLineCount(countWriteLine.get());
        logsRepository.save(fileFromDataBase);
        log.debug("Updated line statistics in DB");
        return fileFromDataBase;
    }

    private void cleanUpResources(ExecutorService executor, LogFile fileFromDataBase) {
        try {
            queueRepository.deleteByFile(fileFromDataBase);
        } finally {
            executor.shutdownNow();
        }
    }

    private String processLine(String url, String line) {
        String cleanUrl = url.replaceAll("^(?i)https?://", "");

        String regex = "(?i)^(https?://)?" + Pattern.quote(cleanUrl) + ".*?:";

        String result = line.replaceFirst(regex, "");

        result = result.replaceAll("^:|:$", "");

        return result;
    }
}
