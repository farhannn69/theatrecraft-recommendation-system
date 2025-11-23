package com.farhan.theatrecraft.core.search;

public class SearchFrequency {
    private String searchTerm;
    private int count;

    public SearchFrequency() {
    }

    public SearchFrequency(String searchTerm, int count) {
        this.searchTerm = searchTerm;
        this.count = count;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
