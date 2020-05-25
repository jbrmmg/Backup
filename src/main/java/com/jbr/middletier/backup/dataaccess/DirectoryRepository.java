package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.Source;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
public interface DirectoryRepository extends CrudRepository<DirectoryInfo, Integer>, JpaSpecificationExecutor {
    Optional<DirectoryInfo> findBySourceAndPath(Source source, String path);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("UPDATE DirectoryInfo SET removed=true")
    void markAllRemoved();
}
