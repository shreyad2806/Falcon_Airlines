import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import * as bookingsApi from '../api/bookings';
import * as paymentsApi from '../api/payments';
import PageHeader from '../components/PageHeader';
import { colors, spacing, radius, fonts, buttons } from '../styles/theme';
import { formatINR, formatDateTime } from '../utils/format';

const PAYMENT_METHODS = [
  { id: 'UPI', label: 'UPI', icon: '💳', desc: 'Google Pay, PhonePe, Paytm' },
  { id: 'QR', label: 'QR Payment', icon: '📱', desc: 'Scan & Pay' },
  { id: 'CARD', label: 'Card', icon: '💳', desc: 'Debit / Credit Card' },
  { id: 'NETBANKING', label: 'Net Banking', icon: '🏦', desc: 'All major banks' },
];

function QRCodeDisplay({ bookingRef, amount }) {
  // Generate a simple UPI QR payload
  const upiPayload = `upi://pay?pa=falconairlines@demo&pn=Falcon+Airlines&am=${amount}&cu=INR&tn=Booking+${bookingRef}`;

  return (
    <div style={{
      width: 200, height: 200, border: `2px solid ${colors.border}`,
      borderRadius: radius.md, display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'white', position: 'relative', margin: '0 auto',
    }}>
      {/* Simplified QR representation using styled divs */}
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: fonts.xl, marginBottom: spacing.sm }}>📱</div>
        <div style={{ fontSize: fonts.xs, color: colors.textSecondary, maxWidth: 160, wordBreak: 'break-all' }}>
          UPI QR Code
        </div>
        <div style={{ fontSize: fonts.xs, color: colors.textMuted, marginTop: 4 }}>
          Scan with any UPI app
        </div>
      </div>
    </div>
  );
}

