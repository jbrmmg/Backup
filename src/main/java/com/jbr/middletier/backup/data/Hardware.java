package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
@Table(name="hardware")
public class Hardware {
    @Getter
    @Id
    @Column(name="macaddress")
    private String macAddress;

    @Getter
    @Column(name="reservedip")
    private String reservedIP;

    @Column(name="ip")
    private String ip;

    @Setter
    @Getter
    @Column(name="name")
    private String name;

    public Hardware() {
        macAddress = "";
        reservedIP = "";
    }

    public String getIP() { return this.ip; }

    public void setMacAddress(@NotNull String macAddress) { this.macAddress = macAddress; }

    public void setReservedIP(@NotNull String reservedIP) { this.reservedIP = reservedIP; }

    public void setIP(String ip) { this.ip = ip; }
}
