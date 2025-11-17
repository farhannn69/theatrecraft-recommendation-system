package com.farhan.theatrecraft.core.crawler;

import com.farhan.theatrecraft.core.model.Brand;
import com.farhan.theatrecraft.core.model.Product;

import java.util.List;

public interface ProductCrawler {
    Brand getBrand();
    List<Product> crawlProducts();
}
