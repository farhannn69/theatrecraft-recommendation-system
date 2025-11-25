// ========================
// THEATRECRAFT COMPARATOR.JS
// ========================

// State management
let selectedProduct1 = null;
let selectedProduct2 = null;
let autocompleteTimeout1 = null;
let autocompleteTimeout2 = null;

// DOM Elements
const product1Section = document.getElementById('product1-section');
const product2Section = document.getElementById('product2-section');
const comparisonSection = document.getElementById('comparison-section');
const resetButton = document.getElementById('reset-button');

// Product 1 elements
const searchInput1 = document.getElementById('search-input-1');
const searchButton1 = document.getElementById('search-button-1');
const autocompleteDropdown1 = document.getElementById('autocomplete-dropdown-1');
const spellChecker1 = document.getElementById('spell-checker-1');
const selectedProductDisplay1 = document.getElementById('selected-product-1');

// Product 2 elements
const searchInput2 = document.getElementById('search-input-2');
const searchButton2 = document.getElementById('search-button-2');
const autocompleteDropdown2 = document.getElementById('autocomplete-dropdown-2');
const spellChecker2 = document.getElementById('spell-checker-2');
const selectedProductDisplay2 = document.getElementById('selected-product-2');
const errorMessage2 = document.getElementById('error-message-2');

// Comparison table
const comparisonTbody = document.getElementById('comparison-tbody');

// ========================
// INITIALIZATION
// ========================

document.addEventListener("DOMContentLoaded", () => {
    setupProduct1Search();
    setupProduct2Search();
    setupResetButton();
});

// ========================
// PRODUCT 1 SEARCH SETUP
// ========================

function setupProduct1Search() {
    // Search button click
    searchButton1.addEventListener("click", () => searchProduct(1));
    
    // Enter key to search
    searchInput1.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
            searchProduct(1);
        }
    });
    
    // Autocomplete on input
    searchInput1.addEventListener("input", (e) => {
        const query = e.target.value.trim();
        
        // Clear previous timeout
        if (autocompleteTimeout1) {
            clearTimeout(autocompleteTimeout1);
        }
        
        // Hide spell checker when typing
        hideSpellChecker(1);
        
        // Hide dropdown if query is less than 3 characters
        if (query.length < 3) {
            hideAutocomplete(1);
            return;
        }
        
        // Debounce - wait 300ms after user stops typing
        autocompleteTimeout1 = setTimeout(async () => {
            try {
                const response = await fetch(`/api/search/autocomplete?prefix=${encodeURIComponent(query)}`);
                
                if (!response.ok) {
                    throw new Error("Autocomplete failed");
                }
                
                const result = await response.json();
                
                if (result.suggestions && result.suggestions.length > 0) {
                    showAutocomplete(1, result.suggestions);
                } else {
                    hideAutocomplete(1);
                }
                
            } catch (error) {
                console.error("Error in autocomplete:", error);
                hideAutocomplete(1);
            }
        }, 300);
    });
    
    // Hide autocomplete when clicking outside
    document.addEventListener("click", (e) => {
        if (!e.target.closest('#product1-section .search-wrapper')) {
            hideAutocomplete(1);
            hideSpellChecker(1);
        }
    });
}

// ========================
// PRODUCT 2 SEARCH SETUP
// ========================

