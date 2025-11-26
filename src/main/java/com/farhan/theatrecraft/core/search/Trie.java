package com.farhan.theatrecraft.core.search;

import com.farhan.theatrecraft.core.model.Product;
import java.util.ArrayList;
import java.util.List;

public class Trie {
    private final TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    /**
     * Insert a product name into the trie (normalized to lowercase)
     */
    public void insert(String word, Product product) {
        if (word == null || word.isEmpty()) {
            return;
        }
        
        String normalized = word.toLowerCase().trim();
        TrieNode current = root;
        
        for (char ch : normalized.toCharArray()) {
            current.getChildren().putIfAbsent(ch, new TrieNode());
            current = current.getChildren().get(ch);
        }
        
        current.setEndOfWord(true);
        current.setProduct(product);
    }

    /**
     * Search for products with the given prefix
     * Returns up to maxResults products
     * Only returns results if prefix length >= minLength
     */
    public List<Product> searchByPrefix(String prefix, int minLength, int maxResults) {
        List<Product> results = new ArrayList<>();
        
        if (prefix == null || prefix.length() < minLength) {
            return results; // Returns empty list []
        }
        
        String normalized = prefix.toLowerCase().trim();
        TrieNode current = root;
        
        // Navigate to the prefix node
        for (char ch : normalized.toCharArray()) {
            TrieNode next = current.getChildren().get(ch);
            if (next == null) {
                return results; // Prefix not found
            }
            current = next;
        }
        
        // Collect all products under this prefix
        collectProducts(current, results, maxResults);
        return results; // Returns result
    }

    /**
     * Recursively collect products from a node
     */
    private void collectProducts(TrieNode node, List<Product> results, int maxResults) {
        if (results.size() >= maxResults) {
            return;
        }
        
        if (node.isEndOfWord() && node.getProduct() != null) {
            results.add(node.getProduct());
        }
        
        for (TrieNode child : node.getChildren().values()) {
            collectProducts(child, results, maxResults);
            if (results.size() >= maxResults) {
                break;
            }
        }
    }

    /**
     * Get all products in the trie (for spell checking)
     */
    public List<Product> getAllProducts() {
        List<Product> results = new ArrayList<>();
        collectProducts(root, results, Integer.MAX_VALUE);
        return results;
    }
}
