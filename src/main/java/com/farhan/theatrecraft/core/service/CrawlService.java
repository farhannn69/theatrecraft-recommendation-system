package com.farhan.theatrecraft.core.service;

import com.farhan.theatrecraft.core.crawler.ProductCrawler;
import com.farhan.theatrecraft.core.model.Brand;
import com.farhan.theatrecraft.core.model.Product;
import com.farhan.theatrecraft.core.storage.ProductCsvRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrawlService {

    private final ProductCsvRepository csvRepository;
    private final List<ProductCrawler> crawlers;
    private List<Product> products;

    public CrawlService(ProductCsvRepository csvRepository, List<ProductCrawler> crawlers) {
        this.csvRepository = csvRepository;
        this.crawlers = crawlers;
        this.products = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        // Load products from CSV on startup
        this.products = csvRepository.loadAll();
        System.out.println("Loaded " + products.size() + " products from CSV");
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products);
    }

    public List<Product> getProductsByBrand(Brand brand) {
        return products.stream()
                .filter(p -> p.getBrand() == brand)
                .collect(Collectors.toList());
    }

    public List<Product> getLatestProducts(int limit) {
        return products.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Crawl only the selected brand, refresh that brand inside CSV,
     * and keep the in-memory list updated.
     */
    public List<Product> crawlBrand(Brand brand) {
    // Find crawler for this brand
    ProductCrawler crawler = crawlers.stream()
            .filter(c -> c.getBrand() == brand)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No crawler found for brand: " + brand));

    // Crawl fresh data for this brand
    List<Product> crawledProducts = crawler.crawlProducts();

    // Remove old products for this brand from the in-memory list
    products.removeIf(p -> p.getBrand() == brand);

    // Add new products for this brand
    products.addAll(crawledProducts);

    // Save the full product list (all brands) back to CSV
    csvRepository.saveAll(products);

    System.out.println("Crawled " + crawledProducts.size() + " products for brand: " + brand);

    return crawledProducts;
}

}
