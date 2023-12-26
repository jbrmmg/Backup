package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="print")
public class Print {
    @EmbeddedId
    private PrintId id;

    @Column(name="border")
    private Boolean border;

    @Column(name="black_white")
    private Boolean blackWhite;

    public PrintId getId() {
        return id;
    }

    public void setId(PrintId id) {
        this.id = id;
    }

    public Boolean getBorder() {
        return border;
    }

    public void setBorder(Boolean border) {
        this.border = border;
    }

    public Boolean getBlackWhite() {
        return blackWhite;
    }

    public void setBlackWhite(Boolean blackWhite) {
        this.blackWhite = blackWhite;
    }
}
