package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.MD5;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ImportFileDTO {
    private String filename;
    private String status;
    private LocalDateTime date;
    private Long size;
    private String md5;

    public static class SimilarFile {
        private LocalDateTime date;
        private Long size;
        private String md5;

        public SimilarFile(FileInfo file) {
            this.date = file.getDate();
            this.size = file.getSize();
            this.md5 = file.getMD5().toString();
        }

        public LocalDateTime getDate() {
            return date;
        }

        public void setDate(LocalDateTime date) {
            this.date = date;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(MD5 md5) {
            this.md5 = md5.toString();
        }
    }

    List<SimilarFile> similarFileList;

    public ImportFileDTO() {
        similarFileList = new ArrayList<>();
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(ImportFileStatusType status) {
        this.status = status.getTypeName();
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getMD5() {
        return md5;
    }

    public void setMD5(MD5 md5) {
        this.md5 = md5.toString();
    }

    public void addSimilarFile(FileInfo file) {
        similarFileList.add(new SimilarFile(file));
    }
}
