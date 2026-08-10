-- Phase 6: Booking Engine Database Schema
-- Adds seat inventory, seat allocation, and optimistic locking support

-- Create seats table for structured seat inventory
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    aircraft_id BIGINT NOT NULL REFERENCES aircraft(id),
    seat_number VARCHAR(10) NOT NULL,
    seat_class VARCHAR(20) NOT NULL CHECK (seat_class IN ('ECONOMY', 'BUSINESS', 'FIRST')),
    row_number SMALLINT,
    column_letter CHAR(1),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create seat_allocations table to track seat assignments
CREATE TABLE IF NOT EXISTS seat_allocations (
    id BIGSERIAL PRIMARY KEY,
    seat_id BIGINT NOT NULL REFERENCES seats(id),
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    flight_id BIGINT NOT NULL REFERENCES flights(id),
    allocated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Add version column to bookings for optimistic locking
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Add version column to tickets for optimistic locking
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Unique partial index for seats to prevent duplicate seat numbers per aircraft
CREATE UNIQUE INDEX IF NOT EXISTS uk_seats_aircraft_seat ON seats (aircraft_id, seat_number) WHERE is_deleted = FALSE;

-- Indexes for seats table
CREATE INDEX IF NOT EXISTS idx_seats_aircraft ON seats (aircraft_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_seats_class ON seats (seat_class) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_seats_active ON seats (is_active) WHERE is_active = TRUE AND is_deleted = FALSE;

-- Unique partial index for seat_allocations to prevent duplicate seat assignment on same flight
CREATE UNIQUE INDEX IF NOT EXISTS uk_seat_allocations_seat_flight ON seat_allocations (seat_id, flight_id) WHERE is_deleted = FALSE;

-- Unique partial index for seat_allocations to prevent duplicate ticket assignment
CREATE UNIQUE INDEX IF NOT EXISTS uk_seat_allocations_ticket ON seat_allocations (ticket_id) WHERE is_deleted = FALSE;

-- Indexes for seat_allocations table
CREATE INDEX IF NOT EXISTS idx_seat_allocations_seat ON seat_allocations (seat_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_seat_allocations_ticket ON seat_allocations (ticket_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_seat_allocations_flight ON seat_allocations (flight_id) WHERE is_deleted = FALSE;

-- Indexes for bookings table (optimization for booking queries)
CREATE INDEX IF NOT EXISTS idx_bookings_version ON bookings (version) WHERE is_deleted = FALSE;

-- Indexes for tickets table (optimization for ticket queries)
CREATE INDEX IF NOT EXISTS idx_tickets_version ON tickets (version) WHERE is_deleted = FALSE;
