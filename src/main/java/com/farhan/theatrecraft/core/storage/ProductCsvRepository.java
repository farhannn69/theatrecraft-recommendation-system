package com.farhan.theatrecraft.core.storage;

import com.farhan.theatrecraft.core.model.Brand;
import com.farhan.theatrecraft.core.model.Product;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductCsvRepository {

    private static final String CSV_FILE = "data/products.csv";

    // Order matters: this must match saveAll() and loadAll()
    private static final String HEADER =
            "id;brand;sourceSite;modelName;systemType;category;price;rating;" +
            "imageUrl;productUrl;channel;audioFormat;wifiFormat;bluetoothVersion;weightKg;power";

    public List<Product> loadAll() {
        List<Product> products = new ArrayList<>();

        File file = new File(CSV_FILE);
        if (!file.exists()) {
            return products;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Skip header if present
                if (isFirstLine && line.startsWith("id;")) {
                    isFirstLine = false;
                    continue;
                }
                isFirstLine = false;

                String[] parts = line.split(";", -1);
                // We expect exactly 16 columns
                if (parts.length < 16) {
                    continue;
                }

                Product p = new Product();
                int i = 0;

                p.setId(parts[i++]);
                try {
                    p.setBrand(Brand.valueOf(parts[i++]));
                } catch (Exception e) {
                    p.setBrand(null);
                }
                p.setSourceSite(parts[i++]);
                p.setModelName(parts[i++]);
                p.setSystemType(parts[i++]);
                p.setCategory(parts[i++]);

                // price
                String priceStr = parts[i++];
                try {
                    p.setPrice(priceStr.isEmpty() ? null : Double.parseDouble(priceStr));
                } catch (NumberFormatException e) {
                    p.setPrice(null);
                }

                // rating
                String ratingStr = parts[i++];
                try {
                    p.setRating(ratingStr.isEmpty() ? null : Double.parseDouble(ratingStr));
                } catch (NumberFormatException e) {
                    p.setRating(null);
                }

                p.setImageUrl(parts[i++]);
                p.setProductUrl(parts[i++]);
                p.setChannel(parts[i++]);
                p.setAudioFormat(parts[i++]);
                p.setWifiFormat(parts[i++]);
                p.setBluetoothVersion(parts[i++]);
                p.setWeightKg(parts[i++]);
                p.setPower(parts[i++]);

                products.add(p);
            }

        } catch (IOException e) {
            System.err.println("ProductCsvRepository.loadAll: " + e.getMessage());
        }

        return products;
    }

    public void saveAll(List<Product> products) {
        try {
            // Ensure directory exists
            Files.createDirectories(Paths.get("data"));

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE))) {
                writer.write(HEADER);
                writer.newLine();

                for (Product p : products) {
                    StringBuilder sb = new StringBuilder();

                    sb.append(nullSafe(p.getId())).append(";");
                    sb.append(p.getBrand() != null ? p.getBrand().name() : "").append(";");
                    sb.append(nullSafe(p.getSourceSite())).append(";");
                    sb.append(nullSafe(p.getModelName())).append(";");
                    sb.append(nullSafe(p.getSystemType())).append(";");
                    sb.append(nullSafe(p.getCategory())).append(";");

                    sb.append(p.getPrice() != null ? p.getPrice() : "").append(";");
                    sb.append(p.getRating() != null ? p.getRating() : "").append(";");

                    sb.append(nullSafe(p.getImageUrl())).append(";");
                    sb.append(nullSafe(p.getProductUrl())).append(";");
                    sb.append(nullSafe(p.getChannel())).append(";");
                    sb.append(nullSafe(p.getAudioFormat())).append(";");
                    sb.append(nullSafe(p.getWifiFormat())).append(";");
                    sb.append(nullSafe(p.getBluetoothVersion())).append(";");
                    sb.append(nullSafe(p.getWeightKg())).append(";");
                    sb.append(nullSafe(p.getPower()));

                    writer.write(sb.toString());
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            System.err.println("ProductCsvRepository.saveAll: " + e.getMessage());
        }
    }

    private String nullSafe(String s) {
        return s == null ? "" : s.replace(";", ",");
    }
}
