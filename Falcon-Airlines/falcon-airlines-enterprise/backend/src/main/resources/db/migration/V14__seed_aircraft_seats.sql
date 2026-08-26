-- V14: Seed aircraft seats and create seat_holds table
-- Root cause: seats table was empty, causing "0 seats" / "Sold Out" / "Seat not found" errors

-- 1. Create seat_holds table for temporary seat reservations
CREATE TABLE IF NOT EXISTS seat_holds (
    id BIGSERIAL PRIMARY KEY,
    flight_id BIGINT NOT NULL REFERENCES flights(id),
    seat_id BIGINT NOT NULL REFERENCES seats(id),
    booking_id BIGINT REFERENCES bookings(id),
    held_by_user_id BIGINT REFERENCES users(id),
    hold_expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_seat_holds_flight ON seat_holds (flight_id);
CREATE INDEX IF NOT EXISTS idx_seat_holds_expires ON seat_holds (hold_expires_at) WHERE status = 'HELD';

-- 2. Add seat price column to seats table
ALTER TABLE seats ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) DEFAULT 0;
ALTER TABLE seats ADD COLUMN IF NOT EXISTS seat_type VARCHAR(20) DEFAULT 'STANDARD';

-- 3. Seed seats for Aircraft 1: VT-IXB (A320-200, 180 capacity)
-- Layout: A B C | aisle | D E F — 30 rows economy, 3 rows business
-- Business: rows 1-3, Economy: rows 4-30

-- Business class seats (rows 1-3, wider: A-B | D-E, 2+2 layout = 12 seats)
INSERT INTO seats (aircraft_id, seat_number, seat_class, row_number, column_letter, is_active, price, seat_type, created_at, updated_at, is_deleted)
SELECT
    1,
    r || c,
    'BUSINESS',
    r::smallint,
    c,
    true,
    CASE WHEN c IN ('A', 'E') THEN 800.00 ELSE 500.00 END,
    CASE WHEN c IN ('A', 'E') THEN 'WINDOW' WHEN c IN ('B', 'D') THEN 'AISLE' ELSE 'STANDARD' END,
    NOW(), NOW(), false
FROM generate_series(1, 3) AS r,
     (VALUES ('A'), ('B'), ('D'), ('E')) AS cols(c)
WHERE NOT EXISTS (SELECT 1 FROM seats WHERE aircraft_id = 1 AND seat_number = r || c);

-- Economy class seats (rows 4-30, A B C | D E F = 162 seats)
INSERT INTO seats (aircraft_id, seat_number, seat_class, row_number, column_letter, is_active, price, seat_type, created_at, updated_at, is_deleted)
SELECT
    1,
    r || c,
    'ECONOMY',
    r::smallint,
    c,
    true,
    CASE
        WHEN c IN ('A', 'F') AND r <= 7 THEN 500.00   -- Window, extra legroom rows
        WHEN c IN ('A', 'F') THEN 300.00               -- Window
        WHEN c IN ('C', 'D') AND r <= 7 THEN 400.00   -- Aisle, extra legroom rows
        WHEN c IN ('C', 'D') THEN 200.00               -- Aisle
        WHEN r <= 7 THEN 150.00                         -- Middle, extra legroom
        ELSE 0.00                                       -- Standard middle
    END,
    CASE
        WHEN c IN ('A', 'F') THEN 'WINDOW'
        WHEN c IN ('C', 'D') THEN 'AISLE'
        ELSE 'MIDDLE'
    END,
    NOW(), NOW(), false
FROM generate_series(4, 30) AS r,
     (VALUES ('A'), ('B'), ('C'), ('D'), ('E'), ('F')) AS cols(c)
WHERE NOT EXISTS (SELECT 1 FROM seats WHERE aircraft_id = 1 AND seat_number = r || c);

-- 4. Seed seats for Aircraft 2: VT-ABC (B737-800, 186 capacity)
-- Business: rows 1-4 (2+2=16 seats), Economy: rows 5-34 (6x30=180 seats)

INSERT INTO seats (aircraft_id, seat_number, seat_class, row_number, column_letter, is_active, price, seat_type, created_at, updated_at, is_deleted)
SELECT
    2,
    r || c,
    'BUSINESS',
    r::smallint,
    c,
    true,
    CASE WHEN c IN ('A', 'E') THEN 900.00 ELSE 600.00 END,
    CASE WHEN c IN ('A', 'E') THEN 'WINDOW' WHEN c IN ('B', 'D') THEN 'AISLE' ELSE 'STANDARD' END,
    NOW(), NOW(), false
