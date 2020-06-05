package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="action_confirm")
public class ActionConfirm {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name="path")
    @NotNull
    private String path;

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

    public String getPath() { return this.path; }

    public void setPath(String path) { this.path = path; }

    public String getAction() { return this.action; }

    public void setAction(String action) { this.action = action; }

    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }

    public void setParameterRequired(boolean parameterRequired) { this.parameterRequired = parameterRequired; }

    public String getParameter() { return this.parameter; }

    public boolean confirmed() { return this.confirmed; }
}
