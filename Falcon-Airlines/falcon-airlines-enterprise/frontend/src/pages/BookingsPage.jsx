import { useState } from 'react';
import * as bookingsApi from '../api/bookings';
import * as passengersApi from '../api/passengers';
import * as flightsApi from '../api/flights';

const inputStyle = { padding: '8px 10px', borderRadius: 6, border: '1px solid #ddd', fontSize: 13, width: '100%' };

export default function BookingsPage() {
  const [tab, setTab] = useState('search');
  const [ref, setRef] = useState('');
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Create form
  const [form, setForm] = useState({ customerId: '', flightId: '', passengerId: '', seatNumber: '' });
  const [passengers, setPassengers] = useState([]);
  const [flights, setFlights] = useState([]);

  const searchBooking = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await bookingsApi.getBookingByRef(ref);
      setBooking(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Booking not found');
      setBooking(null);
    } finally {
      setLoading(false);
    }
  };

  const loadFormData = async () => {
    try {
      const [pRes, fRes] = await Promise.all([
        passengersApi.searchPassengers({ size: 100 }),
        flightsApi.searchFlights({ size: 100, status: 'SCHEDULED' }),
      ]);
      setPassengers(pRes.data.data?.content || []);
      setFlights(fRes.data.data?.content || []);
    } catch { /* ignore */ }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = {
        customerId: parseInt(form.customerId),
        flightId: parseInt(form.flightId),
        passengers: [{ passengerId: parseInt(form.passengerId) }],
        requestedSeats: form.seatNumber ? [form.seatNumber] : ['1A'],
      };
      const res = await bookingsApi.createBooking(data);
      setBooking(res.data.data);
      setTab('view');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create booking');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!booking) return;
    setLoading(true);
    try {
      await bookingsApi.cancelBooking(booking.id, 'Cancelled by user');
      setBooking({ ...booking, status: 'CANCELLED' });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to cancel');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Bookings</h2>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <button onClick={() => { setTab('search'); }} style={{ padding: '6px 14px', background: tab === 'search' ? '#0a2744' : '#eee', color: tab === 'search' ? 'white' : '#333', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Search Booking</button>
        <button onClick={() => { setTab('create'); loadFormData(); }} style={{ padding: '6px 14px', background: tab === 'create' ? '#0a2744' : '#eee', color: tab === 'create' ? 'white' : '#333', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Create Booking</button>
      </div>
      {error && <div style={{ color: '#c0392b', background: '#fdecea', padding: 10, borderRadius: 6, marginBottom: 16 }}>{error}</div>}

      {tab === 'search' && (
        <div style={{ marginBottom: 24 }}>
          <form onSubmit={(e) => { e.preventDefault(); searchBooking(); }} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <input placeholder="Booking reference (e.g. BK789)" value={ref} onChange={(e) => setRef(e.target.value)} style={{ ...inputStyle, flex: 1 }} />
            <button type="submit" style={{ padding: '8px 16px', background: '#0a2744', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Search</button>
          </form>
        </div>
      )}

      {tab === 'create' && (
        <form onSubmit={handleCreate} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 24, maxWidth: 600 }}>
          <div>
            <label style={{ fontSize: 12, color: '#666' }}>Customer ID</label>
            <input type="number" value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })} required style={inputStyle} />
          </div>
          <div>
            <label style={{ fontSize: 12, color: '#666' }}>Flight</label>
            <select value={form.flightId} onChange={(e) => setForm({ ...form, flightId: e.target.value })} required style={inputStyle}>
              <option value="">Select flight</option>
              {flights.map(f => <option key={f.id} value={f.id}>{f.flightNumber} ({f.originAirportIataCode} → {f.destinationAirportIataCode})</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: 12, color: '#666' }}>Passenger</label>
            <select value={form.passengerId} onChange={(e) => setForm({ ...form, passengerId: e.target.value })} required style={inputStyle}>
              <option value="">Select passenger</option>
              {passengers.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: 12, color: '#666' }}>Seat</label>
            <input placeholder="e.g. 1A" value={form.seatNumber} onChange={(e) => setForm({ ...form, seatNumber: e.target.value })} style={inputStyle} />
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <button type="submit" disabled={loading} style={{ padding: '10px 20px', background: '#27ae60', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14 }}>
              {loading ? 'Creating...' : 'Create Booking'}
            </button>
          </div>
        </form>
      )}

      {booking && (
        <div style={{ background: '#f8f9fa', borderRadius: 8, padding: 20, marginTop: 16 }}>
          <h3 style={{ marginBottom: 12 }}>Booking Details</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 14 }}>
            <div><strong>Reference:</strong> {booking.bookingReference}</div>
            <div><strong>Status:</strong> <span style={{ padding: '2px 8px', borderRadius: 12, fontSize: 12, background: booking.status === 'CONFIRMED' ? '#eafaf1' : '#fdecea', color: booking.status === 'CONFIRMED' ? '#27ae60' : '#c0392b' }}>{booking.status}</span></div>
            <div><strong>Flight:</strong> {booking.flightNumber}</div>
            <div><strong>Amount:</strong> ${booking.totalAmount} {booking.currency}</div>
            <div><strong>Payment:</strong> {booking.paymentStatus}</div>
            <div><strong>Booked:</strong> {booking.bookingDate ? new Date(booking.bookingDate).toLocaleString() : '—'}</div>
          </div>
          {booking.tickets?.length > 0 && (
            <div style={{ marginTop: 12 }}>
              <strong>Tickets:</strong>
              {booking.tickets.map(t => (
                <div key={t.id} style={{ marginLeft: 8, marginTop: 4 }}>TKT#{t.ticketNumber} — {t.status}</div>
              ))}
            </div>
          )}
          {booking.status !== 'CANCELLED' && (
            <button onClick={handleCancel} disabled={loading} style={{ marginTop: 12, padding: '8px 16px', background: '#c0392b', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
              Cancel Booking
            </button>
          )}
        </div>
      )}
    </div>
  );
}
