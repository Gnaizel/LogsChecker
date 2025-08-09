package ru.gnaizel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogFileShortDto {
    private String fileName;
    private Long cleanLineCount;
    private Long allLineCount;
}