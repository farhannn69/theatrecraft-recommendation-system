// ========================
// THEATRECRAFT APP.JS
// ========================

document.addEventListener("DOMContentLoaded", () => {
    // Initialize the app
    loadLatestProducts();
    setupBrandCardListeners();
    setupSearchButton();
    setupAutocomplete();
    setupModal();
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
    const spellChecker = document.getElementById("spell-checker");
    
    searchButton.addEventListener("click", async () => {
        const query = searchInput.value.trim();
        
        if (!query) {
            alert("Please enter a search query.");
            return;
        }
        
        // Hide autocomplete and spell checker before search
        hideAutocomplete();
        hideSpellChecker();
        
        try {
            // Call search API
            const response = await fetch(`/api/search?query=${encodeURIComponent(query)}`, {
                method: "POST"
            });
            
            if (!response.ok) {
                throw new Error("Search failed");
            }
            
            const result = await response.json();
            
            if (result.exactMatch && result.matchedProduct) {
                // Show product in modal
                showProductModal(result.matchedProduct);
            } else if (result.suggestions && result.suggestions.length > 0) {
                // Show spell checker suggestions
                showSpellChecker(result.suggestions, result.message);
            } else {
                // No results found
                alert(result.message || "No products found");
            }
            
        } catch (error) {
            console.error("Error searching:", error);
            alert("Search failed. Please try again.");
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
// AUTOCOMPLETE
// ========================

let autocompleteTimeout = null;

function setupAutocomplete() {
    const searchInput = document.getElementById("search-input");
    const dropdown = document.getElementById("autocomplete-dropdown");
    
    searchInput.addEventListener("input", (e) => {
        const query = e.target.value.trim();
        
        // Clear previous timeout
        if (autocompleteTimeout) {
            clearTimeout(autocompleteTimeout);
        }
        
        // Hide spell checker when user is typing (autocomplete takes priority)
        hideSpellChecker();
        
        // Hide dropdown if query is less than 3 characters
        if (query.length < 3) {
            hideAutocomplete();
            return;
        }
        
        // Debounce - wait 300ms after user stops typing
        autocompleteTimeout = setTimeout(async () => {
            try {
                const response = await fetch(`/api/search/autocomplete?prefix=${encodeURIComponent(query)}`);
                
                if (!response.ok) {
                    throw new Error("Autocomplete failed");
                }
                
                const result = await response.json();
                
                if (result.suggestions && result.suggestions.length > 0) {
                    showAutocomplete(result.suggestions);
                } else {
                    hideAutocomplete();
                }
                
            } catch (error) {
                console.error("Error in autocomplete:", error);
                hideAutocomplete();
            }
        }, 300);
    });
    
    // Hide autocomplete and spell checker when clicking outside
    document.addEventListener("click", (e) => {
        if (!e.target.closest(".search-container")) {
            hideAutocomplete();
            hideSpellChecker();
        }
    });
}

function showAutocomplete(products) {
    const dropdown = document.getElementById("autocomplete-dropdown");
    
    dropdown.innerHTML = products.map(product => `
        <div class="autocomplete-item" data-product-name="${product.modelName}">
            <div class="autocomplete-product-name">${product.modelName}</div>
            <div class="autocomplete-product-brand">${product.brand}</div>
        </div>
    `).join('');
    
    dropdown.style.display = "block";
    
    // Add click listeners to autocomplete items
    dropdown.querySelectorAll(".autocomplete-item").forEach(item => {
        item.addEventListener("click", async () => {
            const productName = item.getAttribute("data-product-name");
            await selectProduct(productName);
            hideAutocomplete();
        });
    });
}

function hideAutocomplete() {
    const dropdown = document.getElementById("autocomplete-dropdown");
    dropdown.style.display = "none";
    dropdown.innerHTML = "";
}

// ========================
// SPELL CHECKER
// ========================

function showSpellChecker(suggestions, message) {
    const spellChecker = document.getElementById("spell-checker");
    const dropdown = document.getElementById("autocomplete-dropdown");
    
    // Don't show spell checker if autocomplete is active
    if (dropdown.style.display === "block") {
        return;
    }
    
    spellChecker.innerHTML = `
        <div class="spell-checker-message">${message}</div>
        <div class="spell-checker-suggestions">
            ${suggestions.map(suggestion => 
                `<span class="spell-checker-suggestion" data-suggestion="${suggestion}">${suggestion}</span>`
            ).join('')}
        </div>
    `;
    
    spellChecker.classList.add("show");
    
    // Add click listeners to suggestions
    spellChecker.querySelectorAll(".spell-checker-suggestion").forEach(span => {
        span.addEventListener("click", async () => {
            const suggestion = span.getAttribute("data-suggestion");
            await selectProduct(suggestion);
            hideSpellChecker(); // Auto-hide after selection
        });
    });
}

function hideSpellChecker() {
    const spellChecker = document.getElementById("spell-checker");
    spellChecker.classList.remove("show");
    spellChecker.innerHTML = "";
}

// ========================
// SELECT PRODUCT (from autocomplete or spell checker)
// ========================

async function selectProduct(productName) {
    try {
        const response = await fetch(`/api/search/product?name=${encodeURIComponent(productName)}`);
        
        if (!response.ok) {
            throw new Error("Product not found");
        }
        
        const product = await response.json();
        showProductModal(product);
        
    } catch (error) {
        console.error("Error selecting product:", error);
        alert("Failed to load product details.");
    }
}

// ========================
// PRODUCT MODAL
// ========================

function setupModal() {
    const modal = document.getElementById("product-modal");
    const closeButton = modal.querySelector(".modal-close");
    const overlay = modal.querySelector(".modal-overlay");
    
    // Close modal when clicking X button
    closeButton.addEventListener("click", () => {
        modal.style.display = "none";
    });
    
    // Close modal when clicking overlay
    overlay.addEventListener("click", () => {
        modal.style.display = "none";
    });
    
    // Close modal with Escape key
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && modal.style.display === "flex") {
            modal.style.display = "none";
        }
    });
}

function showProductModal(product) {
    const modal = document.getElementById("product-modal");
    const modalCard = document.getElementById("modal-product-card");
    
    // Render product card in modal
    modalCard.innerHTML = `
        <div class="product-card modal-product-card">
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
    `;
    
    modal.style.display = "flex";
}

// ========================
// HELPER FUNCTIONS
// ========================

function showLoading(element, message = "Loading...") {
    element.innerHTML = `<p class="loading">${message}</p>`;
}
