package com.farhan.theatrecraft.core.search;

public class KMPSearch {
    
    /**
     * Knuth-Morris-Pratt algorithm for pattern matching
     * Returns true if pattern is found in text (case-insensitive)
     */
    public static boolean search(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty()) {
            return false;
        }
        
        String normalizedText = text.toLowerCase();
        String normalizedPattern = pattern.toLowerCase();
        
        int[] lps = computeLPSArray(normalizedPattern);
        
        int i = 0; // index for text
        int j = 0; // index for pattern
        
        while (i < normalizedText.length()) {
            if (normalizedPattern.charAt(j) == normalizedText.charAt(i)) {
                i++;
                j++;
            }
            
            if (j == normalizedPattern.length()) {
                return true; // Pattern found
            } else if (i < normalizedText.length() && normalizedPattern.charAt(j) != normalizedText.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        
        return false; // Pattern not found
    }
    
    /**
     * Check for exact match (case-insensitive)
     */
    public static boolean exactMatch(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        return text.trim().equalsIgnoreCase(pattern.trim());
    }
    
    /**
     * Compute the Longest Proper Prefix which is also Suffix array
     */
    private static int[] computeLPSArray(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        int i = 1;
        
        lps[0] = 0; // lps[0] is always 0
        
        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        
        return lps;
    }
}
