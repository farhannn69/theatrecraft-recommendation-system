package com.farhan.theatrecraft.core.crawler;

import com.farhan.theatrecraft.core.model.Brand;
import com.farhan.theatrecraft.core.model.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SamsungCrawler extends BaseSeleniumCrawler implements ProductCrawler {
    
    private static final String BASE_URL = "https://www.samsung.com";
    private static final String LISTING_URL = "https://www.samsung.com/ca/audio-devices/all-audio-devices/?soundbar";

    @Override
    public Brand getBrand() {
        return Brand.SAMSUNG;
    }

    @Override
    public List<Product> crawlProducts() {
        WebDriver driver = null;
        try {
            System.out.println("SamsungCrawler: Starting crawl of " + LISTING_URL);
            driver = createDriver();
            driver.get(LISTING_URL);
            
            sleep(5000);

            PopupHandler.handlePopups(driver, Brand.SAMSUNG);
            sleep(2000);

            scrollToLoadProducts(driver);
            sleep(3000);

            Document listingDoc = Jsoup.parse(driver.getPageSource(), BASE_URL);
            Set<String> productUrls = extractProductUrls(listingDoc);

            System.out.println("SamsungCrawler: Found " + productUrls.size() + " product URLs");

            List<Product> products = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (String url : productUrls) {
                System.out.println("SamsungCrawler: Parsing product " + (successCount + failureCount + 1) + "/" + productUrls.size());
                Product product = parseProductPage(driver, url);
                if (product != null) {
                    products.add(product);
                    successCount++;
                    System.out.println("SamsungCrawler: Successfully parsed - " + product.getModelName());
                } else {
                    failureCount++;
                }
            }

            System.out.println("SamsungCrawler: Completed. Success: " + successCount + ", Failed: " + failureCount);
            return products;

        } catch (Exception e) {
            System.err.println("SamsungCrawler: Error during crawl - " + e.getMessage());
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
            js.executeScript("window.scrollTo(0, 0);");
            sleep(1000);

            long scrollHeight = (Long) js.executeScript("return document.body.scrollHeight;");
            int steps = 10;
            
            for (int i = 1; i <= steps; i++) {
                long y = scrollHeight * i / steps;
                js.executeScript("window.scrollTo(0, arguments[0]);", y);
                System.out.println("SamsungCrawler: Scrolling step " + i + "/" + steps);
                sleep(2000);
            }

            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(3000);
            
            System.out.println("SamsungCrawler: Finished scrolling");
        } catch (Exception e) {
            System.err.println("SamsungCrawler: Error during scrolling - " + e.getMessage());
        }
    }

    private Set<String> extractProductUrls(Document doc) {
        Set<String> urls = new LinkedHashSet<>();
        Elements productLinks = doc.select("a[href*='/ca/audio-devices/soundbar/']");
        
        for (Element link : productLinks) {
            String href = link.attr("href");
            if (href == null || href.isBlank()) continue;
            
            if (href.startsWith("/")) {
                href = BASE_URL + href;
            } else if (!href.startsWith("http")) {
                href = BASE_URL + "/" + href;
            }
            
            if (href.contains("/ca/audio-devices/soundbar/") && !href.contains("#")) {
                urls.add(href);
            }
        }
        
        return urls;
    }

    private Product parseProductPage(WebDriver driver, String productUrl) {
        try {
            driver.get(productUrl);
            sleep(3000);

            // Wait additional time for price and rating to load (they load dynamically)
            sleep(4000);

            // Scroll to top to ensure price/rating elements are in viewport
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, 0);");
            sleep(2000);

            // Wait a bit more for dynamic content to fully render
            sleep(2000);

            // Parse the page to get price and rating while at top
            Document docTop = Jsoup.parse(driver.getPageSource(), productUrl);

            Product product = new Product();
            product.setId(UUID.randomUUID().toString());
            product.setBrand(Brand.SAMSUNG);
            product.setSourceSite(LISTING_URL);
            product.setProductUrl(productUrl);

            Element nameElem = docTop.selectFirst("h1.pdd39-anchor-nav__headline.sg-product-display-name");
            if (nameElem != null) {
                product.setModelName(nameElem.text().trim());
            }

            Element priceElem = docTop.selectFirst("span.pd-buying-price__new-price-currency");
            if (priceElem != null) {
                product.setPrice(parsePrice(priceElem.text().trim()));
            }

            // Rating: <strong class="rating__point"><span class="hidden">Product Ratings : </span><span>4.7</span></strong>
            // We need to select the strong element, then get the last span child (not the hidden one)
            Element ratingStrong = docTop.selectFirst("strong.rating__point");
            if (ratingStrong != null) {
                Elements spans = ratingStrong.select("span");
                // The second span (index 1) contains the actual rating value
                if (spans.size() >= 2) {
                    String ratingText = spans.get(1).text();
                    product.setRating(parseRating(ratingText));
                }
            }

            Element imageElem = docTop.selectFirst("div.image.image--main-loaded img.image__main");
            if (imageElem != null) {
                String imgSrc = imageElem.attr("src");
                if (imgSrc.isBlank()) {
                    imgSrc = imageElem.attr("data-desktop-src");
                }
                product.setImageUrl(imgSrc);
            }

            // Now scroll down to load and extract specifications
            scrollToSpecsSection(driver);
            sleep(2000);

            // Parse the page again to get specifications
            Document docSpecs = Jsoup.parse(driver.getPageSource(), productUrl);
            extractSpecifications(docSpecs, product);

            product.setCategory("Soundbar");
            product.setSystemType("Soundbar");

            return product;

        } catch (Exception e) {
            System.err.println("SamsungCrawler: Error parsing product page " + productUrl + " - " + e.getMessage());
            return null;
        }
    }

    private void scrollToSpecsSection(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            List<WebElement> specsElements = driver.findElements(By.cssSelector("div.pdd32-product-spec__inner"));
            
            if (!specsElements.isEmpty()) {
                js.executeScript("arguments[0].scrollIntoView({behavior: ''smooth'', block: ''center''});", specsElements.get(0));
                sleep(2000);
            } else {
                for (int i = 0; i < 5; i++) {
                    js.executeScript("window.scrollBy(0, 500);");
                    sleep(1000);
                }
            }
        } catch (Exception e) {
            System.err.println("SamsungCrawler: Error scrolling to specs - " + e.getMessage());
        }
    }

    private void extractSpecifications(Document doc, Product product) {
        Elements specItems = doc.select("li.pdd32-product-spec__content-item");
        
        for (Element item : specItems) {
            Element titleElem = item.selectFirst("p.pdd32-product-spec__content-item-title");
            Element descElem = item.selectFirst("p.pdd32-product-spec__content-item-desc");
            
            if (titleElem == null || descElem == null) continue;
            
            String title = titleElem.text().trim();
            String value = descElem.text().trim();
            
            switch (title) {
                case "Number of Channel":
                    product.setChannel(value);
                    break;
                case "Gross Weight (One Packing)":
                    product.setWeightKg(value);
                    break;
                case "Operating Power Consumption (Main)":
                    product.setPower(value);
                    break;
                case "Dolby":
                    product.setAudioFormat(value);
                    break;
                case "Wi-Fi":
                    product.setWifiFormat(value);
                    break;
                case "Bluetooth Version":
                    product.setBluetoothVersion(value);
                    break;
            }
        }
        
        if (product.getChannel() == null || product.getChannel().isBlank()) {
            product.setChannel("Unavailable");
        }
        if (product.getWeightKg() == null || product.getWeightKg().isBlank()) {
            product.setWeightKg("Unavailable");
        }
        if (product.getPower() == null || product.getPower().isBlank()) {
            product.setPower("Unavailable");
        }
        if (product.getAudioFormat() == null || product.getAudioFormat().isBlank()) {
            product.setAudioFormat("Unavailable");
        }
        if (product.getWifiFormat() == null || product.getWifiFormat().isBlank()) {
            product.setWifiFormat("Unavailable");
        }
        if (product.getBluetoothVersion() == null || product.getBluetoothVersion().isBlank()) {
            product.setBluetoothVersion("Unavailable");
        }
    }

    private Double parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank()) return null;
        try {
            String cleaned = priceText.replaceAll("[$,\\s]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            System.err.println("SamsungCrawler: Could not parse price: " + priceText);
            return null;
        }
    }

    private Double parseRating(String ratingText) {
        if (ratingText == null || ratingText.isBlank()) return null;
        try {
            return Double.parseDouble(ratingText.trim());
        } catch (NumberFormatException e) {
            System.err.println("SamsungCrawler: Could not parse rating: " + ratingText);
            return null;
        }
    }
}
