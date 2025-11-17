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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * LG soundbar crawler.
 *
 * Follows the same pattern as your JBL / BOSE crawlers:
 *  - Uses Brand + Product model.
 *  - Returns a List<Product> for the shared merged CSV pipeline.
 */
@Component
public class LgCrawler extends BaseSeleniumCrawler implements ProductCrawler {

    private static final String BASE_URL = "https://www.lg.com";
    private static final String LISTING_URL =
            "https://www.lg.com/ca_en/speakers/home-theater-soundbar/?ec_model_status_code=Active";

    // Accept only these URL prefixes as valid soundbar PDPs:
    private static final String PREFIX_SPEAKERS_SOUNDBARS =
            "https://www.lg.com/ca_en/speakers/soundbars/";
    private static final String PREFIX_TV_SOUNDBARS =
            "https://www.lg.com/ca_en/tv-soundbars/soundbars/";

    @Override
    public Brand getBrand() {
        return Brand.LG;
    }

    @Override
    public List<Product> crawlProducts() {
        WebDriver driver = null;
        try {
            System.out.println("LgCrawler: Starting crawl of " + LISTING_URL);
            driver = createDriver();
            driver.get(LISTING_URL);

            handleLgCookiePopup(driver);
            scrollListingToLoadAllProducts(driver);

            Document listingDoc = Jsoup.parse(driver.getPageSource(), BASE_URL);
            Set<String> productUrls = extractProductUrls(listingDoc);

            System.out.println("LgCrawler: Found " + productUrls.size() + " product URLs");
            
            // Print all URLs for debugging
            int urlIndex = 1;
            for (String url : productUrls) {
                System.out.println("LgCrawler: URL " + urlIndex + ": " + url);
                urlIndex++;
            }

            List<Product> products = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (String url : productUrls) {
                System.out.println("LgCrawler: Parsing product " + (successCount + failureCount + 1) + "/" + productUrls.size());
                Product product = parseProductPage(driver, url);
                if (product != null) {
                    products.add(product);
                    successCount++;
                    System.out.println("LgCrawler: Successfully parsed - " + product.getModelName());
                } else {
                    failureCount++;
                    System.out.println("LgCrawler: Failed to parse product from: " + url);
                }
            }

            System.out.println("LgCrawler: Completed. Success: " + successCount + ", Failed: " + failureCount);
            return products;

        } catch (Exception e) {
            System.err.println("LgCrawler: Error during crawl - " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    // ---------------------------------------------------------------------
    // Listing page helpers
    // ---------------------------------------------------------------------

    private void handleLgCookiePopup(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement rejectBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("onetrust-reject-all-handler")));
            rejectBtn.click();
        } catch (Exception ignored) {
            // If it fails (no popup / timeout), we just continue.
        }
    }

