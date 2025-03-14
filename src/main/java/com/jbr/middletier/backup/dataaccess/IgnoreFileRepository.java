package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.IgnoreFile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface IgnoreFileRepository extends CrudRepository<IgnoreFile, Integer>, JpaSpecificationExecutor<IgnoreFile> {
    List<IgnoreFile> findByName(String name);
    List<IgnoreFile> findByMd5(String md5);

    Iterable<IgnoreFile> findAllByOrderByIdAsc();
}
