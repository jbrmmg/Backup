package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Backup;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by jason on 11/02/17.
 */
@Repository
public interface BackupRepository extends CrudRepository<Backup, String>, JpaSpecificationExecutor<Backup> {
    Iterable<Backup> findAllByOrderByIdAsc();
}
