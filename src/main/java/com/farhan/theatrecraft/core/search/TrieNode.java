package com.farhan.theatrecraft.core.search;

import com.farhan.theatrecraft.core.model.Product;
import java.util.HashMap;
import java.util.Map;

public class TrieNode {
    private Map<Character, TrieNode> children;
    private boolean isEndOfWord;
    private Product product; // Store the product at the end of a word

    public TrieNode() {
        this.children = new HashMap<>();
        this.isEndOfWord = false;
        this.product = null;
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public boolean isEndOfWord() {
        return isEndOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        isEndOfWord = endOfWord;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
