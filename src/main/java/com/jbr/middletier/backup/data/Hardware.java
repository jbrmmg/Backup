package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="hardware")
public class Hardware {
    @Id
    @Column(name="macaddress")
    @NotNull
    private String macAddress;

    @Column(name="reservedip")
    @NotNull
    private String reservedIP;

    @Column(name="ip")
    private String ip;

    @Column(name="name")
    private String name;

    public String getMacAddress() { return this.macAddress; }

    public String getReservedIP() { return this.reservedIP; }

    public String getIP() { return this.ip; }

    public String getName() { return this.name; }

    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public void setReservedIP(String reservedIP) { this.reservedIP = reservedIP; }

    public void setIP(String ip) { this.ip = ip; }

    public void setName(String name) { this.name = name; }
}