function setupProduct2Search() {
    // Search button click
    searchButton2.addEventListener("click", () => searchProduct(2));
    
    // Enter key to search
    searchInput2.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
            searchProduct(2);
        }
    });
    
    // Autocomplete on input
    searchInput2.addEventListener("input", (e) => {
        const query = e.target.value.trim();
        
        // Clear previous timeout
        if (autocompleteTimeout2) {
            clearTimeout(autocompleteTimeout2);
        }
        
        // Hide spell checker and error when typing
        hideSpellChecker(2);
        hideErrorMessage();
        
        // Hide dropdown if query is less than 3 characters
        if (query.length < 3) {
            hideAutocomplete(2);
            return;
        }
        
        // Debounce - wait 300ms after user stops typing
        autocompleteTimeout2 = setTimeout(async () => {
            try {
                const response = await fetch(`/api/search/autocomplete?prefix=${encodeURIComponent(query)}`);
                
                if (!response.ok) {
                    throw new Error("Autocomplete failed");
                }
                
                const result = await response.json();
                
                if (result.suggestions && result.suggestions.length > 0) {
                    showAutocomplete(2, result.suggestions);
                } else {
                    hideAutocomplete(2);
                }
                
            } catch (error) {
                console.error("Error in autocomplete:", error);
                hideAutocomplete(2);
            }
        }, 300);
    });
    
    // Hide autocomplete when clicking outside
    document.addEventListener("click", (e) => {
        if (!e.target.closest('#product2-section .search-wrapper')) {
            hideAutocomplete(2);
            hideSpellChecker(2);
        }
    });
}

// ========================
// SEARCH PRODUCT
// ========================

async function searchProduct(productNumber) {
    const searchInput = productNumber === 1 ? searchInput1 : searchInput2;
    const query = searchInput.value.trim();
    
    if (!query) {
        alert("Please enter a search query.");
        return;
    }
    
    // Hide autocomplete and error
    hideAutocomplete(productNumber);
    hideSpellChecker(productNumber);
    if (productNumber === 2) hideErrorMessage();
    
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
            // Check for duplicate selection in Product 2
            if (productNumber === 2 && selectedProduct1 && 
                result.matchedProduct.id === selectedProduct1.id) {
                showErrorMessage();
                return;
            }
            
            // Select the product
            selectProduct(productNumber, result.matchedProduct);
        } else if (result.suggestions && result.suggestions.length > 0) {
            // Show spell checker suggestions
            showSpellChecker(productNumber, result.suggestions, result.message);
        } else {
            // No results found
            alert(result.message || "No products found");
        }
        
    } catch (error) {
        console.error("Error searching:", error);
        alert("Search failed. Please try again.");
    }
}

// ========================
// AUTOCOMPLETE
// ========================

function showAutocomplete(productNumber, products) {
    const dropdown = productNumber === 1 ? autocompleteDropdown1 : autocompleteDropdown2;
    
    dropdown.innerHTML = products.map(product => `
        <div class="autocomplete-item" data-product-name="${product.modelName}">
            <div class="autocomplete-product-name">${product.modelName}</div>
            <div class="autocomplete-product-brand">${product.brand}</div>
        </div>
    `).join('');
    
    dropdown.classList.add('show');
    
    // Add click listeners to autocomplete items
    dropdown.querySelectorAll('.autocomplete-item').forEach(item => {
        item.addEventListener('click', async () => {
            const productName = item.getAttribute('data-product-name');
            await selectProductByName(productNumber, productName);
            hideAutocomplete(productNumber);
        });
    });
}

function hideAutocomplete(productNumber) {
    const dropdown = productNumber === 1 ? autocompleteDropdown1 : autocompleteDropdown2;
    dropdown.classList.remove('show');
    dropdown.innerHTML = '';
}

// ========================
// SPELL CHECKER
// ========================

function showSpellChecker(productNumber, suggestions, message) {
    const spellChecker = productNumber === 1 ? spellChecker1 : spellChecker2;
    
    spellChecker.innerHTML = `
        <p><strong>${message}</strong></p>
        <div>
            ${suggestions.map(suggestion => 
                `<span class="spell-suggestion" data-suggestion="${suggestion}">${suggestion}</span>`
            ).join('')}
        </div>
    `;
    
    spellChecker.classList.add('show');
    
    // Add click listeners to suggestions
    spellChecker.querySelectorAll('.spell-suggestion').forEach(span => {
        span.addEventListener('click', async () => {
            const suggestion = span.getAttribute('data-suggestion');
            await selectProductByName(productNumber, suggestion);
            hideSpellChecker(productNumber);
        });
    });
}

