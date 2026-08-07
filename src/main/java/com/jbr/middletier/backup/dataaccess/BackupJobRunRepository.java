package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.BackupJobRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackupJobRunRepository extends CrudRepository<BackupJobRun, Long> {
    List<BackupJobRun> findByBackupIdOrderByStartedAtDesc(String backupId, Pageable pageable);
    void deleteByStartedAtBefore(LocalDateTime cutoff);
}
