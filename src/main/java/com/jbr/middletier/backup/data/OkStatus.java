package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public class OkStatus {
    private String status;

    @Contract(pure = true)
    public OkStatus() {
        status = "OK";
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public static OkStatus getOkStatus() {
        return new OkStatus();
    }
}
