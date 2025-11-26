package com.farhan.theatrecraft.api;

import com.farhan.theatrecraft.core.service.PageRankingService;
import com.farhan.theatrecraft.core.service.PageRankingService.PageRankingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pageranking")
public class PageRankingController {

    @Autowired
    private PageRankingService pageRankingService;

    /**
     * Search for a keyword and get top 10 URLs ranked by occurrence count.
     * 
     * @param keyword The keyword to search for
     * @return PageRankingResult with top URLs or spell check suggestions
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
            
            // Minimum keyword length check (at least 2 characters)
            if (normalizedKeyword.length() < 3) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Keyword must be at least 3 characters long");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Execute search
            PageRankingResult result = pageRankingService.search(normalizedKeyword);
            
            // Return result
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("Error in PageRankingController.search(): " + e.getMessage());
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
            List<String> suggestions = pageRankingService.autocomplete(normalizedPrefix);
            
            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            System.err.println("Error in PageRankingController.autocomplete(): " + e.getMessage());
            e.printStackTrace();
            
            // Return empty list on error
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Health check endpoint for page ranking service.
     * 
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Page Ranking");
        return ResponseEntity.ok(response);
    }
}
