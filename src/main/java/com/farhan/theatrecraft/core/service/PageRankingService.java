package com.farhan.theatrecraft.core.service;

import com.farhan.theatrecraft.core.model.Product;
import com.farhan.theatrecraft.core.search.*;
import com.farhan.theatrecraft.core.storage.ProductCsvRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Page Ranking Service
 * Searches for keywords in product URLs and ranks them by occurrence count
 * Uses Boyer-Moore algorithm, Jsoup for fetching, and Min Heap for sorting
 */
@Service
public class PageRankingService {
    
    @Autowired
    private ProductCsvRepository productRepository;
    
    @Autowired
    private URLContentCache urlCache;
    
    private final WordTrie wordTrie;
    private final InvertedIndex invertedIndex;
    
    // Pattern to split text into words (alphanumeric sequences)
    private static final Pattern WORD_PATTERN = Pattern.compile("\\w+");
    
    public PageRankingService() {
        this.wordTrie = new WordTrie();
        this.invertedIndex = new InvertedIndex();
    }
    
    /**
     * Search for a keyword across all product URLs
     * Returns top 10 URLs ranked by occurrence count
     * 
     * @param keyword The keyword to search for
     * @return PageRankingResult with top 10 URLs
     */
    public PageRankingResult search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new PageRankingResult(false, Collections.emptyList(), Collections.emptyList(), 
                "Please enter a search keyword");
        }
        
        String normalizedKeyword = keyword.trim().toLowerCase();
        
        // Build inverted index for this keyword on-demand
        buildIndexForKeyword(normalizedKeyword);
        
        // Get top 10 URLs using Min Heap
        List<InvertedIndex.URLOccurrence> topURLs = invertedIndex.getTopURLsWithHeap(normalizedKeyword, 10);
        
        if (topURLs.isEmpty()) {
            // No results - try spell checker
            List<String> suggestions = getSpellCheckSuggestions(normalizedKeyword, 3);
            return new PageRankingResult(false, Collections.emptyList(), suggestions,
                "No results found. Did you mean:");
        }
        
        return new PageRankingResult(true, topURLs, Collections.emptyList(), 
            "Found " + topURLs.size() + " results");
    }
    
    /**
     * Build inverted index for a specific keyword
     * Fetches all product URLs, searches for keyword using Boyer-Moore
     * 
     * @param keyword The keyword to index (should be normalized)
     */
    private void buildIndexForKeyword(String keyword) {
        List<Product> products = productRepository.loadAll();
        
        System.out.println("PageRankingService: Building index for keyword '" + keyword + "' across " + products.size() + " products");
        
        // Clear previous index for this keyword
        invertedIndex.clear();
        
        int successCount = 0;
        int failCount = 0;
        
        for (Product product : products) {
            String url = product.getProductUrl();
            
            if (url == null || url.isEmpty() || url.equals("https://example.com/product")) {
                continue; // Skip invalid URLs
            }
            
            try {
                // Fetch URL content from cache or web
                String content = urlCache.getContent(url);
                
                if (content == null) {
                    failCount++;
                    continue; // Skip failed fetches
                }
                
                // Extract words and build Trie (for autocomplete/spell-check)
                extractAndIndexWords(content);
                
                // Search for keyword as whole word using Boyer-Moore
                int occurrenceCount = BoyerMoore.countWholeWordOccurrences(content, keyword);
                
                if (occurrenceCount > 0) {
                    // Add to inverted index
                    invertedIndex.addEntry(keyword, url, occurrenceCount);
                    successCount++;
                }
                
            } catch (Exception e) {
                System.err.println("PageRankingService: Error processing " + url + " - " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("PageRankingService: Index built. Success: " + successCount + ", Failed: " + failCount);
    }
    
    /**
     * Extract words from text content and add to Trie
     * Used for autocomplete and spell-check dictionary
     * 
     * @param content Text content to extract words from
     */
    private void extractAndIndexWords(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        
        // Split content into words (alphanumeric sequences)
        String[] words = content.toLowerCase().split("\\W+");
        
        for (String word : words) {
            // Only index words with 3+ characters (autocomplete minimum)
            if (word.length() >= 3) {
                wordTrie.insert(word);
            }
        }
    }
    
    /**
     * Autocomplete - get up to 5 word suggestions for a prefix
     * 
     * @param prefix The prefix to search for (min 3 chars)
     * @return List of word suggestions
     */
    public List<String> autocomplete(String prefix) {
        if (prefix == null || prefix.length() < 3) {
            return Collections.emptyList();
        }
        
        return wordTrie.searchByPrefix(prefix, 3, 5);
    }
    
    /**
     * Get spell check suggestions using edit distance
     * Returns top N closest words from Trie
     * 
     * @param query The search query
     * @param maxSuggestions Maximum number of suggestions (typically 3)
     * @return List of suggested words
     */
    private List<String> getSpellCheckSuggestions(String query, int maxSuggestions) {
        List<String> allWords = wordTrie.getAllWords();
        
        if (allWords.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Calculate edit distance for each word
        List<WordDistance> distances = new ArrayList<>();
        for (String word : allWords) {
            int distance = EditDistance.calculate(query, word);
            distances.add(new WordDistance(word, distance));
        }
        
        // Sort by distance and return top N
        return distances.stream()
                .sorted(Comparator.comparingInt(WordDistance::getDistance))
                .limit(maxSuggestions)
                .map(WordDistance::getWord)
                .collect(Collectors.toList());
    }
    
    /**
     * Get Word Trie size (for debugging)
     * 
     * @return Number of words in Trie
     */
    public int getTrieSize() {
        return wordTrie.size();
    }
    
    /**
     * Clear cache (for testing/debugging)
     */
    public void clearCache() {
        urlCache.clearCache();
        invertedIndex.clear();
        wordTrie.clear();
    }
    
    /**
     * Helper class for word with edit distance
     */
    private static class WordDistance {
        private final String word;
        private final int distance;
        
        WordDistance(String word, int distance) {
            this.word = word;
            this.distance = distance;
        }
        
        String getWord() {
            return word;
        }
        
        int getDistance() {
            return distance;
        }
    }
    
    /**
     * Result class for Page Ranking searches
     */
    public static class PageRankingResult {
        private final boolean success;
        private final List<InvertedIndex.URLOccurrence> topURLs;
        private final List<String> suggestions;
        private final String message;
        
        public PageRankingResult(boolean success, List<InvertedIndex.URLOccurrence> topURLs,
                                 List<String> suggestions, String message) {
            this.success = success;
            this.topURLs = topURLs;
            this.suggestions = suggestions;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public List<InvertedIndex.URLOccurrence> getTopURLs() {
            return topURLs;
        }
        
        public List<String> getSuggestions() {
            return suggestions;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
