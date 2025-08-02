package ru.gnaizel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UploadApp {
    public static void main(String[] args) {
        SpringApplication.run(UploadApp.class, args);
    }
}