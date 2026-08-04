import pandas as pd
import numpy as np
from flask import Flask, jsonify
from prophet import Prophet
from datetime import datetime, timedelta
import random

app = Flask(__name__)

# Simulate sample flight price data
def simulate_price_data(days=120):
    date_rng = pd.date_range(datetime.now() - timedelta(days=days), periods=days, freq='D')
    base_price = 5000  # base fare in INR
    # Simulate a seasonal price pattern with random noise
    prices = [base_price + 500*np.sin(2*np.pi*i/30) + random.randint(-300, 300) for i in range(days)]
    df = pd.DataFrame({'ds': date_rng, 'y': prices})
    return df

# Train Prophet model and predict next 30 days
def get_price_forecast():
    df = simulate_price_data()
    model = Prophet()
    model.fit(df)
    future = model.make_future_dataframe(periods=30)
    forecast = model.predict(future)
    # Only return new predictions (future)
    result = forecast[['ds', 'yhat', 'yhat_lower', 'yhat_upper']].tail(30)
    # Detect drops/rises
    alerts = []
    prev = None
    for i, row in result.iterrows():
        if prev is not None:
            change = row['yhat'] - prev
            if change < -100:
                alerts.append({'date': row['ds'].strftime('%Y-%m-%d'), 'type': 'drop', 'amount': round(-change, 2)})
            elif change > 100:
                alerts.append({'date': row['ds'].strftime('%Y-%m-%d'), 'type': 'rise', 'amount': round(change, 2)})
        prev = row['yhat']
    return result.to_dict(orient='records'), alerts

@app.route('/api/fare-prediction')
def fare_prediction():
    forecast, alerts = get_price_forecast()
    return jsonify({'forecast': forecast, 'alerts': alerts})

if __name__ == '__main__':
    app.run(debug=True)
