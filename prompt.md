# TheatreCraft – Copilot Development Instructions

You are Copilot helping me develop a full-stack application.

## PROJECT INFO

- Language: **Java (Spring Boot)**, HTML, CSS, JS  
- Group: `com.farhan`  
- Artifact: `theatrecraft`  
- Repo: `theatrecraft-recommendation-system`

## BACKEND
- Framework: **Spring Boot**
- Crawling: **Selenium + WebDriverManager + Jsoup**
- Storage: **CSV**
- Algorithms coming later: **KMP**, **Trie**, **Edit Distance**, **HashMap frequency**, **Inverted Index**
- Architecture:
  - `core/` → crawling, models, services, CSV, logic
  - `api/` → REST controllers (JSON responses)
  - `resources/static/` → frontend HTML/CSS/JS

---

# ========================
# 1. ADD DEPENDENCIES
# ========================

Add these to `pom.xml`:

<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.21.0</version>
</dependency>

<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.8.0</version>
</dependency>

<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>

<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>

---

# ========================
# 2. CORE MODELS + CSV
# ========================

## 2.1 Create Brand enum
- File: core/model/Brand.java

Enum values:

BOSE
SONOS
SONY
LG
SAMSUNG

- Include displayName.

## 2.2 Create Product.java

Fields:

String id
Brand brand
String sourceSite
String modelName
String systemType
String category
Double price
Double rating
String features
String imageUrl
String productUrl

Include getters, setters, toString().

## 2.3 Create Product.java
- File: core/storage/ProductCsvRepository.java

- Responsibilities:
    - File path: src/main/resources/data/products.csv
    - Methods:
        - List<Product> loadAll()
        - void saveAll(List<Product> products)
    - Use OpenCSV

- Create folder: src/main/resources/data/
- Create file: products.csv with header: id,brand,sourceSite,modelName,systemType,category,price,rating,features,imageUrl,productUrl

---

# ========================
# 3. CRAWLER FRAMEWORK (SELENIUM + JSOUP)
# ========================

## 3.1 ProductCrawler Interface

- core/crawler/ProductCrawler.java
- Methods:
    - Brand getBrand()
    - List<Product> crawlProducts()

## 3.2 BaseSeleniumCrawler

- File: core/crawler/BaseSeleniumCrawler.java
- Contains:
    - createDriver() using WebDriverManager
    - sleep() helper
    - WebDriver setup & teardown pattern

## 3.3 PopupHandler

- File: core/crawler/PopupHandler.java
- Contains: public static void handlePopups(WebDriver driver, Brand brand)

- Switch based on brand → call:
    - handleBosePopups
    - handleSonosPopups
    - handleSonyPopups
    - handleLgPopups
    - handleSamsungPopups
- Each method contains TODO selectors.

## 3.4 Brand Crawlers (5 Files)
- Files:
    - BoseCrawler.java
    - SonosCrawler.java
    - SonyCrawler.java
    - LgCrawler.java
    - SamsungCrawler.java

- Each:
    - Implements ProductCrawler
    - Extends BaseSeleniumCrawler
    - Opens brand URL:
        - BOSE: https://www.bose.ca/en/c/home-theater
        - SONOS: https://www.sonos.com/en-ca/shop/home-theater
        - SONY: https://electronics.sony.ca/en/audio/speakers/c/home-theater-speakers
        - LG: https://www.lg.com/ca_en/tv-soundbars/home-theater-soundbar/
        - SAMSUNG: https://www.samsung.com/ca/audio-devices/all-audio-devices/

- Calls PopupHandler
- Parses with Jsoup
- Returns dummy product list for now (test pipeline)

---

# ========================
# 4. CRAWL SERVICE + API ENDPOINTS
# ========================

## 4.1 CrawlService
- File: core/service/CrawlService.java

- Responsibilities:
    - Inject ProductCsvRepository
    - Inject all ProductCrawler implementations
    - Load CSV on startup (@PostConstruct)
    - Maintain internal product list

- Methods:
    - getAllProducts()
    - getProductsByBrand(Brand brand)
    - getLatestProducts(int limit)
    - crawlBrand(Brand brand)
        - Runs crawler
        - Replaces old brand products
        - Writes CSV
        - Returns crawled list

## 4.2 ProductController
- File: api/ProductController.java
- Endpoints: 
    - GET  /api/products
    - GET  /api/products?brand=BOSE
    - GET  /api/products/latest?limit=12
    - POST /api/crawl/{brand}
- Return JSON only.

---

# ========================
# 5. FRONTEND UI (index.html DESIGN)
# ========================

- File: src/main/resources/static/index.html

- Sections:

    - (1) NAVBAR
        - Logo left: “TheatreCraft”
        - Right links:
            - Home
            - Analytics (dropdown): Top Frequency, Frequency Count, Page Ranking
            - Compare
            - Recommend
        - Hover effect on links
        - Sticky top preferred

    - (2) HERO SECTION
        - Large heading: “TheatreCraft: Home Theatre Search & Recommendations”
        - Subheading paragraph

    - (3) BRAND CARDS SECTION
        - 5 cards, equal sized
        - Each card has:
            - Logo placeholder
            - Brand name
            - data-brand="BOSE" etc
        - Clicking triggers crawl

    - (4) SEARCH SECTION
        - Input: #search-input
        - Button: #search-button
        - Search functionality added later

    - (5) LATEST PRODUCTS SECTION
        - Heading: “Latest Crawled Products”
        - Grid: #latest-products-grid
        - Each card:
            - Placeholder image
            - Brand
            - Model name
            - Price
            - Button: “Go to website”

---

# ========================
# 6. FRONTEND UI (styles.css DESIGN)
# ========================

- File path: src/main/resources/static/styles.css

- Requirements:

- Primary colors will be white and red (in HEX #B22234) and black (HEX #212427) where necessary.
- Global:
    - white background, light text.
    - body uses a system sans-serif font.
    - create a .main-container with max-width around 1200px, centered with padding.

- Navbar:
    - full-width at top, slightly darker background, subtle box-shadow.
    - flex layout: logo on the left, nav links on the right.
    - nav links should have hover effects (color change and maybe an underline or bottom border).

- Hero section:
    - center text, with larger heading and smaller subheading.
    - add spacing so it doesn't feel cramped.

- Brand cards section:
    - use CSS grid (e.g. grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));).
    - .brand-card should have a dark card background, rounded corners, padding, and a hover effect:
        - slight transform: translateY(-4px) scale(1.02);
        - increased shadow and pointer cursor.

- Search section:
    - horizontally align input and button on desktop, stack on smaller screens.
    - input should have rounded corners and reasonable width.
    - button should be visually prominent.

- Latest products grid:
    - use grid similar to brand cards.
    - each product card has:
        - a placeholder image area (fixed height, darker background).
        - product title, brand, price text.
        - a full-width "Go to website" button styled as a primary action.
- Add a few simple media queries for responsiveness on small screens.

---

# ========================
# 7. FRONTEND JS (app.js)
# ========================

- File: src/main/resources/static/app.js

- Implement:

    - On page load:
        - GET /api/products/latest?limit=12
        - Render items into #latest-products-grid

    - Brand card clicks:
        - Add event listeners
        - On click:
            - Show loading indicator
            - POST /api/crawl/{brand}
            - Refresh product list afterward

    - Helper function: function renderProducts(products)
        
    - Search button:
        - For now: console.log("search coming soon")
        - Real search logic added later

---