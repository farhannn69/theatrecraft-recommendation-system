# TheatreCraft Recommendation System

A smart home theater product search and recommendation system that uses advanced java algorithms. This project is part of the final project defense for the COMP 8547 course at the University of Windsor.

## Features

- **Web Crawling**: Automated product data collection from major brands using Selenium, Jsoup.
- **Intelligent Search**: KMP (Knuth-Morris-Pratt) Algorithm and Boyer-Moore Algorithm for pattern matching.
- **Smart Autocomplete**: Trie-based suggestions with minimum 3 characters.
- **Spell Check**: Edit Distance (Levenshtein) Algorithm for product suggestions.
- **Search Analytics**: Track and display top searched products with frequency analysis.
- **Page Ranking**: Min Heap-based ranking for top URLs by keyword frequency.
- **Product Comparison**: Compare multiple products side-by-side

## Technologies Used

### Backend
- **Java**
- **Spring Boot** - REST API framework
- **Selenium WebDriver** - Web scraping and crawling
- **Maven** - Build and dependency management

### Frontend
- **HTML5, CSS3, JavaScript** - Pure vanilla JavaScript (no frameworks)
- **Fetch API** - Asynchronous HTTP requests

### Algorithms & Data Structures
- **KMP Search** - Pattern matching
- **Trie** - Autocomplete and prefix search
- **Edit Distance** - Spell checking with dynamic programming
- **Min Heap** - Top-K URL ranking
- **Inverted Index** - Keyword-to-URL mapping
- **Boyer-Moore** - Efficient string searching
- **HashMap** - Search frequency tracking

### Storage
- **CSV Files** - Product and frequency data persistence

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Chrome browser (for Selenium web crawling)

## How to Run

### 1. Clone the Repository
```bash
git clone https://github.com/farhannn69/theatrecraft-recommendation-system.git
cd theatrecraft-recommendation-system
```

### 2. Build the Project
```bash
./mvnw clean install
```

### 3. Run the Application
```bash
./mvnw spring-boot:run
```

The application will start on **http://localhost:8080**

## Project Structure (Truncated)

```
src/
├── main/
│   ├── java/com/farhan/theatrecraft/
│   │   ├── TheatrecraftApplication.java
│   │   ├── api/                    # REST Controllers
│   │   └── core/
│   │       ├── crawler/            # Web crawlers (Selenium)
│   │       ├── model/              # Domain models
│   │       ├── search/             # Search algorithms
│   │       ├── service/            # Business logic
│   │       └── storage/            # CSV repositories
│   └── resources/
│       ├── application.properties
│       └── static/                 # Frontend files
│           ├── index.html
│           ├── app.js
│           └── styles.css
```
## Author

**COMP 8547 | Group 16 | Fall 2025**