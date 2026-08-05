-- Phase 2: full Falcon Airlines schema
-- Generated from docs/design/DATABASE_DESIGN.md

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(20) UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'LOCKED', 'SUSPENDED')),
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts SMALLINT DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    valid_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS passengers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    passport_number VARCHAR(50),
    nationality VARCHAR(3),
    gender CHAR(1) CHECK (gender IN ('M', 'F', 'X', 'O')),
    redress_number VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS travel_documents (
    id BIGSERIAL PRIMARY KEY,
    passenger_id BIGINT NOT NULL REFERENCES passengers(id),
    document_type VARCHAR(20) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    issuing_country VARCHAR(3) NOT NULL,
    issue_date DATE,
    expiry_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS airports (
    id BIGSERIAL PRIMARY KEY,
    iata_code CHAR(3) NOT NULL UNIQUE,
    icao_code CHAR(4) UNIQUE,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country CHAR(2) NOT NULL,
    time_zone VARCHAR(50) NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS aircraft (
    id BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(20) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    total_capacity SMALLINT NOT NULL CHECK (total_capacity > 0),
    configuration JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS flights (
    id BIGSERIAL PRIMARY KEY,
    flight_number VARCHAR(10) NOT NULL,
    origin_airport_id BIGINT NOT NULL REFERENCES airports(id),
    destination_airport_id BIGINT NOT NULL REFERENCES airports(id),
    aircraft_id BIGINT NOT NULL REFERENCES aircraft(id),
    scheduled_departure TIMESTAMPTZ NOT NULL,
    scheduled_arrival TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    terminal VARCHAR(10),
    gate VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(10) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL CHECK (total_amount >= 0),
    currency CHAR(3) NOT NULL,
    booking_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    time_limit TIMESTAMPTZ,
    payment_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS booking_passengers (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    passenger_id BIGINT NOT NULL REFERENCES passengers(id),
    fare_class CHAR(1) NOT NULL,
    cabin VARCHAR(20) NOT NULL,
    ssr_codes VARCHAR(20)[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(20) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    passenger_id BIGINT NOT NULL REFERENCES passengers(id),
    flight_id BIGINT NOT NULL REFERENCES flights(id),
    fare_basis VARCHAR(10) NOT NULL,
    fare DECIMAL(15,2) NOT NULL,
    taxes DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    amount DECIMAL(15,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_reference VARCHAR(255),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    booking_id BIGINT REFERENCES bookings(id),
    channel VARCHAR(20) NOT NULL,
    template_code VARCHAR(50),
    subject VARCHAR(255),
    content TEXT,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS delay_predictions (
    id BIGSERIAL PRIMARY KEY,
    flight_id BIGINT NOT NULL REFERENCES flights(id),
    prediction_time TIMESTAMPTZ NOT NULL,
    predicted_delay_minutes INTEGER NOT NULL CHECK (predicted_delay_minutes >= 0),
    probability DECIMAL(5,4) NOT NULL CHECK (probability BETWEEN 0 AND 1),
    risk_level VARCHAR(20) NOT NULL,
    factors JSONB,
    model_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_status_deleted ON users (status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_permissions_resource_action ON permissions (resource, action);
CREATE INDEX IF NOT EXISTS idx_passengers_user ON passengers (user_id);
CREATE INDEX IF NOT EXISTS idx_passengers_name_dob ON passengers (last_name, first_name, date_of_birth);
CREATE INDEX IF NOT EXISTS idx_travel_documents_passenger ON travel_documents (passenger_id);
CREATE INDEX IF NOT EXISTS idx_airports_country_active ON airports (country, is_active);
CREATE INDEX IF NOT EXISTS idx_aircraft_type ON aircraft (type);
CREATE INDEX IF NOT EXISTS idx_flights_search ON flights (origin_airport_id, destination_airport_id, scheduled_departure);
CREATE INDEX IF NOT EXISTS idx_flights_aircraft ON flights (aircraft_id);
CREATE INDEX IF NOT EXISTS idx_flights_status ON flights (status);
CREATE INDEX IF NOT EXISTS idx_flights_active ON flights (is_active) WHERE is_active = TRUE AND is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_customer ON bookings (customer_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status_payment ON bookings (status, payment_status);
CREATE INDEX IF NOT EXISTS idx_bookings_date ON bookings (booking_date);
CREATE INDEX IF NOT EXISTS idx_booking_passengers_booking ON booking_passengers (booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_passengers_passenger ON booking_passengers (passenger_id);
CREATE INDEX IF NOT EXISTS idx_tickets_booking ON tickets (booking_id);
CREATE INDEX IF NOT EXISTS idx_tickets_passenger ON tickets (passenger_id);
CREATE INDEX IF NOT EXISTS idx_tickets_flight ON tickets (flight_id);
CREATE INDEX IF NOT EXISTS idx_tickets_issued ON tickets (status) WHERE status = 'ISSUED';
CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_status_created ON payments (status, created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications (recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_booking ON notifications (booking_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status_created ON notifications (status, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_delay_predictions_flight ON delay_predictions (flight_id);
CREATE INDEX IF NOT EXISTS idx_delay_predictions_flight_time ON delay_predictions (flight_id, prediction_time);
CREATE INDEX IF NOT EXISTS idx_delay_predictions_risk ON delay_predictions (risk_level);

-- Unique partial index for active user-role assignments
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_roles_active ON user_roles (user_id, role_id) WHERE is_deleted = FALSE;
