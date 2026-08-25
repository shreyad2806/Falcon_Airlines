import { useState, useEffect } from 'react';
import * as flightsApi from '../api/flights';

const inputStyle = { padding: '8px 10px', borderRadius: 6, border: '1px solid #ddd', fontSize: 13 };

export default function FlightsPage() {
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: 10 };
      if (search) params.flightNumber = search;
      const res = await flightsApi.searchFlights(params);
      setFlights(res.data.data?.content || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load flights');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    load();
  };

  const formatInstant = (s) => s ? new Date(s).toLocaleString() : '—';

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Flights</h2>
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input placeholder="Search by flight number..." value={search}
          onChange={(e) => setSearch(e.target.value)} style={{ ...inputStyle, flex: 1 }} />
        <button type="submit" style={{ padding: '8px 16px', background: '#0a2744', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Search</button>
      </form>
      {error && <div style={{ color: '#c0392b', marginBottom: 12 }}>{error}</div>}
      {loading ? <p>Loading...</p> : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #eee', textAlign: 'left' }}>
              <th style={{ padding: 8 }}>Flight #</th>
              <th style={{ padding: 8 }}>Origin</th>
              <th style={{ padding: 8 }}>Destination</th>
              <th style={{ padding: 8 }}>Departure</th>
              <th style={{ padding: 8 }}>Arrival</th>
              <th style={{ padding: 8 }}>Status</th>
              <th style={{ padding: 8 }}>Gate</th>
            </tr>
          </thead>
          <tbody>
            {flights.map((f) => (
              <tr key={f.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                <td style={{ padding: 8, fontWeight: 600 }}>{f.flightNumber}</td>
                <td style={{ padding: 8 }}>{f.originAirportIataCode}</td>
                <td style={{ padding: 8 }}>{f.destinationAirportIataCode}</td>
                <td style={{ padding: 8 }}>{formatInstant(f.scheduledDeparture)}</td>
                <td style={{ padding: 8 }}>{formatInstant(f.scheduledArrival)}</td>
                <td style={{ padding: 8 }}>
                  <span style={{ padding: '2px 8px', borderRadius: 12, fontSize: 11, background: f.status === 'SCHEDULED' ? '#eafaf1' : '#fdecea', color: f.status === 'SCHEDULED' ? '#27ae60' : '#c0392b' }}>
                    {f.status}
                  </span>
                </td>
                <td style={{ padding: 8 }}>{f.gate || '—'}</td>
              </tr>
            ))}
            {flights.length === 0 && <tr><td colSpan={7} style={{ padding: 16, textAlign: 'center', color: '#999' }}>No flights found</td></tr>}
          </tbody>
        </table>
      )}
      <div style={{ display: 'flex', gap: 8, marginTop: 12, justifyContent: 'center' }}>
        <button disabled={page === 0} onClick={() => setPage(page - 1)} style={{ padding: '6px 12px' }}>Prev</button>
        <span style={{ padding: '6px 12px' }}>Page {page + 1}</span>
        <button onClick={() => setPage(page + 1)} style={{ padding: '6px 12px' }}>Next</button>
      </div>
    </div>
  );
}
