package com.jbr.middletier.backup.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FileSearch {
    public enum SearchType { NAME, MD5, DATETIME, SIZE }

    private static class StringAnalysis {
        private boolean letter = false;
        private boolean number = false;
        private boolean space = false;
        private boolean dash = false;
        private boolean other = false;

        private boolean isNumber(char c) {
            return c >= '0' && c <= '9';
        }

        private boolean isLetter(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        StringAnalysis(String search) {
            for(char c : search.toCharArray()){
                switch(c){
                    case '-':
                        dash = true;
                        break;
                    case ' ':
                        space = true;
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
        }

        public boolean hasLetter() { return this.letter; }

        public boolean hasNumber() { return this.number; }

        public boolean hasNoSpace() { return !this.space; }

        public boolean hasDash() { return this.dash; }

        public boolean hasNoOther() { return !this.other; }
    }

    private final SearchType searchType;
    private final String search;
    private LocalDateTime dateTime;

    private boolean isDate(String date) {
        // If the string is a date then set the date time and return true.
        DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try {
            this.dateTime = LocalDateTime.parse(date, dtf1);
            return true;
        } catch(DateTimeParseException ignored) {
            // Ignore the exception, try below with different format.
        }

        try {
            this.dateTime = LocalDateTime.parse(date, dtf2);
            return true;
        } catch(DateTimeParseException ignored) {
            // Ignore the exception, if not valid then the string is not a date.
        }

        return false;
    }

    public FileSearch(String search) {
        this.search = search;
        this.dateTime = null;

        // Possible types of search; name, md5, date or size.
        StringAnalysis stringAnalysis = new StringAnalysis(search);

        // Is this an MD5?
        if(search.length() == 32 && stringAnalysis.hasNoSpace() && stringAnalysis.hasNoOther() && !stringAnalysis.hasDash()){
            this.searchType = SearchType.MD5;
            return;
        }

        // If just number, then its size.
        if(stringAnalysis.hasNumber() && !stringAnalysis.hasLetter() && !stringAnalysis.hasDash() && stringAnalysis.hasNoOther() && stringAnalysis.hasNoSpace()){
            this.searchType = SearchType.SIZE;
            return;
        }

        // If this looks like a date.
        if(stringAnalysis.hasNumber() && stringAnalysis.hasDash() && isDate(search)) {
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
