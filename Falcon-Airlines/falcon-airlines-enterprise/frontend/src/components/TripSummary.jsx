import { colors, spacing, radius, fonts } from '../styles/theme';
import { PlaneTakeoffIcon } from './Icons';
import { formatINR, formatDate, formatTime, calculateDuration } from '../utils/format';

/**
 * Price breakdown row.
 */
function PriceRow({ label, value, bold, large }) {
  return (
    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: `${bold ? spacing.sm : spacing.xs}px 0`,
      fontSize: large ? fonts.lg : fonts.base,
      fontWeight: bold ? 700 : 400,
      color: bold ? colors.text : colors.textSecondary,
    }}>
      <span>{label}</span>
      <span>{value}</span>
    </div>
  );
}

/**
 * Sticky trip summary panel showing flight, passengers, seats, and INR price breakdown.
 */
export default function TripSummary({
  flight,
  passengers = [],
  selectedSeat,
  seatPrice = 0,
  baseFare = 0,
  taxes = 0,
  total = 0,
}) {
  return (
    <div style={{
      background: colors.bgCard,
      borderRadius: radius.lg,
      border: `1px solid ${colors.border}`,
      padding: spacing.xl,
      position: 'sticky',
      top: 80,
    }}>
      <h4 style={{
        fontSize: fonts.base,
        fontWeight: 600,
        color: colors.text,
        marginBottom: spacing.lg,
        textTransform: 'uppercase',
        letterSpacing: 0.5,
      }}>
        Trip Summary
      </h4>

      {/* Flight info */}
      {flight && (
        <div style={{ marginBottom: spacing.lg }}>
          <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: spacing.xs }}>Flight</div>
          <div style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.text }}>
            {flight.flightNumber}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: spacing.sm, marginTop: spacing.xs }}>
            <span style={{ fontWeight: 600, fontSize: fonts.base }}>{flight.originAirportIataCode}</span>
            <PlaneTakeoffIcon size={14} color={colors.textMuted} />
            <span style={{ fontWeight: 600, fontSize: fonts.base }}>{flight.destinationAirportIataCode}</span>
          </div>
          <div style={{ fontSize: fonts.sm, color: colors.textSecondary, marginTop: spacing.xs }}>
            {formatDate(flight.scheduledDeparture)} · {formatTime(flight.scheduledDeparture)} → {formatTime(flight.scheduledArrival)}
          </div>
          <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>
            Duration: {calculateDuration(flight.scheduledDeparture, flight.scheduledArrival)}
          </div>
        </div>
      )}

      {/* Passengers */}
      {passengers.length > 0 && (
        <div style={{ marginBottom: spacing.lg }}>
          <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: spacing.xs }}>Passengers</div>
          {passengers.map((p, i) => (
            <div key={i} style={{
              fontSize: fonts.base,
              color: colors.text,
              padding: `${spacing.xs}px 0`,
              display: 'flex',
              justifyContent: 'space-between',
            }}>
              <span>{p.firstName} {p.lastName}</span>
              <span style={{ fontSize: fonts.sm, color: colors.textSecondary }}>{p._type || 'Adult'}</span>
            </div>
          ))}
        </div>
      )}

      {/* Seat */}
      {selectedSeat && (
        <div style={{ marginBottom: spacing.lg }}>
          <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: spacing.xs }}>Seat</div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: fonts.base }}>
            <span style={{ fontWeight: 600 }}>{selectedSeat}</span>
            <span style={{ color: colors.textSecondary }}>{seatPrice > 0 ? `+${formatINR(seatPrice)}` : 'Free'}</span>
          </div>
        </div>
      )}

      {/* Price breakdown */}
      <div style={{
        borderTop: `1px solid ${colors.border}`,
        paddingTop: spacing.md,
      }}>
        <PriceRow label="Base Fare" value={formatINR(baseFare)} />
        <PriceRow label="Taxes & Fees (12% GST)" value={formatINR(taxes)} />
        {seatPrice > 0 && <PriceRow label="Seat Selection" value={formatINR(seatPrice)} />}
        <div style={{ borderTop: `1px solid ${colors.border}`, marginTop: spacing.sm }}>
          <PriceRow label="Total" value={formatINR(total)} bold large />
        </div>
      </div>
    </div>
  );
}
