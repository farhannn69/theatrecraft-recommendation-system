package com.farhan.theatrecraft.core.search;

public class EditDistance {
    
    /**
     * Calculate Levenshtein distance (edit distance) between two strings
     */
    public static int calculate(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return Integer.MAX_VALUE;
        }
        
        String s1 = str1.toLowerCase().trim();
        String s2 = str2.toLowerCase().trim();
        
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        // Initialize base cases
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        // Fill the dp table
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j - 1], // Replace
                        Math.min(
                            dp[i - 1][j],  // Delete
                            dp[i][j - 1]   // Insert
                        )
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
