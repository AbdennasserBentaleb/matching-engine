// DOM Elements
const form = document.getElementById('order-form');
const formMsg = document.getElementById('form-msg');
const tickerSelect = document.getElementById('ticker');
const submitBtn = document.getElementById('submit-btn');
const currentTickerDisplay = document.getElementById('current-ticker-display');
const spreadValue = document.getElementById('spread-value');
const bidsBody = document.getElementById('bids-body');
const asksBody = document.getElementById('asks-body');
const tradesBody = document.getElementById('trades-body');

// State
let currentTicker = tickerSelect.value;
let recentTrades = [];
const MAX_TRADES = 50;

// API Base URL
const API_BASE = '/api/v1';

// Initialization
function init() {
    updateTickerDisplay();

    // Listen for ticker change
    tickerSelect.addEventListener('change', (e) => {
        currentTicker = e.target.value;
        updateTickerDisplay();
        fetchOrderBook(); // Fetch immediately on change
    });

    // Handle form submission
    form.addEventListener('submit', handleOrderSubmit);

    // Listen to radio buttons to update submit button color
    document.querySelectorAll('input[name="side"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            if (e.target.value === 'BUY') {
                submitBtn.className = 'btn-submit btn-buy';
            } else {
                submitBtn.className = 'btn-submit btn-sell';
            }
        });
    });

    // Start polling the order book
    fetchOrderBook();
    setInterval(fetchOrderBook, 1000); // Poll every 1 second
}

function updateTickerDisplay() {
    currentTickerDisplay.textContent = currentTicker;
}

// Format numbers
function formatPrice(price) {
    return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(price);
}

function formatQty(qty) {
    return new Intl.NumberFormat('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 4 }).format(qty);
}

function formatTime(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' }) +
        '.' + date.getMilliseconds().toString().padStart(3, '0');
}

// Utility for formatting time
function getCurrentTimeFormatted() {
    const now = new Date();
    return formatTime(now.getTime());
}

// API Interactions
async function handleOrderSubmit(e) {
    e.preventDefault();

    const ticker = currentTicker;
    const side = document.querySelector('input[name="side"]:checked').value;
    const price = parseFloat(document.getElementById('price').value);
    const quantity = parseFloat(document.getElementById('quantity').value);

    const payload = {
        ticker,
        side,
        price,
        quantity
    };

    try {
        submitBtn.disabled = true;

        const response = await fetch(`${API_BASE}/orders`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`Error: ${response.status}`);
        }

        const trades = await response.json();

        // Show success message
        showMessage(`Order submitted successfully!`, 'success');

        // Process any returned trades
        if (trades && trades.length > 0) {
            trades.forEach(addTrade);
        }

        // Clear inputs
        document.getElementById('price').value = '';
        document.getElementById('quantity').value = '';

        // Optimistically fetch the orderbook right away
        fetchOrderBook();

    } catch (error) {
        console.error('Failed to submit order:', error);
        showMessage('Failed to submit order. Please try again.', 'error');
    } finally {
        submitBtn.disabled = false;
    }
}

function showMessage(text, type) {
    formMsg.textContent = text;
    formMsg.className = `form-msg ${type}`;

    setTimeout(() => {
        formMsg.style.opacity = '0';
        setTimeout(() => {
            formMsg.textContent = '';
            formMsg.className = 'form-msg';
            formMsg.style.opacity = '1';
        }, 300);
    }, 3000);
}

async function fetchOrderBook() {
    try {
        const response = await fetch(`${API_BASE}/orderbook/${currentTicker}`);
        if (!response.ok) return;

        const orderbook = await response.json();
        renderOrderBook(orderbook);
    } catch (error) {
        console.error('Failed to fetch orderbook:', error);
    }
}

