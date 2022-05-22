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
//    List<DirectoryInfo> findBySourceAndParent(Source source, DirectoryInfo parent);

//    List<DirectoryInfo> findBySource(Source source);

//    @Query("SELECT new com.jbr.middletier.backup.data.HierarchyResponse ( " +
//           "d.source.id, " +
//           "LENGTH(d.name) - LENGTH(REPLACE(d.name,'/','')), " +
//           "d.name, " +
//           "d.id ) " +
//           "FROM DirectoryInfo AS d " +
//           "WHERE d.source.id = ?1 " +
//           "AND LENGTH(d.name) - LENGTH(REPLACE(d.name,'/','')) = ?2 " +
//           "AND name like ?3 " +
//           "ORDER BY d.name ")
//    List<HierarchyResponse> findAtLevel(int source, int level, String path);
}
