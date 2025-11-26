package com.farhan.theatrecraft.api;

import com.farhan.theatrecraft.core.service.FrequencyCountService;
import com.farhan.theatrecraft.core.service.FrequencyCountService.FrequencyCountResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/frequencycount")
public class FrequencyCountController {

    @Autowired
    private FrequencyCountService frequencyCountService;

    /**
     * Search for a keyword and get frequency statistics across all URLs.
     * 
     * @param keyword The keyword to search for
     * @return FrequencyCountResult with statistics and found URLs or spell check suggestions
     */
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestParam("keyword") String keyword) {
        try {
            // Validate input
            if (keyword == null || keyword.trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Keyword cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Trim and normalize keyword
            String normalizedKeyword = keyword.trim().toLowerCase();
            
            // Minimum keyword length check (at least 3 characters)
            if (normalizedKeyword.length() < 3) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Keyword must be at least 3 characters long");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Execute search
            FrequencyCountResult result = frequencyCountService.search(normalizedKeyword);
            
            // Return result
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("Error in FrequencyCountController.search(): " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An error occurred while processing your request");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get autocomplete suggestions for a given prefix.
     * 
     * @param prefix The prefix to get suggestions for (minimum 3 characters)
     * @return List of up to 5 word suggestions
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<?> autocomplete(@RequestParam("prefix") String prefix) {
        try {
            // Validate input
            if (prefix == null || prefix.trim().isEmpty()) {
                return ResponseEntity.ok(List.of()); // Return empty list for empty prefix
            }

            // Trim and normalize prefix
            String normalizedPrefix = prefix.trim().toLowerCase();
            
            // Return empty list if prefix is too short (need at least 3 chars)
            if (normalizedPrefix.length() < 3) {
                return ResponseEntity.ok(List.of());
            }

            // Get autocomplete suggestions
            List<String> suggestions = frequencyCountService.autocomplete(normalizedPrefix);
            
            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            System.err.println("Error in FrequencyCountController.autocomplete(): " + e.getMessage());
            e.printStackTrace();
            
            // Return empty list on error
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Health check endpoint for frequency count service.
     * 
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Frequency Count");
        return ResponseEntity.ok(response);
    }
}
