import { useState, useEffect } from 'react';
import { colors, spacing, radius, fonts } from '../styles/theme';
import { checkSeatAvailability } from '../api/bookings';

/**
 * Seat pricing categories (INR) — used as fallback when backend prices unavailable.
 */
export const SEAT_PRICING = {
  STANDARD: { label: 'Standard Seat', price: 0 },
  WINDOW: { label: 'Window Seat', price: 300 },
  AISLE: { label: 'Aisle Seat', price: 200 },
  MIDDLE: { label: 'Middle Seat', price: 0 },
  BUSINESS: { label: 'Business Class', price: 800 },
};

/**
 * Determine seat category from seat data.
 */
function getSeatCategory(seatData) {
  if (!seatData) return 'STANDARD';
  const type = seatData.seatType || seatData.seat_type;
  if (type === 'WINDOW') return 'WINDOW';
  if (type === 'AISLE') return 'AISLE';
  if (type === 'MIDDLE') return 'MIDDLE';
  if (seatData.seatClass === 'BUSINESS') return 'BUSINESS';
  return 'STANDARD';
}

export function getSeatPrice(seatId, seatDataMap) {
  if (seatDataMap && seatDataMap[seatId]) {
    const price = seatDataMap[seatId].price;
    return price != null ? parseFloat(price) : 0;
  }
  // Fallback to heuristic
  if (!seatId || seatId.length < 2) return 0;
  const col = seatId.slice(-1);
  const row = parseInt(seatId.slice(0, -1));
  if (col === 'A' || col === 'F') return 300;
  if (col === 'C' || col === 'D') return 200;
  return 0;
}

export function getSeatTypeLabel(seatId, seatDataMap) {
  if (seatDataMap && seatDataMap[seatId]) {
    const type = seatDataMap[seatId].seatType || seatDataMap[seatId].seat_type;
    const cls = seatDataMap[seatId].seatClass || seatDataMap[seatId].seat_class;
    if (cls === 'BUSINESS') return 'Business Class';
    if (type === 'WINDOW') return 'Window Seat';
    if (type === 'AISLE') return 'Aisle Seat';
    if (type === 'MIDDLE') return 'Middle Seat';
    return 'Standard Seat';
  }
  if (!seatId || seatId.length < 2) return 'Standard';
  const col = seatId.slice(-1);
  if (col === 'A' || col === 'F') return 'Window Seat';
  if (col === 'C' || col === 'D') return 'Aisle Seat';
  return 'Standard Seat';
}

const seatStyle = (isSelected, status) => {
  const isOccupied = status === 'OCCUPIED';
  const isBlocked = status === 'BLOCKED';
  return {
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
  };
};

