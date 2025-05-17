package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name="action_confirm")
public class ActionConfirm {
    @Getter
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Setter
    @JoinColumn(name="fileId")
    @ManyToOne(optional = false)
    private FileInfo fileInfo;

    @Column(name="action")
    @NotNull
    private String action;

    @Setter
    @Column(name="confirmed")
    @NotNull
    private Boolean confirmed;

    @Getter
    @Column(name="parameter_required")
    private Boolean parameterRequired;

    @Setter
    @Getter
    @Column(name="parameter")
    private String parameter;

    @Getter
    @Setter
    @Column(name="flags")
    private String flags;

    public FileInfo getPath() { return this.fileInfo; }

    public ActionConfirmType getAction() { return ActionConfirmType.getActionConfirmType(this.action); }

    public void setAction(ActionConfirmType action) { this.action = action.getTypeName(); }

    public void setParameterRequired(boolean parameterRequired) { this.parameterRequired = parameterRequired; }

    public boolean confirmed() { return this.confirmed; }

    @Override
    public String toString() {
        return "Action Confirmed [" +
                this.id +
                "] (" +
                (this.fileInfo == null ? "No File" : this.fileInfo.getIdAndType().toString()) +
                "," +
                this.action +
                "," +
                this.confirmed.toString() +
                "," +
                (this.parameterRequired == null ? "" : this.parameterRequired.toString()) +
                "," +
                this.parameter +
                "," +
                this.flags +
                ")";
    }
}
