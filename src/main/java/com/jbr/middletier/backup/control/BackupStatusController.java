package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.BackupJobRun;
import com.jbr.middletier.backup.dataaccess.BackupJobRunRepository;
import com.jbr.middletier.backup.dto.BackupJobRunDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1//backup/status")
@Tag(name = "Backup Status", description = "Backup job run status and history")
public class BackupStatusController {

    private final BackupJobRunRepository backupJobRunRepository;

    @Autowired
    public BackupStatusController(BackupJobRunRepository backupJobRunRepository) {
        this.backupJobRunRepository = backupJobRunRepository;
    }

    private BackupJobRunDTO toDTO(BackupJobRun run) {
        BackupJobRunDTO dto = new BackupJobRunDTO();
        dto.setBackupId(run.getBackupId());
        dto.setRunDate(run.getRunDate());
        dto.setStartedAt(run.getStartedAt());
        dto.setFinishedAt(run.getFinishedAt());
        dto.setStatus(run.getStatus());
        dto.setMessage(run.getMessage());
        return dto;
    }

    @GetMapping("/jobs")
    public List<BackupJobRunDTO> allJobRuns() {
        return backupJobRunRepository.findAllByOrderByRunDateDescStartedAtAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/jobs/{backupId}")
    public List<BackupJobRunDTO> jobHistory(@PathVariable String backupId,
                                            @RequestParam(defaultValue = "10") int limit) {
        return backupJobRunRepository
                .findByBackupIdOrderByRunDateDesc(backupId, PageRequest.of(0, limit))
                .stream()
                .map(this::toDTO)
                .toList();
    }
}
