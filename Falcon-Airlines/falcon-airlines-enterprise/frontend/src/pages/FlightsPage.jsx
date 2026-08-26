import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import * as flightsApi from '../api/flights';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import { colors, spacing, radius, fonts, input as inputStyle, buttons } from '../styles/theme';
import { SearchIcon, ChevronLeftIcon, ChevronRightIcon, PlaneTakeoffIcon } from '../components/Icons';
import { formatINR, formatTime, calculateDuration, formatDate } from '../utils/format';

/**
 * Airport codes for the search dropdown.
 */
const AIRPORTS = [
  { code: 'DEL', name: 'Delhi', city: 'New Delhi' },
  { code: 'BOM', name: 'Mumbai', city: 'Mumbai' },
  { code: 'JFK', name: 'New York JFK', city: 'New York' },
  { code: 'LHR', name: 'London Heathrow', city: 'London' },
  { code: 'DXB', name: 'Dubai', city: 'Dubai' },
  { code: 'SIN', name: 'Singapore Changi', city: 'Singapore' },
];

/**
 * Seat availability indicator.
 */
function SeatIndicator({ available, total }) {
  if (available == null || total == null) return <span style={{ color: colors.textMuted }}>—</span>;

  let color = colors.success;
  let label = `${available} seats`;
  if (available === 0) { color = colors.danger; label = 'Sold Out'; }
  else if (available < 20) { color = '#DC3545'; label = `Almost Full · ${available}`; }
  else if (available < 100) { color = '#F59E0B'; label = `Limited · ${available}`; }

  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 4,
      fontSize: fonts.sm, fontWeight: 500, color,
      padding: '3px 10px', borderRadius: radius.full,
      background: color + '10',
    }}>
      <span style={{ width: 6, height: 6, borderRadius: '50%', background: color }} />
      {label}
    </span>
  );
}

/**
 * Skeleton row for loading state.
 */
function SkeletonRow() {
  const shimmer = { background: `linear-gradient(90deg, ${colors.borderLight} 25%, ${colors.bg} 50%, ${colors.borderLight} 75%)`, backgroundSize: '200% 100%', animation: 'shimmer 1.5s infinite' };
  return (
    <tr style={{ borderBottom: `1px solid ${colors.borderLight}` }}>
      {[120, 80, 140, 140, 60, 100, 80, 80].map((w, i) => (
        <td key={i} style={{ padding: `${spacing.lg}px ${spacing.lg}px` }}>
          <div style={{ width: w, height: 16, borderRadius: 4, ...shimmer }} />
        </td>
      ))}
    </tr>
  );
}

