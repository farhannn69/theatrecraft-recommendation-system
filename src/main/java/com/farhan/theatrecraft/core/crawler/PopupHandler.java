package com.farhan.theatrecraft.core.crawler;

import com.farhan.theatrecraft.core.model.Brand;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Centralized popup handler for all brand crawls.
 */
public final class PopupHandler {

    private PopupHandler() {
        // utility class
    }

    /**
     * Entry point: handle popups for a given brand.
     */
    public static void handlePopups(WebDriver driver, Brand brand) {
        if (driver == null || brand == null) {
            return;
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

        switch (brand) {
            case BOSE -> handleBosePopups(driver, wait);
            case SONOS -> handleSonosPopups(driver, wait);
            case SAMSUNG -> handleSamsungPopups(driver, wait);
            case LG -> handleLgPopups(driver, wait);
            case JBL -> handleJblPopups(driver, wait);
            default -> {
                // no-op
            }
        }
    }

    // ==========================
    // BOSE POPUPS
    // ==========================

    private static void handleBosePopups(WebDriver driver, WebDriverWait wait) {
        // Your previous logic adapted into this method

        try {
            // HANDLE LANGUAGE SELECTION MODAL
            WebElement langButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.className("modal-language"))
            );
            langButton.click();
            System.out.println("BOSE: Closed language modal");
        } catch (TimeoutException e) {
            System.out.println("BOSE: No language modal found");
        }

        try {
            // HANDLE EMAIL SIGNUP MODAL
            WebElement crossMail = wait.until(
                    ExpectedConditions.elementToBeClickable(By.className("evg-btn-dismissal"))
            );
            crossMail.click();
            System.out.println("BOSE: Closed email modal");
        } catch (TimeoutException e) {
            System.out.println("BOSE: No email modal found");
        }

        try {
            // HANDLE COOKIE CONSENT BANNER
            WebElement cookieAccept = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler"))
            );
            cookieAccept.click();
            System.out.println("BOSE: Accepted cookies");
        } catch (TimeoutException e) {
            System.out.println("BOSE: No cookie consent found");
        }
    }

    // ==========================
    // SONOS POPUPS (TODO)
    // ==========================

    private static void handleSonosPopups(WebDriver driver, WebDriverWait wait) {
        System.out.println("[Sonos] Attempting to handle popups...");
        
        // Handle lightbox popup with close button
        try {
            System.out.println("[Sonos] Looking for lightbox close button...");
            WebElement closeButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div[title='Close'].sidebar-iframe-close, div[aria-label='Close Modal']")
                )
            );
            closeButton.click();
            System.out.println("[Sonos] Lightbox popup closed.");
            Thread.sleep(500);
        } catch (TimeoutException e) {
            System.out.println("[Sonos] No lightbox popup found or already dismissed.");
        } catch (Exception e) {
            System.out.println("[Sonos] Error handling lightbox popup: " + e.getMessage());
        }
        
        System.out.println("[Sonos] Popup handling complete.");
    }

    // ==========================
    // LG POPUPS (TODO)
    // ==========================

    private static void handleLgPopups(WebDriver driver, WebDriverWait wait) {
        // TODO: inspect LG site and add real selectors
        System.out.println("LG: No popup handling implemented yet");
    }

    // ==========================
    // SAMSUNG POPUPS
    // ==========================

    private static void handleSamsungPopups(WebDriver driver, WebDriverWait wait) {
        try {
            // Handle Samsung cookie consent popup
            // Selector: div.truste-custom-samsung-link#truste-consent-required
            WebElement continueButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("div.truste-custom-samsung-link#truste-consent-required")
                    )
            );
            continueButton.click();
            System.out.println("SAMSUNG: Clicked 'Continue without accepting' button");
        } catch (TimeoutException e) {
            System.out.println("SAMSUNG: No cookie consent popup found");
        } catch (Exception e) {
            System.out.println("SAMSUNG: Error handling popup - " + e.getMessage());
        }
    }

    // ==========================
    // JBL POPUPS (TODO)
    // ==========================

    private static void handleJblPopups(WebDriver driver, WebDriverWait wait) {
        // TODO: inspect JBL site and add real selectors
        System.out.println("JBL: No popup handling implemented yet");
    }
}
