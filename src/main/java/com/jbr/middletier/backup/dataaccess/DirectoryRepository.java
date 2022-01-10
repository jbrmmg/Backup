package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.HierarchyResponse;
import com.jbr.middletier.backup.data.Source;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryRepository extends CrudRepository<DirectoryInfo, Integer>, JpaSpecificationExecutor<DirectoryInfo> {
    Optional<DirectoryInfo> findBySourceAndPath(Source source, String path);

    List<DirectoryInfo> findBySource(Source source);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("UPDATE DirectoryInfo SET removed=true WHERE source.id = ?1")
    void markAllRemoved(int source);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("DELETE FROM DirectoryInfo WHERE removed=?1")
    void deleteRemoved(boolean removed);

    @Query("SELECT new com.jbr.middletier.backup.data.HierarchyResponse ( " +
           "d.source.id, " +
           "LENGTH(d.path) - LENGTH(REPLACE(d.path,'/','')), " +
           "d.path, " +
           "d.id ) " +
           "FROM DirectoryInfo AS d " +
           "WHERE d.source.id = ?1 " +
           "AND LENGTH(d.path) - LENGTH(REPLACE(d.path,'/','')) = ?2 " +
           "AND path like ?3 " +
           "ORDER BY d.path ")
    List<HierarchyResponse> findAtLevel(int source, int level, String path);
}
