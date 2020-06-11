package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.SynchronizeStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends CrudRepository<FileInfo, Integer>, JpaSpecificationExecutor {
    Optional<FileInfo> findByDirectoryInfoAndName(DirectoryInfo directoryInfo, String name);

    List<FileInfo> findByName(String name);

    List<FileInfo> findByDirectoryInfoId(Integer id);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("UPDATE FileInfo SET removed=true WHERE id IN (SELECT id FROM DirectoryInfo WHERE source.id = ?1)")
    void markAllRemoved(int source);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("DELETE FROM FileInfo WHERE removed=true")
    void deleteRemoved();

    @Query("SELECT new com.jbr.middletier.backup.data.SynchronizeStatus ( " +
            "f," +
            "d," +
            "c," +
            "s.source," +
            "s.destination," +
            "f2," +
            "d2" +
            ") FROM Synchronize AS s " +
            "INNER JOIN DirectoryInfo AS d ON d.source = s.source " +
            "INNER JOIN FileInfo AS f ON f.directoryInfo.id = d.id " +
            "LEFT OUTER JOIN DirectoryInfo AS d2 ON d2.path = d.path AND d2.source = s.destination " +
            "LEFT OUTER JOIN FileInfo AS f2 ON f2.directoryInfo.id = d2.id AND f.name = f2.name " +
            "LEFT OUTER JOIN Classification AS c ON f.classification.id = c.id " +
            "WHERE s.id = ?1"
    )
    List<SynchronizeStatus> findSynchronizeStatus(int synchronize);

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

    @Query("SELECT f.name " +
            "FROM Source s " +
            "INNER JOIN DirectoryInfo d ON d.source.id = s.id " +
            "INNER JOIN FileInfo f ON f.directoryInfo.id = d.id " +
            "INNER JOIN Classification c ON f.classification.id = c.id " +
            "WHERE s.id = ?1 " +
            "AND c.action = 'BACKUP' " +
            "GROUP BY f.name " +
            "HAVING count(*) > 1 ")
    List<String> findDuplicates(int source);
}
