package com.farhan.theatrecraft.core.search;

import com.farhan.theatrecraft.core.model.Product;
import java.util.List;

public class SearchResult {
    private boolean exactMatch;
    private Product matchedProduct;
    private List<String> suggestions; // For "Did you mean"
    private String message;

    public SearchResult() {
    }

    public SearchResult(boolean exactMatch, Product matchedProduct, List<String> suggestions, String message) {
        this.exactMatch = exactMatch;
        this.matchedProduct = matchedProduct;
        this.suggestions = suggestions;
        this.message = message;
    }

    // Getters and Setters
    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public Product getMatchedProduct() {
        return matchedProduct;
    }

    public void setMatchedProduct(Product matchedProduct) {
        this.matchedProduct = matchedProduct;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