function renderOrderBook(book) {
    const asksObj = book.asks || {};
    const bidsObj = book.bids || {};

    const asks = Object.entries(asksObj).map(([price, quantity]) => ({ price: parseFloat(price), quantity }));
    const bids = Object.entries(bidsObj).map(([price, quantity]) => ({ price: parseFloat(price), quantity }));

    // Sort asks descending (highest price at top)
    const sortedAsks = [...asks].sort((a, b) => b.price - a.price);

    // Sort bids descending (highest price at top)
    const sortedBids = [...bids].sort((a, b) => b.price - a.price);

    // Calculate max volume for depth bars
    let maxVolume = 0;

    // Calculate totals
    let askTotal = 0;
    const asksWithTotals = sortedAsks.map(ask => {
        askTotal += ask.quantity;
        if (askTotal > maxVolume) maxVolume = askTotal;
        return { ...ask, total: askTotal };
    });

    let bidTotal = 0;
    const bidsWithTotals = sortedBids.map(bid => {
        bidTotal += bid.quantity;
        if (bidTotal > maxVolume) maxVolume = bidTotal;
        return { ...bid, total: bidTotal };
    });

    // Render Asks
    if (asksWithTotals.length === 0) {
        asksBody.innerHTML = '<div class="empty-state" style="padding: 10px;">No asks</div>';
    } else {
        asksBody.innerHTML = '';
        asksWithTotals.forEach(ask => {
            const depthPercentage = maxVolume > 0 ? (ask.total / maxVolume) * 100 : 0;
            const row = document.createElement('div');
            row.className = 'book-row ask-row';
            row.innerHTML = `
                <div class="depth-bar" style="width: ${depthPercentage}%"></div>
                <span class="price">${formatPrice(ask.price)}</span>
                <span class="size">${formatQty(ask.quantity)}</span>
                <span class="total">${formatQty(ask.total)}</span>
            `;
            // Add click-to-fill capability
            row.addEventListener('click', () => {
                document.getElementById('price').value = ask.price;
                document.getElementById('side-buy').checked = true;
                submitBtn.className = 'btn-submit btn-buy';
            });
            asksBody.appendChild(row);
        });
    }

    // Calculate and render spread
    if (sortedAsks.length > 0 && sortedBids.length > 0) {
        // Lowest ask and highest bid
        const lowestAsk = sortedAsks[sortedAsks.length - 1].price;
        const highestBid = sortedBids[0].price;
        const spread = lowestAsk - highestBid;
        spreadValue.textContent = formatPrice(spread);
    } else {
        spreadValue.textContent = '-';
    }

    // Render Bids
    if (bidsWithTotals.length === 0) {
        bidsBody.innerHTML = '<div class="empty-state" style="padding: 10px;">No bids</div>';
    } else {
        bidsBody.innerHTML = '';
        bidsWithTotals.forEach(bid => {
            const depthPercentage = maxVolume > 0 ? (bid.total / maxVolume) * 100 : 0;
            const row = document.createElement('div');
            row.className = 'book-row bid-row';
            row.innerHTML = `
                <div class="depth-bar" style="width: ${depthPercentage}%"></div>
                <span class="price">${formatPrice(bid.price)}</span>
                <span class="size">${formatQty(bid.quantity)}</span>
                <span class="total">${formatQty(bid.total)}</span>
            `;
            // Add click-to-fill capability
            row.addEventListener('click', () => {
                document.getElementById('price').value = bid.price;
                document.getElementById('side-sell').checked = true;
                submitBtn.className = 'btn-submit btn-sell';
            });
            bidsBody.appendChild(row);
        });
    }
}

function addTrade(trade) {
    const isBuy = Math.random() > 0.5;
    const sideClass = isBuy ? 'buy' : 'sell';

    const row = document.createElement('div');
    row.className = `trade-row ${sideClass}`;
    row.innerHTML = `
        <span class="time">${getCurrentTimeFormatted()}</span>
        <span class="price">${formatPrice(trade.price)}</span>
        <span class="size">${formatQty(trade.quantity)}</span>
    `;

    // Remove empty state if present
    const emptyState = tradesBody.querySelector('.empty-state');
    if (emptyState) {
        emptyState.remove();
    }

    // Insert at top
    tradesBody.insertBefore(row, tradesBody.firstChild);

    // Keep only max trades
    if (tradesBody.children.length > MAX_TRADES) {
        tradesBody.removeChild(tradesBody.lastChild);
    }
}

// Start application
document.addEventListener('DOMContentLoaded', init);
