// Flight Delay Prediction Integration
class DelayPredictor {
    constructor() {
        this.apiUrl = 'http://localhost:5000/api';
        this.isModelLoaded = false;
        this.checkModelStatus();
    }

    async checkModelStatus() {
        try {
            const response = await fetch(`${this.apiUrl}/health`);
            const data = await response.json();
            this.isModelLoaded = data.model_loaded;
            console.log('Delay prediction model status:', data);
        } catch (error) {
            console.error('Error checking model status:', error);
            this.isModelLoaded = false;
        }
    }

    async predictDelay(flightData) {
        if (!this.isModelLoaded) {
            throw new Error('Delay prediction model is not available');
        }

        try {
            const response = await fetch(`${this.apiUrl}/predict-delay`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(flightData)
            });

            if (!response.ok) {
                throw new Error(`API error: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error predicting delay:', error);
            throw error;
        }
    }

    async batchPredict(flightsData) {
        if (!this.isModelLoaded) {
            throw new Error('Delay prediction model is not available');
        }

        try {
            const response = await fetch(`${this.apiUrl}/batch-predict`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(flightsData)
            });

            if (!response.ok) {
                throw new Error(`API error: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error batch predicting delays:', error);
            throw error;
        }
    }

    createDelayIndicator(prediction) {
        const indicator = document.createElement('div');
        indicator.className = `delay-indicator ${prediction.risk_level.toLowerCase()}`;
        
        const riskColor = {
            'low': '#28a745',
            'medium': '#ffc107',
            'high': '#dc3545'
        };

        indicator.innerHTML = `
            <div class="delay-risk-badge" style="background-color: ${riskColor[prediction.risk_level.toLowerCase()]}">
                <span class="risk-score">${prediction.risk_score}</span>
                <span class="risk-label">${prediction.risk_level} Risk</span>
            </div>
            <div class="delay-details">
                <div class="probability">Delay Probability: ${(prediction.delay_probability * 100).toFixed(1)}%</div>
                <div class="recommendations">
                    ${prediction.recommendations ? prediction.recommendations.map(rec => `<div class="recommendation">• ${rec}</div>`).join('') : ''}
                </div>
            </div>
        `;

        return indicator;
    }

    createDetailedDelayInfo(prediction) {
        const modal = document.createElement('div');
        modal.className = 'delay-prediction-modal';
        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h3>Flight Delay Prediction</h3>
                    <span class="close-modal">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="flight-info">
                        <h4>${prediction.flight_info.route}</h4>
                        <p>Airline: ${prediction.flight_info.airline}</p>
                        <p>Departure: ${new Date(prediction.flight_info.departure_time).toLocaleString()}</p>
                    </div>
                    <div class="risk-summary">
                        <div class="risk-circle ${prediction.risk_level.toLowerCase()}">
                            <div class="risk-score">${prediction.risk_score}</div>
                            <div class="risk-label">${prediction.risk_level}</div>
                        </div>
                        <div class="risk-description">
                            <p>Delay Probability: <strong>${(prediction.delay_probability * 100).toFixed(1)}%</strong></p>
                        </div>
                    </div>
                    <div class="recommendations">
                        <h4>Recommendations:</h4>
                        <ul>
                            ${prediction.recommendations.map(rec => `<li>${rec}</li>`).join('')}
                        </ul>
                    </div>
                </div>
            </div>
        `;

        // Add close functionality
        modal.querySelector('.close-modal').onclick = () => {
            modal.remove();
        };

        modal.onclick = (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        };

        return modal;
    }

    // Integration with existing flight booking/search
    async enhanceFlightResults(flights) {
        if (!Array.isArray(flights) || flights.length === 0) {
            return flights;
        }

        try {
            // Prepare flight data for batch prediction
            const flightDataForPrediction = flights.map(flight => ({
                airline: flight.airline || 'AA',
                origin_airport: flight.origin || flight.from,
                dest_airport: flight.destination || flight.to,
                departure_time: flight.departureTime || new Date().toISOString(),
                distance: flight.distance || 1000,
                temperature: 70, // Default values - in real app, get from weather API
                wind_speed: 10,
                visibility: 10,
                precipitation: 0,
                origin_congestion: 0.5,
                dest_congestion: 0.5
            }));

            const predictions = await this.batchPredict(flightDataForPrediction);
            
            // Enhance flights with prediction data
            return flights.map((flight, index) => {
                const prediction = predictions.predictions.find(p => p.flight_index === index);
                if (prediction && !prediction.error) {
                    flight.delayPrediction = prediction;
                }
                return flight;
            });

        } catch (error) {
            console.error('Error enhancing flights with delay predictions:', error);
            return flights; // Return original flights if prediction fails
        }
    }

    // Add delay prediction to flight booking form
    async addDelayPredictionToBooking() {
        const bookingForms = document.querySelectorAll('.booking-form, .flight-search-form');
        
        bookingForms.forEach(form => {
            // Add a delay prediction button
            const predictButton = document.createElement('button');
            predictButton.type = 'button';
            predictButton.className = 'btn btn-info delay-predict-btn';
            predictButton.textContent = 'Check Delay Risk';
            predictButton.style.marginTop = '10px';

            predictButton.onclick = async () => {
                try {
                    // Extract form data
                    const formData = new FormData(form);
                    const flightData = {
                        airline: formData.get('airline') || 'AA',
                        origin_airport: formData.get('from') || formData.get('origin'),
                        dest_airport: formData.get('to') || formData.get('destination'),
                        departure_time: formData.get('departureDate') + 'T' + (formData.get('departureTime') || '08:00') + ':00Z'
                    };

                    predictButton.textContent = 'Predicting...';
                    predictButton.disabled = true;

                    const prediction = await this.predictDelay(flightData);
                    
                    // Show detailed modal
                    const modal = this.createDetailedDelayInfo(prediction);
                    document.body.appendChild(modal);

                } catch (error) {
                    alert('Error predicting delay: ' + error.message);
                } finally {
                    predictButton.textContent = 'Check Delay Risk';
                    predictButton.disabled = false;
                }
            };

            form.appendChild(predictButton);
        });
    }

    // Add delay indicators to flight results
    addDelayIndicatorsToResults() {
        const flightResults = document.querySelectorAll('.flight-result, .flight-card');
        
        flightResults.forEach(result => {
            if (result.dataset.enhanced) return; // Already enhanced
            
            const flight = this.extractFlightDataFromElement(result);
            if (flight) {
                this.predictDelay(flight).then(prediction => {
                    const indicator = this.createDelayIndicator(prediction);
                    result.appendChild(indicator);
                    result.dataset.enhanced = 'true';
                }).catch(error => {
                    console.error('Error adding delay indicator:', error);
                });
            }
        });
    }

    extractFlightDataFromElement(element) {
        // Extract flight data from DOM element
        // This would need to be customized based on your HTML structure
        const airline = element.querySelector('.airline')?.textContent || 'AA';
        const route = element.querySelector('.route')?.textContent || '';
        const [origin, dest] = route.split('→').map(s => s.trim());
        const departureTime = element.querySelector('.departure-time')?.textContent || '';
        
        if (!origin || !dest) return null;

        return {
            airline: airline,
            origin_airport: origin,
            dest_airport: dest,
            departure_time: new Date().toISOString() // Default to current time
        };
    }
}

// Initialize delay predictor when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.delayPredictor = new DelayPredictor();
    
    // Add delay prediction functionality to existing forms
    setTimeout(() => {
        window.delayPredictor.addDelayPredictionToBooking();
    }, 1000);
});

// CSS Styles for delay prediction UI
const delayPredictionStyles = `
<style>
.delay-indicator {
    margin: 10px 0;
    padding: 10px;
    border-radius: 8px;
    border-left: 4px solid;
}

.delay-indicator.low {
    background-color: #d4edda;
    border-left-color: #28a745;
}

.delay-indicator.medium {
    background-color: #fff3cd;
    border-left-color: #ffc107;
}

.delay-indicator.high {
    background-color: #f8d7da;
    border-left-color: #dc3545;
}

.delay-risk-badge {
    display: inline-flex;
    align-items: center;
    padding: 5px 10px;
    border-radius: 20px;
    color: white;
    font-weight: bold;
    margin-bottom: 5px;
}

.risk-score {
    font-size: 18px;
    margin-right: 5px;
}

.risk-label {
    font-size: 12px;
}

.delay-details {
    font-size: 14px;
}

.probability {
    font-weight: bold;
    margin-bottom: 5px;
}

.recommendation {
    font-size: 12px;
    color: #666;
    margin: 2px 0;
}

.delay-prediction-modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

.modal-content {
    background: white;
    border-radius: 10px;
    padding: 0;
    max-width: 500px;
    width: 90%;
    max-height: 80vh;
    overflow-y: auto;
}

.modal-header {
    background: #007bff;
    color: white;
    padding: 15px 20px;
    border-radius: 10px 10px 0 0;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.close-modal {
    font-size: 24px;
    cursor: pointer;
    line-height: 1;
}

.modal-body {
    padding: 20px;
}

.flight-info {
    text-align: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #eee;
}

.risk-summary {
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 20px 0;
}

.risk-circle {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: white;
    font-weight: bold;
    margin-right: 20px;
}

.risk-circle.low {
    background: #28a745;
}

.risk-circle.medium {
    background: #ffc107;
}

.risk-circle.high {
    background: #dc3545;
}

.risk-circle .risk-score {
    font-size: 24px;
}

.risk-circle .risk-label {
    font-size: 12px;
}

.recommendations ul {
    list-style-type: none;
    padding: 0;
}

.recommendations li {
    background: #f8f9fa;
    padding: 8px 12px;
    margin: 5px 0;
    border-radius: 5px;
    border-left: 3px solid #007bff;
}

.delay-predict-btn {
    background: #17a2b8;
    border: none;
    padding: 8px 16px;
    border-radius: 5px;
    color: white;
    cursor: pointer;
    font-size: 14px;
}

.delay-predict-btn:hover {
    background: #138496;
}

.delay-predict-btn:disabled {
    background: #6c757d;
    cursor: not-allowed;
}
</style>
`;

// Inject styles into the document
document.head.insertAdjacentHTML('beforeend', delayPredictionStyles);
