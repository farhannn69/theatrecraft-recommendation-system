package com.farhan.theatrecraft.core.search;

import java.util.*;

/**
 * Inverted Index data structure for efficient word-to-URL mapping
 * Stores: word -> List of (URL, occurrence count) pairs
 * Used for Page Ranking and Frequency Count features
 */
public class InvertedIndex {
    
    // Index structure: word -> Map of (URL -> occurrence count)
    private final Map<String, Map<String, Integer>> index;
    
    public InvertedIndex() {
        this.index = new HashMap<>();
    }
    
    /**
     * Add or update an entry in the inverted index
     * 
     * @param word The word to index (should be normalized/lowercase)
     * @param url The URL where word was found
     * @param occurrenceCount Number of times word appears in that URL
     */
    public void addEntry(String word, String url, int occurrenceCount) {
        if (word == null || url == null || occurrenceCount <= 0) {
            return;
        }
        
        // Get or create URL map for this word
        Map<String, Integer> urlMap = index.computeIfAbsent(word, k -> new HashMap<>());
        
        // Add or update occurrence count for this URL
        urlMap.put(url, occurrenceCount);
    }
    
    /**
     * Get all URLs where a word appears, with occurrence counts
     * 
     * @param word The word to look up
     * @return Map of URL -> occurrence count, or empty map if word not found
     */
    public Map<String, Integer> getURLs(String word) {
        if (word == null) {
            return new HashMap<>();
        }
        
        return index.getOrDefault(word.toLowerCase(), new HashMap<>());
    }
    
    /**
     * Get top N URLs ranked by occurrence count for a given word
     * Uses sorting (could be optimized with heap for very large datasets)
     * 
     * @param word The word to look up
     * @param topN Number of top results to return
     * @return List of URLOccurrence objects sorted by count descending
     */
    public List<URLOccurrence> getTopURLs(String word, int topN) {
        Map<String, Integer> urlCounts = getURLs(word);
        
        if (urlCounts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Convert to list of URLOccurrence objects
        List<URLOccurrence> occurrences = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : urlCounts.entrySet()) {
            occurrences.add(new URLOccurrence(entry.getKey(), entry.getValue()));
        }
        
        // Sort by occurrence count descending using Collections.sort
        occurrences.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        
        // Return top N results
        return occurrences.subList(0, Math.min(topN, occurrences.size()));
    }
    
    /**
     * Get top N URLs using Min Heap for efficient sorting
     * More efficient than full sort for large datasets
     * 
     * @param word The word to look up
     * @param topN Number of top results to return
     * @return List of URLOccurrence objects sorted by count descending
     */
    public List<URLOccurrence> getTopURLsWithHeap(String word, int topN) {
        Map<String, Integer> urlCounts = getURLs(word);
        
        if (urlCounts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Use PriorityQueue as Min Heap (smallest element at top)
        PriorityQueue<URLOccurrence> minHeap = new PriorityQueue<>(
            Comparator.comparingInt(URLOccurrence::getCount)
        );
        
        // Process each URL
        for (Map.Entry<String, Integer> entry : urlCounts.entrySet()) {
            URLOccurrence occurrence = new URLOccurrence(entry.getKey(), entry.getValue());
            
            if (minHeap.size() < topN) {
                // Heap not full yet, add element
                minHeap.offer(occurrence);
            } else if (occurrence.getCount() > minHeap.peek().getCount()) {
                // Current element is larger than smallest in heap
                minHeap.poll(); // Remove smallest
                minHeap.offer(occurrence); // Add current
            }
        }
        
        // Extract elements from heap and reverse order (largest first)
        List<URLOccurrence> result = new ArrayList<>(minHeap);
        result.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        
        return result;
    }
    
    /**
     * Calculate total occurrences of a word across all URLs
     * 
     * @param word The word to count
     * @return Total occurrence count
     */
    public int getTotalOccurrences(String word) {
        Map<String, Integer> urlCounts = getURLs(word);
        return urlCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get number of URLs where word appears
     * 
     * @param word The word to check
     * @return Count of URLs containing the word
     */
    public int getURLCount(String word) {
        return getURLs(word).size();
    }
    
    /**
     * Check if a word exists in the index
     * 
     * @param word The word to check
     * @return true if word is indexed, false otherwise
     */
    public boolean contains(String word) {
        return word != null && index.containsKey(word.toLowerCase());
    }
    
    /**
     * Clear the entire index
     */
    public void clear() {
        index.clear();
    }
    
    /**
     * Get all indexed words
     * 
     * @return Set of all words in the index
     */
    public Set<String> getAllWords() {
        return new HashSet<>(index.keySet());
    }
    
    /**
     * Inner class representing a URL with its occurrence count
     * Used for ranking results
     */
    public static class URLOccurrence {
        private final String url;
        private final int count;
        
        public URLOccurrence(String url, int count) {
            this.url = url;
            this.count = count;
        }
        
        public String getUrl() {
            return url;
        }
        
        public int getCount() {
            return count;
        }
        
        @Override
        public String toString() {
            return url + " (" + count + " occurrences)";
        }
    }
}
