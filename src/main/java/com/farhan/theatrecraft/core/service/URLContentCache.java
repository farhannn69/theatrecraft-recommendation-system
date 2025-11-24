package com.farhan.theatrecraft.core.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache for URL content to avoid redundant HTTP requests
 * Stores fetched HTML content and parsed text from product URLs
 */
@Component
public class URLContentCache {
    
    // Cache structure: URL -> CachedContent
    private final Map<String, CachedContent> cache;
    
    // Jsoup connection timeout (10 seconds)
    private static final int TIMEOUT_MS = 10000;
    
    public URLContentCache() {
        this.cache = new HashMap<>();
    }
    
    /**
     * Get text content from URL (from cache or fetch if not cached)
     * 
     * @param url The URL to fetch content from
     * @return Extracted text content, or null if fetch fails
     */
    public String getContent(String url) {
        // Check cache first
        if (cache.containsKey(url)) {
            CachedContent cached = cache.get(url);
            System.out.println("URLContentCache: Cache hit for " + url);
            return cached.textContent;
        }
        
        // Not in cache - fetch from URL
        System.out.println("URLContentCache: Fetching content from " + url);
        String content = fetchContent(url);
        
        // Cache the result (even if null, to avoid re-fetching failed URLs)
        cache.put(url, new CachedContent(url, content, System.currentTimeMillis()));
        
        return content;
    }
    
    /**
     * Fetch and parse text content from URL using Jsoup
     * 
     * @param url The URL to fetch
     * @return Extracted text content, or null if fetch fails
     */
    private String fetchContent(String url) {
        try {
            // Fetch HTML document with Jsoup
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();
            
            // Extract all text from HTML (removes tags)
            String text = doc.body().text();
            
            System.out.println("URLContentCache: Successfully fetched " + text.length() + " characters from " + url);
            return text;
            
        } catch (Exception e) {
            System.err.println("URLContentCache: Failed to fetch " + url + " - " + e.getMessage());
            return null; // Return null on failure
        }
    }
    
    /**
     * Check if URL content is cached
     * 
     * @param url The URL to check
     * @return true if cached, false otherwise
     */
    public boolean isCached(String url) {
        return cache.containsKey(url);
    }
    
    /**
     * Clear entire cache
     */
    public void clearCache() {
        cache.clear();
        System.out.println("URLContentCache: Cache cleared");
    }
    
    /**
     * Remove specific URL from cache
     * 
     * @param url The URL to remove
     */
    public void invalidate(String url) {
        cache.remove(url);
        System.out.println("URLContentCache: Invalidated cache for " + url);
    }
    
    /**
     * Get cache size
     * 
     * @return Number of cached URLs
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Inner class to store cached content with metadata
     */
    private static class CachedContent {
        final String url;
        final String textContent; // Can be null if fetch failed
        final long timestamp;
        
        CachedContent(String url, String textContent, long timestamp) {
            this.url = url;
            this.textContent = textContent;
            this.timestamp = timestamp;
        }
    }
}
