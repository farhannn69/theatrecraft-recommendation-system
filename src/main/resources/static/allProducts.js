// All Products JavaScript

let allProductsData = [];
let filteredProducts = [];
let currentBrand = 'all';
let currentSort = 'default';

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    loadAllProducts();
    setupEventListeners();
});

// Setup Event Listeners
function setupEventListeners() {
    // Brand filter change
    document.getElementById('brand-filter').addEventListener('change', (e) => {
        currentBrand = e.target.value;
        applyFiltersAndSort();
    });

    // Sort options change
    document.getElementById('sort-options').addEventListener('change', (e) => {
        currentSort = e.target.value;
        applyFiltersAndSort();
    });

    // Modal close buttons - these elements exist on page load
    const closeBtn = document.querySelector('.all-products-modal-close');
    const overlay = document.querySelector('.all-products-modal-overlay');

    if (closeBtn) {
        closeBtn.addEventListener('click', closeModal);
    }
    
    if (overlay) {
        overlay.addEventListener('click', closeModal);
    }

    // Close modal on Escape key
    document.addEventListener('keydown', (e) => {
        const modal = document.getElementById('all-products-info-modal');
        if (e.key === 'Escape' && modal && modal.classList.contains('show')) {
            closeModal();
        }
    });
}

// Load All Products from API
async function loadAllProducts() {
    try {
        showLoading();
        const response = await fetch('/api/products');
        
        if (!response.ok) {
            throw new Error('Failed to fetch products');
        }

        allProductsData = await response.json();
        filteredProducts = [...allProductsData];
        
        applyFiltersAndSort();
    } catch (error) {
        console.error('Error loading products:', error);
        showError('Failed to load products. Please try again later.');
    }
}

// Apply Filters and Sort
function applyFiltersAndSort() {
    // Filter by brand
    if (currentBrand === 'all') {
        filteredProducts = [...allProductsData];
    } else {
        filteredProducts = allProductsData.filter(product => product.brand === currentBrand);
    }

    // Sort products
    sortProducts();

    // Render products and update count
    renderProducts();
    updateProductCount();
}

// Sort Products
function sortProducts() {
    switch (currentSort) {
        case 'price-low':
            filteredProducts.sort((a, b) => {
                const priceA = a.price || Number.MAX_SAFE_INTEGER;
                const priceB = b.price || Number.MAX_SAFE_INTEGER;
                return priceA - priceB;
            });
            break;
        
        case 'price-high':
            filteredProducts.sort((a, b) => {
                const priceA = a.price || Number.MIN_SAFE_INTEGER;
                const priceB = b.price || Number.MIN_SAFE_INTEGER;
                if (priceA === Number.MIN_SAFE_INTEGER && priceB === Number.MIN_SAFE_INTEGER) return 0;
                if (priceA === Number.MIN_SAFE_INTEGER) return 1;
                if (priceB === Number.MIN_SAFE_INTEGER) return -1;
                return priceB - priceA;
            });
            break;
        
        case 'rating-high':
            filteredProducts.sort((a, b) => {
                const ratingA = a.rating || Number.MIN_SAFE_INTEGER;
                const ratingB = b.rating || Number.MIN_SAFE_INTEGER;
                if (ratingA === Number.MIN_SAFE_INTEGER && ratingB === Number.MIN_SAFE_INTEGER) return 0;
                if (ratingA === Number.MIN_SAFE_INTEGER) return 1;
                if (ratingB === Number.MIN_SAFE_INTEGER) return -1;
                return ratingB - ratingA;
            });
            break;
        
        case 'rating-low':
            filteredProducts.sort((a, b) => {
                const ratingA = a.rating || Number.MAX_SAFE_INTEGER;
                const ratingB = b.rating || Number.MAX_SAFE_INTEGER;
                return ratingA - ratingB;
            });
            break;
        
        case 'default':
        default:
            // Keep original order (by ID)
            filteredProducts.sort((a, b) => a.id - b.id);
            break;
    }
}

