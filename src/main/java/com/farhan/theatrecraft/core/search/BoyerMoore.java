package com.farhan.theatrecraft.core.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Boyer-Moore string searching algorithm implementation
 * Used for finding pattern occurrences in text
 * Case-insensitive search with occurrence counting
 */
public class BoyerMoore {
    
    /**
     * Search for all occurrences of pattern in text using Boyer-Moore algorithm
     * Returns list of starting positions where pattern is found
     * Case-insensitive search
     * 
     * @param text The text to search in
     * @param pattern The pattern to search for
     * @return List of starting indices where pattern occurs
     */
    public static List<Integer> searchAll(String text, String pattern) {
        List<Integer> occurrences = new ArrayList<>();
        
        if (text == null || pattern == null || pattern.isEmpty() || text.length() < pattern.length()) {
            return occurrences;
        }
        
        // Normalize to lowercase for case-insensitive search
        String normalizedText = text.toLowerCase();
        String normalizedPattern = pattern.toLowerCase();
        
        int m = normalizedPattern.length();
        int n = normalizedText.length();
        
        // Build bad character table
        Map<Character, Integer> badChar = buildBadCharTable(normalizedPattern);
        
        int shift = 0; // Shift of the pattern relative to text
        
        while (shift <= (n - m)) {
            int j = m - 1;
            
            // Keep reducing j while characters match
            while (j >= 0 && normalizedPattern.charAt(j) == normalizedText.charAt(shift + j)) {
                j--;
            }
            
            // If pattern is found (j became -1)
            if (j < 0) {
                occurrences.add(shift);
                
                // Shift pattern to align with next potential match
                shift += (shift + m < n) ? m - badChar.getOrDefault(normalizedText.charAt(shift + m), -1) : 1;
            } else {
                // Shift pattern based on bad character heuristic
                char mismatchChar = normalizedText.charAt(shift + j);
                int badCharShift = j - badChar.getOrDefault(mismatchChar, -1);
                shift += Math.max(1, badCharShift);
            }
        }
        
        return occurrences;
    }
    
    /**
     * Count total occurrences of pattern in text
     * Case-insensitive counting
     * 
     * @param text The text to search in
     * @param pattern The pattern to count
     * @return Number of occurrences found
     */
    public static int countOccurrences(String text, String pattern) {
        return searchAll(text, pattern).size();
    }
    
    /**
     * Count occurrences of whole words only (with word boundaries)
     * A whole word is surrounded by non-alphanumeric characters or start/end of text
     * Case-insensitive counting
     * 
     * @param text The text to search in
     * @param word The word to count
     * @return Number of whole word occurrences found
     */
    public static int countWholeWordOccurrences(String text, String word) {
        List<Integer> allOccurrences = searchAll(text, word);
        int wholeWordCount = 0;
        
        if (text == null || word == null || word.isEmpty()) {
            return 0;
        }
        
        String normalizedText = text.toLowerCase();
        String normalizedWord = word.toLowerCase();
        int wordLength = normalizedWord.length();
        
        for (int pos : allOccurrences) {
            // Check if this is a whole word occurrence
            boolean isWholeWord = true;
            
            // Check character before (must be non-alphanumeric or start of text)
            if (pos > 0) {
                char beforeChar = normalizedText.charAt(pos - 1);
                if (Character.isLetterOrDigit(beforeChar)) {
                    isWholeWord = false;
                }
            }
            
            // Check character after (must be non-alphanumeric or end of text)
            if (isWholeWord && pos + wordLength < normalizedText.length()) {
                char afterChar = normalizedText.charAt(pos + wordLength);
                if (Character.isLetterOrDigit(afterChar)) {
                    isWholeWord = false;
                }
            }
            
            if (isWholeWord) {
                wholeWordCount++;
            }
        }
        
        return wholeWordCount;
    }
    
    /**
     * Check if pattern exists in text (at least one occurrence)
     * Case-insensitive check
     * 
     * @param text The text to search in
     * @param pattern The pattern to find
     * @return true if pattern exists, false otherwise
     */
    public static boolean contains(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty() || text.length() < pattern.length()) {
            return false;
        }
        
        String normalizedText = text.toLowerCase();
        String normalizedPattern = pattern.toLowerCase();
        
        int m = normalizedPattern.length();
        int n = normalizedText.length();
        
        Map<Character, Integer> badChar = buildBadCharTable(normalizedPattern);
        int shift = 0;
        
        while (shift <= (n - m)) {
            int j = m - 1;
            
            while (j >= 0 && normalizedPattern.charAt(j) == normalizedText.charAt(shift + j)) {
                j--;
            }
            
            if (j < 0) {
                return true; // Found at least one occurrence
            }
            
            char mismatchChar = normalizedText.charAt(shift + j);
            int badCharShift = j - badChar.getOrDefault(mismatchChar, -1);
            shift += Math.max(1, badCharShift);
        }
        
        return false;
    }
    
    /**
     * Build bad character table for Boyer-Moore algorithm
     * Maps each character to its rightmost position in the pattern
     * 
     * @param pattern The pattern to build table for
     * @return Map of character to rightmost index
     */
    private static Map<Character, Integer> buildBadCharTable(String pattern) {
        Map<Character, Integer> badChar = new HashMap<>();
        int m = pattern.length();
        
        // Store rightmost occurrence of each character
        // Characters not in pattern get default value -1
        for (int i = 0; i < m; i++) {
            badChar.put(pattern.charAt(i), i);
        }
        
        return badChar;
    }
}
