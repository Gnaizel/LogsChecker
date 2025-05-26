package ru.gnaizel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.gnaizel.enums.log.FileSizeUnit;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "log")
public class LogFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String fileName;
    private String originalFileName;
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_size_unit")
    private FileSizeUnit fileSizeUnit;

    private Long ownerId;
    private Long cleanLineCount;
    private Long allLineCount;
}
