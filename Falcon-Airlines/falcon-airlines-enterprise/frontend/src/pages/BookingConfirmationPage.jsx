import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import * as bookingsApi from '../api/bookings';
import PageHeader from '../components/PageHeader';
import StatusBadge from '../components/StatusBadge';
import { colors, spacing, radius, fonts, buttons } from '../styles/theme';
import { formatINR, formatDateTime, formatDate, formatTime } from '../utils/format';

export default function BookingConfirmationPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const bookingId = searchParams.get('bookingId');
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!bookingId) {
      navigate('/bookings');
      return;
    }
    loadBooking();
  }, [bookingId]);

  const loadBooking = async () => {
    try {
      const res = await bookingsApi.getBooking(bookingId);
      setBooking(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load booking');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <div style={{ color: colors.textSecondary }}>Loading booking details...</div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div style={{ textAlign: 'center', padding: spacing.xxxl }}>
        <div style={{ fontSize: fonts.xl, color: colors.danger }}>{error || 'Booking not found'}</div>
        <button onClick={() => navigate('/bookings')} style={{ ...buttons.primary, marginTop: spacing.lg }}>
          Go to Bookings
        </button>
      </div>
    );
  }

  const isConfirmed = booking.status === 'CONFIRMED';

  return (
    <div style={{ maxWidth: 700, margin: '0 auto' }}>
      <PageHeader title="Booking Confirmation" description="Your booking details." />

      {/* Success header */}
      <div style={{
        textAlign: 'center', padding: spacing.xxxl,
        background: isConfirmed ? colors.successLight : colors.warningLight,
        borderRadius: radius.lg, marginBottom: spacing.xl,
        border: `1px solid ${isConfirmed ? colors.success : colors.warning}20`,
      }}>
        <div style={{ fontSize: 48, marginBottom: spacing.md }}>
          {isConfirmed ? '✅' : booking.status === 'PAYMENT_FAILED' ? '❌' : '⏳'}
        </div>
        <h2 style={{
          fontSize: fonts.xxl, fontWeight: 700,
          color: isConfirmed ? colors.success : booking.status === 'PAYMENT_FAILED' ? colors.danger : colors.warning,
          marginBottom: spacing.sm,
        }}>
          {isConfirmed ? 'Booking Confirmed!' :
           booking.status === 'PAYMENT_FAILED' ? 'Payment Failed' : 'Booking Pending'}
        </h2>
        {isConfirmed && (
          <p style={{ color: colors.textSecondary, fontSize: fonts.base }}>
            Your booking has been successfully confirmed.
          </p>
        )}
      </div>

      {/* Booking details card */}
      <div style={{
        background: colors.bgCard, borderRadius: radius.lg,
        border: `1px solid ${colors.border}`, overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{
          padding: spacing.xl, background: colors.bg,
          borderBottom: `1px solid ${colors.border}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: spacing.sm,
        }}>
          <div>
            <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>Booking Reference</div>
            <div style={{ fontSize: fonts.xxl, fontWeight: 700, color: colors.primary, letterSpacing: 1 }}>
              {booking.bookingReference}
            </div>
          </div>
          <StatusBadge status={booking.status} size="lg" />
        </div>

        {/* Flight details */}
        <div style={{ padding: spacing.xl }}>
          <div style={{ marginBottom: spacing.xl }}>
            <h4 style={{
              fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted,
              textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: spacing.md,
            }}>
              Flight
            </h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: spacing.md }}>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>Flight Number</div>
                <div style={{ fontSize: fonts.lg, fontWeight: 700 }}>{booking.flightNumber}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>Status</div>
                <StatusBadge status={booking.status} size="md" />
              </div>
            </div>
          </div>

          {/* Tickets */}
          {booking.tickets && booking.tickets.length > 0 && (
            <div style={{ marginBottom: spacing.xl }}>
              <h4 style={{
                fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted,
                textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: spacing.md,
              }}>
                Passenger{booking.tickets.length > 1 ? 's' : ''}
              </h4>
              {booking.tickets.map((ticket, i) => (
                <div key={i} style={{
                  padding: spacing.lg, background: colors.bg,
                  borderRadius: radius.md, marginBottom: spacing.sm,
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                }}>
                  <div>
                    <div style={{ fontWeight: 600 }}>{ticket.passengerName}</div>
                    <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>
                      Ticket: {ticket.ticketNumber}
                    </div>
                  </div>
                  {ticket.seatNumber && (
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontWeight: 600 }}>Seat {ticket.seatNumber}</div>
                      <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>{ticket.seatClass}</div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Payment */}
          <div style={{ marginBottom: spacing.xl }}>
            <h4 style={{
              fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted,
              textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: spacing.md,
            }}>
              Payment
            </h4>
            <div style={{ padding: spacing.lg, background: colors.bg, borderRadius: radius.md }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>Amount Paid</div>
                  <div style={{ fontSize: fonts.xl, fontWeight: 700, color: colors.success }}>
                    {formatINR(booking.totalAmount)}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>Payment Status</div>
                  <StatusBadge status={booking.paymentStatus} size="md" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', gap: spacing.sm, marginTop: spacing.xl, justifyContent: 'center' }}>
        <Link to="/tickets" style={{ textDecoration: 'none' }}>
          <button style={buttons.primary}>View Tickets</button>
        </Link>
        <Link to="/boarding-passes" style={{ textDecoration: 'none' }}>
          <button style={buttons.secondary}>Boarding Passes</button>
        </Link>
        <Link to="/bookings" style={{ textDecoration: 'none' }}>
          <button style={buttons.ghost}>Go to My Bookings</button>
        </Link>
      </div>
    </div>
  );
}
