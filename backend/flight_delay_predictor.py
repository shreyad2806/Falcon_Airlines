import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import joblib
import json
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')

class FlightDelayPredictor:
    def __init__(self):
        self.model = None
        self.label_encoders = {}
        self.scaler = StandardScaler()
        self.feature_columns = []
        
    def generate_synthetic_data(self, n_samples=10000):
        """Generate synthetic flight data for training"""
        np.random.seed(42)
        
        # Airlines
        airlines = ['AA', 'DL', 'UA', 'SW', 'JB', 'AS', 'NK', 'F9', 'G4', 'B6']
        
        # Airports (major US airports)
        airports = ['ATL', 'LAX', 'ORD', 'DFW', 'DEN', 'JFK', 'SFO', 'SEA', 'LAS', 'MCO',
                   'EWR', 'CLT', 'PHX', 'IAH', 'MIA', 'BOS', 'MSP', 'FLL', 'DTW', 'PHL']
        
        # Generate synthetic data
        data = []
        for _ in range(n_samples):
            # Basic flight info
            airline = np.random.choice(airlines)
            origin = np.random.choice(airports)
            dest = np.random.choice(airports)
            while dest == origin:  # Ensure different airports
                dest = np.random.choice(airports)
            
            # Time features
            month = np.random.randint(1, 13)
            day_of_week = np.random.randint(1, 8)
            hour = np.random.choice([6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22])
            
            # Weather features (simplified)
            temperature = np.random.normal(65, 20)  # Fahrenheit
            wind_speed = np.random.exponential(10)  # mph
            visibility = np.random.normal(10, 2)  # miles
            precipitation = np.random.exponential(0.1)  # inches
            
            # Airport congestion (synthetic)
            origin_congestion = np.random.uniform(0.1, 0.9)
            dest_congestion = np.random.uniform(0.1, 0.9)
            
            # Distance (simplified based on airport pairs)
            distance = np.random.uniform(200, 3000)
            
            # Calculate delay probability based on features
            delay_prob = 0.1  # Base probability
            
            # Airline effect
            if airline in ['NK', 'F9', 'G4']:  # Budget airlines tend to have more delays
                delay_prob += 0.15
            elif airline in ['DL', 'AA']:  # Premium airlines tend to be more punctual
                delay_prob -= 0.05
                
            # Time effects
            if hour in [7, 8, 17, 18, 19]:  # Rush hours
                delay_prob += 0.2
            if day_of_week in [1, 7]:  # Monday and Sunday
                delay_prob += 0.1
            if month in [6, 7, 12]:  # Summer and December (holiday season)
                delay_prob += 0.15
                
            # Weather effects
            if wind_speed > 25:
                delay_prob += 0.3
            if visibility < 5:
                delay_prob += 0.25
            if precipitation > 0.5:
                delay_prob += 0.2
            if temperature < 20 or temperature > 95:
                delay_prob += 0.1
                
            # Congestion effects
            delay_prob += origin_congestion * 0.2
            delay_prob += dest_congestion * 0.2
            
            # Distance effect
            if distance > 2000:
                delay_prob += 0.05
                
            # Cap probability
            delay_prob = min(delay_prob, 0.85)
            
            # Determine if delayed (>15 minutes)
            is_delayed = np.random.random() < delay_prob
            
            data.append({
                'airline': airline,
                'origin_airport': origin,
                'dest_airport': dest,
                'month': month,
                'day_of_week': day_of_week,
                'departure_hour': hour,
                'distance': distance,
                'temperature': temperature,
                'wind_speed': wind_speed,
                'visibility': visibility,
                'precipitation': precipitation,
                'origin_congestion': origin_congestion,
                'dest_congestion': dest_congestion,
                'is_delayed': int(is_delayed)
            })
        
        return pd.DataFrame(data)
    
    def preprocess_data(self, df):
        """Preprocess the data for training"""
        # Create a copy to avoid modifying original data
        df_processed = df.copy()
        
        # Encode categorical variables
        categorical_columns = ['airline', 'origin_airport', 'dest_airport']
        
        for col in categorical_columns:
            if col not in self.label_encoders:
                self.label_encoders[col] = LabelEncoder()
                df_processed[col] = self.label_encoders[col].fit_transform(df_processed[col])
            else:
                # Handle unseen categories
                df_processed[col] = df_processed[col].map(
                    lambda x: self.label_encoders[col].transform([x])[0] 
                    if x in self.label_encoders[col].classes_ else -1
                )
        
        return df_processed
    
    def train_model(self, df):
        """Train the delay prediction model"""
        print("Preprocessing data...")
        df_processed = self.preprocess_data(df)
        
        # Separate features and target
        X = df_processed.drop('is_delayed', axis=1)
        y = df_processed['is_delayed']
        
        self.feature_columns = X.columns.tolist()
        
        # Split the data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y
        )
        
        # Scale numerical features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Train Random Forest model
        print("Training Random Forest model...")
        self.model = RandomForestClassifier(
            n_estimators=100,
            max_depth=15,
            min_samples_split=10,
            min_samples_leaf=5,
            random_state=42,
            n_jobs=-1
        )
        
        self.model.fit(X_train_scaled, y_train)
        
        # Evaluate model
        y_pred = self.model.predict(X_test_scaled)
        y_pred_proba = self.model.predict_proba(X_test_scaled)[:, 1]
        
        print(f"Model Accuracy: {accuracy_score(y_test, y_pred):.3f}")
        print("\nClassification Report:")
        print(classification_report(y_test, y_pred))
        
        # Feature importance
        feature_importance = pd.DataFrame({
            'feature': self.feature_columns,
            'importance': self.model.feature_importances_
        }).sort_values('importance', ascending=False)
        
        print("\nTop 10 Most Important Features:")
        print(feature_importance.head(10))
        
        return self.model
    
    def predict_delay_risk(self, flight_data):
        """Predict delay risk for a single flight"""
        if self.model is None:
            raise ValueError("Model not trained yet. Call train_model() first.")
        
        # Convert to DataFrame if it's a dictionary
        if isinstance(flight_data, dict):
            flight_data = pd.DataFrame([flight_data])
        
        # Preprocess the data
        flight_processed = self.preprocess_data(flight_data)
        
        # Ensure all required columns are present
        for col in self.feature_columns:
            if col not in flight_processed.columns:
                flight_processed[col] = 0
        
        # Reorder columns to match training data
        flight_processed = flight_processed[self.feature_columns]
        
        # Scale the data
        flight_scaled = self.scaler.transform(flight_processed)
        
        # Predict probability
        delay_probability = self.model.predict_proba(flight_scaled)[0, 1]
        
        # Convert to risk score (0-100)
        risk_score = int(delay_probability * 100)
        
        # Determine risk level
        if risk_score < 30:
            risk_level = "Low"
        elif risk_score < 60:
            risk_level = "Medium"
        else:
            risk_level = "High"
        
        return {
            'delay_probability': delay_probability,
            'risk_score': risk_score,
            'risk_level': risk_level
        }
    
    def save_model(self, filepath='flight_delay_model.pkl'):
        """Save the trained model and preprocessors"""
        model_data = {
            'model': self.model,
            'label_encoders': self.label_encoders,
            'scaler': self.scaler,
            'feature_columns': self.feature_columns
        }
        joblib.dump(model_data, filepath)
        print(f"Model saved to {filepath}")
    
    def load_model(self, filepath='flight_delay_model.pkl'):
        """Load a trained model and preprocessors"""
        model_data = joblib.load(filepath)
        self.model = model_data['model']
        self.label_encoders = model_data['label_encoders']
        self.scaler = model_data['scaler']
        self.feature_columns = model_data['feature_columns']
        print(f"Model loaded from {filepath}")

