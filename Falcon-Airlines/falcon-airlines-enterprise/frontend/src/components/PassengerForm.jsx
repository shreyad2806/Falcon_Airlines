import { useState } from 'react';
import { colors, spacing, radius, fonts, input as inputStyle, buttons } from '../styles/theme';
import { XIcon, PlusIcon } from './Icons';
import { calculateAge, getPassengerType } from '../utils/format';

/**
 * Empty passenger template.
 */
const emptyPassenger = () => ({
  _key: Date.now() + Math.random(),
  firstName: '',
  lastName: '',
  dateOfBirth: '',
  gender: '',
  nationality: '',
  email: '',
  phone: '',
  documentType: 'PASSPORT',
  documentNumber: '',
});

/**
 * Validate a single passenger's required fields.
 */
function validatePassenger(p) {
  const errors = {};
  if (!p.firstName?.trim()) errors.firstName = 'First name is required';
  if (!p.lastName?.trim()) errors.lastName = 'Last name is required';
  if (!p.dateOfBirth) errors.dateOfBirth = 'Date of birth is required';
  if (p.dateOfBirth) {
    const age = calculateAge(p.dateOfBirth);
    if (age < 0) errors.dateOfBirth = 'Date of birth cannot be in the future';
    if (age > 120) errors.dateOfBirth = 'Please enter a valid date of birth';
  }
  if (!p.gender) errors.gender = 'Gender is required';
  if (p.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(p.email)) errors.email = 'Please enter a valid email';
  if (p.phone && !/^\+?[0-9\s\-()]{7,20}$/.test(p.phone)) errors.phone = 'Please enter a valid phone number';
  return errors;
}

/**
 * Passenger form fields with validation.
 * Supports "Myself" (pre-filled) and "Someone else" (blank) modes.
 */
