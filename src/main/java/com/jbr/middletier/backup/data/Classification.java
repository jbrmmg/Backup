package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import jakarta.persistence.*;

@SuppressWarnings("unused")
@Entity
@Table(name="classification")
public class Classification {
    @Setter
    @Getter
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="order_val")
    @NotNull
    private Integer order;

    @Setter
    @Getter
    @Column(name="regex")
    private String regex;

    @Column(name="action")
    private String action;

    @Setter
    @Getter
    @Column(name="icon")
    private String icon;

    @Column(name="useMD5")
    @NotNull
    private Boolean useMD5;

    @Setter
    @Getter
    @Column(name="is_image")
    private Boolean isImage;

    @Setter
    @Getter
    @Column(name="is_video")
    private Boolean isVideo;

    @Setter
    @Column(name="check_meta_data")
    private Boolean checkMetaData;

    public Classification() {
        this.order = 0;
        this.useMD5 = false;
    }

    public ClassificationActionType getAction() { return ClassificationActionType.getClassificationActionType(this.action); }

    public void setAction(ClassificationActionType action) { this.action = action.getTypeName(); }

    @NotNull public Boolean getUseMD5() { return this.useMD5; }

    public void setUseMD5(@NotNull Boolean useMD5) { this.useMD5 = useMD5; }

    public boolean getCheckMetaData() {
        return checkMetaData != null && checkMetaData;
    }

    public @NotNull Integer getOrder() { return this.order; }

    public void setOrder(@NotNull Integer order) { this.order = order; }

    @Override
    public String toString() {
        return id + "-" + regex;
    }
}
