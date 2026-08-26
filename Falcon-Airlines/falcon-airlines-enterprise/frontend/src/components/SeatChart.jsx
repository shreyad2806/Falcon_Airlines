import { useState } from 'react';
import { colors, spacing, radius, fonts } from '../styles/theme';

/**
 * Seat pricing categories (INR).
 */
export const SEAT_PRICING = {
  STANDARD: { label: 'Standard Seat', price: 0 },
  WINDOW: { label: 'Window Seat', price: 500 },
  AISLE: { label: 'Aisle Seat', price: 400 },
  EXTRA_LEGROOM: { label: 'Extra Legroom', price: 1500 },
  EXIT_ROW: { label: 'Exit Row', price: 2000 },
};

/**
 * Determine seat category based on column letter and row.
 */
function getSeatCategory(col, row) {
  if (col === 'A' || col === 'F') return 'WINDOW';
  if (col === 'C' || col === 'D') return 'AISLE';
  if (row >= 6) return 'EXTRA_LEGROOM';
  if (row === 7) return 'EXIT_ROW';
  return 'STANDARD';
}

function getSeatPrice(seatId) {
  if (!seatId || seatId.length < 2) return 0;
  const col = seatId.slice(-1);
  const row = parseInt(seatId.slice(0, -1));
  const cat = getSeatCategory(col, row);
  return SEAT_PRICING[cat].price;
}

function getSeatTypeLabel(seatId) {
  if (!seatId || seatId.length < 2) return 'Standard';
  const col = seatId.slice(-1);
  const row = parseInt(seatId.slice(0, -1));
  const cat = getSeatCategory(col, row);
  return SEAT_PRICING[cat].label;
}

const COLUMNS = ['A', 'B', 'C', 'D', 'E', 'F'];
const ROWS = [1, 2, 3, 4, 5, 6, 7];
const OCCUPIED_SEATS = new Set(['3A', '3B', '4C', '5D', '5E', '6A']);
const BLOCKED_SEATS = new Set(['1C', '1D', '7A', '7F']);

export { getSeatPrice, getSeatTypeLabel };

const seatStyle = (isSelected, isOccupied, isBlocked) => ({
  width: 38,
  height: 38,
  borderRadius: radius.sm,
  border: `2px solid ${
    isSelected ? colors.success
    : isBlocked ? '#D4A0A0'
    : isOccupied ? colors.border
    : '#CBD5E1'
  }`,
  background: isSelected
    ? colors.success
    : isOccupied ? '#D1D5DB'
    : isBlocked ? '#F3D4D4'
    : colors.white,
  color: isSelected ? 'white' : isOccupied ? '#9CA3AF' : colors.textSecondary,
  cursor: isOccupied || isBlocked ? 'not-allowed' : 'pointer',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: fonts.sm,
  fontWeight: 600,
  transition: 'all 0.15s ease',
  opacity: isOccupied ? 0.6 : 1,
});

