package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Backup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupRepository extends CrudRepository<Backup, String> {
    Iterable<Backup> findAllByOrderByIdAsc();
    List<Backup> findAllByOrderByTimeAsc();
}
