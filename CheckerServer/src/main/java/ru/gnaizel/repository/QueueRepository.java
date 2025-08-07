package ru.gnaizel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gnaizel.model.LogFileInExpect;
import ru.gnaizel.model.LogFile;

@Repository
public interface QueueRepository extends JpaRepository<LogFileInExpect, Long> {
    void deleteByFile(LogFile file);
}
