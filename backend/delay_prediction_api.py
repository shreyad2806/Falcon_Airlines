from flask import Flask, request, jsonify
from flask_cors import CORS
import json
from datetime import datetime
from flight_delay_predictor import FlightDelayPredictor
import os

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Initialize the predictor
predictor = FlightDelayPredictor()

# Load the model if it exists, otherwise train a new one
model_path = 'flight_delay_model.pkl'
if os.path.exists(model_path):
    try:
        predictor.load_model(model_path)
        print("Loaded existing model")
    except Exception as e:
        print(f"Error loading model: {e}")
        print("Training new model...")
        train_data = predictor.generate_synthetic_data(n_samples=10000)
        predictor.train_model(train_data)
        predictor.save_model(model_path)
else:
    print("No existing model found. Training new model...")
    train_data = predictor.generate_synthetic_data(n_samples=10000)
    predictor.train_model(train_data)
    predictor.save_model(model_path)

@app.route('/api/predict-delay', methods=['POST'])
def predict_delay():
    """API endpoint to predict flight delay risk"""
    try:
        # Get flight data from request
        flight_data = request.json
        
        # Validate required fields
        required_fields = ['airline', 'origin_airport', 'dest_airport', 'departure_time']
        for field in required_fields:
            if field not in flight_data:
                return jsonify({'error': f'Missing required field: {field}'}), 400
        
        # Parse departure time
        try:
            departure_time = datetime.fromisoformat(flight_data['departure_time'].replace('Z', '+00:00'))
        except ValueError:
            return jsonify({'error': 'Invalid departure_time format. Use ISO format.'}), 400
        
        # Extract time features
        month = departure_time.month
        day_of_week = departure_time.weekday() + 1  # Monday = 1
        departure_hour = departure_time.hour
        
        # Prepare data for prediction
        prediction_data = {
            'airline': flight_data['airline'],
            'origin_airport': flight_data['origin_airport'],
            'dest_airport': flight_data['dest_airport'],
            'month': month,
            'day_of_week': day_of_week,
            'departure_hour': departure_hour,
            'distance': flight_data.get('distance', 1000),  # Default distance
            'temperature': flight_data.get('temperature', 70),  # Default temp
            'wind_speed': flight_data.get('wind_speed', 10),  # Default wind
            'visibility': flight_data.get('visibility', 10),  # Default visibility
            'precipitation': flight_data.get('precipitation', 0),  # Default precipitation
            'origin_congestion': flight_data.get('origin_congestion', 0.5),  # Default congestion
            'dest_congestion': flight_data.get('dest_congestion', 0.5)  # Default congestion
        }
        
        # Make prediction
        result = predictor.predict_delay_risk(prediction_data)
        
        # Add additional context
        result['flight_info'] = {
            'airline': flight_data['airline'],
            'route': f"{flight_data['origin_airport']} → {flight_data['dest_airport']}",
            'departure_time': flight_data['departure_time'],
            'day_of_week': departure_time.strftime('%A'),
            'month': departure_time.strftime('%B')
        }
        
        # Add recommendations based on risk level
        if result['risk_level'] == 'High':
            result['recommendations'] = [
                "Consider booking an earlier flight",
                "Allow extra time for connections",
                "Check weather conditions before departure",
                "Consider travel insurance"
            ]
        elif result['risk_level'] == 'Medium':
            result['recommendations'] = [
                "Monitor flight status closely",
                "Allow some buffer time for connections",
                "Check weather forecast"
            ]
        else:
            result['recommendations'] = [
                "Flight is likely to be on time",
                "Standard arrival planning should be sufficient"
            ]
        
        return jsonify(result)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/batch-predict', methods=['POST'])
def batch_predict():
    """API endpoint to predict delay risk for multiple flights"""
    try:
        flights_data = request.json
        
        if not isinstance(flights_data, list):
            return jsonify({'error': 'Expected a list of flight objects'}), 400
        
        results = []
        for i, flight_data in enumerate(flights_data):
            try:
                # Use the same logic as single prediction
                departure_time = datetime.fromisoformat(flight_data['departure_time'].replace('Z', '+00:00'))
                
                prediction_data = {
                    'airline': flight_data['airline'],
                    'origin_airport': flight_data['origin_airport'],
                    'dest_airport': flight_data['dest_airport'],
                    'month': departure_time.month,
                    'day_of_week': departure_time.weekday() + 1,
                    'departure_hour': departure_time.hour,
                    'distance': flight_data.get('distance', 1000),
                    'temperature': flight_data.get('temperature', 70),
                    'wind_speed': flight_data.get('wind_speed', 10),
                    'visibility': flight_data.get('visibility', 10),
                    'precipitation': flight_data.get('precipitation', 0),
                    'origin_congestion': flight_data.get('origin_congestion', 0.5),
                    'dest_congestion': flight_data.get('dest_congestion', 0.5)
                }
                
                result = predictor.predict_delay_risk(prediction_data)
                result['flight_index'] = i
                result['flight_info'] = {
                    'airline': flight_data['airline'],
                    'route': f"{flight_data['origin_airport']} → {flight_data['dest_airport']}",
                    'departure_time': flight_data['departure_time']
                }
                results.append(result)
                
            except Exception as e:
                results.append({
                    'flight_index': i,
                    'error': str(e)
                })
        
        return jsonify({'predictions': results})
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/model-info', methods=['GET'])
def model_info():
    """Get information about the trained model"""
    try:
        if predictor.model is None:
            return jsonify({'error': 'Model not loaded'}), 500
        
        # Get feature importance
        feature_importance = []
        if hasattr(predictor.model, 'feature_importances_'):
            for i, importance in enumerate(predictor.model.feature_importances_):
                feature_importance.append({
                    'feature': predictor.feature_columns[i],
                    'importance': float(importance)
                })
            feature_importance.sort(key=lambda x: x['importance'], reverse=True)
        
        model_info = {
            'model_type': 'Random Forest Classifier',
            'n_features': len(predictor.feature_columns),
            'features': predictor.feature_columns,
            'feature_importance': feature_importance[:10],  # Top 10
            'airlines': list(predictor.label_encoders.get('airline', {}).classes_ if 'airline' in predictor.label_encoders else []),
            'airports': list(predictor.label_encoders.get('origin_airport', {}).classes_ if 'origin_airport' in predictor.label_encoders else [])
        }
        
        return jsonify(model_info)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'model_loaded': predictor.model is not None,
        'timestamp': datetime.now().isoformat()
    })

if __name__ == '__main__':
    print("Starting Flight Delay Prediction API...")
    print("API Endpoints:")
    print("  POST /api/predict-delay - Predict delay for a single flight")
    print("  POST /api/batch-predict - Predict delay for multiple flights")
    print("  GET /api/model-info - Get model information")
    print("  GET /api/health - Health check")
    print("\nExample request:")
    print("""
    POST /api/predict-delay
    {
        "airline": "AA",
        "origin_airport": "JFK",
        "dest_airport": "LAX",
        "departure_time": "2024-07-15T08:00:00Z",
        "distance": 2500,
        "temperature": 85,
        "wind_speed": 15,
        "visibility": 10,
        "precipitation": 0,
        "origin_congestion": 0.7,
        "dest_congestion": 0.6
    }
    """)
    
    app.run(debug=True, host='0.0.0.0', port=5000)
