package com.farhan.theatrecraft.core.util;

/**
 * Utility class for normalizing and cleaning scraped product data.
 * All methods are static and the class cannot be instantiated.
 */
public final class ProductNormalizer {

    // Private constructor to prevent instantiation
    private ProductNormalizer() {
        throw new AssertionError("ProductNormalizer is a utility class and cannot be instantiated");
    }

    /**
     * Parses a price string and extracts the numeric value.
     * 
     * @param priceText Raw price text (e.g., "$999.99", "From $1,299.00")
     * @return Parsed price as Double, or null if parsing fails
     */
    public static Double parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank()) {
            return null;
        }

        try {
            // Remove currency symbols, commas, and extra whitespace
            String cleaned = priceText
                    .replaceAll("[^0-9.]", "") // Remove everything except digits and decimal point
                    .trim();

            // Handle empty string after cleaning
            if (cleaned.isEmpty()) {
                return null;
            }

            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            // Return null instead of throwing exception
            return null;
        }
    }

    /**
     * Normalizes system type based on name and extra text.
     * 
     * @param name Product name
     * @param extraText Additional product text
     * @return Normalized system type (e.g., "7.1", "5.1", "Soundbar", "Unknown")
     */
    public static String normalizeSystemType(String name, String extraText) {
        // Combine and lowercase for inspection
        String combined = ((name != null ? name : "") + " " + (extraText != null ? extraText : ""))
                .toLowerCase()
                .trim();

        // Check for specific channel configurations
        if (combined.contains("7.1")) {
            return "7.1";
        } else if (combined.contains("5.1")) {
            return "5.1";
        } else if (combined.contains("3.1")) {
            return "3.1";
        } else if (combined.contains("2.1")) {
            return "2.1";
        } else if (combined.contains("soundbar")) {
            return "Soundbar";
        }

        return "Unknown";
    }

    /**
     * Normalizes product category based on name and extra text.
     * 
     * @param name Product name
     * @param extraText Additional product text
     * @return Normalized category (HOME_THEATER, SOUNDBAR, or AUDIO_SYSTEM)
     */
    public static String normalizeCategory(String name, String extraText) {
        // Combine and lowercase for inspection
        String combined = ((name != null ? name : "") + " " + (extraText != null ? extraText : ""))
                .toLowerCase()
                .trim();

        // Check for category indicators
        if (combined.contains("home theater") || combined.contains("home theatre")) {
            return "HOME_THEATER";
        } else if (combined.contains("soundbar")) {
            return "SOUNDBAR";
        }

        return "AUDIO_SYSTEM";
    }

    /**
     * Cleans and normalizes feature text.
     * 
     * @param rawText Raw feature text
     * @return Cleaned and truncated feature text, or null if input is null/blank
     */
    public static String cleanFeatures(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        // Collapse multiple spaces and newlines into single space
        String cleaned = rawText
                .replaceAll("\\s+", " ")
                .trim();

        // Truncate to maximum length of 500 characters
        int maxLength = 500;
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength) + "...";
        }

        return cleaned;
    }
}
