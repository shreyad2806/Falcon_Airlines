-- V13: Add external flight tracking columns and seed 30 days of future flights

-- 1. Add external flight tracking columns (idempotent)
ALTER TABLE flights ADD COLUMN IF NOT EXISTS external_flight_id VARCHAR(50);
ALTER TABLE flights ADD COLUMN IF NOT EXISTS airline_code VARCHAR(5);
ALTER TABLE flights ADD COLUMN IF NOT EXISTS airline_name VARCHAR(100);
ALTER TABLE flights ADD COLUMN IF NOT EXISTS origin_name VARCHAR(200);
ALTER TABLE flights ADD COLUMN IF NOT EXISTS destination_name VARCHAR(200);
ALTER TABLE flights ADD COLUMN IF NOT EXISTS estimated_departure TIMESTAMP;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS estimated_arrival TIMESTAMP;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS actual_departure TIMESTAMP;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS actual_arrival TIMESTAMP;
ALTER TABLE flights ADD COLUMN IF NOT EXISTS data_source VARCHAR(20) DEFAULT 'FALCON';
ALTER TABLE flights ADD COLUMN IF NOT EXISTS last_external_update TIMESTAMP;

-- Unique constraint to prevent duplicate external flights
CREATE UNIQUE INDEX IF NOT EXISTS idx_flights_external_id
    ON flights (external_flight_id) WHERE external_flight_id IS NOT NULL;

-- 2. Seed 30 days of future flights for common Indian routes
-- Uses generate_series to create flights relative to CURRENT_DATE

-- Route: DEL → BOM
INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, data_source, is_deleted, created_at, updated_at)
SELECT
    'FA9' || LPAD(d::text, 2, '0'),
    (SELECT id FROM airports WHERE iata_code='DEL'),
    (SELECT id FROM airports WHERE iata_code='BOM'),
    (SELECT id FROM aircraft ORDER BY id LIMIT 1),
    (CURRENT_DATE + (d || ' days')::interval + '08:00:00'::interval),
    (CURRENT_DATE + (d || ' days')::interval + '10:15:00'::interval),
    'SCHEDULED', 'T1', 'A' || LPAD((10 + (d % 20))::text, 2, '0'),
    true,
    (7500 + (d * 150) % 3000)::numeric(12,2),
    'INR', 'FALCON', false, NOW(), NOW()
FROM generate_series(0, 29) AS d
WHERE NOT EXISTS (
    SELECT 1 FROM flights f
    WHERE f.flight_number = 'FA9' || LPAD(d::text, 2, '0')
    AND f.scheduled_departure::date = CURRENT_DATE + (d || ' days')::interval
    AND f.is_deleted = false
);

-- Route: BOM → DEL
INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, data_source, is_deleted, created_at, updated_at)
SELECT
    'FA9' || LPAD((30 + d)::text, 2, '0'),
    (SELECT id FROM airports WHERE iata_code='BOM'),
    (SELECT id FROM airports WHERE iata_code='DEL'),
    (SELECT id FROM aircraft ORDER BY id LIMIT 1),
    (CURRENT_DATE + (d || ' days')::interval + '14:00:00'::interval),
    (CURRENT_DATE + (d || ' days')::interval + '16:15:00'::interval),
    'SCHEDULED', 'T1', 'B' || LPAD((10 + (d % 20))::text, 2, '0'),
    true,
    (7800 + (d * 120) % 2800)::numeric(12,2),
    'INR', 'FALCON', false, NOW(), NOW()
FROM generate_series(0, 29) AS d
WHERE NOT EXISTS (
    SELECT 1 FROM flights f
    WHERE f.flight_number = 'FA9' || LPAD((30 + d)::text, 2, '0')
    AND f.scheduled_departure::date = CURRENT_DATE + (d || ' days')::interval
    AND f.is_deleted = false
);

-- Route: DEL → DXB
INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, data_source, is_deleted, created_at, updated_at)
SELECT
    'FA8' || LPAD((d % 3 + 1)::text, 2, '0'),
    (SELECT id FROM airports WHERE iata_code='DEL'),
    (SELECT id FROM airports WHERE iata_code='DXB'),
    (SELECT id FROM aircraft ORDER BY id LIMIT 1),
    (CURRENT_DATE + (d || ' days')::interval + '23:30:00'::interval),
    (CURRENT_DATE + (d || ' days')::interval + '1 day' + '03:00:00'::interval),
    'SCHEDULED', 'T2', 'C' || LPAD((10 + (d % 15))::text, 2, '0'),
    true,
    (38000 + (d * 200) % 5000)::numeric(12,2),
    'INR', 'FALCON', false, NOW(), NOW()
FROM generate_series(0, 29) AS d
WHERE NOT EXISTS (
    SELECT 1 FROM flights f
    WHERE f.flight_number = 'FA8' || LPAD((d % 3 + 1)::text, 2, '0')
    AND f.scheduled_departure::date = CURRENT_DATE + (d || ' days')::interval
    AND f.is_deleted = false
);

-- Route: BOM → SIN
INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
                     scheduled_departure, scheduled_arrival, status, terminal, gate,
                     is_active, base_price, currency, data_source, is_deleted, created_at, updated_at)
SELECT
    'FA7' || LPAD((d % 2 + 1)::text, 2, '0'),
    (SELECT id FROM airports WHERE iata_code='BOM'),
    (SELECT id FROM airports WHERE iata_code='SIN'),
    (SELECT id FROM aircraft ORDER BY id LIMIT 1),
    (CURRENT_DATE + (d || ' days')::interval + '01:30:00'::interval),
    (CURRENT_DATE + (d || ' days')::interval + '09:45:00'::interval),
    'SCHEDULED', 'T3', 'E' || LPAD((5 + (d % 10))::text, 2, '0'),
    true,
    (28000 + (d * 180) % 4000)::numeric(12,2),
    'INR', 'FALCON', false, NOW(), NOW()
FROM generate_series(0, 29) AS d
WHERE NOT EXISTS (
    SELECT 1 FROM flights f
    WHERE f.flight_number = 'FA7' || LPAD((d % 2 + 1)::text, 2, '0')
    AND f.scheduled_departure::date = CURRENT_DATE + (d || ' days')::interval
    AND f.is_deleted = false
);