export default function SeatChart({ selectedSeat, onSelectSeat, flightId }) {
  const [hoveredSeat, setHoveredSeat] = useState(null);
  const [seatData, setSeatData] = useState(null);
  const [loading, setLoading] = useState(false);

  // Fetch real seat data from backend if flightId provided
  useEffect(() => {
    if (!flightId) return;
    setLoading(true);
    checkSeatAvailability(flightId)
      .then(res => {
        setSeatData(res.data.data);
      })
      .catch(() => {
        // Fallback to static data
        setSeatData(null);
      })
      .finally(() => setLoading(false));
  }, [flightId]);

  // Build seat map from backend data
  const seats = seatData?.seats || [];
  const seatMap = {};
  seats.forEach(s => { seatMap[s.seatNumber] = s; });

  // Group seats by row
  const columns = ['A', 'B', 'C', 'D', 'E', 'F'];
  const rows = [...new Set(seats.map(s => s.rowNumber))].sort((a, b) => a - b);

  // Limit display to manageable rows (show first 12 rows for UX)
  const displayRows = rows.length > 12 ? rows.slice(0, 12) : rows;

  const getSeatStatus = (seatNumber) => {
    const s = seatMap[seatNumber];
    if (!s) return 'AVAILABLE';
    return s.status || (s.isAvailable ? 'AVAILABLE' : 'OCCUPIED');
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: spacing.xxl, color: colors.textSecondary }}>
        Loading seat map...
      </div>
    );
  }

  if (!seatData || seats.length === 0) {
    // Fallback: render static seat map
    return <StaticSeatChart selectedSeat={selectedSeat} onSelectSeat={onSelectSeat} />;
  }

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

      {/* Aircraft info */}
      <div style={{
        textAlign: 'center', marginBottom: spacing.md,
        fontSize: fonts.sm, color: colors.textSecondary,
      }}>
        {seatData.aircraftType || 'Aircraft'} · {seatData.totalSeats} seats · {seatData.availableSeats} available
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
        {/* Nose */}
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
          {columns.slice(0, 3).map(col => (
            <div key={col} style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted }}>{col}</div>
          ))}
          <div />
          {columns.slice(3).map(col => (
            <div key={col} style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted }}>{col}</div>
          ))}
        </div>

        {/* Seat rows */}
        {displayRows.map(row => (
          <div key={row} style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 38px)',
            gap: spacing.xs,
            marginBottom: spacing.xs,
            alignItems: 'center',
            justifyItems: 'center',
          }}>
            {columns.slice(0, 3).map(col => {
              const seatNumber = `${row}${col}`;
              const status = getSeatStatus(seatNumber);
              const isSelected = selectedSeat === seatNumber;
              return (
                <button
                  key={seatNumber}
                  onClick={() => status === 'AVAILABLE' && onSelectSeat(seatNumber)}
                  onMouseEnter={() => setHoveredSeat(seatNumber)}
                  onMouseLeave={() => setHoveredSeat(null)}
                  disabled={status !== 'AVAILABLE'}
                  title={`${seatNumber} — ${getSeatTypeLabel(seatNumber, seatMap)} ₹${getSeatPrice(seatNumber, seatMap)}${status === 'OCCUPIED' ? ' (Occupied)' : status === 'BLOCKED' ? ' (Blocked)' : ''}`}
                  style={{
                    ...seatStyle(isSelected, status),
                    transform: hoveredSeat === seatNumber && status === 'AVAILABLE' ? 'scale(1.1)' : 'scale(1)',
                  }}
                >
                  {row}{col}
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
            {columns.slice(3).map(col => {
              const seatNumber = `${row}${col}`;
              const status = getSeatStatus(seatNumber);
              const isSelected = selectedSeat === seatNumber;
              return (
                <button
                  key={seatNumber}
                  onClick={() => status === 'AVAILABLE' && onSelectSeat(seatNumber)}
                  onMouseEnter={() => setHoveredSeat(seatNumber)}
                  onMouseLeave={() => setHoveredSeat(null)}
                  disabled={status !== 'AVAILABLE'}
                  title={`${seatNumber} — ${getSeatTypeLabel(seatNumber, seatMap)} ₹${getSeatPrice(seatNumber, seatMap)}${status === 'OCCUPIED' ? ' (Occupied)' : status === 'BLOCKED' ? ' (Blocked)' : ''}`}
                  style={{
                    ...seatStyle(isSelected, status),
                    transform: hoveredSeat === seatNumber && status === 'AVAILABLE' ? 'scale(1.1)' : 'scale(1)',
                  }}
                >
                  {row}{col}
                </button>
              );
            })}
          </div>
        ))}

        {rows.length > 12 && (
          <div style={{ textAlign: 'center', fontSize: fonts.sm, color: colors.textMuted, marginTop: spacing.md }}>
            + {rows.length - 12} more rows
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Fallback static seat chart when backend data unavailable.
 */
function StaticSeatChart({ selectedSeat, onSelectSeat }) {
  const [hoveredSeat, setHoveredSeat] = useState(null);
  const COLUMNS = ['A', 'B', 'C', 'D', 'E', 'F'];
  const ROWS = [1, 2, 3, 4, 5, 6, 7];

  return (
    <div>
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

      <div style={{
        background: colors.bg,
        borderRadius: `${radius.xl}px ${radius.xl}px ${radius.lg * 2}px ${radius.lg * 2}px`,
        padding: `${spacing.xxl}px ${spacing.xl}px`,
        border: `1px solid ${colors.border}`,
        maxWidth: 380,
        margin: '0 auto',
      }}>
        <div style={{
          width: 60, height: 20,
          margin: '0 auto',
          borderRadius: '30px 30px 0 0',
          background: colors.border,
          marginBottom: spacing.lg,
        }} />

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 38px)',
          gap: spacing.xs,
          marginBottom: spacing.sm,
          justifyItems: 'center',
        }}>
          {COLUMNS.slice(0, 3).map(col => (
            <div key={col} style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted }}>{col}</div>
          ))}
          <div />
          {COLUMNS.slice(3).map(col => (
            <div key={col} style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted }}>{col}</div>
          ))}
        </div>

        {ROWS.map(row => (
          <div key={row} style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 38px)',
            gap: spacing.xs,
            marginBottom: spacing.xs,
            alignItems: 'center',
            justifyItems: 'center',
          }}>
            {COLUMNS.slice(0, 3).map(col => {
              const seatId = `${row}${col}`;
              const isSelected = selectedSeat === seatId;
              return (
                <button
                  key={seatId}
                  onClick={() => onSelectSeat(seatId)}
                  onMouseEnter={() => setHoveredSeat(seatId)}
                  onMouseLeave={() => setHoveredSeat(null)}
                  style={{
                    ...seatStyle(isSelected, 'AVAILABLE'),
                    transform: hoveredSeat === seatId ? 'scale(1.1)' : 'scale(1)',
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
            {COLUMNS.slice(3).map(col => {
              const seatId = `${row}${col}`;
              const isSelected = selectedSeat === seatId;
              return (
                <button
                  key={seatId}
                  onClick={() => onSelectSeat(seatId)}
                  onMouseEnter={() => setHoveredSeat(seatId)}
                  onMouseLeave={() => setHoveredSeat(null)}
                  style={{
                    ...seatStyle(isSelected, 'AVAILABLE'),
                    transform: hoveredSeat === seatId ? 'scale(1.1)' : 'scale(1)',
                  }}
                >
                  {seatId}
                </button>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
