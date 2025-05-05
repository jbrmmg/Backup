package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name="print")
public class Print {
    @EmbeddedId
    private PrintId id;

    @Column(name="border")
    private Boolean border;

    @Column(name="black_white")
    private Boolean blackWhite;
}
