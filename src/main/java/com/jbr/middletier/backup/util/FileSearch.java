package com.jbr.middletier.backup.util;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FileSearch {
    public enum SearchType { NAME, MD5, DATETIME, SIZE }

    private final SearchType searchType;
    private final String search;
    private LocalDateTime dateTime;

    private boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isDate(String date) {
        // If the string is a date then set the date time and return true.
        DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try {
            this.dateTime = LocalDateTime.parse(date, dtf1);
            return true;
        } catch(DateTimeParseException ignored) {
        }

        try {
            this.dateTime = LocalDateTime.parse(date, dtf2);
            return true;
        } catch(DateTimeParseException ignored) {
        }

        return false;
    }

    public FileSearch(String search) {
        this.search = search;
        this.dateTime = null;

        // Possible types of search; name, md5, date or size.
        boolean letter = false;
        boolean number = false;
        boolean space = false;
        boolean dash = false;
        boolean other = false;
        boolean colon = false;

        for(char c : search.toCharArray()){
            switch(c){
                case '-':
                    dash = true;
                    break;
                case ' ':
                    space = true;
                    break;
                case ':':
                    colon = true;
                    break;
                default:
                    number = isNumber(c);

                    if(!number){
                        letter = isLetter(c);

                        if(!letter){
                            other = true;
                        }
                    }
            }
        }

        // Is this an MD5?
        if(search.length() == 32 && !space && !other && !dash){
            this.searchType = SearchType.MD5;
            return;
        }

        // If just number, then its size.
        if(number && !letter && !dash && !other && !space){
            this.searchType = SearchType.SIZE;
            return;
        }

        // If this looks like a date.
        if(number && dash && isDate(search)) {
            this.searchType = SearchType.DATETIME;
            return;
        }

        this.searchType = SearchType.NAME;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public String getSearch() {
        return search;
    }

    public LocalDateTime getDateTime() {
        return this.dateTime;
    }
}
