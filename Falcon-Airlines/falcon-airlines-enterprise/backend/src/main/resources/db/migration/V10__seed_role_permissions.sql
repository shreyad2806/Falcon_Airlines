-- V10: Add missing boarding-pass permissions and grant CUSTOMER/AGENT
--      roles the permissions they need for the passenger-facing booking flow.
--
-- Previous migrations seeded all permissions to ADMIN only.
-- Customers (and agents) had zero role_permissions, causing 403 on every
-- @PreAuthorize("hasAnyAuthority(...)") endpoint.

-- 1. Add the missing BOARDING_PASS permissions that Phase 7 controllers reference
INSERT INTO permissions (code, description, resource, action)
VALUES
    ('BOARDING_PASS_READ',  'View boarding passes',        'BOARDING_PASS', 'READ'),
    ('BOARDING_PASS_WRITE', 'Create and manage boarding passes', 'BOARDING_PASS', 'WRITE')
ON CONFLICT (code) DO NOTHING;

-- 1b. Grant the new BOARDING_PASS permissions to ADMIN as well
--      (V3 gave ADMIN all permissions that existed at that time)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id AS role_id, p.id AS permission_id
FROM roles r
JOIN permissions p ON p.code IN ('BOARDING_PASS_READ', 'BOARDING_PASS_WRITE')
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 2. Grant CUSTOMER role the read and write permissions needed for the
--    passenger journey: search flights → create booking → generate ticket →
--    generate boarding pass → view/download PDF.
--    Customers should NOT get FLIGHT_WRITE, USER_*, ROLE_*, PERMISSION_*, PAYMENT_*.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id AS role_id, p.id AS permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'FLIGHT_READ',
    'BOOKING_READ',
    'BOOKING_WRITE',
    'PASSENGER_READ',
    'PASSENGER_WRITE',
    'TICKET_READ',
    'TICKET_WRITE',
    'BOARDING_PASS_READ',
    'BOARDING_PASS_WRITE'
)
WHERE r.name = 'CUSTOMER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Grant AGENT role the same passenger-facing permissions plus extra
--    read access that agents typically need.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id AS role_id, p.id AS permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'FLIGHT_READ',
    'BOOKING_READ',
    'BOOKING_WRITE',
    'PASSENGER_READ',
    'PASSENGER_WRITE',
    'TICKET_READ',
    'TICKET_WRITE',
    'BOARDING_PASS_READ',
    'BOARDING_PASS_WRITE'
)
WHERE r.name = 'AGENT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
