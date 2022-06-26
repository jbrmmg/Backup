package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="action_confirm")
public class ActionConfirm {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name="fileId")
    @ManyToOne(optional = false)
    private FileInfo fileInfo;

    @Column(name="action")
    @NotNull
    private String action;

    @Column(name="confirmed")
    @NotNull
    private Boolean confirmed;

    @Column(name="parameter_required")
    private Boolean parameterRequired;

    @Column(name="parameter")
    private String parameter;

    @Column(name="flags")
    private String flags;

    public Integer getId() { return this.id; }

    public FileInfo getPath() { return this.fileInfo; }

    public void setFileInfo(FileInfo file) { this.fileInfo = file; }

    public ActionConfirmType getAction() { return ActionConfirmType.getActionConfirmType(this.action); }

    public void setAction(ActionConfirmType action) { this.action = action.getTypeName(); }

    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }

    public Boolean getParameterRequired() { return this.parameterRequired; }

    public void setParameterRequired(boolean parameterRequired) { this.parameterRequired = parameterRequired; }

    public String getParameter() { return this.parameter; }

    public void setParameter(String parameter) { this.parameter = parameter; }

    public boolean confirmed() { return this.confirmed; }

    public void setFlags(String flags) { this.flags = flags; }

    public String getFlags() { return this.flags; }

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
