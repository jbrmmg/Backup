package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.FileInfo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface FileRepository extends CrudRepository<FileInfo, Integer>, JpaSpecificationExecutor<FileInfo> {
    Iterable<FileInfo> findByName(String name);

    Iterable<FileInfo> findByMd5(String md5);

    Iterable<FileInfo> findByDate(LocalDateTime date);

    Iterable<FileInfo> findAllByOrderByIdAsc();

    Iterable<FileInfo> findByParentId(Integer parentId);
}