    private void scrollListingToLoadAllProducts(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(1000);

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    break;
                }
                lastHeight = newHeight;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    private Set<String> extractProductUrls(Document listingDoc) {
        Set<String> urls = new LinkedHashSet<>();

        // Select product cards specifically from the listing page
        // Look for product links within the product listing structure
        Elements productCards = listingDoc.select(".c-product-card, .product-item, [data-product-code], .product");
        
        System.out.println("LgCrawler: Found " + productCards.size() + " product cards on listing page");
        
        for (Element card : productCards) {
            // Find the main product link within each card
            Element link = card.selectFirst("a[href*='/speakers/soundbars/'], a[href*='/tv-soundbars/soundbars/']");
            
            if (link != null) {
                String href = link.attr("abs:href");
                if (href == null || href.isEmpty()) continue;

                // Strip #hash or ?query
                int hashIdx = href.indexOf('#');
                if (hashIdx != -1) {
                    href = href.substring(0, hashIdx);
                }
                int queryIdx = href.indexOf('?');
                if (queryIdx != -1) {
                    href = href.substring(0, queryIdx);
                }

                // Only accept URLs that match our product patterns and are not category pages
                if ((href.startsWith(PREFIX_SPEAKERS_SOUNDBARS) || href.startsWith(PREFIX_TV_SOUNDBARS)) 
                    && !href.endsWith("/buy") 
                    && !href.endsWith("/speakers/soundbars/")  // Exclude category page
                    && !href.endsWith("/tv-soundbars/soundbars/")  // Exclude category page
                    && !href.endsWith("/speakers/soundbars")  // Exclude category page without trailing slash
                    && !href.endsWith("/tv-soundbars/soundbars")) {  // Exclude category page without trailing slash
                    
                    // Additional check: product URLs should have a model code after the category path
                    String pathAfterCategory = "";
                    if (href.startsWith(PREFIX_SPEAKERS_SOUNDBARS)) {
                        pathAfterCategory = href.substring(PREFIX_SPEAKERS_SOUNDBARS.length());
                    } else if (href.startsWith(PREFIX_TV_SOUNDBARS)) {
                        pathAfterCategory = href.substring(PREFIX_TV_SOUNDBARS.length());
                    }
                    
                    // Only add if there's a model code (non-empty path after category)
                    if (!pathAfterCategory.isEmpty() && !pathAfterCategory.equals("/")) {
                        urls.add(href);
                        System.out.println("LgCrawler: Added product URL: " + href);
                    } else {
                        System.out.println("LgCrawler: Rejected category URL: " + href);
                    }
                }
            }
        }

        // If we didn't find product cards, fall back to searching all links (but with stricter filtering)
        if (urls.isEmpty()) {
            System.out.println("LgCrawler: No products found in cards, trying alternative selector...");
            Elements allLinks = listingDoc.select("a[href]");
            
            for (Element a : allLinks) {
                String href = a.attr("abs:href");
                if (href == null || href.isEmpty()) continue;

                // Strip #hash or ?query
                int hashIdx = href.indexOf('#');
                if (hashIdx != -1) {
                    href = href.substring(0, hashIdx);
                }
                int queryIdx = href.indexOf('?');
                if (queryIdx != -1) {
                    href = href.substring(0, queryIdx);
                }

                if ((href.startsWith(PREFIX_SPEAKERS_SOUNDBARS) || href.startsWith(PREFIX_TV_SOUNDBARS)) 
                    && !href.endsWith("/buy")
                    && !href.endsWith("/speakers/soundbars/")
                    && !href.endsWith("/tv-soundbars/soundbars/")
                    && !href.endsWith("/speakers/soundbars")
                    && !href.endsWith("/tv-soundbars/soundbars")) {
                    
                    String pathAfterCategory = "";
                    if (href.startsWith(PREFIX_SPEAKERS_SOUNDBARS)) {
                        pathAfterCategory = href.substring(PREFIX_SPEAKERS_SOUNDBARS.length());
                    } else if (href.startsWith(PREFIX_TV_SOUNDBARS)) {
                        pathAfterCategory = href.substring(PREFIX_TV_SOUNDBARS.length());
                    }
                    
                    if (!pathAfterCategory.isEmpty() && !pathAfterCategory.equals("/")) {
                        urls.add(href);
                        System.out.println("LgCrawler: Added product URL (fallback): " + href);
                    }
                }
            }
        }

        return urls;
    }

    // ---------------------------------------------------------------------
    // Product page parsing
    // ---------------------------------------------------------------------

