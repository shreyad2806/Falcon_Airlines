-- Phase 2: reference and demo data
-- Roles, admin user, permissions, airports, aircraft and demo flights.

INSERT INTO roles (name, description, is_system) VALUES
('ADMIN', 'System administrator with full access', TRUE),
('CUSTOMER', 'Registered customer who can book flights', TRUE),
('AGENT', 'Booking agent with operational privileges', TRUE);

INSERT INTO permissions (code, description, resource, action) VALUES
('USER_READ', 'View user records', 'USER', 'READ'),
('USER_WRITE', 'Create and edit users', 'USER', 'WRITE'),
('ROLE_READ', 'View roles', 'ROLE', 'READ'),
('ROLE_WRITE', 'Create and edit roles', 'ROLE', 'WRITE'),
('PERMISSION_READ', 'View permissions', 'PERMISSION', 'READ'),
('FLIGHT_READ', 'Search and view flights', 'FLIGHT', 'READ'),
('FLIGHT_WRITE', 'Create and edit flights', 'FLIGHT', 'WRITE'),
('BOOKING_READ', 'View bookings', 'BOOKING', 'READ'),
('BOOKING_WRITE', 'Create and edit bookings', 'BOOKING', 'WRITE'),
('PASSENGER_READ', 'View passengers', 'PASSENGER', 'READ'),
('PASSENGER_WRITE', 'Create and edit passengers', 'PASSENGER', 'WRITE'),
('PAYMENT_READ', 'View payments', 'PAYMENT', 'READ'),
('PAYMENT_WRITE', 'Create and edit payments', 'PAYMENT', 'WRITE'),
('TICKET_READ', 'View tickets', 'TICKET', 'READ'),
('TICKET_WRITE', 'Create and edit tickets', 'TICKET', 'WRITE');

INSERT INTO users (username, email, password_hash, status, mfa_enabled, failed_login_attempts)
VALUES ('admin', 'admin@falconairlines.com', '$2a$10$placeholder', 'ACTIVE', FALSE, 0);

INSERT INTO user_roles (user_id, role_id, valid_from)
VALUES (1, 1, now());

INSERT INTO role_permissions (role_id, permission_id)
SELECT 1 AS role_id, id AS permission_id FROM permissions;

INSERT INTO airports (iata_code, icao_code, name, city, country, time_zone, latitude, longitude, is_active) VALUES
('JFK', 'KJFK', 'John F. Kennedy International Airport', 'New York', 'US', 'America/New_York', 40.6413111, -73.7781391, TRUE),
('LHR', 'EGLL', 'London Heathrow Airport', 'London', 'GB', 'Europe/London', 51.4700200, -0.4542950, TRUE),
('DEL', 'VIDP', 'Indira Gandhi International Airport', 'New Delhi', 'IN', 'Asia/Kolkata', 28.5561600, 77.1001700, TRUE),
('DXB', 'OMDB', 'Dubai International Airport', 'Dubai', 'AE', 'Asia/Dubai', 25.2531745, 55.3659070, TRUE),
('BOM', 'VABB', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'IN', 'Asia/Kolkata', 19.0896000, 72.8656000, TRUE),
('SIN', 'WSSS', 'Singapore Changi Airport', 'Singapore', 'SG', 'Asia/Singapore', 1.3644200, 103.9915300, TRUE);

INSERT INTO aircraft (registration_number, type, model, manufacturer, total_capacity, configuration) VALUES
('VT-IXB', 'A320', 'A320-200', 'Airbus', 180, '{"economy": 150, "business": 30}'),
('VT-ABC', 'B737', '737-800', 'Boeing', 186, '{"economy": 162, "business": 24}'),
('VT-DEF', 'A321', 'A321-200', 'Airbus', 220, '{"economy": 190, "business": 30}');

INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id, scheduled_departure, scheduled_arrival, status, terminal, gate, is_active) VALUES
('FA101', 1, 2, 1, '2026-08-15 08:00:00+00', '2026-08-15 20:00:00+00', 'SCHEDULED', '4', 'A12', TRUE),
('FA102', 2, 1, 1, '2026-08-16 09:00:00+00', '2026-08-16 22:00:00+00', 'SCHEDULED', '3', 'B07', TRUE),
('FA201', 3, 4, 2, '2026-08-15 14:00:00+00', '2026-08-15 18:30:00+00', 'SCHEDULED', '3', 'C21', TRUE),
('FA202', 4, 3, 2, '2026-08-16 02:00:00+00', '2026-08-16 06:30:00+00', 'SCHEDULED', '1', 'D05', TRUE),
('FA301', 5, 6, 3, '2026-08-15 18:00:00+00', '2026-08-16 02:30:00+00', 'SCHEDULED', '2', 'E08', TRUE),
('FA302', 6, 5, 3, '2026-08-16 10:00:00+00', '2026-08-16 18:30:00+00', 'SCHEDULED', '1', 'F03', TRUE);
