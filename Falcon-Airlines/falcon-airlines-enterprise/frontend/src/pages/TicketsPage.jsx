import { useState } from 'react';
import * as ticketsApi from '../api/tickets';

const inputStyle = { padding: '8px 10px', borderRadius: 6, border: '1px solid #ddd', fontSize: 13, width: '100%' };

export default function TicketsPage() {
  const [ticketId, setTicketId] = useState('');
  const [ticket, setTicket] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const searchTicket = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await ticketsApi.getTicket(ticketId);
      setTicket(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Ticket not found');
      setTicket(null);
    } finally {
      setLoading(false);
    }
  };

  const downloadPdf = async () => {
    if (!ticket) return;
    try {
      const blob = await ticketsApi.downloadTicketPdf(ticket.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ticket_${ticket.ticketNumber}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError('Failed to download PDF');
    }
  };

  const handleCancel = async () => {
    if (!ticket) return;
    setLoading(true);
    try {
      await ticketsApi.cancelTicket(ticket.id);
      setTicket({ ...ticket, status: 'CANCELLED' });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to cancel');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Tickets</h2>
      <form onSubmit={(e) => { e.preventDefault(); searchTicket(); }} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input placeholder="Ticket ID" type="number" value={ticketId} onChange={(e) => setTicketId(e.target.value)} style={{ ...inputStyle, flex: 1 }} />
        <button type="submit" style={{ padding: '8px 16px', background: '#0a2744', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Search</button>
      </form>
      {error && <div style={{ color: '#c0392b', background: '#fdecea', padding: 10, borderRadius: 6, marginBottom: 16 }}>{error}</div>}

      {ticket && (
        <div style={{ background: '#f8f9fa', borderRadius: 8, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3>Ticket {ticket.ticketNumber}</h3>
            <span style={{ padding: '4px 12px', borderRadius: 12, fontSize: 12, background: ticket.status === 'ACTIVE' || ticket.status === 'ISSUED' ? '#eafaf1' : '#fdecea', color: ticket.status === 'ACTIVE' || ticket.status === 'ISSUED' ? '#27ae60' : '#c0392b' }}>
              {ticket.status}
            </span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 14 }}>
            <div><strong>Passenger:</strong> {ticket.passengerName}</div>
            <div><strong>Flight:</strong> {ticket.flightNumber}</div>
            <div><strong>Route:</strong> {ticket.originAirportCode} → {ticket.destinationAirportCode}</div>
            <div><strong>Departure:</strong> {ticket.scheduledDeparture ? new Date(ticket.scheduledDeparture).toLocaleString() : '—'}</div>
            <div><strong>Seat:</strong> {ticket.seatNumber || '—'} ({ticket.seatClass || '—'})</div>
            <div><strong>Gate:</strong> {ticket.gate || '—'} Terminal: {ticket.terminal || '—'}</div>
            <div><strong>Fare:</strong> ${ticket.fare}</div>
            <div><strong>Taxes:</strong> ${ticket.taxes}</div>
            <div><strong>Total:</strong> ${ticket.totalAmount}</div>
            <div><strong>Booking Ref:</strong> {ticket.bookingReference}</div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <button onClick={downloadPdf} style={{ padding: '8px 16px', background: '#2980b9', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
              Download PDF
            </button>
            {(ticket.status === 'ACTIVE' || ticket.status === 'ISSUED') && (
              <button onClick={handleCancel} disabled={loading} style={{ padding: '8px 16px', background: '#c0392b', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
                Cancel Ticket
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
