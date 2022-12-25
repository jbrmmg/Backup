package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.FileInfo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends CrudRepository<FileInfo, Integer>, JpaSpecificationExecutor<FileInfo> {
    Iterable<FileInfo> findByName(String name);

    Iterable<FileInfo> findAllByOrderByIdAsc();

    Iterable<FileInfo> findByParentId(Integer parentId);

    Iterable<FileInfo> findByFlagsContaining(String flags);
}
