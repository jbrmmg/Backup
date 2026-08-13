package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.LabelRepository;
import com.jbr.middletier.backup.dataaccess.MetaDataRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.InvalidSearchRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SearchManager {

    private final FileRepository fileRepository;
    private final MetaDataRepository metaDataRepository;
    private final LabelRepository labelRepository;
    private final SourceRepository sourceRepository;
    private final FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    public SearchManager(FileRepository fileRepository,
                         MetaDataRepository metaDataRepository,
                         LabelRepository labelRepository,
                         SourceRepository sourceRepository,
                         FileSystemObjectManager fileSystemObjectManager) {
        this.fileRepository = fileRepository;
        this.metaDataRepository = metaDataRepository;
        this.labelRepository = labelRepository;
        this.sourceRepository = sourceRepository;
        this.fileSystemObjectManager = fileSystemObjectManager;
    }

    private void validate(SearchRequestDTO request) throws InvalidSearchRequestException {
        boolean hasCriteria = request.getFilename() != null
                || request.getDateFrom() != null
                || request.getDateTo() != null
                || request.getSizeMin() != null
                || request.getSizeMax() != null
                || request.getExpiryFrom() != null
                || request.getExpiryTo() != null
                || (request.getLabels() != null && !request.getLabels().isEmpty())
                || request.getLocation() != null;

        if (!hasCriteria) {
            throw new InvalidSearchRequestException("At least one search criterion is required");
        }
        if (request.getPageSize() > 100) {
            throw new InvalidSearchRequestException("pageSize must not exceed 100");
        }
        if (request.getDateFrom() != null && request.getDateTo() != null
                && request.getDateFrom().isAfter(request.getDateTo())) {
            throw new InvalidSearchRequestException("dateFrom must not be after dateTo");
        }
        if (request.getSizeMin() != null && request.getSizeMax() != null
                && request.getSizeMin() > request.getSizeMax()) {
            throw new InvalidSearchRequestException("sizeMin must not exceed sizeMax");
        }
        if (request.getLocation() != null
                && request.getLocation().getSouth() >= request.getLocation().getNorth()) {
            throw new InvalidSearchRequestException("location.south must be less than location.north");
        }
    }

    private Set<Integer> primarySourceFileIds() {
        Set<Integer> fileIds = new HashSet<>();
        for (Source source : sourceRepository.findAllByOrderByIdAsc()) {
            if (!source.getPrimary()) continue;
            List<DirectoryInfo> dirs = new ArrayList<>();
            List<FileInfo> files = new ArrayList<>();
            fileSystemObjectManager.loadByParent(source.getIdAndType().getId(), dirs, files);
            files.stream().map(f -> f.getIdAndType().getId()).forEach(fileIds::add);
        }
        return fileIds;
    }

    private Map<String, Integer> buildLabelNameToIdMap() {
        Map<String, Integer> result = new HashMap<>();
        for (Label label : labelRepository.findAll()) {
            result.put(label.getName(), label.getId());
        }
        return result;
    }

    private Specification<FileInfo> buildSpec(SearchRequestDTO request) {
        List<Specification<FileInfo>> specs = new ArrayList<>();

        Set<Integer> primaryFileIds = primarySourceFileIds();
        if (primaryFileIds.isEmpty()) {
            specs.add((root, query, cb) -> cb.disjunction());
        } else {
            specs.add((root, query, cb) -> root.get("id").in(primaryFileIds));
        }

        if (request.getFilename() != null) {
            String pattern = request.getFilename().replace("*", "%").replace("?", "_").toLowerCase();
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }

        if (request.getDateFrom() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), request.getDateFrom()));
        }
        if (request.getDateTo() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), request.getDateTo()));
        }

        if (request.getSizeMin() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("size"), request.getSizeMin()));
        }
        if (request.getSizeMax() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("size"), request.getSizeMax()));
        }

        if (request.getExpiryFrom() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expiry"), request.getExpiryFrom()));
        }
        if (request.getExpiryTo() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expiry"), request.getExpiryTo()));
        }

        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            Map<String, Integer> labelNameToId = buildLabelNameToIdMap();
            for (String labelName : request.getLabels()) {
                Integer labelId = labelNameToId.get(labelName);
                if (labelId == null) {
                    specs.add((root, query, cb) -> cb.disjunction());
                    break;
                }
                specs.add((root, query, cb) -> {
                    Subquery<Integer> sub = query.subquery(Integer.class);
                    Root<FileLabel> fl = sub.from(FileLabel.class);
                    sub.select(fl.get("id").get("fileId"))
                            .where(cb.and(
                                    cb.equal(fl.get("id").get("fileId"), root.get("id")),
                                    cb.equal(fl.get("id").get("labelId"), labelId)
                            ));
                    return cb.exists(sub);
                });
            }
        }

        if (request.getLocation() != null) {
            SearchLocationDTO loc = request.getLocation();
            specs.add((root, query, cb) -> {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<MetaData> md = sub.from(MetaData.class);
                sub.select(md.get("id"))
                        .where(cb.and(
                                cb.equal(md.get("id"), root.get("id")),
                                cb.between(md.get("latitude"), loc.getSouth(), loc.getNorth()),
                                cb.between(md.get("longitude"), loc.getWest(), loc.getEast())
                        ));
                return cb.exists(sub);
            });
        }

        return specs.stream().reduce(Specification.where(null), Specification::and);
    }

    private SearchResultDTO toResult(FileInfo file) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setId(file.getIdAndType().getId());
        dto.setName(file.getName());

        File associatedFile = fileSystemObjectManager.getFile(file);
        dto.setFullFilename(associatedFile.getPath());
        dto.setPath(associatedFile.getParent() != null ? associatedFile.getParent() : "");
        dto.setLocationName(associatedFile.getParent() != null ? associatedFile.getParent() : "");

        dto.setDate(file.getDate());
        dto.setSize(file.getSize() != null ? file.getSize() : 0L);
        dto.setExpiry(file.getExpiry());

        if (file.getClassification() != null) {
            dto.setImage(Boolean.TRUE.equals(file.getClassification().getIsImage()));
            dto.setVideo(Boolean.TRUE.equals(file.getClassification().getIsVideo()));
            dto.setIcon(file.getClassification().getIcon());
        } else {
            dto.setImage(false);
            dto.setVideo(false);
            dto.setIcon("fa-file-o");
        }

        file.getMd5().ifPresent(md5 -> dto.setMd5(md5.toString()));

        Optional<MetaData> metaData = metaDataRepository.findById(file.getIdAndType().getId());
        metaData.ifPresent(md -> {
            dto.setLatitude(md.getLatitude());
            dto.setLongitude(md.getLongitude());
        });

        return dto;
    }

    public SearchResponseDTO search(SearchRequestDTO request) throws InvalidSearchRequestException {
        validate(request);

        int pageSize = request.getPageSize() <= 0 ? 20 : request.getPageSize();
        Pageable pageable = PageRequest.of(request.getPage(), pageSize, Sort.by(Sort.Direction.DESC, "date"));
        Page<FileInfo> page = fileRepository.findAll(buildSpec(request), pageable);

        SearchResponseDTO response = new SearchResponseDTO();
        response.setPage(request.getPage());
        response.setPageSize(pageSize);
        response.setTotalCount(page.getTotalElements());
        response.setResults(page.getContent().stream().map(this::toResult).toList());

        return response;
    }
}