export default function FlightsPage() {
  const navigate = useNavigate();
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [provider, setProvider] = useState('');

  // Search state
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [departureDate, setDepartureDate] = useState(() => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  });
  const [flightNumber, setFlightNumber] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: pageSize };
      if (origin) params.originAirport = origin;
      if (destination) params.destinationAirport = destination;
      if (departureDate) params.departureFrom = departureDate + 'T00:00:00Z';
      if (flightNumber) params.flightNumber = flightNumber;
      if (statusFilter) params.status = statusFilter;

      const res = await flightsApi.searchFlights(params);
      const content = res.data.data?.content || [];
      setFlights(content);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load flights');
    } finally {
      setLoading(false);
    }
  }, [page, origin, destination, departureDate, flightNumber, statusFilter]);

  useEffect(() => { load(); }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    load();
  };

  const clearFilters = () => {
    setOrigin('');
    setDestination('');
    setDepartureDate(new Date().toISOString().split('T')[0]);
    setFlightNumber('');
    setStatusFilter('');
    setPage(0);
  };

  const isPast = (f) => f.scheduledDeparture && new Date(f.scheduledDeparture) < new Date();
  const hasFilters = origin || destination || flightNumber || statusFilter;

  // Swap origin/destination
  const swapRoute = () => {
    setOrigin(destination);
    setDestination(origin);
  };

  return (
    <div>
      <PageHeader title="Flights" description="Find and book your next journey." />

      {/* Search bar */}
      <form onSubmit={handleSearch} style={{
        background: colors.bgCard,
        borderRadius: radius.lg,
        border: `1px solid ${colors.border}`,
        padding: spacing.xl,
        marginBottom: spacing.lg,
      }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto 1fr 160px auto', gap: spacing.sm, alignItems: 'end' }} className="flight-search-grid">
          {/* Origin */}
          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.textSecondary, display: 'block', marginBottom: spacing.xs }}>From</label>
            <select value={origin} onChange={(e) => setOrigin(e.target.value)} style={{ ...inputStyle, height: 44 }}>
              <option value="">Any Origin</option>
              {AIRPORTS.map(a => <option key={a.code} value={a.code}>{a.code} — {a.name}</option>)}
            </select>
          </div>

          {/* Swap button */}
          <button type="button" onClick={swapRoute} style={{
            ...buttons.icon, marginBottom: 2, color: colors.primary,
            width: 36, height: 36, alignSelf: 'end',
          }}>⇄</button>

          {/* Destination */}
          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.textSecondary, display: 'block', marginBottom: spacing.xs }}>To</label>
            <select value={destination} onChange={(e) => setDestination(e.target.value)} style={{ ...inputStyle, height: 44 }}>
              <option value="">Any Destination</option>
              {AIRPORTS.map(a => <option key={a.code} value={a.code}>{a.code} — {a.name}</option>)}
            </select>
          </div>

          {/* Date */}
          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.textSecondary, display: 'block', marginBottom: spacing.xs }}>Departure</label>
            <input type="date" value={departureDate} onChange={(e) => setDepartureDate(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              style={{ ...inputStyle, height: 44 }} />
          </div>

          {/* Search button */}
          <button type="submit" disabled={loading} style={{ ...buttons.primary, height: 44, minWidth: 120 }}>
            <SearchIcon size={16} color="white" />
            Search Flights
          </button>
        </div>

        {/* Advanced filters toggle */}
        <div style={{ marginTop: spacing.md }}>
          <button type="button" onClick={() => setShowAdvanced(!showAdvanced)}
            style={{ ...buttons.ghost, fontSize: fonts.sm, color: colors.primary, padding: '4px 8px' }}>
            {showAdvanced ? 'Hide' : 'Advanced Filters'}
          </button>
        </div>

        {/* Advanced filters */}
        {showAdvanced && (
          <div style={{ display: 'flex', gap: spacing.sm, marginTop: spacing.sm, flexWrap: 'wrap' }}>
            <input placeholder="Flight number" value={flightNumber} onChange={(e) => setFlightNumber(e.target.value)}
              style={{ ...inputStyle, width: 160, height: 38 }} />
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}
              style={{ ...inputStyle, width: 140, height: 38 }}>
              <option value="">All Status</option>
              <option value="SCHEDULED">Scheduled</option>
              <option value="DELAYED">Delayed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
            {hasFilters && (
              <button type="button" onClick={clearFilters}
                style={{ ...buttons.ghost, color: colors.primary, fontSize: fonts.sm, height: 38 }}>
                Clear All
              </button>
            )}
          </div>
        )}
      </form>

      {/* Error */}
      {error && (
        <div style={{
          color: colors.danger, background: colors.dangerLight,
          padding: `${spacing.md}px ${spacing.lg}px`, borderRadius: radius.md,
          marginBottom: spacing.lg, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <span>{error}</span>
          <button onClick={load} style={{ ...buttons.secondary, height: 32, fontSize: fonts.sm }}>Try Again</button>
        </div>
      )}

      {/* Results header */}
      {!loading && !error && (
        <div style={{ marginBottom: spacing.md, fontSize: fonts.sm, color: colors.textSecondary }}>
          Showing {flights.length} flight{flights.length !== 1 ? 's' : ''}
          {origin && destination ? ` from ${origin} → ${destination}` : ''}
          {departureDate ? ` on ${formatDate(departureDate)}` : ''}
        </div>
      )}

      {/* Flight table */}
      {loading ? (
        <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              {[1, 2, 3, 4, 5].map(i => <SkeletonRow key={i} />)}
            </tbody>
          </table>
        </div>
      ) : flights.length === 0 ? (
        <EmptyState
          icon="✈"
          heading={hasFilters ? 'No flights found' : 'No flights available'}
          text={hasFilters ? 'Try changing your route or travel date.' : 'No flights match the current criteria.'}
          ctaLabel={hasFilters ? 'Clear Filters' : undefined}
          ctaTo={hasFilters ? undefined : undefined}
        />
      ) : (
        <>
          <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, overflow: 'hidden' }}>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: fonts.base }}>
                <thead>
                  <tr style={{ borderBottom: `2px solid ${colors.border}` }}>
                    {['Flight', 'Route', 'Departure', 'Arrival', 'Duration', 'Status', 'Seats', 'Price', 'Action'].map(h => (
                      <th key={h} style={{
                        padding: `${spacing.md}px ${spacing.lg}px`, textAlign: 'left',
                        fontSize: fonts.sm, fontWeight: 600, color: colors.textSecondary,
                        background: colors.bg, whiteSpace: 'nowrap',
                      }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {flights.map((f) => {
                    const past = isPast(f);
                    const cancelled = f.status === 'CANCELLED';
                    const canBook = !past && !cancelled && f.availableSeats > 0;
                    return (
                      <tr key={f.id || f.flightNumber} style={{
                        borderBottom: `1px solid ${colors.borderLight}`,
                        opacity: past ? 0.55 : 1,
                        transition: 'background 0.1s',
                      }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = colors.bg; }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
                      >
                        {/* Flight */}
                        <td style={{ padding: `${spacing.lg}px` }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
                            <div style={{
                              width: 32, height: 32, borderRadius: radius.sm,
                              background: colors.primary + '0D',
                              display: 'flex', alignItems: 'center', justifyContent: 'center',
                              fontSize: 14, color: colors.primary, fontWeight: 600,
                            }}>
                              <PlaneTakeoffIcon size={16} color={colors.primary} />
                            </div>
                            <div>
                              <div style={{ fontWeight: 600, color: colors.text }}>{f.flightNumber}</div>
                              <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>{f.airlineName || 'Falcon Airlines'}</div>
                            </div>
                          </div>
                        </td>

                        {/* Route */}
                        <td style={{ padding: `${spacing.lg}px` }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
                            <span style={{ fontWeight: 600, fontSize: fonts.lg }}>{f.originAirportIataCode}</span>
                            <span style={{ color: colors.textMuted, fontSize: fonts.sm }}>→</span>
                            <span style={{ fontWeight: 600, fontSize: fonts.lg }}>{f.destinationAirportIataCode}</span>
                          </div>
                          <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>
                            {f.originAirportName || f.originAirportIataCode} → {f.destinationAirportName || f.destinationAirportIataCode}
                          </div>
                        </td>

                        {/* Departure */}
                        <td style={{ padding: `${spacing.lg}px`, whiteSpace: 'nowrap' }}>
                          <div style={{ fontWeight: 500 }}>{formatTime(f.scheduledDeparture)}</div>
                          <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>{formatDate(f.scheduledDeparture)}</div>
                        </td>

                        {/* Arrival */}
                        <td style={{ padding: `${spacing.lg}px`, whiteSpace: 'nowrap' }}>
                          <div style={{ fontWeight: 500 }}>{formatTime(f.scheduledArrival)}</div>
                          <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>{formatDate(f.scheduledArrival)}</div>
                        </td>

                        {/* Duration */}
                        <td style={{ padding: `${spacing.lg}px`, color: colors.textSecondary }}>
                          {calculateDuration(f.scheduledDeparture, f.scheduledArrival)}
                        </td>

                        {/* Status */}
                        <td style={{ padding: `${spacing.lg}px` }}>
                          <StatusBadge status={f.status} size="sm" />
                        </td>

                        {/* Seats */}
                        <td style={{ padding: `${spacing.lg}px` }}>
                          <SeatIndicator available={f.availableSeats} total={f.totalSeats} />
                        </td>

                        {/* Price */}
                        <td style={{ padding: `${spacing.lg}px` }}>
                          {f.basePrice ? (
                            <div>
                              <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>From</div>
                              <div style={{ fontWeight: 700, color: colors.success, fontSize: fonts.lg }}>
                                {formatINR(f.basePrice)}
                              </div>
                            </div>
                          ) : '—'}
                        </td>

                        {/* Action */}
                        <td style={{ padding: `${spacing.lg}px` }}>
                          {past ? (
                            <span style={{ fontSize: fonts.sm, color: colors.danger, fontWeight: 500 }}>Departed</span>
                          ) : cancelled ? (
                            <span style={{ fontSize: fonts.sm, color: colors.danger }}>Cancelled</span>
                          ) : f.availableSeats === 0 ? (
                            <span style={{ fontSize: fonts.sm, color: colors.textMuted }}>Sold Out</span>
                          ) : (
                            <button
                              onClick={() => {
                                // Navigate to bookings with this flight pre-selected
                                navigate('/bookings', { state: { preselectedFlight: f } });
                              }}
                              style={{ ...buttons.primary, height: 36, fontSize: fonts.sm, padding: '6px 14px' }}
                            >
                              Select Flight
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: spacing.lg }}>
            <span style={{ fontSize: fonts.sm, color: colors.textMuted }}>
              Page {page + 1} · {flights.length} flight{flights.length !== 1 ? 's' : ''}
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: spacing.xs }}>
              <button disabled={page === 0} onClick={() => setPage(page - 1)}
                style={{ ...buttons.secondary, padding: '6px 10px', height: 32, opacity: page === 0 ? 0.4 : 1 }}>
                <ChevronLeftIcon size={14} />
              </button>
              <span style={{ padding: '6px 12px', fontSize: fonts.sm, fontWeight: 500, background: colors.bg, borderRadius: radius.sm }}>
                {page + 1}
              </span>
              <button onClick={() => setPage(page + 1)}
                style={{ ...buttons.secondary, padding: '6px 10px', height: 32 }}>
                <ChevronRightIcon size={14} />
              </button>
            </div>
          </div>
        </>
      )}

      <style>{`
        @keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        @media (max-width: 900px) {
          .flight-search-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
