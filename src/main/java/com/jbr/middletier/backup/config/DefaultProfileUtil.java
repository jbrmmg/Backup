package com.jbr.middletier.backup.config;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

public class DefaultProfileUtil {
    private static final String SPRING_PROFILE_DEFAULT = "spring.profiles.default";

    private DefaultProfileUtil() {
    }

    public static void addDefaultProfile(SpringApplication app) {
        Map<String,Object> defProperties = new HashMap<>();

        defProperties.put(SPRING_PROFILE_DEFAULT,"dbg");
        app.setDefaultProperties(defProperties);
    }
}
