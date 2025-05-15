package ru.gnaizel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gnaizel.model.FileInExpect;
import ru.gnaizel.model.FileUpload;

import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<FileInExpect, Long> {
    List<FileInExpect> findByOwnerTelegramId(long ownerTelegramId);

    void deleteByFile(FileUpload file);
}
