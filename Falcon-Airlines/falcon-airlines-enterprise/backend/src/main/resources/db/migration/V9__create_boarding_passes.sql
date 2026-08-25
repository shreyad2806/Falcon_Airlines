-- Phase 7: Boarding Pass table
-- Adds boarding pass generation, check-in, QR verification, and PDF support

CREATE TABLE IF NOT EXISTS boarding_passes (
    id BIGSERIAL PRIMARY KEY,
    boarding_pass_number VARCHAR(20) NOT NULL UNIQUE,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    passenger_id BIGINT NOT NULL REFERENCES passengers(id),
    flight_id BIGINT NOT NULL REFERENCES flights(id),
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    seat_number VARCHAR(10),
    seat_class VARCHAR(20),
    boarding_group VARCHAR(5),
    gate VARCHAR(10),
    boarding_time TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    qr_code_payload VARCHAR(500),
    verification_token VARCHAR(500),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked_in_at TIMESTAMPTZ,
    boarded_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Unique partial index for boarding_pass_number
CREATE UNIQUE INDEX IF NOT EXISTS uk_boarding_passes_number
    ON boarding_passes (boarding_pass_number) WHERE is_deleted = FALSE;

-- Unique partial index: one boarding pass per ticket
CREATE UNIQUE INDEX IF NOT EXISTS uk_boarding_passes_ticket
    ON boarding_passes (ticket_id) WHERE is_deleted = FALSE;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_boarding_passes_passenger ON boarding_passes (passenger_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_boarding_passes_flight ON boarding_passes (flight_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_boarding_passes_booking ON boarding_passes (booking_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_boarding_passes_status ON boarding_passes (status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_boarding_passes_verification_token ON boarding_passes (verification_token) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_boarding_passes_version ON boarding_passes (version) WHERE is_deleted = FALSE;
