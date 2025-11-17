package com.farhan.theatrecraft.api;

import com.farhan.theatrecraft.core.model.Brand;
import com.farhan.theatrecraft.core.model.Product;
import com.farhan.theatrecraft.core.service.CrawlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CrawlService crawlService;

    public ProductController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    // GET /api/products
    // Optional ?brand=BOSE etc.
    @GetMapping
    public ResponseEntity<List<Product>> getProducts(@RequestParam(required = false) String brand) {
        if (brand != null && !brand.isBlank()) {
            try {
                Brand brandEnum = Brand.valueOf(brand.toUpperCase());
                return ResponseEntity.ok(crawlService.getProductsByBrand(brandEnum));
            } catch (IllegalArgumentException e) {
                // invalid brand
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(crawlService.getAllProducts());
    }

    // GET /api/products/latest?limit=12
    @GetMapping("/latest")
    public ResponseEntity<List<Product>> getLatestProducts(@RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(crawlService.getLatestProducts(limit));
    }

    // POST /api/products/crawl/{brand}
    // e.g. POST /api/products/crawl/BOSE
    @PostMapping("/crawl/{brand}")
    public ResponseEntity<List<Product>> crawlBrand(@PathVariable String brand) {
        try {
            Brand brandEnum = Brand.valueOf(brand.toUpperCase());
            List<Product> crawledProducts = crawlService.crawlBrand(brandEnum);
            return ResponseEntity.ok(crawledProducts);
        } catch (IllegalArgumentException e) {
            // invalid brand enum
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
