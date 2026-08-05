# Flight Delay Prediction System - Setup Guide

## Overview
The Flight Delay Prediction System is an AI-powered tool that predicts flight delay risks using machine learning. It consists of a Flask API backend and an interactive HTML frontend.

## System Components

### 1. Backend API (`delay_prediction_api.py`)
- Flask-based REST API
- Endpoints for single and batch flight delay predictions
- Automatic model training with synthetic data
- CORS enabled for frontend integration

### 2. Machine Learning Model (`flight_delay_predictor.py`)
- Random Forest classifier
- Features: airline, airports, time, weather, congestion
- Generates synthetic training data if no real data available
- Model persistence with joblib

### 3. Frontend Demo (`delay_prediction_demo.html`)
- Interactive web interface
- Real-time API status checking
- Manual flight prediction form
- Sample flight predictions
- Beautiful, responsive design

## Setup Instructions

### Prerequisites
- Python 3.7 or higher
- pip package manager

### Installation Steps

1. **Install Dependencies**
   ```bash
   pip install -r requirements.txt
   ```

2. **Start the API Server**
   ```bash
   python delay_prediction_api.py
   ```
   
   The API will:
   - Train a new model if none exists (first run)
   - Load existing model on subsequent runs
   - Start server on http://localhost:5000

3. **Open the Frontend**
   - Open `delay_prediction_demo.html` in your web browser
   - The page will automatically check API connectivity
   - Green "API Online" indicator confirms successful connection

## API Endpoints

### Health Check
```
GET /api/health
```
Returns API status and model information.

### Single Flight Prediction
```
POST /api/predict-delay
Content-Type: application/json

{
    "airline": "AA",
    "origin_airport": "JFK",
    "dest_airport": "LAX",
    "departure_time": "2024-08-03T08:00:00Z",
    "distance": 2500,
    "temperature": 75,
    "wind_speed": 10,
    "visibility": 10,
    "precipitation": 0,
    "origin_congestion": 0.7,
    "dest_congestion": 0.6
}
```

### Batch Prediction
```
POST /api/batch-predict
Content-Type: application/json

{
    "flights": [
        { /* flight data 1 */ },
        { /* flight data 2 */ }
    ]
}
```

### Model Information
```
GET /api/model-info
```
Returns model statistics and feature importance.

## Usage Examples

### Testing with curl
```bash
# Health check
curl http://localhost:5000/api/health

# Single prediction
curl -X POST -H "Content-Type: application/json" \
  -d '{"airline":"AA","origin_airport":"JFK","dest_airport":"LAX","departure_time":"2024-08-03T08:00:00Z"}' \
  http://localhost:5000/api/predict-delay
```

### Testing with PowerShell
```powershell
# Single prediction
Invoke-RestMethod -Uri "http://localhost:5000/api/predict-delay" -Method POST -ContentType "application/json" -Body '{"airline":"AA","origin_airport":"JFK","dest_airport":"LAX","departure_time":"2024-08-03T08:00:00Z"}'
```

## Response Format

### Prediction Response
```json
{
    "delay_probability": 0.507,
    "risk_level": "Medium",
    "risk_score": 50,
    "flight_info": {
        "airline": "AA",
        "route": "JFK → LAX",
        "departure_time": "2024-08-03T08:00:00Z",
        "day_of_week": "Saturday",
        "month": "August"
    },
    "recommendations": [
        "Monitor flight status closely",
        "Allow some buffer time for connections",
        "Check weather forecast"
    ]
}
```

## Features

### Risk Assessment
- **Low Risk** (0-33%): Green indicator, minimal delay expected
- **Medium Risk** (34-66%): Yellow indicator, moderate delay possible
- **High Risk** (67-100%): Red indicator, significant delay likely

### Supported Airlines
AA, DL, UA, SW, JB, AS, NK, F9, G4, B6

### Supported Airports
Major US airports: ATL, LAX, ORD, DFW, DEN, JFK, SFO, SEA, LAS, MCO, EWR, CLT, PHX, IAH, MIA, BOS, MSP, FLL, DTW, PHL

## Troubleshooting

### Common Issues

1. **API Not Starting**
   - Ensure all dependencies are installed: `pip install -r requirements.txt`
   - Check if port 5000 is available
   - Verify Python version compatibility

2. **Frontend Not Connecting**
   - Confirm API is running on http://localhost:5000
   - Check browser console for CORS errors
   - Ensure API status shows "Online"

3. **Model Training Errors**
   - Verify scikit-learn and numpy versions
   - Check available disk space for model file
   - Review console output for specific error messages

### Performance Notes
- First run takes longer due to model training
- Model file (`flight_delay_model.pkl`) is saved for future use
- Synthetic data generation creates 10,000 sample flights

## Development

### Adding New Features
1. Modify `flight_delay_predictor.py` for model changes
2. Update `delay_prediction_api.py` for new endpoints
3. Enhance `delay_prediction_demo.html` for UI improvements

### Model Retraining
Delete `flight_delay_model.pkl` and restart the API to retrain with fresh data.

## Status
✅ **Setup Complete** - All components tested and working
- Backend API: Running on port 5000
- Frontend Demo: Connected and functional
- Model: Trained and ready for predictions
- Dependencies: All installed and verified

The delay prediction system is now ready for use!
