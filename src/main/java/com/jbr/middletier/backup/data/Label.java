package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Setter
@Getter
@Entity
@Table(name="label")
public class Label {
    @Id
    @Column(name="id")
    private Integer id;

    @Column(name="name")
    private String name;
}