function hideSpellChecker(productNumber) {
    const spellChecker = productNumber === 1 ? spellChecker1 : spellChecker2;
    spellChecker.classList.remove('show');
    spellChecker.innerHTML = '';
}

// ========================
// ERROR MESSAGE
// ========================

function showErrorMessage() {
    errorMessage2.classList.add('show');
}

function hideErrorMessage() {
    errorMessage2.classList.remove('show');
}

// ========================
// SELECT PRODUCT BY NAME
// ========================

async function selectProductByName(productNumber, productName) {
    try {
        const response = await fetch(`/api/search/product?name=${encodeURIComponent(productName)}`);
        
        if (!response.ok) {
            throw new Error("Product not found");
        }
        
        const product = await response.json();
        
        // Check for duplicate selection in Product 2
        if (productNumber === 2 && selectedProduct1 && product.id === selectedProduct1.id) {
            showErrorMessage();
            return;
        }
        
        selectProduct(productNumber, product);
        
    } catch (error) {
        console.error("Error selecting product:", error);
        alert("Failed to load product details.");
    }
}

// ========================
// SELECT PRODUCT
// ========================

function selectProduct(productNumber, product) {
    if (productNumber === 1) {
        selectedProduct1 = product;
        
        // Lock Product 1 input and button
        searchInput1.disabled = true;
        searchButton1.disabled = true;
        product1Section.classList.add('locked');
        
        // Display selected product
        displaySelectedProduct(1, product);
        
        // Show Product 2 section
        product2Section.classList.remove('hidden');
        
        // Show reset button
        resetButton.classList.add('show');
        
        // Focus on Product 2 input
        searchInput2.focus();
        
    } else {
        selectedProduct2 = product;
        
        // Lock Product 2 input and button
        searchInput2.disabled = true;
        searchButton2.disabled = true;
        product2Section.classList.add('locked');
        
        // Display selected product
        displaySelectedProduct(2, product);
        
        // Render comparison table
        renderComparisonTable();
    }
}

// ========================
// DISPLAY SELECTED PRODUCT
// ========================

function displaySelectedProduct(productNumber, product) {
    const display = productNumber === 1 ? selectedProductDisplay1 : selectedProductDisplay2;
    
    display.innerHTML = `
        <div class="selected-product-info">
            <img src="${product.imageUrl || 'placeholder.png'}" alt="${product.modelName}" class="selected-product-image">
            <div class="selected-product-details">
                <h3>${product.brand}</h3>
                <p>${product.modelName}</p>
            </div>
        </div>
    `;
    
    display.classList.add('show');
}

// ========================
// RENDER COMPARISON TABLE
// ========================

