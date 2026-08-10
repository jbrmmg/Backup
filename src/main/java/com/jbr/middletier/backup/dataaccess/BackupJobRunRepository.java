package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.BackupJobRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackupJobRunRepository extends CrudRepository<BackupJobRun, Long> {
    Optional<BackupJobRun> findByBackupIdAndRunDate(String backupId, LocalDate runDate);
    List<BackupJobRun> findAllByOrderByRunDateDescStartedAtAsc();
    List<BackupJobRun> findByBackupIdOrderByRunDateDesc(String backupId, Pageable pageable);
    void deleteByRunDateBefore(LocalDate cutoff);
}
