import { useState } from 'react';
import * as bpApi from '../api/boardingPasses';

const inputStyle = { padding: '8px 10px', borderRadius: 6, border: '1px solid #ddd', fontSize: 13, width: '100%' };

export default function BoardingPassesPage() {
  const [tab, setTab] = useState('view');
  const [bpId, setBpId] = useState('');
  const [ticketId, setTicketId] = useState('');
  const [bp, setBp] = useState(null);
  const [qrBase64, setQrBase64] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const searchBp = async () => {
    setError('');
    setQrBase64(null);
    setLoading(true);
    try {
      const res = await bpApi.getBoardingPass(bpId);
      setBp(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Boarding pass not found');
      setBp(null);
    } finally {
      setLoading(false);
    }
  };

  const generate = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await bpApi.generateBoardingPass(ticketId);
      setBp(res.data.data);
      setTab('view');
      setBpId(res.data.data.id.toString());
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to generate boarding pass');
    } finally {
      setLoading(false);
    }
  };

  const loadQr = async () => {
    if (!bp) return;
    try {
      const res = await bpApi.getQrCode(bp.id);
      setQrBase64(res.data.data.qrCode);
    } catch {
      setError('Failed to load QR code');
    }
  };

  const downloadPdf = async () => {
    if (!bp) return;
    try {
      const blob = await bpApi.downloadBoardingPassPdf(bp.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `boarding_pass_${bp.boardingPassNumber}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setError('Failed to download PDF');
    }
  };

  const handleCheckIn = async () => {
    if (!bp) return;
    setLoading(true);
    try {
      const res = await bpApi.checkIn(bp.id);
      setBp(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Check-in failed');
    } finally {
      setLoading(false);
    }
  };

  const handleBoard = async () => {
    if (!bp) return;
    setLoading(true);
    try {
      const res = await bpApi.boardPassenger(bp.id);
      setBp(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Boarding failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Boarding Passes</h2>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <button onClick={() => setTab('view')} style={{ padding: '6px 14px', background: tab === 'view' ? '#0a2744' : '#eee', color: tab === 'view' ? 'white' : '#333', border: 'none', borderRadius: 6, cursor: 'pointer' }}>View Boarding Pass</button>
        <button onClick={() => setTab('generate')} style={{ padding: '6px 14px', background: tab === 'generate' ? '#0a2744' : '#eee', color: tab === 'generate' ? 'white' : '#333', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Generate from Ticket</button>
      </div>
      {error && <div style={{ color: '#c0392b', background: '#fdecea', padding: 10, borderRadius: 6, marginBottom: 16 }}>{error}</div>}

      {tab === 'view' && (
        <form onSubmit={(e) => { e.preventDefault(); searchBp(); }} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          <input placeholder="Boarding Pass ID" type="number" value={bpId} onChange={(e) => setBpId(e.target.value)} style={{ ...inputStyle, flex: 1 }} />
          <button type="submit" style={{ padding: '8px 16px', background: '#0a2744', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Search</button>
        </form>
      )}

      {tab === 'generate' && (
        <form onSubmit={(e) => { e.preventDefault(); generate(); }} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          <input placeholder="Ticket ID" type="number" value={ticketId} onChange={(e) => setTicketId(e.target.value)} style={{ ...inputStyle, flex: 1 }} />
          <button type="submit" disabled={loading} style={{ padding: '8px 16px', background: '#27ae60', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
            {loading ? 'Generating...' : 'Generate'}
          </button>
        </form>
      )}

      {bp && (
        <div style={{ background: '#f8f9fa', borderRadius: 8, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3>Boarding Pass {bp.boardingPassNumber}</h3>
            <span style={{ padding: '4px 12px', borderRadius: 12, fontSize: 12, background: bp.status === 'GENERATED' ? '#fff3cd' : bp.status === 'CHECKED_IN' ? '#d4edda' : '#e2e3e5', color: '#333' }}>
              {bp.status}
            </span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 14 }}>
            <div><strong>Passenger:</strong> {bp.passengerName}</div>
            <div><strong>Flight:</strong> {bp.flightNumber}</div>
            <div><strong>Route:</strong> {bp.originAirportCode} → {bp.destinationAirportCode}</div>
            <div><strong>Departure:</strong> {bp.scheduledDeparture ? new Date(bp.scheduledDeparture).toLocaleString() : '—'}</div>
            <div><strong>Gate:</strong> {bp.gate || '—'}</div>
            <div><strong>Boarding:</strong> {bp.boardingTime ? new Date(bp.boardingTime).toLocaleTimeString() : '—'}</div>
            <div><strong>Seat:</strong> {bp.seatNumber || '—'} ({bp.seatClass || '—'})</div>
            <div><strong>Group:</strong> {bp.boardingGroup || '—'}</div>
            <div><strong>Booking Ref:</strong> {bp.bookingReference}</div>
            <div><strong>Ticket #:</strong> {bp.ticketNumber}</div>
          </div>

          <div style={{ display: 'flex', gap: 8, marginTop: 16, flexWrap: 'wrap' }}>
            <button onClick={loadQr} style={{ padding: '8px 16px', background: '#8e44ad', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Show QR Code</button>
            <button onClick={downloadPdf} style={{ padding: '8px 16px', background: '#2980b9', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Download PDF</button>
            {bp.status === 'GENERATED' && (
              <button onClick={handleCheckIn} disabled={loading} style={{ padding: '8px 16px', background: '#27ae60', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Check In</button>
            )}
            {bp.status === 'CHECKED_IN' && (
              <button onClick={handleBoard} disabled={loading} style={{ padding: '8px 16px', background: '#f39c12', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Board</button>
            )}
          </div>

          {qrBase64 && (
            <div style={{ marginTop: 16, textAlign: 'center' }}>
              <h4>QR Code</h4>
              <img src={`data:image/png;base64,${qrBase64}`} alt="Boarding Pass QR Code" style={{ border: '1px solid #ddd', borderRadius: 8, padding: 12, background: 'white', maxWidth: 200 }} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