// Render Products
function renderProducts() {
    const grid = document.getElementById('products-grid');
    
    if (filteredProducts.length === 0) {
        grid.innerHTML = `
            <div class="empty-message" style="grid-column: 1 / -1;">
                No products found matching your criteria.
            </div>
        `;
        return;
    }

    grid.innerHTML = filteredProducts.map(product => `
        <div class="product-card">
            <div class="product-image">
                <img src="${product.imageUrl || 'placeholder.jpg'}" alt="${product.modelName}">
            </div>
            <div class="product-info">
                <div class="product-header">
                    <div class="product-brand">${product.brand || 'Unknown'}</div>
                    <div class="product-model">${product.modelName || 'N/A'}</div>
                </div>
            </div>
            <div class="product-footer">
                <div class="product-details">
                    <div class="product-price">$${formatPrice(product.price)}</div>
                    <div class="product-rating">⭐ ${formatRating(product.rating)}</div>
                </div>
                <div class="product-actions">
                    <a href="${product.productUrl || '#'}" target="_blank" class="product-button-small btn-website">Website</a>
                    <button class="product-button-small btn-info" data-product-id="${product.id}">See Info</button>
                </div>
            </div>
        </div>
    `).join('');
    
    // Add event listeners for See Info buttons after rendering
    const infoButtons = document.querySelectorAll('.btn-info');
    console.log('Found info buttons:', infoButtons.length); // Debug log
    
    infoButtons.forEach(button => {
        button.addEventListener('click', (e) => {
            const productId = e.target.getAttribute('data-product-id');
            console.log('Button clicked, product ID:', productId); // Debug log
            showProductModal(productId);
        });
    });
}

// Update Product Count
function updateProductCount() {
    const countElement = document.getElementById('product-count');
    const count = filteredProducts.length;
    countElement.textContent = `${count} product${count !== 1 ? 's' : ''}`;
}

// Show Product Modal
function showProductModal(productId) {
    console.log('showProductModal called with ID:', productId); // Debug log
    
    // Compare as strings since IDs are UUIDs
    const product = allProductsData.find(p => String(p.id) === String(productId));
    
    if (!product) {
        console.error('Product not found:', productId);
        return;
    }

    console.log('Product found:', product); // Debug log

    const modalDetails = document.getElementById('all-products-modal-details');
    
    if (!modalDetails) {
        console.error('Modal details element not found!');
        return;
    }
    
    modalDetails.innerHTML = `
        <div class="all-products-modal-header">
            <img src="${product.imageUrl || 'placeholder.jpg'}" alt="${product.modelName}" class="all-products-modal-image">
            <div class="all-products-modal-brand">${product.brand || 'Unknown'}</div>
            <h2 class="all-products-modal-title">${product.modelName || 'N/A'}</h2>
        </div>
        <div class="all-products-modal-body">
            <table class="all-products-info-table">
                <tr>
                    <td>Brand</td>
                    <td>${product.brand || 'N/A'}</td>
                </tr>
                <tr>
                    <td>Model Name</td>
                    <td>${product.modelName || 'N/A'}</td>
                </tr>
                <tr>
                    <td>Price</td>
                    <td class="all-products-info-price">$${formatPrice(product.price)}</td>
                </tr>
                <tr>
                    <td>Rating</td>
                    <td class="all-products-info-rating">⭐ ${formatRating(product.rating)}</td>
                </tr>
                <tr>
                    <td>Channel</td>
                    <td>${product.channel || 'N/A'}</td>
                </tr>
                <tr>
                    <td>Audio Format</td>
                    <td>${product.audioFormat || 'N/A'}</td>
                </tr>
                <tr>
                    <td>WiFi Format</td>
                    <td>${product.wifiFormat || 'N/A'}</td>
                </tr>
                <tr>
                    <td>Bluetooth Version</td>
                    <td>${product.bluetoothVersion || 'N/A'}</td>
                </tr>
                <tr>
                    <td>Weight</td>
                    <td>${product.weightKg ? product.weightKg : 'N/A'}</td>
                </tr>
                <tr>
                    <td>Power</td>
                    <td>${product.power || 'N/A'}</td>
                </tr>
            </table>
        </div>
    `;

    console.log('Opening modal...'); // Debug log
    openModal();
}

// Open Modal
function openModal() {
    const modal = document.getElementById('all-products-info-modal');
    
    if (!modal) {
        console.error('Modal element not found!');
        return;
    }
    
    console.log('Modal element found, adding show class'); // Debug log
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
    
    console.log('Modal classes:', modal.className); // Debug log
}

// Close Modal
function closeModal() {
    const modal = document.getElementById('all-products-info-modal');
    
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
}

// Show Loading Message
function showLoading() {
    const grid = document.getElementById('products-grid');
    grid.innerHTML = `
        <div class="loading-message" style="grid-column: 1 / -1;">
            Loading products...
        </div>
    `;
    
    document.getElementById('product-count').textContent = 'Loading...';
}

// Show Error Message
function showError(message) {
    const grid = document.getElementById('products-grid');
    grid.innerHTML = `
        <div class="empty-message" style="grid-column: 1 / -1;">
            ${message}
        </div>
    `;
    
    document.getElementById('product-count').textContent = '0 products';
}

// Format Price
function formatPrice(price) {
    if (price == null || price === undefined) {
        return 'N/A';
    }
    return parseFloat(price).toFixed(2);
}

// Format Rating
function formatRating(rating) {
    if (rating == null || rating === undefined) {
        return 'N/A';
    }
    return parseFloat(rating).toFixed(1);
}