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
public interface DirectoryRepository extends CrudRepository<DirectoryInfo, Integer>, JpaSpecificationExecutor {
    Optional<DirectoryInfo> findBySourceAndPath(Source source, String path);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("UPDATE DirectoryInfo SET removed=true")
    void markAllRemoved();

    /*
    @Query("SELECT new com.jbr.middletier.backup.data.SynchronizeStatus ( " +
            "f," +
            "d," +
            "c," +
            "s.source," +
            "s.destination," +
            "f2," +
            "d2" +
            ") FROM Synchronize AS s " +
            "INNER JOIN DirectoryInfo AS d ON d.source = s.destination " +
            "INNER JOIN FileInfo AS f ON f.directoryInfo.id = d.id " +
            "LEFT OUTER JOIN DirectoryInfo AS d2 ON d2.path = d.path AND d2.source = s.source " +
            "LEFT OUTER JOIN FileInfo AS f2 ON f2.directoryInfo.id = d2.id AND f.name = f2.name " +
            "LEFT OUTER JOIN Classification AS c ON f.classification.id = c.id " +
            "WHERE s.id = ?1 " +
            "AND f2. name is null"
    )
    List<SynchronizeStatus> findSynchronizeExtraFiles(int synchronize);
     */

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("DELETE FROM DirectoryInfo WHERE removed=true")
    void deleteRemoved();

    @Query("SELECT new com.jbr.middletier.backup.data.HierarchyResponse ( " +
           "d.source.id, " +
           "LENGTH(d.path) - LENGTH(REPLACE(d.path,'/','')), " +
           "d.path, " +
           "d.id ) " +
           "FROM DirectoryInfo AS d \n" +
           "WHERE d.source.id = ?1 " +
           "AND LENGTH(d.path) - LENGTH(REPLACE(d.path,'/','')) = ?2 " +
           "AND path like ?3 " +
           "ORDER BY d.path ")
    List<HierarchyResponse> findAtLevel(int source, int level, String path);
}
