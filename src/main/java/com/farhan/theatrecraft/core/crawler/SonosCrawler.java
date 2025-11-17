package com.farhan.theatrecraft.core.crawler;

import com.farhan.theatrecraft.core.model.Brand;
import com.farhan.theatrecraft.core.model.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class SonosCrawler extends BaseSeleniumCrawler implements ProductCrawler {
    
    private static final String BASE_URL = "https://www.sonos.com";
    private static final String LISTING_URL = "https://www.sonos.com/en-ca/shop/home-theater?filterTokenSetProductTypes=soundbars%2Csubwoofers";

    @Override
    public Brand getBrand() {
        return Brand.SONOS;
    }

    @Override
    public List<Product> crawlProducts() {
        WebDriver driver = null;
        try {
            System.out.println("SonosCrawler: Starting crawl of " + LISTING_URL);
            driver = createDriver();
            driver.get(LISTING_URL);
            
            // Wait for initial page load
            sleep(4000);

            // Handle popups
            PopupHandler.handlePopups(driver, Brand.SONOS);
            sleep(2000);

            // Scroll to load all products
            scrollToLoadProducts(driver);
            sleep(2000);

            // Parse listing page to extract product URLs
            Document listingDoc = Jsoup.parse(driver.getPageSource(), BASE_URL);
            Set<String> productUrls = extractProductUrls(listingDoc);

            System.out.println("SonosCrawler: Found " + productUrls.size() + " product URLs");
            
            // Print all URLs for debugging
            int urlIndex = 1;
            for (String url : productUrls) {
                System.out.println("SonosCrawler: URL " + urlIndex + ": " + url);
                urlIndex++;
            }

            List<Product> products = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (String url : productUrls) {
                System.out.println("SonosCrawler: Parsing product " + (successCount + failureCount + 1) + "/" + productUrls.size());
                Product product = parseProductPage(driver, url);
                if (product != null) {
                    products.add(product);
                    successCount++;
                    System.out.println("SonosCrawler: Successfully parsed - " + product.getModelName());
                } else {
                    failureCount++;
                    System.out.println("SonosCrawler: Failed to parse product from: " + url);
                }
            }

            System.out.println("SonosCrawler: Completed. Success: " + successCount + ", Failed: " + failureCount);
            return products;

        } catch (Exception e) {
            System.err.println("SonosCrawler: Error during crawl - " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (driver != null) {
                tearDown(driver);
            }
        }
    }

    private void scrollToLoadProducts(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Scroll down to load all products
            for (int i = 0; i < 3; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("SonosCrawler: Error scrolling - " + e.getMessage());
        }
    }

    private Set<String> extractProductUrls(Document listingDoc) {
        Set<String> urls = new LinkedHashSet<>();

        System.out.println("SonosCrawler: Extracting product URLs from filtered page...");
        
        // Collect all links from the page
        Elements allLinks = listingDoc.select("a[href^='/en-ca/shop/']");
        System.out.println("SonosCrawler: Found " + allLinks.size() + " total shop links");
        
        for (Element link : allLinks) {
            String href = link.attr("abs:href");
            if (href == null || href.isEmpty()) continue;

            // Clean URL (remove query params and hash)
            int hashIdx = href.indexOf('#');
            if (hashIdx != -1) {
                href = href.substring(0, hashIdx);
            }
            int queryIdx = href.indexOf('?');
            if (queryIdx != -1) {
                href = href.substring(0, queryIdx);
            }

            // STRICT FILTERING: Only accept specific product patterns
            // We only want: sub-*, beam-*, ray-*, arc-*
            
            // Must start with the shop URL
            if (!href.startsWith("https://www.sonos.com/en-ca/shop/")) {
                continue;
            }
            
            // Extract the part after /shop/
            String productPart = href.substring("https://www.sonos.com/en-ca/shop/".length());
            
            // Skip if empty or just slash
            if (productPart.isEmpty() || productPart.equals("/")) {
                continue;
            }
            
            // ONLY accept URLs that start with: sub-, beam-, ray-, or arc-
            // This ensures we only get the 5 actual products we need
            // Using word boundaries (hyphen) to avoid matching "architectural" as "arc"
            String lowerPart = productPart.toLowerCase();
            boolean isTargetProduct = lowerPart.startsWith("sub-") || lowerPart.equals("sub")
                                   || lowerPart.startsWith("beam-") || lowerPart.equals("beam")
                                   || lowerPart.startsWith("ray-") || lowerPart.equals("ray")
                                   || lowerPart.startsWith("arc-") || lowerPart.equals("arc");
            
            if (!isTargetProduct) {
                System.out.println("SonosCrawler: Skipped (not a target product): " + href);
                continue;
            }
            
            // Exclude mount/set products (even if they start with our target words)
            if (lowerPart.contains("mount") || lowerPart.contains("-set")) {
                System.out.println("SonosCrawler: Skipped mount/set: " + href);
                continue;
            }
            
            // Skip if it contains slashes (nested pages)
            if (productPart.contains("/")) {
                System.out.println("SonosCrawler: Skipped nested page: " + href);
                continue;
            }
            
            // Handle Sub Mini duplicate:
            // Only keep sub-mini-black (or first sub-mini variant we find)
            // Skip plain "sub-mini" if we already have a variant
            if (productPart.equals("sub-mini")) {
                // Check if we already have a sub-mini variant
                boolean hasSubMiniVariant = urls.stream().anyMatch(u -> u.contains("/sub-mini-"));
                if (hasSubMiniVariant) {
                    System.out.println("SonosCrawler: Skipped base sub-mini (already have variant): " + href);
                    continue;
                }
            } else if (productPart.startsWith("sub-mini-")) {
                // Check if we already have any sub-mini
                boolean hasAnySubMini = urls.stream().anyMatch(u -> u.contains("/sub-mini"));
                if (hasAnySubMini) {
                    System.out.println("SonosCrawler: Skipped duplicate sub-mini variant: " + href);
                    continue;
                }
            }
            
            // Valid product URL - add it
            urls.add(href);
            System.out.println("SonosCrawler: ✓ Added product: " + href);
        }

        System.out.println("SonosCrawler: Total products extracted: " + urls.size());
        return urls;
    }

    private Product parseProductPage(WebDriver driver, String productUrl) {
        try {
            System.out.println("SonosCrawler: Loading product page: " + productUrl);
            driver.get(productUrl);
            sleep(3000); // Wait for page to load

            Document doc = Jsoup.parse(driver.getPageSource(), BASE_URL);

            // Extract basic product information
            String modelName = extractModelName(doc);
            String priceText = extractPrice(doc);
            String ratingText = extractRating(doc);
            String imageUrl = extractImageUrl(doc);

            // Scroll to specs section
            scrollToSpecsSection(driver);
            sleep(2000);

            // Re-parse after scrolling
            doc = Jsoup.parse(driver.getPageSource(), BASE_URL);

            // Extract specifications
            String audioFormat = extractSpec(doc, "Home Theatre Audio Formats");
            String weightKg = extractSpec(doc, "Weight");
            String wifiFormat = extractSpec(doc, "WiFi");
            String power = extractSpec(doc, "Voltage");

            // Convert price and rating
            Double price = parsePrice(priceText);
            Double rating = parseRating(ratingText);

            // Log extracted data
            System.out.println("SonosCrawler: Extracted data for product:");
            System.out.println("  Model: " + modelName);
            System.out.println("  Price: " + priceText + " -> " + price);
            System.out.println("  Rating: " + ratingText + " -> " + rating);
            System.out.println("  Image: " + imageUrl);
            System.out.println("  Audio Format: " + audioFormat);
            System.out.println("  Weight: " + weightKg);

            // Build Product
            Product product = new Product();
            product.setId(UUID.randomUUID().toString());
            product.setBrand(Brand.SONOS);
            product.setSourceSite("Sonos");
            product.setModelName(modelName);
            product.setSystemType("Soundbar");
            product.setCategory("Home Theatre");

            product.setPrice(price);
            product.setRating(rating);
            product.setImageUrl(imageUrl);
            product.setProductUrl(productUrl);

            product.setAudioFormat(audioFormat);
            product.setWeightKg(weightKg);
            product.setWifiFormat(wifiFormat);
            product.setPower(power);
            product.setBluetoothVersion("Unavailable");
            product.setChannel("Unavailable");

            System.out.println("SonosCrawler: Product object created successfully");
            return product;

        } catch (Exception e) {
            System.err.println("SonosCrawler: Failed to parse product page: " + productUrl);
            System.err.println("SonosCrawler: Error message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void scrollToSpecsSection(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Scroll to tech specs section
            js.executeScript(
                "var el = document.querySelector('[data-testid=\"tech-specks\"]');" +
                "if (el) { el.scrollIntoView({behavior: 'smooth', block: 'start'}); }"
            );
            sleep(1000);
        } catch (Exception e) {
            System.err.println("SonosCrawler: Error scrolling to specs: " + e.getMessage());
        }
    }

    private String extractModelName(Document doc) {
        Element nameEl = doc.selectFirst("h1[itemprop='name']");
        return nameEl != null ? nameEl.text().trim() : "Unavailable";
    }

    private String extractPrice(Document doc) {
        Element priceEl = doc.selectFirst("span[itemprop='price']");
        return priceEl != null ? priceEl.attr("content") : "Unavailable";
    }

    private String extractRating(Document doc) {
        // Rating format: "4.9/5" from span containing the rating
        Elements ratingSpans = doc.select("span:contains(/5)");
        for (Element span : ratingSpans) {
            String text = span.text();
            if (text.contains("/5") && text.matches(".*\\d+\\.?\\d*/5.*")) {
                return text.split("/")[0].trim();
            }
        }
        return "Unavailable";
    }

    private String extractImageUrl(Document doc) {
        // Look for the first product image
        Element imgContainer = doc.selectFirst("div[data-testid='product-image']");
        if (imgContainer != null) {
            Element img = imgContainer.selectFirst("img");
            if (img != null) {
                String src = img.attr("src");
                if (src != null && !src.isEmpty()) {
                    System.out.println("SonosCrawler: Extracted image from product-image: " + src);
                    return src;
                }
                
                // Try srcset as fallback
                String srcset = img.attr("srcset");
                if (srcset != null && !srcset.isEmpty()) {
                    // Extract the highest quality image from srcset
                    String[] sources = srcset.split(",");
                    if (sources.length > 0) {
                        // Get the last one (usually highest quality)
                        String lastSrc = sources[sources.length - 1].trim().split(" ")[0];
                        System.out.println("SonosCrawler: Extracted image from srcset: " + lastSrc);
                        return lastSrc;
                    }
                }
            }
        }
        
        System.out.println("SonosCrawler: No image found, returning Unavailable");
        return "Unavailable";
    }

    private String extractSpec(Document doc, String specLabel) {
        try {
            // Find the specs section
            Element specsSection = doc.selectFirst("div[data-testid='tech-specks']");
            if (specsSection == null) {
                System.out.println("SonosCrawler: Tech specs section not found");
                return "Unavailable";
            }

            // Find all tab panels - they contain the actual spec data
            Elements tabPanels = specsSection.select("div[role='tabpanel']");
            
            for (Element panel : tabPanels) {
                // Each panel has a grid with flex-col divs containing spec pairs
                Elements specDivs = panel.select("div.flex.flex-col");
                
                for (Element specDiv : specDivs) {
                    // Find the label (p with subhead-sm)
                    Element labelEl = specDiv.selectFirst("p[data-testid='subhead-sm']");
                    if (labelEl == null) continue;
                    
                    String labelText = labelEl.text().trim();
                    
                    if (labelText.equalsIgnoreCase(specLabel)) {
                        // Found the matching label, now get the value from the next p element
                        Element valueEl = specDiv.selectFirst("p[data-testid='para-sm-pt-text']");
                        if (valueEl != null) {
                            // For audio formats, extract only the first format from the first span
                            if (specLabel.equalsIgnoreCase("Home Theatre Audio Formats")) {
                                Element firstSpan = valueEl.selectFirst("span[data-testid='color-annotation pt-text']");
                                if (firstSpan != null) {
                                    String fullText = firstSpan.text().trim();
                                    return fullText;
                                }
                            }
                            
                            // For Weight, extract only the kg value
                            if (specLabel.equalsIgnoreCase("Weight")) {
                                Elements spans = valueEl.select("span[data-testid='color-annotation pt-text']");
                                if (!spans.isEmpty()) {
                                    for (Element span : spans) {
                                        String text = span.text().trim();
                                        String kgValue = extractKgValue(text);
                                        if (!kgValue.equals("Unavailable")) {
                                            return kgValue;
                                        }
                                    }
                                }
                                // Fallback: try to extract kg from full text
                                return extractKgValue(valueEl.text().trim());
                            }
                            
                            // For other specs, get all text from spans
                            Elements spans = valueEl.select("span[data-testid='color-annotation pt-text']");
                            if (!spans.isEmpty()) {
                                StringBuilder result = new StringBuilder();
                                for (Element span : spans) {
                                    String text = span.text().trim();
                                    if (!text.isEmpty() && !text.startsWith("*") && !text.startsWith("Learn more")) {
                                        if (result.length() > 0) result.append(" ");
                                        result.append(text);
                                    }
                                }
                                String finalResult = result.toString().trim();
                                if (!finalResult.isEmpty()) {
                                    return finalResult;
                                }
                            }
                            
                            // Fallback: get all text
                            return valueEl.text().trim();
                        }
                        
                        System.out.println("SonosCrawler: Found label '" + specLabel + "' but no value element");
                        return "Unavailable";
                    }
                }
            }
            
            System.out.println("SonosCrawler: Spec label '" + specLabel + "' not found in any panel");
        } catch (Exception e) {
            System.err.println("SonosCrawler: Error extracting spec '" + specLabel + "': " + e.getMessage());
            e.printStackTrace();
        }
        
        return "Unavailable";
    }

    private String extractKgValue(String weightText) {
        if (weightText == null || weightText.isEmpty()) {
            return "Unavailable";
        }
        
        // Look for kg value in formats like:
        // "6.35 lbs (2.88 kg)" -> extract "2.88 kg"
        // "2.88 kg" -> return as is
        // "25.99 lbs (11.79 kg)" -> extract "11.79 kg"
        
        // Pattern to match kg value with optional parentheses
        // Matches: (2.88 kg) or 2.88 kg or (11.79 kg)
        if (weightText.contains("kg")) {
            // Try to extract from parentheses first
            int openParen = weightText.indexOf('(');
            int closeParen = weightText.indexOf(')');
            
            if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                // Extract content inside parentheses
                String insideParens = weightText.substring(openParen + 1, closeParen).trim();
                if (insideParens.contains("kg")) {
                    return insideParens;
                }
            }
            
            // If not in parentheses, try to extract the kg part directly
            // Find the number before "kg"
            String[] parts = weightText.split("kg");
            if (parts.length > 0) {
                String kgPart = parts[0].trim();
                // Extract the last number from this part
                String[] tokens = kgPart.split("\\s+");
                for (int i = tokens.length - 1; i >= 0; i--) {
                    String token = tokens[i].replaceAll("[^0-9.]", "");
                    if (!token.isEmpty() && token.matches("\\d+\\.?\\d*")) {
                        return token + " kg";
                    }
                }
            }
        }
        
        return "Unavailable";
    }

    private Double parsePrice(String priceText) {
        if (priceText == null || priceText.equalsIgnoreCase("Unavailable")) {
            return null;
        }
        try {
            String cleaned = priceText.replaceAll("[^0-9.]", "");
            return cleaned.isEmpty() ? null : Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseRating(String ratingText) {
        if (ratingText == null || ratingText.equalsIgnoreCase("Unavailable")) {
            return null;
        }
        try {
            String cleaned = ratingText.replaceAll("[^0-9.]", "");
            return cleaned.isEmpty() ? null : Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