export default function PaymentPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const bookingId = searchParams.get('bookingId');
  const { user } = useAuth();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState('');
  const [selectedMethod, setSelectedMethod] = useState('UPI');
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [countdown, setCountdown] = useState(15 * 60); // 15 minutes

  // Load booking details
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
      const data = res.data.data;
      setBooking(data);
      if (data.status !== 'PENDING_PAYMENT') {
        // Already paid or cancelled — redirect
        navigate(`/booking-confirmation?bookingId=${bookingId}`);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load booking');
    } finally {
      setLoading(false);
    }
  };

  // Countdown timer
  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setInterval(() => setCountdown(c => c - 1), 1000);
    return () => clearInterval(timer);
  }, [countdown]);

  const formatCountdown = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const handlePayment = async () => {
    setProcessing(true);
    setError('');
    try {
      const res = await paymentsApi.processPayment(booking.id, selectedMethod);
      if (res.data.data.status === 'SUCCESS') {
        setPaymentSuccess(true);
        setTimeout(() => {
          navigate(`/booking-confirmation?bookingId=${booking.id}`);
        }, 1500);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Payment failed. Please try again.');
    } finally {
      setProcessing(false);
    }
  };

  const handleSimulateFailure = async () => {
    setProcessing(true);
    setError('');
    try {
      await paymentsApi.simulateFailure(booking.id);
      setError('Payment failed. Your seat reservation has been released.');
      setTimeout(() => navigate('/bookings'), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Simulation failed');
    } finally {
      setProcessing(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <div style={{ color: colors.textSecondary, fontSize: fonts.base }}>Loading payment details...</div>
      </div>
    );
  }

  if (!booking) {
    return (
      <div style={{ textAlign: 'center', padding: spacing.xxxl }}>
        <div style={{ fontSize: fonts.xl, color: colors.danger }}>Booking not found</div>
      </div>
    );
  }

  if (paymentSuccess) {
    return (
      <div style={{ textAlign: 'center', padding: spacing.xxxl, maxWidth: 500, margin: '0 auto' }}>
        <div style={{ fontSize: 60, marginBottom: spacing.lg }}>✅</div>
        <h2 style={{ fontSize: fonts.xxl, color: colors.success, marginBottom: spacing.md }}>Payment Successful!</h2>
        <p style={{ color: colors.textSecondary, marginBottom: spacing.lg }}>
          Your booking has been confirmed. Redirecting to confirmation...
        </p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>
      <PageHeader title="Payment" description="Complete your booking payment." />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: spacing.xl }} className="payment-layout">
        {/* Left: Payment methods */}
        <div>
          {error && (
            <div style={{
              color: colors.danger, background: colors.dangerLight,
              padding: `${spacing.md}px ${spacing.lg}px`, borderRadius: radius.md,
              marginBottom: spacing.lg, fontSize: fonts.base,
            }}>
              {error}
            </div>
          )}

          {/* Payment methods */}
          <div style={{
            background: colors.bgCard, borderRadius: radius.lg,
            border: `1px solid ${colors.border}`, padding: spacing.xl,
            marginBottom: spacing.lg,
          }}>
            <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, marginBottom: spacing.lg }}>
              Select Payment Method
            </h3>

            <div style={{ display: 'grid', gap: spacing.sm }}>
              {PAYMENT_METHODS.map(method => (
                <div
                  key={method.id}
                  onClick={() => setSelectedMethod(method.id)}
                  style={{
                    padding: `${spacing.md}px ${spacing.lg}px`,
                    border: `2px solid ${selectedMethod === method.id ? colors.primary : colors.border}`,
                    borderRadius: radius.md,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: spacing.lg,
                    background: selectedMethod === method.id ? `${colors.primary}08` : 'white',
                    transition: 'all 0.15s',
                  }}
                >
                  <span style={{ fontSize: fonts.xl }}>{method.icon}</span>
                  <div>
                    <div style={{ fontWeight: 600, color: colors.text }}>{method.label}</div>
                    <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>{method.desc}</div>
                  </div>
                  {selectedMethod === method.id && (
                    <div style={{ marginLeft: 'auto', color: colors.primary, fontWeight: 700 }}>✓</div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* QR Code section (if QR selected) */}
          {selectedMethod === 'QR' && (
            <div style={{
              background: colors.bgCard, borderRadius: radius.lg,
              border: `1px solid ${colors.border}`, padding: spacing.xl,
              marginBottom: spacing.lg, textAlign: 'center',
            }}>
              <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, marginBottom: spacing.lg }}>
                Scan to Pay
              </h3>
              <QRCodeDisplay bookingRef={booking.bookingReference} amount={booking.totalAmount} />
              <div style={{ marginTop: spacing.lg }}>
                <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>UPI ID</div>
                <div style={{ fontSize: fonts.base, fontWeight: 600, color: colors.text }}>falconairlines@demo</div>
              </div>
              <button
                onClick={() => navigator.clipboard?.writeText('falconairlines@demo')}
                style={{ ...buttons.ghost, color: colors.primary, fontSize: fonts.sm, marginTop: spacing.sm }}
              >
                📋 Copy UPI ID
              </button>
            </div>
          )}

          {/* Payment actions */}
          <div style={{ display: 'flex', gap: spacing.sm }}>
            <button onClick={() => navigate('/bookings')} style={buttons.secondary}>
              ← Back
            </button>
            <button
              onClick={handlePayment}
              disabled={processing}
              style={{ ...buttons.success, flex: 1, justifyContent: 'center' }}
            >
              {processing ? 'Processing...' : `Pay ${formatINR(booking.totalAmount)}`}
            </button>
          </div>

          {/* Demo failure button */}
          <div style={{ marginTop: spacing.lg, textAlign: 'center' }}>
            <button
              onClick={handleSimulateFailure}
              disabled={processing}
              style={{ ...buttons.ghost, color: colors.danger, fontSize: fonts.sm }}
            >
              ⚠ Simulate Payment Failure (Demo)
            </button>
          </div>
        </div>

        {/* Right: Order summary */}
        <div style={{
          background: colors.bgCard, borderRadius: radius.lg,
          border: `1px solid ${colors.border}`, padding: spacing.xl,
          height: 'fit-content', position: 'sticky', top: spacing.xl,
        }}>
          <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, marginBottom: spacing.lg }}>
            Payment Summary
          </h3>

          <div style={{ marginBottom: spacing.lg }}>
            <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Booking Reference</div>
            <div style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.primary }}>{booking.bookingReference}</div>
          </div>

          <div style={{ marginBottom: spacing.lg }}>
            <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Flight</div>
            <div style={{ fontWeight: 600 }}>{booking.flightNumber}</div>
          </div>

          <div style={{
            borderTop: `1px solid ${colors.border}`,
            paddingTop: spacing.lg, marginTop: spacing.lg,
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: spacing.sm }}>
              <span style={{ color: colors.textSecondary }}>Total Amount</span>
              <span style={{ fontWeight: 700, fontSize: fonts.xl, color: colors.success }}>
                {formatINR(booking.totalAmount)}
              </span>
            </div>
            <div style={{ fontSize: fonts.xs, color: colors.textMuted, textAlign: 'right' }}>
              {booking.currency || 'INR'}
            </div>
          </div>

          {/* Countdown */}
          <div style={{
            marginTop: spacing.xl, padding: spacing.md,
            background: countdown < 300 ? colors.dangerLight : colors.warningLight,
            borderRadius: radius.md, textAlign: 'center',
          }}>
            <div style={{
              fontSize: fonts.sm,
              color: countdown < 300 ? colors.danger : colors.warning,
              fontWeight: 500,
            }}>
              Payment window expires in {formatCountdown(countdown)}
            </div>
          </div>
        </div>
      </div>

      <style>{`
        @media (max-width: 900px) {
          .payment-layout { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
