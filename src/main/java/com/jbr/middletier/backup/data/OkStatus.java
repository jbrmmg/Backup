package com.jbr.middletier.backup.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unused", "WeakerAccess"})
public class OkStatus {
    private String status;

    @Contract(pure = true)
    public OkStatus() {
        status = "OK";
    }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public static OkStatus getOkStatus() {
        return new OkStatus();
    }
}