export default function SeatChart({ selectedSeat, onSelectSeat }) {
  const [hoveredSeat, setHoveredSeat] = useState(null);

  return (
    <div>
      {/* Legend */}
      <div style={{ display: 'flex', gap: spacing.xl, marginBottom: spacing.xl, flexWrap: 'wrap' }}>
        {[
          { color: colors.white, border: '#CBD5E1', label: 'Available' },
          { color: colors.success, border: colors.success, label: 'Selected' },
          { color: '#D1D5DB', border: colors.border, label: 'Occupied' },
          { color: '#F3D4D4', border: '#D4A0A0', label: 'Blocked' },
        ].map((item) => (
          <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
            <div style={{
              width: 20, height: 20, borderRadius: 4,
              background: item.color, border: `2px solid ${item.border}`,
            }} />
            <span style={{ fontSize: fonts.sm, color: colors.textSecondary }}>{item.label}</span>
          </div>
        ))}
      </div>

      {/* Airplane body */}
      <div style={{
        background: colors.bg,
        borderRadius: `${radius.xl}px ${radius.xl}px ${radius.lg * 2}px ${radius.lg * 2}px`,
        padding: `${spacing.xxl}px ${spacing.xl}px`,
        border: `1px solid ${colors.border}`,
        maxWidth: 380,
        margin: '0 auto',
      }}>
        {/* Airplane nose */}
        <div style={{
          width: 60, height: 20,
          margin: '0 auto',
          borderRadius: '30px 30px 0 0',
          background: colors.border,
          marginBottom: spacing.lg,
        }} />

        {/* Column headers */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 38px)',
          gap: spacing.xs,
          marginBottom: spacing.sm,
          justifyItems: 'center',
        }}>
          {COLUMNS.slice(0, 3).map((col) => (
            <div key={col} style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted }}>{col}</div>
          ))}
          <div />
          {COLUMNS.slice(3).map((col) => (
            <div key={col} style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted }}>{col}</div>
          ))}
        </div>

        {/* Seat rows */}
        {ROWS.map((row) => (
          <div key={row} style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 38px)',
            gap: spacing.xs,
            marginBottom: spacing.xs,
            alignItems: 'center',
            justifyItems: 'center',
          }}>
            {COLUMNS.slice(0, 3).map((col) => {
              const seatId = `${row}${col}`;
              const isOccupied = OCCUPIED_SEATS.has(seatId);
              const isBlocked = BLOCKED_SEATS.has(seatId);
              const isSelected = selectedSeat === seatId;
              return (
                <button
                  key={seatId}
                  onClick={() => !isOccupied && !isBlocked && onSelectSeat(seatId)}
                  onMouseEnter={() => setHoveredSeat(seatId)}
                  onMouseLeave={() => setHoveredSeat(null)}
                  disabled={isOccupied || isBlocked}
                  title={`${seatId} — ${SEAT_PRICING[getSeatCategory(col, row)].label} ₹${SEAT_PRICING[getSeatCategory(col, row)].price}${isOccupied ? ' (Occupied)' : isBlocked ? ' (Blocked)' : ''}`}
                  style={{
                    ...seatStyle(isSelected, isOccupied, isBlocked),
                    transform: hoveredSeat === seatId && !isOccupied && !isBlocked ? 'scale(1.1)' : 'scale(1)',
                  }}
                >
                  {seatId}
                </button>
              );
            })}
            <div style={{
              width: 20,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: fonts.xs,
              color: colors.textMuted,
            }}>♪</div>
            {COLUMNS.slice(3).map((col) => {
              const seatId = `${row}${col}`;
              const isOccupied = OCCUPIED_SEATS.has(seatId);
              const isBlocked = BLOCKED_SEATS.has(seatId);
              const isSelected = selectedSeat === seatId;
              return (
                <button
                  key={seatId}
                  onClick={() => !isOccupied && !isBlocked && onSelectSeat(seatId)}
                  onMouseEnter={() => setHoveredSeat(seatId)}
                  onMouseLeave={() => setHoveredSeat(null)}
                  disabled={isOccupied || isBlocked}
                  title={`${seatId} — ${SEAT_PRICING[getSeatCategory(col, row)].label} ₹${SEAT_PRICING[getSeatCategory(col, row)].price}${isOccupied ? ' (Occupied)' : isBlocked ? ' (Blocked)' : ''}`}
                  style={{
                    ...seatStyle(isSelected, isOccupied, isBlocked),
                    transform: hoveredSeat === seatId && !isOccupied && !isBlocked ? 'scale(1.1)' : 'scale(1)',
                  }}
                >
                  {seatId}
                </button>
              );
            })}
          </div>
        ))}

        {/* Exit row indicator */}
        <div style={{
          marginTop: spacing.md,
          marginBottom: spacing.sm,
          textAlign: 'center',
          fontSize: fonts.xs,
          color: colors.textMuted,
          letterSpacing: 1,
          fontWeight: 500,
        }}>
          ✦ EXIT ✦
        </div>
      </div>
    </div>
  );
}
