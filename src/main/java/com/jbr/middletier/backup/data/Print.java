package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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
