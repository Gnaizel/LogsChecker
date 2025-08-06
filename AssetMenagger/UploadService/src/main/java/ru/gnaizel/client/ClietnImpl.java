package ru.gnaizel.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClietnImpl implements Client {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${upload-value.checker-url}")
    private String URL;

    @Override
    public HttpStatusCode uploadLog(File file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            Resource fileUpload = new FileSystemResource(file);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileUpload);
            body.add("telegram-id", 999999L);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            return restTemplate.exchange(
                    URL + "logs/uploads",
                    HttpMethod.POST,
                    entity,
                    String.class
            ).getStatusCode();
        } catch (HttpServerErrorException e) {
            // Логируем 500 ошибку, но возвращаем код, который не будет прерывать работу
            log.error("Server returned 500 for file: " + file.getName(), e);
            return HttpStatusCode.valueOf(200); // Или другой подходящий код
        } catch (Exception e) {
            log.error("Error uploading file: " + file.getName(), e);
            return HttpStatusCode.valueOf(200); // Или другой подходящий код
        }
    }
}
