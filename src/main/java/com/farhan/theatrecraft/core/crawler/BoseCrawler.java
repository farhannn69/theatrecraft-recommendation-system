package com.farhan.theatrecraft.core.crawler;

import com.farhan.theatrecraft.core.model.Brand;
import com.farhan.theatrecraft.core.model.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BoseCrawler extends BaseSeleniumCrawler implements ProductCrawler {

    private static final String BOSE_URL = "https://www.bose.ca/en/c/home-theater";

    @Override
    public Brand getBrand() {
        return Brand.BOSE;
    }

    @Override
    public List<Product> crawlProducts() {
        WebDriver driver = null;
        List<Product> products = new ArrayList<>();

        System.out.println("BoseCrawler: Starting crawl of " + BOSE_URL);

        try {
            driver = createDriver();
            driver.get(BOSE_URL);
            sleep(2000);

            // Handle popups via your shared handler
            PopupHandler.handlePopups(driver, Brand.BOSE);
            sleep(1500);

            // Scroll to trigger lazy load of all product cards
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollTo(0, 0);");
                sleep(800);

                long scrollHeight = (Long) js.executeScript("return document.body.scrollHeight;");
                int steps = 4;
                for (int i = 1; i <= steps; i++) {
                    long y = scrollHeight * i / steps;
                    js.executeScript("window.scrollTo(0, arguments[0]);", y);
                    sleep(800);
                }

                sleep(1200);
            } catch (Exception e) {
                System.out.println("BoseCrawler: scrolling failed, continuing. " + e.getMessage());
            }

            // Collect product URLs
            Set<String> productUrls = new LinkedHashSet<>();
            List<WebElement> productLinks = driver.findElements(By.cssSelector("a[href*='/p/home-theater/']"));
            for (WebElement link : productLinks) {
                String href = link.getAttribute("href");
                if (href == null || href.isBlank()) continue;

                if (href.startsWith("/")) {
                    href = "https://www.bose.ca" + href;
                } else if (!href.startsWith("http")) {
                    href = "https://www.bose.ca" + (href.startsWith("/") ? href : "/" + href);
                }
                productUrls.add(href);
            }

            System.out.println("BoseCrawler: Found " + productUrls.size() + " unique product URLs.");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Visit each product page
            for (String productUrl : productUrls) {
                try {
                    driver.get(productUrl);
                    sleep(1500);

                    // Open "Technical Specifications" accordion if present
                    try {
                        WebElement techButton = wait.until(
                                ExpectedConditions.presenceOfElementLocated(
                                        By.cssSelector("button[data-target='#accordion-collapse-pdp-2']"))
                        );
                        String expanded = techButton.getAttribute("aria-expanded");
                        if ("false".equalsIgnoreCase(expanded)) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", techButton);
                            sleep(500);
                            techButton.click();
                            sleep(1000);
                        }
                    } catch (Exception e) {
                        System.out.println("BoseCrawler: Tech spec accordion not found for " + productUrl);
                    }

                    // Get page HTML and parse with Jsoup
                    String html = driver.getPageSource();
                    Document doc = Jsoup.parse(html);

                    Product product = parseBoseProductPage(doc, productUrl);
                    if (product != null) {
                        products.add(product);
                        System.out.println("BoseCrawler: Parsed Bose product -> "
                                + product.getModelName() + " | URL: " + productUrl);
                    }

                } catch (Exception e) {
                    System.out.println("BoseCrawler: Error processing product URL " + productUrl + " -> " + e.getMessage());
                }
            }

            System.out.println("BoseCrawler: Total Bose products parsed: " + products.size());

        } catch (Exception e) {
            System.err.println("BoseCrawler: Error during crawl - " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                tearDown(driver);
            }
        }

        return products;
    }

    private Product parseBoseProductPage(Document doc, String productUrl) {
        try {
            // Title
            Element titleEl = doc.selectFirst("h1.product-name");
            String modelName = titleEl != null ? titleEl.text().trim() : null;
            if (modelName == null || modelName.isEmpty()) {
                return null;
            }

            // Price
            Double price = null;
            Element priceEl = doc.selectFirst("span.value[content]");
            if (priceEl != null) {
                String content = priceEl.attr("content").trim();
                try {
                    price = Double.parseDouble(content);
                } catch (NumberFormatException ignored) {
                }
            }

            // Rating
            Double rating = null;
            Element ratingEl = doc.selectFirst(".bv_avgRating_component_container");
            if (ratingEl != null) {
                String ratingText = ratingEl.text().trim();
                try {
                    rating = Double.parseDouble(ratingText);
                } catch (NumberFormatException ignored) {
                }
            }

            // Image: prefer bynder image with fetchpriority="high"
            String imageUrl = null;
            Element imgEl = doc.selectFirst("img.bynder__image[fetchpriority=high]");
            if (imgEl == null) {
                imgEl = doc.selectFirst("img.bynder__image");
            }
            if (imgEl != null) {
                imageUrl = imgEl.attr("src").trim();
                if (imageUrl.isEmpty()) {
                    imageUrl = imgEl.attr("data-src").trim();
                }
                if (imageUrl.startsWith("/")) {
                    imageUrl = "https://assets.bosecreative.com" + imageUrl;
                }
            }

            // Specs table rows
            Elements specRows = doc.select(".product-specification__row");
            String soundOptions = null;
            String wirelessConnectivity = null;
            String bluetoothVersion = null;
            String entireProductSystem = null;

            for (Element row : specRows) {
                Element th = row.selectFirst(".product-specification__sub-title");
                Element td = row.selectFirst(".product-specification__description");
                if (th == null || td == null) continue;

                String label = th.text().trim();
                String value = td.text().trim();

                switch (label) {
                    case "Sound Options":
                        soundOptions = value;
                        break;
                    case "Wireless Connectivity":
                        wirelessConnectivity = value;
                        break;
                    case "Bluetooth Version":
                        bluetoothVersion = value;
                        break;
                    case "Entire Product System":
                        entireProductSystem = value;
                        break;
                    default:
                        // ignore other rows
                        break;
                }
            }

            // Map to new fields
            String channel = soundOptions != null && !soundOptions.isEmpty()
                    ? soundOptions
                    : "Unavailable";

            String wifiFormat = wirelessConnectivity != null && !wirelessConnectivity.isEmpty()
                    ? wirelessConnectivity
                    : "Unavailable";

            String btVersion = bluetoothVersion != null && !bluetoothVersion.isEmpty()
                    ? bluetoothVersion
                    : "Unavailable";

            String weightKg = extractWeightKg(entireProductSystem);
            if (weightKg == null || weightKg.isEmpty()) {
                weightKg = "Unavailable";
            }

            // For now Bose audio format & power are not clearly exposed
            String audioFormat = "Unavailable";
            String power = "Unavailable";

            Product p = new Product();
            p.setId(UUID.randomUUID().toString());
            p.setBrand(Brand.BOSE);
            p.setSourceSite(BOSE_URL);
            p.setModelName(modelName);
            p.setSystemType("Soundbar / Home Theater");
            p.setCategory("Home Theater");
            p.setPrice(price);
            p.setRating(rating);
            p.setImageUrl(imageUrl);
            p.setProductUrl(productUrl);

            p.setChannel(channel);
            p.setAudioFormat(audioFormat);
            p.setWifiFormat(wifiFormat);
            p.setBluetoothVersion(btVersion);
            p.setWeightKg(weightKg);
            p.setPower(power);

            return p;

        } catch (Exception e) {
            System.out.println("BoseCrawler: parseBoseProductPage error -> " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract something like "3.13 kg" from the full "Entire Product System" text.
     * Example: "5.61 cm H x 69.44 cm W x 10.39 cm D (3.13 kg)" -> "3.13 kg"
     */
    private String extractWeightKg(String fullText) {
        if (fullText == null || fullText.isEmpty()) return null;

        // First try to capture "(3.13 kg)" pattern
        Pattern parenPattern = Pattern.compile("\\(([^)]+kg)\\)");
        Matcher m1 = parenPattern.matcher(fullText);
        if (m1.find()) {
            return m1.group(1).trim();  // "3.13 kg"
        }

        // Fallback: find "<number> kg"
        Pattern plainPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kg");
        Matcher m2 = plainPattern.matcher(fullText);
        if (m2.find()) {
            return m2.group(1).trim() + " kg";
        }

        return null;
    }
}
