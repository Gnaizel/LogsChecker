package ru.gnaizel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gnaizel.model.LogFile;

@Repository
public interface LogsRepository extends JpaRepository<LogFile, Long> {
    LogFile findByFileName(String fileName);
}