def main():
    """Main function to train and test the model"""
    print("Flight Delay Prediction Model")
    print("=" * 40)
    
    # Initialize predictor
    predictor = FlightDelayPredictor()
    
    # Generate synthetic training data
    print("Generating synthetic training data...")
    train_data = predictor.generate_synthetic_data(n_samples=10000)
    print(f"Generated {len(train_data)} training samples")
    
    # Train the model
    predictor.train_model(train_data)
    
    # Save the model
    predictor.save_model()
    
    # Test with sample predictions
    print("\n" + "=" * 40)
    print("Sample Predictions:")
    print("=" * 40)
    
    # Test cases
    test_flights = [
        {
            'airline': 'AA',
            'origin_airport': 'JFK',
            'dest_airport': 'LAX',
            'month': 7,
            'day_of_week': 1,  # Monday
            'departure_hour': 8,  # Rush hour
            'distance': 2500,
            'temperature': 85,
            'wind_speed': 15,
            'visibility': 10,
            'precipitation': 0,
            'origin_congestion': 0.7,
            'dest_congestion': 0.6
        },
        {
            'airline': 'SW',
            'origin_airport': 'ORD',
            'dest_airport': 'DEN',
            'month': 12,
            'day_of_week': 5,  # Friday
            'departure_hour': 18,  # Evening rush
            'distance': 900,
            'temperature': 25,
            'wind_speed': 30,  # High wind
            'visibility': 3,   # Low visibility
            'precipitation': 0.8,  # Heavy rain
            'origin_congestion': 0.8,
            'dest_congestion': 0.4
        },
        {
            'airline': 'DL',
            'origin_airport': 'ATL',
            'dest_airport': 'MIA',
            'month': 3,
            'day_of_week': 3,  # Wednesday
            'departure_hour': 14,  # Afternoon
            'distance': 600,
            'temperature': 75,
            'wind_speed': 8,
            'visibility': 10,
            'precipitation': 0,
            'origin_congestion': 0.3,
            'dest_congestion': 0.2
        }
    ]
    
    for i, flight in enumerate(test_flights, 1):
        result = predictor.predict_delay_risk(flight)
        print(f"\nFlight {i}:")
        print(f"  Route: {flight['origin_airport']} → {flight['dest_airport']}")
        print(f"  Airline: {flight['airline']}")
        print(f"  Risk Score: {result['risk_score']}/100")
        print(f"  Risk Level: {result['risk_level']}")
        print(f"  Delay Probability: {result['delay_probability']:.2%}")

if __name__ == "__main__":
    main()
