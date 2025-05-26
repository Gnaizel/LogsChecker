package ru.gnaizel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogFileInExpect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long ownerTelegramId;

    @JoinColumn(name = "fileId")
    @OneToOne
    private LogFile file;

}
