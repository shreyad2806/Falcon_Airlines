import { useState } from 'react';
import * as ticketsApi from '../api/tickets';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import { colors, spacing, radius, fonts, input as inputStyle, buttons } from '../styles/theme';
import { SearchIcon, DownloadIcon, EyeIcon, PlaneTakeoffIcon } from '../components/Icons';

export default function TicketsPage() {
  const [ticketId, setTicketId] = useState('');
  const [ticket, setTicket] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const searchTicket = async () => {
    setError('');
    setLoading(true);
    setHasSearched(true);
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
    } catch {
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
      <PageHeader title="My Tickets" description="View and download your flight tickets." />

      {/* Search */}
      <form onSubmit={(e) => { e.preventDefault(); searchTicket(); }} style={{
        display: 'flex',
        gap: spacing.sm,
        marginBottom: spacing.xxl,
      }}>
        <div style={{ flex: 1, position: 'relative' }}>
          <input
            placeholder="Search by ticket or booking reference..."
            type="number"
            value={ticketId}
            onChange={(e) => setTicketId(e.target.value)}
            style={{ ...inputStyle, paddingLeft: 40 }}
          />
          <SearchIcon
            size={16}
            color={colors.textMuted}
            style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)' }}
          />
        </div>
        <button type="submit" disabled={loading || !ticketId} style={{ ...buttons.primary, minWidth: 100 }}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </form>

      {/* Error */}
      {error && (
        <div style={{
          color: colors.danger,
          background: colors.dangerLight,
          padding: `${spacing.md}px ${spacing.lg}px`,
          borderRadius: radius.md,
          marginBottom: spacing.lg,
          fontSize: fonts.base,
        }}>
          {error}
        </div>
      )}

      {!hasSearched && !ticket && (
        <EmptyState
          icon="🎫"
          heading="No tickets yet"
          text="Your issued flight tickets will appear here. Search by ticket ID to view details."
        />
      )}

      {/* Ticket card */}
      {ticket && (
        <div style={{
          background: colors.bgCard,
          borderRadius: radius.lg,
          border: `1px solid ${colors.border}`,
          overflow: 'hidden',
        }}>
          {/* Ticket header */}
          <div style={{
            padding: `${spacing.xl}px`,
            background: colors.bg,
            borderBottom: `1px solid ${colors.border}`,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: spacing.sm,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: spacing.md }}>
              <div style={{
                width: 40,
                height: 40,
                borderRadius: radius.md,
                background: '#EDE9FE',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 20,
              }}>🎫</div>
              <div>
                <h3 style={{ fontSize: fonts.xl, fontWeight: 600, color: colors.text, margin: 0 }}>
                  Ticket {ticket.ticketNumber}
                </h3>
                <span style={{ fontSize: fonts.sm, color: colors.textSecondary }}>
                  Booking {ticket.bookingReference || '—'}
                </span>
              </div>
            </div>
            <StatusBadge status={ticket.status} size="lg" />
          </div>

          {/* Ticket body */}
          <div style={{ padding: spacing.xl }}>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: spacing.xl,
              marginBottom: spacing.xl,
            }}>
              {/* Flight info */}
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Flight</div>
                <div style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.text }}>{ticket.flightNumber}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Route</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
                  <span style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.text }}>{ticket.originAirportCode}</span>
                  <PlaneTakeoffIcon size={14} color={colors.textMuted} />
                  <span style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.text }}>{ticket.destinationAirportCode}</span>
                </div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Passenger</div>
                <div style={{ fontSize: fonts.base, fontWeight: 500, color: colors.text }}>{ticket.passengerName}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Seat</div>
                <div style={{ fontSize: fonts.base, fontWeight: 500, color: colors.text }}>
                  {ticket.seatNumber || '—'} <span style={{ color: colors.textSecondary }}>({ticket.seatClass || '—'})</span>
                </div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Date</div>
                <div style={{ fontSize: fonts.base, fontWeight: 500, color: colors.text }}>
                  {ticket.scheduledDeparture ? new Date(ticket.scheduledDeparture).toLocaleDateString('en-US', {
                    day: 'numeric', month: 'short', year: 'numeric',
                  }) : '—'}
                </div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Gate</div>
                <div style={{ fontSize: fonts.base, fontWeight: 500, color: colors.text }}>{ticket.gate || 'TBA'}</div>
              </div>
            </div>

            {/* Price summary */}
            <div style={{
              display: 'flex',
              gap: spacing.xxl,
              padding: `${spacing.lg}px`,
              background: colors.bg,
              borderRadius: radius.md,
              marginBottom: spacing.xl,
            }}>
              <div>
                <span style={{ fontSize: fonts.sm, color: colors.textMuted }}>Fare: </span>
                <span style={{ fontWeight: 600 }}>${ticket.fare}</span>
              </div>
              <div>
                <span style={{ fontSize: fonts.sm, color: colors.textMuted }}>Taxes: </span>
                <span style={{ fontWeight: 600 }}>${ticket.taxes}</span>
              </div>
              <div>
                <span style={{ fontSize: fonts.sm, color: colors.textMuted }}>Total: </span>
                <span style={{ fontWeight: 700, color: colors.text }}>${ticket.totalAmount}</span>
              </div>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', gap: spacing.sm, flexWrap: 'wrap' }}>
              <button
                onClick={downloadPdf}
                style={{
                  ...buttons.primary,
                  background: colors.primary,
                }}
              >
                <DownloadIcon size={16} color="white" />
                Download PDF
              </button>
              {(ticket.status === 'ACTIVE' || ticket.status === 'ISSUED') && (
                <button onClick={handleCancel} disabled={loading} style={buttons.danger}>
                  Cancel Ticket
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {hasSearched && !ticket && !loading && !error && (
        <EmptyState
          icon="🔍"
          heading="No ticket found"
          text="No ticket matches that ID. Please check and try again."
        />
      )}
    </div>
  );
}