FROM generate_series(1, 4) AS r,
     (VALUES ('A'), ('B'), ('D'), ('E')) AS cols(c)
WHERE NOT EXISTS (SELECT 1 FROM seats WHERE aircraft_id = 2 AND seat_number = r || c);

INSERT INTO seats (aircraft_id, seat_number, seat_class, row_number, column_letter, is_active, price, seat_type, created_at, updated_at, is_deleted)
SELECT
    2,
    r || c,
    'ECONOMY',
    r::smallint,
    c,
    true,
    CASE
        WHEN c IN ('A', 'F') AND r <= 8 THEN 500.00
        WHEN c IN ('A', 'F') THEN 300.00
        WHEN c IN ('C', 'D') AND r <= 8 THEN 400.00
        WHEN c IN ('C', 'D') THEN 200.00
        WHEN r <= 8 THEN 150.00
        ELSE 0.00
    END,
    CASE
        WHEN c IN ('A', 'F') THEN 'WINDOW'
        WHEN c IN ('C', 'D') THEN 'AISLE'
        ELSE 'MIDDLE'
    END,
    NOW(), NOW(), false
FROM generate_series(5, 34) AS r,
     (VALUES ('A'), ('B'), ('C'), ('D'), ('E'), ('F')) AS cols(c)
WHERE NOT EXISTS (SELECT 1 FROM seats WHERE aircraft_id = 2 AND seat_number = r || c);

-- 5. Seed seats for Aircraft 3: VT-DEF (A321-200, 220 capacity)
-- Business: rows 1-4 (2+2=16 seats), Economy: rows 5-38 (6x34=204 seats)

INSERT INTO seats (aircraft_id, seat_number, seat_class, row_number, column_letter, is_active, price, seat_type, created_at, updated_at, is_deleted)
SELECT
    3,
    r || c,
    'BUSINESS',
    r::smallint,
    c,
    true,
    CASE WHEN c IN ('A', 'E') THEN 1000.00 ELSE 700.00 END,
    CASE WHEN c IN ('A', 'E') THEN 'WINDOW' WHEN c IN ('B', 'D') THEN 'AISLE' ELSE 'STANDARD' END,
    NOW(), NOW(), false
FROM generate_series(1, 4) AS r,
     (VALUES ('A'), ('B'), ('D'), ('E')) AS cols(c)
WHERE NOT EXISTS (SELECT 1 FROM seats WHERE aircraft_id = 3 AND seat_number = r || c);

INSERT INTO seats (aircraft_id, seat_number, seat_class, row_number, column_letter, is_active, price, seat_type, created_at, updated_at, is_deleted)
SELECT
    3,
    r || c,
    'ECONOMY',
    r::smallint,
    c,
    true,
    CASE
        WHEN c IN ('A', 'F') AND r <= 10 THEN 500.00
        WHEN c IN ('A', 'F') THEN 300.00
        WHEN c IN ('C', 'D') AND r <= 10 THEN 400.00
        WHEN c IN ('C', 'D') THEN 200.00
        WHEN r <= 10 THEN 150.00
        ELSE 0.00
    END,
    CASE
        WHEN c IN ('A', 'F') THEN 'WINDOW'
        WHEN c IN ('C', 'D') THEN 'AISLE'
        ELSE 'MIDDLE'
    END,
    NOW(), NOW(), false
FROM generate_series(5, 38) AS r,
     (VALUES ('A'), ('B'), ('C'), ('D'), ('E'), ('F')) AS cols(c)
WHERE NOT EXISTS (SELECT 1 FROM seats WHERE aircraft_id = 3 AND seat_number = r || c);

-- 6. Seed some occupied seats for demo data (simulate existing bookings)
-- Mark seats 1A, 2A, 3A on aircraft 1 as occupied via seat_allocations (if tickets exist)
-- This is handled by the application, not migration. Instead, we'll block a few seats.

-- Block exit row seats for safety (rows 13 on A320 = row 13)
UPDATE seats SET is_active = false
WHERE aircraft_id = 1 AND row_number = 13 AND is_deleted = false;

-- Block exit row on B737
UPDATE seats SET is_active = false
WHERE aircraft_id = 2 AND row_number = 14 AND is_deleted = false;

-- Block exit row on A321
UPDATE seats SET is_active = false
WHERE aircraft_id = 3 AND row_number = 15 AND is_deleted = false;