export default function PassengerForm({ passengers, onChange, myPassenger }) {
  const [bookingFor, setBookingFor] = useState('myself');
  const [expandedIdx, setExpandedIdx] = useState(0);

  const handleBookingForChange = (mode) => {
    setBookingFor(mode);
    if (mode === 'myself' && myPassenger) {
      onChange([{
        ...emptyPassenger(),
        firstName: myPassenger.firstName || '',
        lastName: myPassenger.lastName || '',
        email: myPassenger.email || '',
        _isMe: true,
      }]);
    } else {
      onChange([emptyPassenger()]);
    }
    setExpandedIdx(0);
  };

  const updatePassenger = (idx, field, value) => {
    const updated = [...passengers];
    updated[idx] = { ...updated[idx], [field]: value };
    onChange(updated);
  };

  const addPassenger = () => {
    onChange([...passengers, emptyPassenger()]);
    setExpandedIdx(passengers.length);
  };

  const removePassenger = (idx) => {
    if (passengers.length <= 1) return;
    const updated = passengers.filter((_, i) => i !== idx);
    onChange(updated);
    if (expandedIdx >= updated.length) setExpandedIdx(updated.length - 1);
  };

  const allValid = passengers.every(p => Object.keys(validatePassenger(p)).length === 0);

  const fieldStyle = (hasError) => ({
    ...inputStyle,
    borderColor: hasError ? colors.danger : colors.border,
    height: 40,
  });

  return (
    <div>
      {/* Booking for toggle */}
      <div style={{ marginBottom: spacing.xl }}>
        <label style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.text, display: 'block', marginBottom: spacing.sm }}>
          Booking for
        </label>
        <div style={{ display: 'flex', gap: spacing.sm }}>
          <button
            onClick={() => handleBookingForChange('myself')}
            style={{
              ...buttons.secondary,
              background: bookingFor === 'myself' ? colors.primary : colors.white,
              color: bookingFor === 'myself' ? 'white' : colors.text,
              borderColor: bookingFor === 'myself' ? colors.primary : colors.border,
            }}
          >
            Myself
          </button>
          <button
            onClick={() => handleBookingForChange('someone')}
            style={{
              ...buttons.secondary,
              background: bookingFor === 'someone' ? colors.primary : colors.white,
              color: bookingFor === 'someone' ? 'white' : colors.text,
              borderColor: bookingFor === 'someone' ? colors.primary : colors.border,
            }}
          >
            Someone else
          </button>
        </div>
      </div>

      {/* Passenger cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: spacing.lg }}>
        {passengers.map((p, idx) => {
          const errors = validatePassenger(p);
          const isExpanded = expandedIdx === idx;
          const age = calculateAge(p.dateOfBirth);
          const pType = getPassengerType(p.dateOfBirth);
          const hasErrors = Object.keys(errors).length > 0;

          return (
            <div key={p._key || idx} style={{
              background: colors.bgCard,
              border: `1px solid ${hasErrors ? colors.danger : colors.border}`,
              borderRadius: radius.lg,
              overflow: 'hidden',
            }}>
              {/* Card header */}
              <div
                onClick={() => setExpandedIdx(isExpanded ? -1 : idx)}
                style={{
                  padding: `${spacing.md}px ${spacing.lg}px`,
                  background: colors.bg,
                  borderBottom: `1px solid ${colors.border}`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  cursor: 'pointer',
                  gap: spacing.sm,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: spacing.md }}>
                  <div style={{
                    width: 32, height: 32, borderRadius: '50%',
                    background: p._isMe ? colors.primary + '15' : '#EDE9FE',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 14, fontWeight: 600, color: p._isMe ? colors.primary : '#7C3AED',
                  }}>
                    {p.firstName ? p.firstName[0].toUpperCase() : (idx + 1)}
                  </div>
                  <div>
                    <span style={{ fontWeight: 600, fontSize: fonts.base, color: colors.text }}>
                      {p.firstName && p.lastName ? `${p.firstName} ${p.lastName}` : `Passenger ${idx + 1}`}
                    </span>
                    {p.dateOfBirth && (
                      <span style={{ fontSize: fonts.sm, color: colors.textSecondary, marginLeft: spacing.sm }}>
                        {pType} · {age} yrs
                      </span>
                    )}
                    {p._isMe && (
                      <span style={{ fontSize: fonts.xs, color: colors.primary, marginLeft: spacing.sm, fontWeight: 500 }}>
                        (You)
                      </span>
                    )}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
                  {hasErrors && (
                    <span style={{ fontSize: fonts.xs, color: colors.danger }}>⚠ Incomplete</span>
                  )}
                  {passengers.length > 1 && (
                    <button
                      onClick={(e) => { e.stopPropagation(); removePassenger(idx); }}
                      style={{ ...buttons.icon, color: colors.danger, width: 28, height: 28 }}
                      title="Remove passenger"
                    >
                      <XIcon size={14} />
                    </button>
                  )}
                </div>
              </div>

              {/* Expanded form */}
              {isExpanded && (
                <div style={{ padding: spacing.xl }}>
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: '1fr 1fr',
                    gap: `${spacing.lg}px`,
                  }}>
                    {/* First name */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        First Name *
                      </label>
                      <input
                        value={p.firstName}
                        onChange={(e) => updatePassenger(idx, 'firstName', e.target.value)}
                        placeholder="First name"
                        style={fieldStyle(errors.firstName)}
                      />
                      {errors.firstName && <div style={{ color: colors.danger, fontSize: fonts.xs, marginTop: 2 }}>{errors.firstName}</div>}
                    </div>

                    {/* Last name */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Last Name *
                      </label>
                      <input
                        value={p.lastName}
                        onChange={(e) => updatePassenger(idx, 'lastName', e.target.value)}
                        placeholder="Last name"
                        style={fieldStyle(errors.lastName)}
                      />
                      {errors.lastName && <div style={{ color: colors.danger, fontSize: fonts.xs, marginTop: 2 }}>{errors.lastName}</div>}
                    </div>

                    {/* Date of birth */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Date of Birth *
                      </label>
                      <input
                        type="date"
                        value={p.dateOfBirth}
                        onChange={(e) => updatePassenger(idx, 'dateOfBirth', e.target.value)}
                        style={fieldStyle(errors.dateOfBirth)}
                      />
                      {errors.dateOfBirth && <div style={{ color: colors.danger, fontSize: fonts.xs, marginTop: 2 }}>{errors.dateOfBirth}</div>}
                      {p.dateOfBirth && !errors.dateOfBirth && (
                        <div style={{ fontSize: fonts.xs, color: colors.textSecondary, marginTop: 2 }}>
                          {pType} · {age} years old
                        </div>
                      )}
                    </div>

                    {/* Gender */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Gender *
                      </label>
                      <select
                        value={p.gender}
                        onChange={(e) => updatePassenger(idx, 'gender', e.target.value)}
                        style={fieldStyle(errors.gender)}
                      >
                        <option value="">Select gender</option>
                        <option value="M">Male</option>
                        <option value="F">Female</option>
                        <option value="O">Other</option>
                      </select>
                      {errors.gender && <div style={{ color: colors.danger, fontSize: fonts.xs, marginTop: 2 }}>{errors.gender}</div>}
                    </div>

                    {/* Email */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Email
                      </label>
                      <input
                        type="email"
                        value={p.email}
                        onChange={(e) => updatePassenger(idx, 'email', e.target.value)}
                        placeholder="email@example.com"
                        style={fieldStyle(errors.email)}
                      />
                      {errors.email && <div style={{ color: colors.danger, fontSize: fonts.xs, marginTop: 2 }}>{errors.email}</div>}
                    </div>

                    {/* Phone */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Phone
                      </label>
                      <input
                        value={p.phone}
                        onChange={(e) => updatePassenger(idx, 'phone', e.target.value)}
                        placeholder="+91 98765 43210"
                        style={fieldStyle(errors.phone)}
                      />
                      {errors.phone && <div style={{ color: colors.danger, fontSize: fonts.xs, marginTop: 2 }}>{errors.phone}</div>}
                    </div>

                    {/* Document type */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Document Type
                      </label>
                      <select
                        value={p.documentType}
                        onChange={(e) => updatePassenger(idx, 'documentType', e.target.value)}
                        style={fieldStyle(false)}
                      >
                        <option value="PASSPORT">Passport</option>
                        <option value="AADHAAR">Aadhaar Card</option>
                        <option value="PAN">PAN Card</option>
                        <option value="DRIVING_LICENSE">Driving License</option>
                      </select>
                    </div>

                    {/* Document number */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Document Number
                      </label>
                      <input
                        value={p.documentNumber}
                        onChange={(e) => updatePassenger(idx, 'documentNumber', e.target.value)}
                        placeholder="Document number"
                        style={fieldStyle(false)}
                      />
                    </div>

                    {/* Nationality */}
                    <div>
                      <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.xs }}>
                        Nationality
                      </label>
                      <input
                        value={p.nationality}
                        onChange={(e) => updatePassenger(idx, 'nationality', e.target.value)}
                        placeholder="e.g. IND"
                        maxLength={3}
                        style={{ ...fieldStyle(false), textTransform: 'uppercase' }}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Add passenger button */}
      <button
        onClick={addPassenger}
        style={{
          ...buttons.secondary,
          marginTop: spacing.lg,
          borderStyle: 'dashed',
          width: '100%',
          justifyContent: 'center',
          color: colors.primary,
        }}
      >
        <PlusIcon size={16} />
        Add Passenger
      </button>
    </div>
  );
}

export { validatePassenger };
