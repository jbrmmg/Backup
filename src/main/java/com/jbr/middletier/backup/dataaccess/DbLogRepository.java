package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.DbLog;
import org.springframework.data.repository.CrudRepository;

public interface DbLogRepository extends CrudRepository<DbLog,Integer> {
    Iterable<DbLog> findAllByOrderByDateAsc();
}
