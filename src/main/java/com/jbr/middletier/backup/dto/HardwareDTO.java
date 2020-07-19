package com.jbr.middletier.backup.dto;

@SuppressWarnings("unused")
public class HardwareDTO {
    private String macAddress;
    private String reservedIP;
    private String ip;
    private String name;

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getReservedIP() {
        return reservedIP;
    }

    public void setReservedIP(String reservedIP) {
        this.reservedIP = reservedIP;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
