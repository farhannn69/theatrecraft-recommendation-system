package com.farhan.theatrecraft.core.search;

import com.farhan.theatrecraft.core.model.Product;
import java.util.List;

public class AutocompleteResult {
    private List<Product> suggestions;

    public AutocompleteResult() {
    }

    public AutocompleteResult(List<Product> suggestions) {
        this.suggestions = suggestions;
    }

    public List<Product> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<Product> suggestions) {
        this.suggestions = suggestions;
    }
}
