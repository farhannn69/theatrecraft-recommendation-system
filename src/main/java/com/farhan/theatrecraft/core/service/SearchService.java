package com.farhan.theatrecraft.core.service;

import com.farhan.theatrecraft.core.model.Product;
import com.farhan.theatrecraft.core.search.*;
import com.farhan.theatrecraft.core.storage.ProductCsvRepository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    
    private final Trie trie;
    private final Map<String, Integer> searchFrequency; // HashMap for frequency tracking
    private final ProductCsvRepository productRepository;
    private final String frequencyFilePath = "search_frequency.csv";
    private List<Product> allProducts;

    public SearchService(ProductCsvRepository productRepository) {
        this.productRepository = productRepository;
        this.trie = new Trie();
        this.searchFrequency = new HashMap<>();
        this.allProducts = new ArrayList<>();
        
        // Initialize on startup
        loadProducts();
        loadSearchFrequency();
    }

    /**
     * Load all products and build the Trie
     */
    private void loadProducts() {
        allProducts = productRepository.loadAll();
        
        // Build Trie with product names
        for (Product product : allProducts) {
            if (product.getModelName() != null && !product.getModelName().isEmpty()) {
                trie.insert(product.getModelName(), product);
            }
        }
        
        System.out.println("SearchService: Loaded " + allProducts.size() + " products into Trie");
    }

    /**
     * Reload products (call this after crawling new data)
     */
    public void reloadProducts() {
        allProducts.clear();
        loadProducts();
    }

    /**
     * Autocomplete - returns up to 5 suggestions when input length >= 3
     */
    public AutocompleteResult autocomplete(String prefix) {
        List<Product> suggestions = trie.searchByPrefix(prefix, 3, 5);
        return new AutocompleteResult(suggestions);
    }

    /**
     * Main search method using KMP algorithm
     * Returns exact match or suggestions
     */
    public SearchResult search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchResult(false, null, Collections.emptyList(), "Please enter a search query");
        }

        String normalizedQuery = query.trim();

        // Step 1: Try exact match using KMP
        for (Product product : allProducts) {
            if (product.getModelName() != null && KMPSearch.exactMatch(product.getModelName(), normalizedQuery)) {
                // Exact match found - increment frequency
                incrementSearchFrequency(product.getModelName());
                return new SearchResult(true, product, Collections.emptyList(), "Exact match found");
            }
        }

        // Step 2: No exact match - try spell checker using Edit Distance
        List<String> suggestions = getSpellCheckSuggestions(normalizedQuery, 3);
        
        if (!suggestions.isEmpty()) {
            return new SearchResult(false, null, suggestions, "No exact match. Did you mean:");
        }

        // Step 3: No match at all
        return new SearchResult(false, null, Collections.emptyList(), "No products found");
    }

    /**
     * Search by product name (from autocomplete or "Did you mean" click)
     * This increments frequency when user selects a suggestion
     */
    public Product searchByProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return null;
        }

        // Find exact match
        for (Product product : allProducts) {
            if (product.getModelName() != null && product.getModelName().equalsIgnoreCase(productName.trim())) {
                // Increment frequency for successful selection
                incrementSearchFrequency(product.getModelName());
                return product;
            }
        }

        return null;
    }

    /**
     * Get spell check suggestions using Trie and Edit Distance
     * Returns top N closest product names with minimum edit distance
     * NO CUTOFF - always returns the closest matches
     */
    private List<String> getSpellCheckSuggestions(String query, int maxSuggestions) {
        List<ProductDistance> distances = new ArrayList<>();
        
        // Get all products from Trie
        List<Product> allTrieProducts = trie.getAllProducts();
        
        // Calculate edit distance for each product
        for (Product product : allTrieProducts) {
            if (product.getModelName() != null && !product.getModelName().isEmpty()) {
                int distance = EditDistance.calculate(query.toLowerCase(), product.getModelName().toLowerCase());
                distances.add(new ProductDistance(product.getModelName(), distance));
            }
        }
        
        // Sort by distance (closest first) and return top N
        // NO CUTOFF - always return the closest matches regardless of distance
        List<String> suggestions = distances.stream()
                .sorted(Comparator.comparingInt(ProductDistance::getDistance))
                .limit(maxSuggestions)
                .map(ProductDistance::getProductName)
                .collect(Collectors.toList());
        
        System.out.println("SearchService: Found " + suggestions.size() + " spell check suggestions for '" + query + "'");
        if (!suggestions.isEmpty()) {
            System.out.println("  Top suggestion: " + suggestions.get(0) + " (distance: " + 
                distances.stream()
                    .filter(pd -> pd.getProductName().equals(suggestions.get(0)))
                    .findFirst()
                    .map(ProductDistance::getDistance)
                    .orElse(-1) + ")");
        }
        
        return suggestions;
    }

    /**
     * Increment search frequency for a product name
     */
    private void incrementSearchFrequency(String productName) {
        String normalized = productName.trim();
        searchFrequency.put(normalized, searchFrequency.getOrDefault(normalized, 0) + 1);
        saveSearchFrequency();
        System.out.println("SearchService: Incremented frequency for '" + normalized + "' to " + searchFrequency.get(normalized));
    }

    /**
     * Get top N most searched products
     */
    public List<SearchFrequency> getTopSearches(int limit) {
        return searchFrequency.entrySet().stream()
                .map(entry -> new SearchFrequency(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount())) // Sort descending
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Load search frequency from CSV file
     */
    private void loadSearchFrequency() {
        File file = new File(frequencyFilePath);
        if (!file.exists()) {
            System.out.println("SearchService: No search frequency file found. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String productName = parts[0].trim();
                    int count = Integer.parseInt(parts[1].trim());
                    searchFrequency.put(productName, count);
                }
            }
            
            System.out.println("SearchService: Loaded " + searchFrequency.size() + " search frequencies");
        } catch (Exception e) {
            System.err.println("SearchService: Error loading search frequency: " + e.getMessage());
        }
    }

    /**
     * Save search frequency to CSV file
     */
    private void saveSearchFrequency() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(frequencyFilePath))) {
            writer.println("productName,count");
            
            for (Map.Entry<String, Integer> entry : searchFrequency.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
        } catch (Exception e) {
            System.err.println("SearchService: Error saving search frequency: " + e.getMessage());
        }
    }

    /**
     * Helper class for storing product name with its edit distance
     */
    private static class ProductDistance {
        private final String productName;
        private final int distance;

        public ProductDistance(String productName, int distance) {
            this.productName = productName;
            this.distance = distance;
        }

        public String getProductName() {
            return productName;
        }

        public int getDistance() {
            return distance;
        }
    }
}
