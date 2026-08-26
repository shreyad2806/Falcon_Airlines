-- V11: Add pricing columns to flights for INR-based fare display

ALTER TABLE flights
    ADD COLUMN IF NOT EXISTS base_price NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'INR';

-- Set realistic INR prices for existing flights
UPDATE flights SET base_price = 42850.00, currency = 'INR' WHERE flight_number = 'FA101';
UPDATE flights SET base_price = 45200.00, currency = 'INR' WHERE flight_number = 'FA102';
UPDATE flights SET base_price = 38500.00, currency = 'INR' WHERE flight_number = 'FA201';
UPDATE flights SET base_price = 39800.00, currency = 'INR' WHERE flight_number = 'FA202';
UPDATE flights SET base_price = 28900.00, currency = 'INR' WHERE flight_number = 'FA301';
UPDATE flights SET base_price = 31200.00, currency = 'INR' WHERE flight_number = 'FA302';
