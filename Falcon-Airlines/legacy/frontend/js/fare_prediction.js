// Fare Prediction functionality
const FARE_API_URL = 'http://localhost:5000/api/fare-prediction';

// Initialize fare prediction when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeFarePrediction();
});

function initializeFarePrediction() {
    // Check if we're on the fare prediction section
    const fareSection = document.getElementById('fare-prediction');
    if (fareSection) {
        checkFareApiStatus();
        loadFarePredictions();
        
        // Set up periodic updates every 5 minutes
        setInterval(loadFarePredictions, 5 * 60 * 1000);
    }
}

async function checkFareApiStatus() {
    const statusElement = document.getElementById('fare-api-status');
    if (!statusElement) return;

    try {
        const response = await fetch(FARE_API_URL, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (response.ok) {
            statusElement.textContent = 'Backend Online';
            statusElement.className = 'status-indicator status-online';
        } else {
            throw new Error('API not responding');
        }
    } catch (error) {
        console.error('Fare API status check failed:', error);
        statusElement.textContent = 'Backend Offline';
        statusElement.className = 'status-indicator status-offline';
    }
}

async function loadFarePredictions() {
    try {
        const response = await fetch(FARE_API_URL);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        displayFareAlerts(data.alerts);
        displayFareForecast(data.forecast);
        
        // Update API status to online
        const statusElement = document.getElementById('fare-api-status');
        if (statusElement) {
            statusElement.textContent = 'Backend Online';
            statusElement.className = 'status-indicator status-online';
        }
        
    } catch (error) {
        console.error('Failed to load fare predictions:', error);
        displayFareError('Unable to load fare predictions. Please check if the backend server is running.');
        
        // Update API status to offline
        const statusElement = document.getElementById('fare-api-status');
        if (statusElement) {
            statusElement.textContent = 'Backend Offline';
            statusElement.className = 'status-indicator status-offline';
        }
    }
}

function displayFareAlerts(alerts) {
    const alertsContainer = document.getElementById('fareAlerts');
    if (!alertsContainer) return;

    if (!alerts || alerts.length === 0) {
        alertsContainer.innerHTML = '<div class="loading-message">No significant price changes expected in the next 30 days.</div>';
        return;
    }

    let alertsHtml = '';
    alerts.forEach(alert => {
        const alertClass = alert.type === 'drop' ? 'drop' : 'rise';
        const icon = alert.type === 'drop' ? 'fas fa-arrow-down' : 'fas fa-arrow-up';
        const actionText = alert.type === 'drop' ? 'Price Drop Expected' : 'Price Rise Expected';
        const adviceText = alert.type === 'drop' ? 'Good time to book!' : 'Consider booking soon.';
        
        alertsHtml += `
            <div class="alert-card ${alertClass}">
                <div class="alert-icon">
                    <i class="${icon}"></i>
                </div>
                <div class="alert-content">
                    <h4>${actionText}</h4>
                    <p>${new Date(alert.date).toLocaleDateString('en-US', { 
                        weekday: 'long', 
                        year: 'numeric', 
                        month: 'long', 
                        day: 'numeric' 
                    })}</p>
                    <p>Expected change: ₹${alert.amount} • ${adviceText}</p>
                </div>
            </div>
        `;
    });

    alertsContainer.innerHTML = alertsHtml;
}

function displayFareForecast(forecast) {
    const forecastContainer = document.getElementById('fareForecast');
    if (!forecastContainer) return;

    if (!forecast || forecast.length === 0) {
        forecastContainer.innerHTML = '<div class="loading-message">No forecast data available.</div>';
        return;
    }

    let tableHtml = `
        <table class="forecast-table">
            <thead>
                <tr>
                    <th>Date</th>
                    <th>Predicted Price</th>
                    <th>Price Range</th>
                    <th>Trend</th>
                </tr>
            </thead>
            <tbody>
    `;

    forecast.forEach((item, index) => {
        const date = new Date(item.ds);
        const price = Math.round(item.yhat);
        const lowerBound = Math.round(item.yhat_lower);
        const upperBound = Math.round(item.yhat_upper);
        
        // Determine trend compared to previous day
        let trend = 'stable';
        let trendIcon = 'fas fa-minus';
        if (index > 0) {
            const prevPrice = Math.round(forecast[index - 1].yhat);
            if (price > prevPrice + 50) {
                trend = 'up';
                trendIcon = 'fas fa-arrow-up';
            } else if (price < prevPrice - 50) {
                trend = 'down';
                trendIcon = 'fas fa-arrow-down';
            }
        }

        tableHtml += `
            <tr>
                <td>${date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</td>
                <td>₹${price.toLocaleString()}</td>
                <td>₹${lowerBound.toLocaleString()} - ₹${upperBound.toLocaleString()}</td>
                <td>
                    <span class="price-trend ${trend}">
                        <i class="${trendIcon}"></i>
                        ${trend.charAt(0).toUpperCase() + trend.slice(1)}
                    </span>
                </td>
            </tr>
        `;
    });

    tableHtml += `
            </tbody>
        </table>
    `;

    forecastContainer.innerHTML = tableHtml;
}

function displayFareError(message) {
    const alertsContainer = document.getElementById('fareAlerts');
    const forecastContainer = document.getElementById('fareForecast');
    
    const errorHtml = `<div class="error-message">${message}</div>`;
    
    if (alertsContainer) {
        alertsContainer.innerHTML = errorHtml;
    }
    
    if (forecastContainer) {
        forecastContainer.innerHTML = errorHtml;
    }
}

// Add fare prediction to the main navigation handler
document.addEventListener('DOMContentLoaded', function() {
    // Add click handler for fare prediction nav link
    const fareNavLink = document.querySelector('a[href="#fare-prediction"]');
    if (fareNavLink) {
        fareNavLink.addEventListener('click', function() {
            // Load predictions when section is accessed
            setTimeout(loadFarePredictions, 100);
        });
    }
});
