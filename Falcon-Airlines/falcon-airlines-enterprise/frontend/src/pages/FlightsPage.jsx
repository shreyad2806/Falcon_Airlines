import { useState, useEffect } from 'react';
import * as flightsApi from '../api/flights';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import { colors, spacing, radius, fonts, input as inputStyle, buttons } from '../styles/theme';
import { SearchIcon, ChevronLeftIcon, ChevronRightIcon, PlaneTakeoffIcon } from '../components/Icons';
import { formatINR, formatDateTime, calculateDuration } from '../utils/format';

export default function FlightsPage() {
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [originFilter, setOriginFilter] = useState('');
  const [destFilter, setDestFilter] = useState('');
  const [page, setPage] = useState(0);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: 10 };
      if (search) params.flightNumber = search;
      if (statusFilter) params.status = statusFilter;
      if (originFilter) params.originAirport = originFilter;
      if (destFilter) params.destinationAirport = destFilter;
      const res = await flightsApi.searchFlights(params);
      setFlights(res.data.data?.content || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load flights');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page, statusFilter]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    load();
  };

  const clearFilters = () => {
    setSearch('');
    setStatusFilter('');
    setOriginFilter('');
    setDestFilter('');
    setPage(0);
  };

  const hasFilters = search || statusFilter || originFilter || destFilter;
  const isPast = (f) => f.scheduledDeparture && new Date(f.scheduledDeparture) < new Date();

  const origins = [...new Set(flights.map(f => f.originAirportIataCode).filter(Boolean))];
  const destinations = [...new Set(flights.map(f => f.destinationAirportIataCode).filter(Boolean))];

  return (
    <div>
      <PageHeader title="Flights" description="Search and book your next journey." />

      {/* Search */}
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: spacing.sm, marginBottom: spacing.lg }}>
        <div style={{ flex: 1, position: 'relative' }}>
          <input placeholder="Search by flight number, origin, or destination..." value={search} onChange={(e) => setSearch(e.target.value)} style={{ ...inputStyle, paddingLeft: 40 }} />
          <SearchIcon size={16} color={colors.textMuted} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)' }} />
        </div>
        <button type="submit" style={{ ...buttons.primary, minWidth: 100 }}><SearchIcon size={16} color="white" /> Search</button>
      </form>

      {/* Filters */}
      <div style={{ display: 'flex', gap: spacing.sm, marginBottom: spacing.xl, flexWrap: 'wrap', alignItems: 'center' }}>
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }} style={{ ...inputStyle, width: 'auto', minWidth: 130, padding: '8px 12px', height: 38 }}>
          <option value="">All Status</option>
          <option value="SCHEDULED">Scheduled</option>
          <option value="DELAYED">Delayed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <select value={originFilter} onChange={(e) => { setOriginFilter(e.target.value); setPage(0); }} style={{ ...inputStyle, width: 'auto', minWidth: 130, padding: '8px 12px', height: 38 }}>
          <option value="">All Origin</option>
          {origins.map(o => <option key={o} value={o}>{o}</option>)}
        </select>
        <select value={destFilter} onChange={(e) => { setDestFilter(e.target.value); setPage(0); }} style={{ ...inputStyle, width: 'auto', minWidth: 130, padding: '8px 12px', height: 38 }}>
          <option value="">All Destination</option>
          {destinations.map(d => <option key={d} value={d}>{d}</option>)}
        </select>
        {hasFilters && <button onClick={clearFilters} style={{ ...buttons.ghost, color: colors.primary, fontSize: fonts.sm, height: 38 }}>Clear filters</button>}
      </div>

      {error && <div style={{ color: colors.danger, background: colors.dangerLight, padding: `${spacing.md}px ${spacing.lg}px`, borderRadius: radius.md, marginBottom: spacing.lg }}>{error}</div>}

      {loading ? (
        <div style={{ padding: spacing.xxxl, textAlign: 'center', color: colors.textMuted }}>
          <div style={{ width: 32, height: 32, border: `3px solid ${colors.border}`, borderTopColor: colors.primary, borderRadius: '50%', margin: `0 auto ${spacing.lg}px`, animation: 'spin 0.8s linear infinite' }} />
          Loading flights...
        </div>
      ) : flights.length === 0 ? (
        <EmptyState icon="✈" heading={hasFilters ? 'No flights match your search' : 'No flights found'} text={hasFilters ? 'Try adjusting your filters.' : 'Try adjusting your search or check back later.'} />
      ) : (
        <>
          <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, overflow: 'hidden' }}>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: fonts.base }}>
                <thead>
                  <tr style={{ borderBottom: `1px solid ${colors.border}` }}>
                    {['Flight', 'Route', 'Departure', 'Duration', 'Status', 'Seats', 'Price', 'Action'].map(h => (
                      <th key={h} style={{ padding: `${spacing.md}px ${spacing.lg}px`, textAlign: 'left', fontSize: fonts.sm, fontWeight: 600, color: colors.textSecondary, background: colors.bg, whiteSpace: 'nowrap' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {flights.map((f) => {
                    const past = isPast(f);
                    return (
                      <tr key={f.id} style={{ borderBottom: `1px solid ${colors.borderLight}`, opacity: past ? 0.55 : 1, transition: 'background 0.1s' }}
                        onMouseEnter={(e) => { e.currentTarget.style.background = colors.bg; }}
                        onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
                      >
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px`, fontWeight: 600 }}>{f.flightNumber}</td>
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px` }}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
                            <span style={{ fontWeight: 500 }}>{f.originAirportIataCode}</span>
                            <PlaneTakeoffIcon size={12} color={colors.textMuted} />
                            <span style={{ fontWeight: 500 }}>{f.destinationAirportIataCode}</span>
                          </span>
                        </td>
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px`, color: colors.textSecondary, whiteSpace: 'nowrap' }}>{formatDateTime(f.scheduledDeparture)}</td>
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px`, color: colors.textSecondary }}>{calculateDuration(f.scheduledDeparture, f.scheduledArrival)}</td>
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px` }}><StatusBadge status={f.status} size="sm" /></td>
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px`, fontWeight: 500 }}>{f.availableSeats ?? '—'}</td>
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px` }}>
                          {f.basePrice ? (
                            <span style={{ fontWeight: 700, color: colors.success }}>{formatINR(f.basePrice)}</span>
                          ) : '—'}
                        </td>
                        <td style={{ padding: `${spacing.md}px ${spacing.lg}px` }}>
                          {past ? (
                            <span style={{ fontSize: fonts.sm, color: colors.danger }}>Departed</span>
                          ) : (
                            <button style={{ ...buttons.ghost, color: colors.primary, fontSize: fonts.sm, padding: '4px 10px', height: 28, fontWeight: 500 }}>View</button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: spacing.lg, padding: `${spacing.sm}px 0` }}>
            <span style={{ fontSize: fonts.sm, color: colors.textMuted }}>Showing {flights.length} flights</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: spacing.xs }}>
              <button disabled={page === 0} onClick={() => setPage(page - 1)} style={{ ...buttons.secondary, padding: '6px 10px', height: 32, opacity: page === 0 ? 0.4 : 1 }}><ChevronLeftIcon size={14} /></button>
              <span style={{ padding: '6px 12px', fontSize: fonts.sm, fontWeight: 500, background: colors.bg, borderRadius: radius.sm, minWidth: 32, textAlign: 'center' }}>{page + 1}</span>
              <button onClick={() => setPage(page + 1)} style={{ ...buttons.secondary, padding: '6px 10px', height: 32 }}><ChevronRightIcon size={14} /></button>
            </div>
          </div>
        </>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
