package com.farhan.theatrecraft.api;

import com.farhan.theatrecraft.core.model.Product;
import com.farhan.theatrecraft.core.search.*;
import com.farhan.theatrecraft.core.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * Main search endpoint
     * POST /api/search?query=search+term
     */
    @PostMapping
    public ResponseEntity<SearchResult> search(@RequestParam String query) {
        SearchResult result = searchService.search(query);
        return ResponseEntity.ok(result);
    }

    /**
     * Search by exact product name (from autocomplete or "Did you mean" selection)
     * GET /api/search/product?name=exact+product+name
     */
    @GetMapping("/product")
    public ResponseEntity<Product> searchByProductName(@RequestParam String name) {
        Product product = searchService.searchByProductName(name);
        
        if (product != null) {
            return ResponseEntity.ok(product);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Autocomplete endpoint
     * GET /api/search/autocomplete?prefix=text
     * Returns up to 5 suggestions when prefix length >= 3
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<AutocompleteResult> autocomplete(@RequestParam String prefix) {
        if (prefix == null || prefix.length() < 3) {
            return ResponseEntity.ok(new AutocompleteResult(List.of()));
        }
        
        AutocompleteResult result = searchService.autocomplete(prefix);
        return ResponseEntity.ok(result);
    }

    /**
     * Get top N most searched products
     * GET /api/search/frequencies?limit=10
     */
    @GetMapping("/frequencies")
    public ResponseEntity<List<SearchFrequency>> getTopSearches(@RequestParam(defaultValue = "10") int limit) {
        List<SearchFrequency> topSearches = searchService.getTopSearches(limit);
        return ResponseEntity.ok(topSearches);
    }

    /**
     * Reload products (useful after crawling new data)
     * POST /api/search/reload
     */
    @PostMapping("/reload")
    public ResponseEntity<String> reloadProducts() {
        searchService.reloadProducts();
        return ResponseEntity.ok("Products reloaded successfully");
    }
}
