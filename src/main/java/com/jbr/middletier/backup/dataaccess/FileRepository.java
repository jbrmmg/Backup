package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
public interface FileRepository extends CrudRepository<FileInfo, Integer>, JpaSpecificationExecutor {
    Optional<FileInfo> findByDirectoryInfoAndName(DirectoryInfo directoryInfo, String name);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("UPDATE FileInfo SET removed=true")
    void markAllRemoved();
}
