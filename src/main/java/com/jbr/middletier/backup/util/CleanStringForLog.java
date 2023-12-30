package com.jbr.middletier.backup.util;

public class CleanStringForLog {
    public static String cleanString(String input) {
        StringBuilder result = new StringBuilder();

        for(int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if(((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z') || ((c >= '0') && (c <= '9')))) {
                result.append(c);
            } else {
                result.append(' ');
            }
        }

        return result.toString();
    }
}
