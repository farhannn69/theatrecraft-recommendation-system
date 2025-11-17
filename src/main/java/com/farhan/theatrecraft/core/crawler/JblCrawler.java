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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JblCrawler extends BaseSeleniumCrawler implements ProductCrawler {

    private static final String JBL_LISTING_URL =
            "https://ca.jbl.com/en_CA/home-audio/?prefn1=isRefurbished&prefv1=false&prefn2=isSupport&prefv2=false&prefn3=name&prefv3=Soundbars";

    @Override
    public Brand getBrand() {
        return Brand.JBL;
    }

    @Override
    public List<Product> crawlProducts() {
        WebDriver driver = null;
        List<Product> products = new ArrayList<>();

        System.out.println("JblCrawler: Starting crawl of " + JBL_LISTING_URL);

        try {
            driver = createDriver();
            driver.get(JBL_LISTING_URL);
            sleep(2000);

            // Optional: if JBL shows a cookie popup, you can add a handler here later

            // ==============================
            // Scroll to load all 9 products
            // ==============================
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollTo(0, 0);");
                sleep(800);

                long lastHeight = (Long) js.executeScript("return document.body.scrollHeight;");
                for (int i = 0; i < 6; i++) {
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    sleep(1200);
                    long newHeight = (Long) js.executeScript("return document.body.scrollHeight;");
                    if (newHeight == lastHeight) {
                        break;
                    }
                    lastHeight = newHeight;
                }

                sleep(1000);
                System.out.println("JblCrawler: Finished scrolling listing page.");
            } catch (Exception e) {
                System.out.println("JblCrawler: Scrolling listing failed, continuing anyway: " + e.getMessage());
            }

            // ==============================
            // Collect product URLs from listing
            // ==============================
            String listingHtml = driver.getPageSource();
            Document listingDoc = Jsoup.parse(listingHtml);

            Set<String> productUrls = new LinkedHashSet<>();

            // Find all anchors and keep only /BAR-... or /SB5... under /en_CA
            for (Element a : listingDoc.select("a[href]")) {
                String href = a.attr("href").trim();
                if (href.isEmpty()) continue;

                // Only product patterns you mentioned
                if (!(href.contains("/BAR-") || href.contains("/SB5"))) {
                    continue;
                }

                // Normalize
                if (href.startsWith("/")) {
                    href = "https://ca.jbl.com" + href;
                } else if (!href.startsWith("http")) {
                    href = "https://ca.jbl.com" + (href.startsWith("/") ? href : "/" + href);
                }

                // Optional: strip query params
                int qIdx = href.indexOf('?');
                if (qIdx > 0) {
                    href = href.substring(0, qIdx);
                }

                productUrls.add(href);
            }

            System.out.println("JblCrawler: Found " + productUrls.size() + " potential product URLs.");
            productUrls.forEach(u -> System.out.println("  -> " + u));

            // ==============================
            // Visit each product page
            // ==============================
            for (String productUrl : productUrls) {
                try {
                    driver.get(productUrl);
                    sleep(2000);

                    // Scroll toward specs section so all content is loaded
                    try {
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        // Try to scroll to the "Specs" section link first
                        js.executeScript(
                                "var el = document.querySelector('li.js-scroll[data-section=\"specs\"]');" +
                                "if (el) { el.scrollIntoView(true); }"
                        );
                        sleep(1000);

                        // Then scroll a bit further down to ensure section content is rendered
                        js.executeScript("window.scrollBy(0, 400);");
                        sleep(1000);

                    } catch (Exception e) {
                        System.out.println("JblCrawler: Could not scroll to specs for " + productUrl +
                                " -> " + e.getMessage());
                    }

                    String productHtml = driver.getPageSource();
                    Document productDoc = Jsoup.parse(productHtml);

                    Product p = parseJblProductPage(productDoc, productUrl);
                    if (p != null) {
                        products.add(p);
                        System.out.println("JblCrawler: Parsed JBL product -> "
                                + p.getModelName() + " | URL: " + p.getProductUrl());
                    } else {
                        System.out.println("JblCrawler: Skipped product page (parse returned null): " + productUrl);
                    }

                } catch (Exception e) {
                    System.out.println("JblCrawler: Error processing product URL " + productUrl +
                            " -> " + e.getMessage());
                }
            }

            System.out.println("JblCrawler: Total JBL products parsed: " + products.size());

        } catch (Exception e) {
            System.err.println("JblCrawler: Error during crawl - " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                tearDown(driver);
            }
        }

        return products;
    }

    // =====================================================
    // Parse a single JBL product detail page (with specs)
    // =====================================================

    private Product parseJblProductPage(Document doc, String productUrl) {
        try {
            // ---------- Model Name ----------
            String modelName = null;

            Element h1Name = doc.selectFirst("h1.product-name, h1.product-title, h1[itemprop=name]");
            if (h1Name != null) {
                modelName = h1Name.text().trim();
            }

            // Fallback: OG title
            if (modelName == null || modelName.isEmpty()) {
                Element metaOgTitle = doc.selectFirst("meta[property=og:title]");
                if (metaOgTitle != null) {
                    String content = metaOgTitle.attr("content").trim();
                    if (!content.isEmpty()) {
                        int pipeIdx = content.indexOf('|');
                        modelName = (pipeIdx > 0) ? content.substring(0, pipeIdx).trim() : content;
                    }
                }
            }

            if (modelName == null || modelName.isEmpty()) {
                System.out.println("JblCrawler: No model name found, skipping product: " + productUrl);
                return null;
            }

            // ---------- Price ----------
            Double price = null;
            Element priceEl = doc.selectFirst(
                    ".price .sales .value[content], .price .sales .value, [itemprop=price]"
            );
            if (priceEl != null) {
                String priceText = priceEl.hasAttr("content")
                        ? priceEl.attr("content").trim()
                        : priceEl.text().trim();
                if (!priceText.isEmpty()) {
                    priceText = priceText.replace("$", "").replace(",", "").trim();
                    try {
                        price = Double.parseDouble(priceText);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // ---------- Rating ----------
            Double rating = null;
            Element ratingEl = doc.selectFirst(".bv_avgRating_component_container, .bv_avgRating, [itemprop=ratingValue]");
            if (ratingEl != null) {
                String ratingText = ratingEl.text().trim();
                if (!ratingText.isEmpty()) {
                    try {
                        rating = Double.parseDouble(ratingText);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // ---------- Image URL ----------
            String imageUrl = null;
            Element mainImg = doc.selectFirst(".slick-slide.slick-current img[itemprop=image], img[itemprop=image]");
            if (mainImg == null) {
                mainImg = doc.selectFirst("img.primary-image, img.product-primary-image");
            }
            if (mainImg != null) {
                imageUrl = mainImg.attr("src").trim();
                if (imageUrl.isEmpty()) {
                    imageUrl = mainImg.attr("data-src").trim();
                }
                if (!imageUrl.isEmpty() && imageUrl.startsWith("//")) {
                    imageUrl = "https:" + imageUrl;
                } else if (!imageUrl.isEmpty() && imageUrl.startsWith("/")) {
                    imageUrl = "https://ca.jbl.com" + imageUrl;
                }
            }

            // ---------- Initialize spec fields ----------
            String channel = "Unavailable";
            String power = "Unavailable";
            String audioFormat = "Unavailable";
            String weightKg = "Unavailable";
            String bluetoothVersion = "Unavailable";
            String wifiFormat = "Unavailable";

            // ---------- Specs by section (your exact requirement) ----------
            Element specsWrapper = doc.selectFirst("section.pdp-specs[data-section=specs] .specs-wrapper");
            if (specsWrapper != null) {
                for (Element accordion : specsWrapper.select("> .spec-accordion")) {
                    Element h3 = accordion.selectFirst("h3");
                    if (h3 == null) continue;

                    String sectionTitle = clean(h3.text());  // e.g. "general specifications"

                    Elements rows = accordion.select(".accordion-content .spec-row");
                    for (Element row : rows) {
                        Element keyEl = row.selectFirst(".spec-key");
                        Element valueEl = row.selectFirst(".spec-value");
                        if (keyEl == null || valueEl == null) continue;

                        String key = clean(keyEl.text());      // e.g. "sound system"
                        String value = valueEl.text().trim();  // keep original

                        // -------- General Specifications --------
                        if ("general specifications".equals(sectionTitle)) {
                            if ("sound system".equals(key) && !value.isEmpty()) {
                                channel = value; // e.g. "5.1 channel"
                            }
                        }

                        // -------- Audio Specifications --------
                        else if ("audio specifications".equals(sectionTitle)) {
                            if (key.startsWith("total speaker power output") && !value.isEmpty()) {
                                power = value; // e.g. "620W"
                            } else if ("audio inputs".equals(key) && !value.isEmpty()) {
                                audioFormat = value; // full string
                            }
                        }

                        // -------- Dimensions --------
                        else if ("dimensions".equals(sectionTitle)) {
                            if ("packaging weight".equals(key) && !value.isEmpty()) {
                                String extracted = extractWeightKg(value); // from "18.7 kg / 41.1 lbs"
                                if (extracted != null && !extracted.isEmpty()) {
                                    weightKg = extracted; // "18.7 kg"
                                }
                            }
                        }

                        // -------- Control and Connection Specifications --------
                        else if ("control and connection specifications".equals(sectionTitle)) {
                            if ("bluetooth version".equals(key) && !value.isEmpty()) {
                                bluetoothVersion = value; // "5.0"
                            } else if ((key.equals("wi-fi network") || key.equals("wifi network")) && !value.isEmpty()) {
                                wifiFormat = value; // "IEEE 802.11 a/b/g/n/ac/ax (2.4GHz/5GHz)"
                            }
                        }
                    }
                }
            } else {
                System.out.println("JblCrawler: specs-wrapper not found for " + productUrl);
            }

            // ---------- Build Product ----------
            Product p = new Product();
            p.setId(UUID.randomUUID().toString());
            p.setBrand(Brand.JBL);
            p.setSourceSite(JBL_LISTING_URL);
            p.setModelName(modelName);
            p.setSystemType("Soundbar");
            p.setCategory("Home Audio");
            p.setPrice(price);
            p.setRating(rating);
            p.setImageUrl(imageUrl);
            p.setProductUrl(productUrl);

            p.setChannel(channel);
            p.setAudioFormat(audioFormat);
            p.setWifiFormat(wifiFormat);
            p.setBluetoothVersion(bluetoothVersion);
            p.setWeightKg(weightKg);
            p.setPower(power);

            return p;

        } catch (Exception e) {
            System.out.println("JblCrawler: parseJblProductPage error -> " + e.getMessage());
            return null;
        }
    }

    // =====================================================
    // Helpers
    // =====================================================

    /**
     * Normalizes text for matching: lowercases, removes extra spaces,
     * converts weird dashes and non-breaking spaces.
     */
    private String clean(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace('\u00A0', ' ')  // non-breaking space
                .replace('–', '-')       // en dash
                .replace('—', '-')       // em dash
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Extract something like "18.7 kg" from text such as "18.7 kg / 41.1 lbs".
     */
    private String extractWeightKg(String fullText) {
        if (fullText == null || fullText.isEmpty()) return null;

        Pattern plainPattern =
                Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kg", Pattern.CASE_INSENSITIVE);
        Matcher m = plainPattern.matcher(fullText);
        if (m.find()) {
            return m.group(1).trim() + " kg";
        }
        return null;
    }
}
