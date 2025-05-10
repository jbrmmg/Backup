package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Setter
@Getter
@Entity
@Table(name="print_size")
public class PrintSize {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    private String name;

    @Column(name="width_in")
    private Double width;

    @Column(name="height_in")
    private Double height;

    @Column(name="retro")
    private Boolean retro;

    @Column(name="panoramic")
    private Boolean panoramic;

}
