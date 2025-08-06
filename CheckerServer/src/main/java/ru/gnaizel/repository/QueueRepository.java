package ru.gnaizel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gnaizel.model.LogFileInExpect;
import ru.gnaizel.model.LogFile;

import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<LogFileInExpect, Long> {
    List<LogFileInExpect> findByOwnerTelegramId(long ownerTelegramId);

    void deleteByFile(LogFile file);
}
