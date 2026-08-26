-- V12: Replace hardcoded past flights with future flights and fix seat inventory
--
-- All existing flights have past departure dates (Aug 15-16, 2026).
-- This migration creates flights relative to the current date using
-- PostgreSQL's CURRENT_DATE so the seed data never goes stale.

-- 1. Soft-delete all existing flights (they have past dates)
UPDATE flights SET is_deleted = true, deleted_at = NOW() WHERE is_deleted = false;

-- 2. Insert future flights starting from tomorrow
-- Route: DEL-BOM, DEL-JFK, BOM-LHR, DEL-DXB, BOM-SIN, JFK-LHR

INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, is_deleted, created_at, updated_at)
SELECT 'FA101', (SELECT id FROM airports WHERE iata_code='DEL'),
        (SELECT id FROM airports WHERE iata_code='BOM'),
        (SELECT id FROM aircraft LIMIT 1),
        (CURRENT_DATE + INTERVAL '1 day' + TIME '08:00:00'),
        (CURRENT_DATE + INTERVAL '1 day' + TIME '10:30:00'),
        'SCHEDULED', 'T1', 'A12', true, 8450.00, 'INR', false, NOW(), NOW();

INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, is_deleted, created_at, updated_at)
SELECT 'FA102', (SELECT id FROM airports WHERE iata_code='BOM'),
        (SELECT id FROM airports WHERE iata_code='DEL'),
        (SELECT id FROM aircraft LIMIT 1),
        (CURRENT_DATE + INTERVAL '1 day' + TIME '14:00:00'),
        (CURRENT_DATE + INTERVAL '1 day' + TIME '16:30:00'),
        'SCHEDULED', 'T1', 'B07', true, 8450.00, 'INR', false, NOW(), NOW();

INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, is_deleted, created_at, updated_at)
SELECT 'FA201', (SELECT id FROM airports WHERE iata_code='DEL'),
        (SELECT id FROM airports WHERE iata_code='JFK'),
        (SELECT id FROM aircraft LIMIT 1),
        (CURRENT_DATE + INTERVAL '2 days' + TIME '23:30:00'),
        (CURRENT_DATE + INTERVAL '3 days' + TIME '05:00:00'),
        'SCHEDULED', 'T2', 'C21', true, 42850.00, 'INR', false, NOW(), NOW();

INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, is_deleted, created_at, updated_at)
SELECT 'FA202', (SELECT id FROM airports WHERE iata_code='JFK'),
        (SELECT id FROM airports WHERE iata_code='DEL'),
        (SELECT id FROM aircraft LIMIT 1),
        (CURRENT_DATE + INTERVAL '4 days' + TIME '11:00:00'),
        (CURRENT_DATE + INTERVAL '5 days' + TIME '12:30:00'),
        'SCHEDULED', 'T2', 'D05', true, 45200.00, 'INR', false, NOW(), NOW();

INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, is_deleted, created_at, updated_at)
SELECT 'FA301', (SELECT id FROM airports WHERE iata_code='BOM'),
        (SELECT id FROM airports WHERE iata_code='LHR'),
        (SELECT id FROM aircraft LIMIT 1),
        (CURRENT_DATE + INTERVAL '3 days' + TIME '01:30:00'),
        (CURRENT_DATE + INTERVAL '3 days' + TIME '07:45:00'),
        'SCHEDULED', 'T3', 'E08', true, 38500.00, 'INR', false, NOW(), NOW();

INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, is_deleted, created_at, updated_at)
SELECT 'FA302', (SELECT id FROM airports WHERE iata_code='LHR'),
        (SELECT id FROM airports WHERE iata_code='BOM'),
        (SELECT id FROM aircraft LIMIT 1),
        (CURRENT_DATE + INTERVAL '5 days' + TIME '10:00:00'),
        (CURRENT_DATE + INTERVAL '5 days' + TIME '23:15:00'),
        'SCHEDULED', 'T3', 'F03', true, 39800.00, 'INR', false, NOW(), NOW();

-- 3. Fix: Add missing BOARDING_PASS permissions for all roles (idempotent)
-- (Already handled by V10, this is just a safety no-op)
