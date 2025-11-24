package com.farhan.theatrecraft.core.service;

import com.farhan.theatrecraft.core.model.Product;
import com.farhan.theatrecraft.core.search.*;
import com.farhan.theatrecraft.core.storage.ProductCsvRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Frequency Count Service
 * Calculates global statistics for keyword occurrences across all product URLs
 * Shows total occurrences, total URLs searched, and URLs where keyword was found
 */
@Service
public class FrequencyCountService {
    
    @Autowired
    private ProductCsvRepository productRepository;
    
    @Autowired
    private URLContentCache urlCache;
    
    private final WordTrie wordTrie;
    private final InvertedIndex invertedIndex;
    
    public FrequencyCountService() {
        this.wordTrie = new WordTrie();
        this.invertedIndex = new InvertedIndex();
    }
    
    /**
     * Search for keyword and return frequency statistics
     * 
     * @param keyword The keyword to search for
     * @return FrequencyCountResult with statistics and URL list
     */
    public FrequencyCountResult search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new FrequencyCountResult(false, 0, 0, 0, Collections.emptyList(),
                Collections.emptyList(), "Please enter a search keyword");
        }
        
        String normalizedKeyword = keyword.trim().toLowerCase();
        
        // Build index for this keyword
        FrequencyStats stats = buildIndexAndCalculateStats(normalizedKeyword);
        
        if (stats.foundOnURLCount == 0) {
            // No results - try spell checker
            List<String> suggestions = getSpellCheckSuggestions(normalizedKeyword, 3);
            return new FrequencyCountResult(false, 0, stats.totalURLsSearched, 0,
                Collections.emptyList(), suggestions, "No results found. Did you mean:");
        }
        
        // Get all URLs where keyword was found
        List<String> foundURLs = new ArrayList<>(invertedIndex.getURLs(normalizedKeyword).keySet());
        
        return new FrequencyCountResult(true, stats.totalOccurrences,
            stats.totalURLsSearched, stats.foundOnURLCount, foundURLs,
            Collections.emptyList(), "Search completed successfully");
    }
    
    /**
     * Build inverted index and calculate frequency statistics
     * 
     * @param keyword The keyword to search for (normalized)
     * @return FrequencyStats object with calculated statistics
     */
    private FrequencyStats buildIndexAndCalculateStats(String keyword) {
        List<Product> products = productRepository.loadAll();
        
        System.out.println("FrequencyCountService: Analyzing keyword '" + keyword + "' across " + products.size() + " products");
        
        // Clear previous index
        invertedIndex.clear();
        
        int totalOccurrences = 0;
        int totalURLsSearched = 0;
        int foundOnURLCount = 0;
        
        for (Product product : products) {
            String url = product.getProductUrl();
            
            if (url == null || url.isEmpty() || url.equals("https://example.com/product")) {
                continue; // Skip invalid URLs
            }
            
            totalURLsSearched++;
            
            try {
                // Fetch URL content from cache or web
                String content = urlCache.getContent(url);
                
                if (content == null) {
                    continue; // Skip failed fetches
                }
                
                // Extract words for Trie (autocomplete/spell-check)
                extractAndIndexWords(content);
                
                // Count whole word occurrences using Boyer-Moore
                int occurrenceCount = BoyerMoore.countWholeWordOccurrences(content, keyword);
                
                if (occurrenceCount > 0) {
                    // Add to inverted index
                    invertedIndex.addEntry(keyword, url, occurrenceCount);
                    totalOccurrences += occurrenceCount;
                    foundOnURLCount++;
                }
                
            } catch (Exception e) {
                System.err.println("FrequencyCountService: Error processing " + url + " - " + e.getMessage());
            }
        }
        
        System.out.println("FrequencyCountService: Analysis complete. Total occurrences: " + totalOccurrences +
            ", Found on " + foundOnURLCount + " URLs out of " + totalURLsSearched + " searched");
        
        return new FrequencyStats(totalOccurrences, totalURLsSearched, foundOnURLCount);
    }
    
    /**
     * Extract words from text and add to Trie
     * 
     * @param content Text content to extract words from
     */
    private void extractAndIndexWords(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        
        // Split content into words
        String[] words = content.toLowerCase().split("\\W+");
        
        for (String word : words) {
            if (word.length() >= 3) {
                wordTrie.insert(word);
            }
        }
    }
    
    /**
     * Autocomplete - get up to 5 word suggestions
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
     * 
     * @param query The search query
     * @param maxSuggestions Maximum number of suggestions
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
     * Clear cache (for testing/debugging)
     */
    public void clearCache() {
        urlCache.clearCache();
        invertedIndex.clear();
        wordTrie.clear();
    }
    
    /**
     * Helper class for frequency statistics
     */
    private static class FrequencyStats {
        final int totalOccurrences;
        final int totalURLsSearched;
        final int foundOnURLCount;
        
        FrequencyStats(int totalOccurrences, int totalURLsSearched, int foundOnURLCount) {
            this.totalOccurrences = totalOccurrences;
            this.totalURLsSearched = totalURLsSearched;
            this.foundOnURLCount = foundOnURLCount;
        }
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
     * Result class for Frequency Count searches
     */
    public static class FrequencyCountResult {
        private final boolean success;
        private final int totalOccurrences;
        private final int totalURLsSearched;
        private final int foundOnURLCount;
        private final List<String> foundURLs;
        private final List<String> suggestions;
        private final String message;
        
        public FrequencyCountResult(boolean success, int totalOccurrences, int totalURLsSearched,
                                   int foundOnURLCount, List<String> foundURLs,
                                   List<String> suggestions, String message) {
            this.success = success;
            this.totalOccurrences = totalOccurrences;
            this.totalURLsSearched = totalURLsSearched;
            this.foundOnURLCount = foundOnURLCount;
            this.foundURLs = foundURLs;
            this.suggestions = suggestions;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        // Getter for stats object (for JSON serialization)
        public StatsData getStats() {
            return new StatsData(totalOccurrences, totalURLsSearched, foundOnURLCount, foundURLs);
        }
        
        public int getTotalOccurrences() {
            return totalOccurrences;
        }
        
        public int getTotalURLsSearched() {
            return totalURLsSearched;
        }
        
        public int getFoundOnURLCount() {
            return foundOnURLCount;
        }
        
        public List<String> getFoundURLs() {
            return foundURLs;
        }
        
        public List<String> getSuggestions() {
            return suggestions;
        }
        
        public String getMessage() {
            return message;
        }
        
        /**
         * Inner class for stats data structure matching JavaScript expectations
         */
        public static class StatsData {
            private final int totalOccurrences;
            private final int totalURLsSearched;
            private final int foundOnURLCount;
            private final List<String> foundURLs;
            
            public StatsData(int totalOccurrences, int totalURLsSearched, 
                           int foundOnURLCount, List<String> foundURLs) {
                this.totalOccurrences = totalOccurrences;
                this.totalURLsSearched = totalURLsSearched;
                this.foundOnURLCount = foundOnURLCount;
                this.foundURLs = foundURLs;
            }
            
            public int getTotalOccurrences() {
                return totalOccurrences;
            }
            
            public int getTotalURLsSearched() {
                return totalURLsSearched;
            }
            
            public int getFoundOnURLCount() {
                return foundOnURLCount;
            }
            
            public List<String> getFoundURLs() {
                return foundURLs;
            }
        }
    }
}
