// ========================
// THEATRECRAFT APP.JS
// ========================

document.addEventListener("DOMContentLoaded", () => {
    // Initialize the app
    loadLatestProducts();
    setupBrandCardListeners();
    setupSearchButton();
});

// ========================
// LOAD LATEST PRODUCTS
// ========================

async function loadLatestProducts() {
    const grid = document.getElementById("latest-products-grid");
    
    try {
        showLoading(grid);
        
        const response = await fetch("/api/products/latest?limit=80");

        
        if (!response.ok) {
            throw new Error("Failed to fetch products");
        }
        
        const products = await response.json();
        renderProducts(products);
        
    } catch (error) {
        console.error("Error loading products:", error);
        grid.innerHTML = '<p class="loading">Failed to load products. Please try again.</p>';
    }
}

// ========================
// RENDER PRODUCTS
// ========================

function renderProducts(products) {
    const grid = document.getElementById("latest-products-grid");
    
    if (products.length === 0) {
        grid.innerHTML = '<p class="loading">No products available. Click on a brand to crawl products.</p>';
        return;
    }
    
    grid.innerHTML = products.map(product => `
        <div class="product-card">
            <div class="product-image">
                ${product.imageUrl && product.imageUrl !== 'https://example.com/image.jpg' 
                    ? `<img src="${product.imageUrl}" alt="${product.modelName}">` 
                    : '<span>No Image Available</span>'}
            </div>
            <div class="product-info">
                <div class="product-header">
                    <div class="product-brand">${product.brand}</div>
                    <div class="product-model">${product.modelName || 'Unknown Model'}</div>
                </div>
                <div class="product-footer">
                    <div class="product-meta">
                        <div class="product-price">
                            ${product.price ? `$${product.price.toFixed(2)}` : 'Price: N/A'}
                        </div>
                        ${product.rating 
                            ? `<div class="product-rating">⭐ ${product.rating}/5.0</div>` 
                            : `<div class="product-rating">⭐ 0.0/5.0</div>`}
                    </div>
                    <a href="${product.productUrl}" target="_blank" class="product-button">Go to website</a>
                </div>
            </div>
        </div>
    `).join('');
}


// ========================
// BRAND CARD LISTENERS
// ========================

function setupBrandCardListeners() {
    const brandCards = document.querySelectorAll(".brand-card");
    
    brandCards.forEach(card => {
        card.addEventListener("click", async () => {
            const brand = card.getAttribute("data-brand");
            await crawlBrand(brand);
        });
    });
}

// ========================
// CRAWL BRAND
// ========================

async function crawlBrand(brand) {
    const grid = document.getElementById("latest-products-grid");
    
    try {
        showLoading(grid, `Crawling ${brand} products... This may take a few moments.`);
        
        const response = await fetch(`/api/products/crawl/${brand}`, {
            method: "POST"
        });
        
        if (!response.ok) {
            throw new Error(`Failed to crawl ${brand} products`);
        }
        
        const crawledProducts = await response.json();
        
        // Reload all latest products after crawling
        await loadLatestProducts();
        
        alert(`Successfully crawled ${crawledProducts.length} products from ${brand}!`);
        
    } catch (error) {
        console.error("Error crawling brand:", error);
        grid.innerHTML = `<p class="loading">Failed to crawl ${brand} products. Please try again.</p>`;
        
        // Try to reload existing products
        setTimeout(() => loadLatestProducts(), 2000);
    }
}

// ========================
// SEARCH BUTTON
// ========================

function setupSearchButton() {
    const searchButton = document.getElementById("search-button");
    const searchInput = document.getElementById("search-input");
    
    searchButton.addEventListener("click", () => {
        const query = searchInput.value.trim();
        
        if (query) {
            console.log("Search coming soon. Query:", query);
            alert("Search functionality coming soon!");
        } else {
            alert("Please enter a search query.");
        }
    });
    
    // Allow Enter key to trigger search
    searchInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
            searchButton.click();
        }
    });
}

// ========================
// HELPER FUNCTIONS
// ========================

function showLoading(element, message = "Loading...") {
    element.innerHTML = `<p class="loading">${message}</p>`;
}