    private Product parseProductPage(WebDriver driver, String productUrl) {
        try {
            System.out.println("LgCrawler: Loading product page: " + productUrl);
            driver.get(productUrl);

            // Wait for page to load with increased timeout
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".c-product-digest-information")));
            } catch (Exception e) {
                System.err.println("LgCrawler: Timeout waiting for .c-product-digest-information on: " + productUrl);
                // Continue anyway, maybe the page loaded with different structure
            }

            scrollToSpecsSection(driver);

            Document doc = Jsoup.parse(driver.getPageSource(), BASE_URL);

            String title = extractTitle(doc);    // modelName
            String priceText = extractPrice(doc);
            String ratingText = extractRating(doc);
            String imageUrl = extractImageUrl(doc);
            // Default spec values (everything "Unavailable" by default)
            String bluetoothVersion = "Unavailable";
            String wifiFormat      = "Unavailable";
            String audioFormat     = "Unavailable"; // always Unavailable as per requirement
            String channel         = "Unavailable";
            String power           = "Unavailable";
            String weightKg        = "Unavailable";

            Element specsRoot = doc.getElementById("pdp-specs-section");
            if (specsRoot != null) {
                SpecResult spec = extractSpecs(specsRoot);

                if (spec.bluetoothVersion != null) {
                    bluetoothVersion = spec.bluetoothVersion;
                }
                if (spec.wifiFormat != null) {
                    wifiFormat = spec.wifiFormat;
                }
                if (spec.channel != null) {
                    channel = spec.channel;
                }
                if (spec.power != null) {
                    power = spec.power;
                }
                if (spec.weightKg != null) {
                    weightKg = spec.weightKg;
                }
            }

            // Convert price and rating to Double
            Double price = parsePriceToDouble(priceText);
            Double rating = parseRatingToDouble(ratingText);

            // Log extracted data for debugging
            System.out.println("LgCrawler: Extracted data for product:");
            System.out.println("  Title: " + title);
            System.out.println("  Price: " + priceText + " -> " + price);
            System.out.println("  Rating: " + ratingText + " -> " + rating);
            System.out.println("  Image: " + imageUrl);
            System.out.println("  Channel: " + channel);
            System.out.println("  Power: " + power);

            // Build Product per your model
            
            Product product = new Product();
            product.setId(UUID.randomUUID().toString());
            product.setBrand(Brand.LG);
            product.setSourceSite("LG");          // like "JBL", "BOSE" etc
            product.setModelName(title);          // main product name
            product.setSystemType("Soundbar");    // consistent with your other crawlers
            product.setCategory("Home Theatre");  // or whatever you used for JBL/BOSE

            product.setPrice(price);
            product.setRating(rating);
            product.setImageUrl(imageUrl);
            product.setProductUrl(productUrl);

            product.setBluetoothVersion(bluetoothVersion);
            product.setWifiFormat(wifiFormat);
            product.setAudioFormat(audioFormat);
            product.setChannel(channel);
            product.setPower(power);
            product.setWeightKg(weightKg);

            System.out.println("LgCrawler: Product object created successfully");
            return product;
        } catch (Exception e) {
            System.err.println("LgCrawler: Failed to parse LG product page: " + productUrl);
            System.err.println("LgCrawler: Error message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void scrollToSpecsSection(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "var el = document.getElementById('pdp-specs-section');" +
                    "if (el) { el.scrollIntoView({behavior: 'instant', block: 'start'}); }");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    private String extractTitle(Document doc) {
        Element titleEl = doc.selectFirst(
                ".c-product-digest-information .c-text-contents__headline .cmp-text");
        if (titleEl == null) {
            titleEl = doc.selectFirst("h1.cmp-text");
        }
        return titleEl != null ? titleEl.text().trim() : "Unavailable";
    }

    private String extractPrice(Document doc) {
        Element priceEl = doc.selectFirst(
                ".c-product-digest-information .c-price__purchase");
        if (priceEl == null) {
            priceEl = doc.selectFirst(".c-price__purchase");
        }
        return priceEl != null ? priceEl.text().trim() : "Unavailable";
    }

    private String extractRating(Document doc) {
        sleep(4000);
        Element ratingEl = doc.selectFirst(".bv_avgRating_component_container");
        if (ratingEl == null) {
            ratingEl = doc.selectFirst("[itemprop=ratingValue]");
        }
        return ratingEl != null ? ratingEl.text().trim() : "Unavailable";
    }

    private String extractImageUrl(Document doc) {
        // Strategy 1: Look for the first image in Swiper carousel with data-swiper-slide-index="0"
        // This handles the carousel structure where slide index 0 is the first real image
        Elements carouselSlides = doc.select(".cmp-carousel__item[data-swiper-slide-index='0']");
        
        if (!carouselSlides.isEmpty()) {
            // Get the first slide with index 0 (avoid duplicates)
            for (Element slide : carouselSlides) {
                // Skip if it has swiper-slide-duplicate class (these are clones)
                if (!slide.hasClass("swiper-slide-duplicate")) {
                    Element img = slide.selectFirst("img.cmp-image__image.c-image__img");
                    if (img != null) {
                        String src = img.attr("src");
                        if (src != null && !src.isEmpty()) {
                            src = normalizeImageUrl(src);
                            System.out.println("LgCrawler: Extracted first carousel image (index 0): " + src);
                            return src;
                        }
                    }
                }
            }
        }

        // Strategy 2: Look for first non-duplicate carousel item
        Elements allCarouselItems = doc.select(".cmp-carousel__item:not(.swiper-slide-duplicate) img.cmp-image__image.c-image__img");
        if (!allCarouselItems.isEmpty()) {
            Element firstImg = allCarouselItems.first();
            String src = firstImg.attr("src");
            if (src != null && !src.isEmpty()) {
                src = normalizeImageUrl(src);
                System.out.println("LgCrawler: Extracted first non-duplicate carousel image: " + src);
                return src;
            }
        }

        // Strategy 3: Look in gallery display area
        Element imgEl = doc.selectFirst(".c-summary-gallery__contents .c-gallery__display img.c-image__img");
        if (imgEl != null) {
            String src = imgEl.attr("src");
            if (src != null && !src.isEmpty()) {
                src = normalizeImageUrl(src);
                System.out.println("LgCrawler: Extracted image from gallery display: " + src);
                return src;
            }
        }

        // Strategy 4: Fallback to any product image
        imgEl = doc.selectFirst("img.cmp-image__image.c-image__img");
        if (imgEl != null) {
            String src = imgEl.attr("src");
            if (src != null && !src.isEmpty()) {
                src = normalizeImageUrl(src);
                System.out.println("LgCrawler: Extracted image from fallback: " + src);
                return src;
            }
        }

        System.out.println("LgCrawler: No image found, returning Unavailable");
        return "Unavailable";
    }

    private String normalizeImageUrl(String src) {
        if (src == null || src.isEmpty()) {
            return "Unavailable";
        }

        // If already absolute URL
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return src;
        }

        // Convert relative URL to absolute
        if (!src.startsWith("/")) {
            src = "/" + src;
        }
        return BASE_URL + src;
    }

    private Double parsePriceToDouble(String priceText) {
        if (priceText == null || priceText.equalsIgnoreCase("Unavailable")) {
            return null;
        }
        // Example: "$799.99" -> 799.99
        String cleaned = priceText.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return null;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseRatingToDouble(String ratingText) {
        if (ratingText == null || ratingText.equalsIgnoreCase("Unavailable")) {
            return null;
        }
        // Example: "4.7" -> 4.7
        String cleaned = ratingText.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return null;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Specs parsing
    // ---------------------------------------------------------------------

    private static class SpecResult {
        String bluetoothVersion;
        String wifiFormat;
        String channel;
        String power;
        String weightKg;
    }

    private SpecResult extractSpecs(Element specsRoot) {
        SpecResult result = new SpecResult();

        Elements items = specsRoot.select("li.c-compare-selling__item");
        for (Element li : items) {
            Element nameEl = li.selectFirst(".c-compare-selling__spec-name p");
            Element valueEl = li.selectFirst(".c-compare-selling__spec-desc p");
            if (nameEl == null || valueEl == null) continue;

            String label = cleanLabel(nameEl.text());
            String value = valueEl.text().trim();

            if (label.equalsIgnoreCase("Bluetooth Version")) {
                result.bluetoothVersion = value;
            } else if (isWifiLabel(label)) {
                result.wifiFormat = value;
            } else if (label.equalsIgnoreCase("Number of Channels")) {
                result.channel = value;
            } else if (label.equalsIgnoreCase("Output Power")) {
                result.power = normalizePower(value);
            } else if (label.equalsIgnoreCase("Gross Weight")) {
                result.weightKg = value;
            }
        }

        return result;
    }

    private String cleanLabel(String text) {
        if (text == null) return "";
        return text
                .replace('\u00A0', ' ')  // non-breaking space -> normal space
                .replace('\u2011', '-')  // non-breaking hyphen -> hyphen
                .trim();
    }

    private boolean isWifiLabel(String label) {
        if (label == null) return false;

        String normalized = label.toLowerCase(Locale.ROOT)
                .replace("\u00A0", " ")
                .replace("-", "")
                .replace(" ", "");

        // Accept "wifi", "wi-fi", "wi fi", etc.
        return "wifi".equals(normalized);
    }

    private String normalizePower(String value) {
        if (value == null) return "Unavailable";
        // "500 W" -> "500W"
        String compact = value.replace(" ", "");
        return compact.isEmpty() ? "Unavailable" : compact;
    }
}