function renderComparisonTable() {
    const product1 = selectedProduct1;
    const product2 = selectedProduct2;
    
    comparisonTbody.innerHTML = `
        <!-- Brand -->
        <tr>
            <td class="feature-label">Brand</td>
            <td class="product-value">${product1.brand || 'N/A'}</td>
            <td class="product-value">${product2.brand || 'N/A'}</td>
        </tr>
        
        <!-- Model Name -->
        <tr>
            <td class="feature-label">Model Name</td>
            <td class="product-value">${product1.modelName || 'N/A'}</td>
            <td class="product-value">${product2.modelName || 'N/A'}</td>
        </tr>
        
        <!-- Image -->
        <tr>
            <td class="feature-label">Product Image</td>
            <td class="product-image-cell">
                ${product1.imageUrl && product1.imageUrl !== 'https://example.com/image.jpg' 
                    ? `<img src="${product1.imageUrl}" alt="${product1.modelName}">` 
                    : '<span>No Image Available</span>'}
            </td>
            <td class="product-image-cell">
                ${product2.imageUrl && product2.imageUrl !== 'https://example.com/image.jpg' 
                    ? `<img src="${product2.imageUrl}" alt="${product2.modelName}">` 
                    : '<span>No Image Available</span>'}
            </td>
        </tr>
        
        <!-- Price -->
        <tr>
            <td class="feature-label">Price</td>
            <td class="product-value">
                <span class="product-price">${product1.price ? `$${product1.price.toFixed(2)}` : 'N/A'}</span>
            </td>
            <td class="product-value">
                <span class="product-price">${product2.price ? `$${product2.price.toFixed(2)}` : 'N/A'}</span>
            </td>
        </tr>
        
        <!-- Rating -->
        <tr>
            <td class="feature-label">Rating</td>
            <td class="product-value">
                <span class="product-rating">${product1.rating ? `⭐ ${product1.rating}/5.0` : 'N/A'}</span>
            </td>
            <td class="product-value">
                <span class="product-rating">${product2.rating ? `⭐ ${product2.rating}/5.0` : 'N/A'}</span>
            </td>
        </tr>
        
        <!-- Channel -->
        <tr>
            <td class="feature-label">Channel</td>
            <td class="product-value">${product1.channel || 'N/A'}</td>
            <td class="product-value">${product2.channel || 'N/A'}</td>
        </tr>
        
        <!-- Audio Format -->
        <tr>
            <td class="feature-label">Audio Format</td>
            <td class="product-value">${product1.audioFormat || 'N/A'}</td>
            <td class="product-value">${product2.audioFormat || 'N/A'}</td>
        </tr>
        
        <!-- WiFi Format -->
        <tr>
            <td class="feature-label">WiFi Format</td>
            <td class="product-value">${product1.wifiFormat || 'N/A'}</td>
            <td class="product-value">${product2.wifiFormat || 'N/A'}</td>
        </tr>
        
        <!-- Bluetooth Version -->
        <tr>
            <td class="feature-label">Bluetooth Version</td>
            <td class="product-value">${product1.bluetoothVersion || 'N/A'}</td>
            <td class="product-value">${product2.bluetoothVersion || 'N/A'}</td>
        </tr>
        
        <!-- Weight -->
        <tr>
            <td class="feature-label">Weight</td>
            <td class="product-value">${product1.weightKg || 'N/A'}</td>
            <td class="product-value">${product2.weightKg || 'N/A'}</td>
        </tr>
        
        <!-- Power -->
        <tr>
            <td class="feature-label">Power</td>
            <td class="product-value">${product1.power || 'N/A'}</td>
            <td class="product-value">${product2.power || 'N/A'}</td>
        </tr>
        
        <!-- Product Link -->
        <tr>
            <td class="feature-label">Product Link</td>
            <td class="product-value">
                <a href="${product1.productUrl}" target="_blank" class="product-link">Visit Website</a>
            </td>
            <td class="product-value">
                <a href="${product2.productUrl}" target="_blank" class="product-link">Visit Website</a>
            </td>
        </tr>
    `;
    
    // Show comparison section
    comparisonSection.classList.add('show');
    
    // Scroll to comparison table
    comparisonSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ========================
// RESET BUTTON
// ========================

function setupResetButton() {
    resetButton.addEventListener('click', () => {
        // Reset state
        selectedProduct1 = null;
        selectedProduct2 = null;
        
        // Unlock and clear Product 1
        searchInput1.value = '';
        searchInput1.disabled = false;
        searchButton1.disabled = false;
        product1Section.classList.remove('locked');
        selectedProductDisplay1.classList.remove('show');
        selectedProductDisplay1.innerHTML = '';
        hideAutocomplete(1);
        hideSpellChecker(1);
        
        // Hide and clear Product 2
        searchInput2.value = '';
        searchInput2.disabled = false;
        searchButton2.disabled = false;
        product2Section.classList.remove('locked');
        product2Section.classList.add('hidden');
        selectedProductDisplay2.classList.remove('show');
        selectedProductDisplay2.innerHTML = '';
        hideAutocomplete(2);
        hideSpellChecker(2);
        hideErrorMessage();
        
        // Hide comparison section
        comparisonSection.classList.remove('show');
        comparisonTbody.innerHTML = '';
        
        // Hide reset button
        resetButton.classList.remove('show');
        
        // Focus on Product 1 input
        searchInput1.focus();
    });
}
