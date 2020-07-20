package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.HardwareDTO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="hardware")
public class Hardware {
    @Id
    @Column(name="macaddress")
    private String macAddress;

    @Column(name="reservedip")
    private String reservedIP;

    @Column(name="ip")
    private String ip;

    @Column(name="name")
    private String name;

    public Hardware() {
        macAddress = "";
        reservedIP = "";
    }

    public Hardware(HardwareDTO source) {
        setMacAddress(source.getMacAddress());
        setReservedIP(source.getReservedIP());
        update(source);
    }

    public void update(HardwareDTO source) {
        setReservedIP(source.getReservedIP());
        setIP(source.getIp());
        setName(source.getName());
    }

    public String getMacAddress() { return this.macAddress; }

    public String getReservedIP() { return this.reservedIP; }

    public String getIP() { return this.ip; }

    public String getName() { return this.name; }

    public void setMacAddress(@NotNull String macAddress) { this.macAddress = macAddress; }

    public void setReservedIP(@NotNull String reservedIP) { this.reservedIP = reservedIP; }

    public void setIP(String ip) { this.ip = ip; }

    public void setName(String name) { this.name = name; }
}
