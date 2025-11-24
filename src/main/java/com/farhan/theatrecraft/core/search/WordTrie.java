package com.farhan.theatrecraft.core.search;

import java.util.*;

/**
 * Trie data structure for words extracted from URLs
 * Separate from product name Trie
 * Used for autocomplete and spell-check dictionary
 */
public class WordTrie {
    
    private final WordTrieNode root;
    
    public WordTrie() {
        this.root = new WordTrieNode();
    }
    
    /**
     * Insert a word into the Trie
     * Normalizes to lowercase
     * 
     * @param word The word to insert
     */
    public void insert(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        
        String normalized = word.toLowerCase().trim();
        WordTrieNode current = root;
        
        for (char c : normalized.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new WordTrieNode());
        }
        
        current.isEndOfWord = true;
        current.word = normalized;
    }
    
    /**
     * Insert multiple words into the Trie
     * 
     * @param words Collection of words to insert
     */
    public void insertAll(Collection<String> words) {
        for (String word : words) {
            insert(word);
        }
    }
    
    /**
     * Search for words by prefix (autocomplete)
     * Returns up to maxResults words matching the prefix
     * 
     * @param prefix The prefix to search for
     * @param minLength Minimum prefix length required (typically 3)
     * @param maxResults Maximum number of results to return (typically 5)
     * @return List of matching words
     */
    public List<String> searchByPrefix(String prefix, int minLength, int maxResults) {
        List<String> results = new ArrayList<>();
        
        if (prefix == null || prefix.length() < minLength) {
            return results;
        }
        
        String normalized = prefix.toLowerCase().trim();
        WordTrieNode current = root;
        
        // Navigate to prefix node
        for (char c : normalized.toCharArray()) {
            WordTrieNode next = current.children.get(c);
            if (next == null) {
                return results; // Prefix not found
            }
            current = next;
        }
        
        // Collect all words with this prefix
        collectWords(current, results, maxResults);
        
        return results;
    }
    
    /**
     * Recursively collect words from a Trie node (DFS)
     * 
     * @param node Current node
     * @param results List to store results
     * @param maxResults Maximum number of results to collect
     */
    private void collectWords(WordTrieNode node, List<String> results, int maxResults) {
        if (results.size() >= maxResults) {
            return; // Already have enough results
        }
        
        if (node.isEndOfWord) {
            results.add(node.word);
        }
        
        // Explore children in alphabetical order
        for (WordTrieNode child : node.children.values()) {
            collectWords(child, results, maxResults);
            if (results.size() >= maxResults) {
                break;
            }
        }
    }
    
    /**
     * Get all words in the Trie
     * Used for spell-check dictionary
     * 
     * @return List of all words
     */
    public List<String> getAllWords() {
        List<String> allWords = new ArrayList<>();
        collectAllWords(root, allWords);
        return allWords;
    }
    
    /**
     * Recursively collect all words from the Trie
     * 
     * @param node Current node
     * @param allWords List to store all words
     */
    private void collectAllWords(WordTrieNode node, List<String> allWords) {
        if (node.isEndOfWord) {
            allWords.add(node.word);
        }
        
        for (WordTrieNode child : node.children.values()) {
            collectAllWords(child, allWords);
        }
    }
    
    /**
     * Check if a word exists in the Trie
     * 
     * @param word The word to check
     * @return true if word exists, false otherwise
     */
    public boolean contains(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        String normalized = word.toLowerCase().trim();
        WordTrieNode current = root;
        
        for (char c : normalized.toCharArray()) {
            WordTrieNode next = current.children.get(c);
            if (next == null) {
                return false;
            }
            current = next;
        }
        
        return current.isEndOfWord;
    }
    
    /**
     * Get the number of words in the Trie
     * 
     * @return Word count
     */
    public int size() {
        return countWords(root);
    }
    
    /**
     * Recursively count words in the Trie
     * 
     * @param node Current node
     * @return Number of words
     */
    private int countWords(WordTrieNode node) {
        int count = node.isEndOfWord ? 1 : 0;
        
        for (WordTrieNode child : node.children.values()) {
            count += countWords(child);
        }
        
        return count;
    }
    
    /**
     * Clear all words from the Trie
     */
    public void clear() {
        root.children.clear();
    }
    
    /**
     * Trie node for word storage
     */
    private static class WordTrieNode {
        Map<Character, WordTrieNode> children;
        boolean isEndOfWord;
        String word; // Store the complete word at end nodes
        
        WordTrieNode() {
            this.children = new HashMap<>();
            this.isEndOfWord = false;
            this.word = null;
        }
    }
}
