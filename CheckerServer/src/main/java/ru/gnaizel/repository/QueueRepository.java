package ru.gnaizel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gnaizel.model.FileInExpect;

@Repository
public interface QueueRepository extends JpaRepository<FileInExpect, Long> {
}
