import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import * as bookingsApi from '../api/bookings';
import * as passengersApi from '../api/passengers';
import * as flightsApi from '../api/flights';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import StepIndicator from '../components/StepIndicator';
import SeatChart, { getSeatPrice, getSeatTypeLabel } from '../components/SeatChart';
import PassengerForm, { validatePassenger } from '../components/PassengerForm';
import TripSummary from '../components/TripSummary';
import { colors, spacing, radius, fonts, input as inputStyle, buttons } from '../styles/theme';
import { SearchIcon, PlaneTakeoffIcon } from '../components/Icons';
import { formatINR, formatDateTime, formatDate, formatTime, calculateDuration, getPassengerType } from '../utils/format';

const CREATE_STEPS = [
  { number: 1, label: 'Select Flight' },
  { number: 2, label: 'Passenger Details' },
  { number: 3, label: 'Select Seat' },
  { number: 4, label: 'Review & Confirm' },
];

const tabStyle = (active) => ({
  padding: '10px 20px',
  borderRadius: `${radius.md}px ${radius.md}px 0 0`,
  border: `1px solid ${active ? colors.primary : colors.border}`,
  borderBottom: active ? `2px solid ${colors.bgCard}` : `1px solid ${colors.border}`,
  background: active ? colors.primary : colors.bgCard,
  color: active ? 'white' : colors.textSecondary,
  fontWeight: active ? 600 : 500,
  cursor: 'pointer',
  fontSize: fonts.base,
  transition: 'all 0.15s',
  marginBottom: -1,
  fontFamily: 'inherit',
});

export default function BookingsPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState('search');
  const [ref, setRef] = useState('');
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  // Create flow
  const [createStep, setCreateStep] = useState(0);
  const [flights, setFlights] = useState([]);
  const [selectedFlight, setSelectedFlight] = useState(null);
  const [passengers, setPassengers] = useState([]);
  const [myPassenger, setMyPassenger] = useState(null);
  const [selectedSeat, setSelectedSeat] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [successBooking, setSuccessBooking] = useState(null);
  const [flightSearch, setFlightSearch] = useState('');

  // Load data
  useEffect(() => {
    loadFormData();
  }, []);

  const loadFormData = async () => {
    try {
      const [fRes, pRes] = await Promise.all([
        flightsApi.searchFlights({ size: 100, status: 'SCHEDULED' }),
        passengersApi.getMyPassenger().catch(() => null),
      ]);
      setFlights(fRes.data.data?.content || []);
      if (pRes?.data?.data) {
        setMyPassenger(pRes.data.data);
      }
    } catch { /* ignore */ }
  };

  // Computed pricing
  const baseFarePerPerson = selectedFlight?.basePrice ? parseFloat(selectedFlight.basePrice) : 35000;
  const passengerCount = Math.max(passengers.length, 1);
  const baseFare = baseFarePerPerson * passengerCount;
  const taxes = Math.round(baseFare * 0.12);
  const seatFee = getSeatPrice(selectedSeat);
  const total = baseFare + taxes + seatFee;

  const searchBooking = async () => {
    setError('');
    setLoading(true);
    setHasSearched(true);
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

  const handleCreate = async () => {
    setError('');
    setSubmitting(true);
    try {
      // Create passengers first
      const createdPassengers = [];
      for (const p of passengers) {
        if (p._isMe && myPassenger) {
          createdPassengers.push({ passengerId: myPassenger.id });
        } else {
          const res = await passengersApi.createPassenger({
            firstName: p.firstName,
            lastName: p.lastName,
            dateOfBirth: p.dateOfBirth || '1990-01-01',
            gender: p.gender || 'M',
            email: p.email,
            phone: p.phone,
            userId: user?.userId,
          });
          createdPassengers.push({ passengerId: res.data.data.id });
        }
      }

      const data = {
        customerId: user?.userId,
        flightId: selectedFlight.id,
        passengers: createdPassengers,
        requestedSeats: selectedSeat ? [selectedSeat] : ['1A'],
      };
      const res = await bookingsApi.createBooking(data);
      setSuccessBooking(res.data.data);
      setTab('view');
      setCreateStep(0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create booking');
    } finally {
      setSubmitting(false);
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

  const isPastFlight = (f) => f.scheduledDeparture && new Date(f.scheduledDeparture) < new Date();

  const filteredFlights = flights.filter(f => {
    if (!flightSearch) return true;
    const q = flightSearch.toLowerCase();
    return f.flightNumber?.toLowerCase().includes(q) ||
           f.originAirportIataCode?.toLowerCase().includes(q) ||
           f.destinationAirportIataCode?.toLowerCase().includes(q);
  });

  // Check if passenger step is valid
  const passengersValid = passengers.length > 0 && passengers.every(p => Object.keys(validatePassenger(p)).length === 0);

  return (
    <div>
      <PageHeader title="Bookings" description="View and manage your flight bookings." />

      {/* Tabs */}
      <div style={{ display: 'flex', borderBottom: `1px solid ${colors.border}`, marginBottom: spacing.xxl }}>
        <button onClick={() => setTab('search')} style={tabStyle(tab === 'search')}>Search Booking</button>
        <button onClick={() => { setTab('create'); if (!myPassenger) loadFormData(); }} style={tabStyle(tab === 'create')}>Create Booking</button>
      </div>

      {error && (
        <div style={{ color: colors.danger, background: colors.dangerLight, padding: `${spacing.md}px ${spacing.lg}px`, borderRadius: radius.md, marginBottom: spacing.lg, fontSize: fonts.base }}>
          {error}
        </div>
      )}

      {/* ============ SEARCH TAB ============ */}
      {tab === 'search' && (
        <>
          <form onSubmit={(e) => { e.preventDefault(); searchBooking(); }} style={{ display: 'flex', gap: spacing.sm, marginBottom: spacing.xl }}>
            <div style={{ flex: 1, position: 'relative' }}>
              <input placeholder="Search by booking reference..." value={ref} onChange={(e) => setRef(e.target.value)} style={{ ...inputStyle, paddingLeft: 40 }} />
              <SearchIcon size={16} color={colors.textMuted} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)' }} />
            </div>
            <button type="submit" disabled={loading || !ref.trim()} style={{ ...buttons.primary, minWidth: 100 }}>
              {loading ? 'Searching...' : 'Search'}
            </button>
          </form>

          {!hasSearched && !booking && (
            <EmptyState icon="📅" heading="Find your booking" text="Enter your booking reference to view flight and passenger details." />
          )}

          {hasSearched && !booking && !loading && !error && (
            <EmptyState icon="🔍" heading="No booking found" text="No booking matches that reference. Please check and try again." />
          )}

          {/* Success message */}
          {successBooking && (
            <div style={{ background: colors.successLight, border: `1px solid ${colors.success}`, borderRadius: radius.lg, padding: spacing.xl, marginBottom: spacing.xl }}>
              <h3 style={{ color: colors.success, fontSize: fonts.lg, marginBottom: spacing.sm }}>Booking Confirmed! 🎉</h3>
              <p style={{ color: colors.text, marginBottom: spacing.md }}>Your booking reference is <strong>{successBooking.bookingReference}</strong></p>
              <p style={{ fontSize: fonts.sm, color: colors.textSecondary }}>Total: {formatINR(successBooking.totalAmount)} {successBooking.currency}</p>
            </div>
          )}

          {/* Booking card */}
          {booking && (
            <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, overflow: 'hidden' }}>
              <div style={{ padding: spacing.xl, background: colors.bg, borderBottom: `1px solid ${colors.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: spacing.sm }}>
                <div>
                  <h3 style={{ fontSize: fonts.xl, fontWeight: 600, color: colors.text, margin: 0 }}>Booking {booking.bookingReference}</h3>
                  <span style={{ fontSize: fonts.sm, color: colors.textSecondary }}>Created {booking.bookingDate ? new Date(booking.bookingDate).toLocaleDateString() : '—'}</span>
                </div>
                <StatusBadge status={booking.status} size="lg" />
              </div>
              <div style={{ padding: spacing.xl }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: spacing.xl, marginBottom: spacing.xl }}>
                  <div><div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Flight</div><div style={{ fontSize: fonts.lg, fontWeight: 600 }}>{booking.flightNumber}</div></div>
                  <div><div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Amount</div><div style={{ fontSize: fonts.lg, fontWeight: 600 }}>{formatINR(booking.totalAmount)}</div></div>
                  <div><div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Payment</div><div style={{ fontSize: fonts.lg, fontWeight: 600 }}>{booking.paymentStatus}</div></div>
                </div>
                <div style={{ display: 'flex', gap: spacing.sm }}>
                  {booking.status !== 'CANCELLED' && <button onClick={handleCancel} disabled={loading} style={buttons.danger}>Cancel Booking</button>}
                  <Link to="/tickets" style={{ textDecoration: 'none' }}><button style={buttons.secondary}>View Tickets</button></Link>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* ============ CREATE TAB ============ */}
      {tab === 'create' && (
        <>
          <StepIndicator steps={CREATE_STEPS} currentStep={createStep} />

          {/* ---- STEP 1: SELECT FLIGHT ---- */}
          {createStep === 0 && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: spacing.xl }} className="booking-layout-grid">
              <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, padding: spacing.xl }}>
                <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, marginBottom: spacing.lg }}>Select a Flight</h3>

                {/* Flight search */}
                <div style={{ marginBottom: spacing.lg, position: 'relative' }}>
                  <input placeholder="Search flights..." value={flightSearch} onChange={(e) => setFlightSearch(e.target.value)} style={{ ...inputStyle, paddingLeft: 40 }} />
                  <SearchIcon size={16} color={colors.textMuted} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)' }} />
                </div>

                {filteredFlights.length === 0 ? (
                  <EmptyState icon="✈" heading="No flights available" text={flightSearch ? 'No flights match your search.' : 'No scheduled flights at the moment.'} />
                ) : (
                  <div style={{ display: 'grid', gap: spacing.md }}>
                    {filteredFlights.map(f => {
                      const past = isPastFlight(f);
                      return (
                        <div
                          key={f.id}
                          onClick={() => !past && setSelectedFlight(f)}
                          style={{
                            padding: spacing.lg,
                            border: `2px solid ${selectedFlight?.id === f.id ? colors.primary : colors.border}`,
                            borderRadius: radius.md,
                            cursor: past ? 'not-allowed' : 'pointer',
                            display: 'grid',
                            gridTemplateColumns: '80px 1fr auto',
                            gap: spacing.lg,
                            alignItems: 'center',
                            opacity: past ? 0.5 : 1,
                            transition: 'border-color 0.15s',
                          }}
                        >
                          <div>
                            <div style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.text }}>{f.flightNumber}</div>
                            <StatusBadge status={f.status} size="sm" />
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: spacing.lg }}>
                            <div style={{ textAlign: 'center' }}>
                              <div style={{ fontSize: fonts.xl, fontWeight: 700 }}>{f.originAirportIataCode}</div>
                              <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>{f.originAirportName || ''}</div>
                            </div>
                            <div style={{ color: colors.textMuted }}><PlaneTakeoffIcon size={16} /></div>
                            <div style={{ textAlign: 'center' }}>
                              <div style={{ fontSize: fonts.xl, fontWeight: 700 }}>{f.destinationAirportIataCode}</div>
                              <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>{f.destinationAirportName || ''}</div>
                            </div>
                            <div style={{ fontSize: fonts.sm, color: colors.textSecondary, textAlign: 'center' }}>
                              <div>{formatTime(f.scheduledDeparture)}</div>
                              <div style={{ color: colors.textMuted }}>{calculateDuration(f.scheduledDeparture, f.scheduledArrival)}</div>
                            </div>
                          </div>
                          <div style={{ textAlign: 'right' }}>
                            {past ? (
                              <div style={{ fontSize: fonts.sm, color: colors.danger, fontWeight: 500 }}>Departed</div>
                            ) : (
                              <>
                                <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>{f.availableSeats ?? '—'} seats</div>
                                <div style={{ fontSize: fonts.xl, fontWeight: 700, color: colors.success }}>
                                  {f.basePrice ? formatINR(f.basePrice) : '—'}
                                </div>
                                <div style={{ fontSize: fonts.xs, color: colors.textMuted }}>per person</div>
                              </>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {selectedFlight && !isPastFlight(selectedFlight) && (
                  <div style={{ marginTop: spacing.lg, display: 'flex', justifyContent: 'flex-end' }}>
                    <button onClick={() => {
                      setPassengers(myPassenger ? [{ ...emptyPassengerObj(), firstName: myPassenger.firstName, lastName: myPassenger.lastName, email: myPassenger.email, _isMe: true }] : [emptyPassengerObj()]);
                      setCreateStep(1);
                    }} style={buttons.primary}>
                      Continue →
                    </button>
                  </div>
                )}
              </div>

              {/* Trip Summary sidebar */}
              {selectedFlight && (
                <TripSummary
                  flight={selectedFlight}
                  passengers={passengers}
                  selectedSeat={selectedSeat}
                  seatPrice={getSeatPrice(selectedSeat)}
                  baseFare={baseFare}
                  taxes={taxes}
                  total={total}
                />
              )}
            </div>
          )}

          {/* ---- STEP 2: PASSENGER DETAILS ---- */}
          {createStep === 1 && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: spacing.xl }} className="booking-layout-grid">
              <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, padding: spacing.xl }}>
                <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, marginBottom: spacing.lg }}>Passenger Details</h3>

                {/* Flight summary */}
                {selectedFlight && (
                  <div style={{ padding: spacing.lg, background: colors.bg, borderRadius: radius.md, marginBottom: spacing.xl, display: 'flex', alignItems: 'center', gap: spacing.lg }}>
                    <div style={{ fontWeight: 700 }}>{selectedFlight.flightNumber}</div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
                      <span style={{ fontWeight: 600 }}>{selectedFlight.originAirportIataCode}</span>
                      <span style={{ color: colors.textMuted }}>→</span>
                      <span style={{ fontWeight: 600 }}>{selectedFlight.destinationAirportIataCode}</span>
                    </div>
                    <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>{formatDateTime(selectedFlight.scheduledDeparture)}</div>
                  </div>
                )}

                <PassengerForm
                  passengers={passengers}
                  onChange={setPassengers}
                  myPassenger={myPassenger}
                />

                <div style={{ display: 'flex', gap: spacing.sm, marginTop: spacing.xl }}>
                  <button onClick={() => setCreateStep(0)} style={buttons.secondary}>← Back</button>
                  <button
                    onClick={() => setCreateStep(2)}
                    disabled={!passengersValid}
                    style={buttons.primary}
                  >
                    Continue →
                  </button>
                </div>
              </div>

              <TripSummary flight={selectedFlight} passengers={passengers} selectedSeat={selectedSeat} seatPrice={getSeatPrice(selectedSeat)} baseFare={baseFare} taxes={taxes} total={total} />
            </div>
          )}

          {/* ---- STEP 3: SELECT SEAT ---- */}
          {createStep === 2 && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: spacing.xl }} className="booking-layout-grid">
              <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, padding: spacing.xl }}>
                <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, marginBottom: spacing.lg }}>Select Your Seat</h3>
                <SeatChart selectedSeat={selectedSeat} onSelectSeat={setSelectedSeat} />

                {/* Seat price info */}
                {selectedSeat && (
                  <div style={{ marginTop: spacing.xl, padding: spacing.lg, background: colors.successLight, borderRadius: radius.md, border: `1px solid ${colors.success}20` }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <div style={{ fontWeight: 600, color: colors.text }}>Seat {selectedSeat}</div>
                        <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>{getSeatTypeLabel(selectedSeat)}</div>
                      </div>
                      <div style={{ fontSize: fonts.xl, fontWeight: 700, color: colors.success }}>
                        {getSeatPrice(selectedSeat) > 0 ? `+${formatINR(getSeatPrice(selectedSeat))}` : 'Free'}
                      </div>
                    </div>
                  </div>
                )}

                <div style={{ display: 'flex', gap: spacing.sm, marginTop: spacing.xl }}>
                  <button onClick={() => setCreateStep(1)} style={buttons.secondary}>← Back</button>
                  <button onClick={() => setCreateStep(3)} disabled={!selectedSeat} style={buttons.primary}>Continue →</button>
                </div>
              </div>

              <TripSummary flight={selectedFlight} passengers={passengers} selectedSeat={selectedSeat} seatPrice={getSeatPrice(selectedSeat)} baseFare={baseFare} taxes={taxes} total={total} />
            </div>
          )}

          {/* ---- STEP 4: REVIEW & CONFIRM ---- */}
          {createStep === 3 && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: spacing.xl }} className="booking-layout-grid">
              <div style={{ background: colors.bgCard, borderRadius: radius.lg, border: `1px solid ${colors.border}`, padding: spacing.xl }}>
                <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, marginBottom: spacing.xl }}>Review & Confirm</h3>

                {/* Flight */}
                {selectedFlight && (
                  <div style={{ marginBottom: spacing.xl }}>
                    <h4 style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted, textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: spacing.sm }}>Flight</h4>
                    <div style={{ padding: spacing.lg, background: colors.bg, borderRadius: radius.md }}>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: spacing.md, fontSize: fonts.base }}>
                        <div><span style={{ color: colors.textSecondary }}>Number:</span> <strong>{selectedFlight.flightNumber}</strong></div>
                        <div><span style={{ color: colors.textSecondary }}>Route:</span> <strong>{selectedFlight.originAirportIataCode} → {selectedFlight.destinationAirportIataCode}</strong></div>
                        <div><span style={{ color: colors.textSecondary }}>Departure:</span> <strong>{formatDateTime(selectedFlight.scheduledDeparture)}</strong></div>
                        <div><span style={{ color: colors.textSecondary }}>Arrival:</span> <strong>{formatDateTime(selectedFlight.scheduledArrival)}</strong></div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Passengers */}
                <div style={{ marginBottom: spacing.xl }}>
                  <h4 style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted, textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: spacing.sm }}>Passengers</h4>
                  {passengers.map((p, i) => (
                    <div key={i} style={{ padding: spacing.lg, background: colors.bg, borderRadius: radius.md, marginBottom: spacing.sm }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <div style={{ fontWeight: 600 }}>{p.firstName} {p.lastName}</div>
                          <div style={{ fontSize: fonts.sm, color: colors.textSecondary }}>
                            {getPassengerType(p.dateOfBirth)} · {p.email || 'No email'}
                          </div>
                        </div>
                        {p._isMe && <span style={{ fontSize: fonts.xs, color: colors.primary, fontWeight: 500 }}>You</span>}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Seat */}
                <div style={{ marginBottom: spacing.xl }}>
                  <h4 style={{ fontSize: fonts.sm, fontWeight: 600, color: colors.textMuted, textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: spacing.sm }}>Seat</h4>
                  <div style={{ padding: spacing.lg, background: colors.bg, borderRadius: radius.md }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ fontWeight: 600 }}>{selectedSeat}</span>
                      <span>{getSeatTypeLabel(selectedSeat)} · {getSeatPrice(selectedSeat) > 0 ? `+${formatINR(getSeatPrice(selectedSeat))}` : 'Free'}</span>
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: spacing.sm, marginTop: spacing.xl, paddingTop: spacing.xl, borderTop: `1px solid ${colors.borderLight}` }}>
                  <button onClick={() => setCreateStep(2)} style={buttons.secondary}>← Back</button>
                  <button onClick={handleCreate} disabled={submitting} style={{ ...buttons.success, flex: 1, justifyContent: 'center' }}>
                    {submitting ? 'Processing...' : 'Confirm & Continue to Payment'}
                  </button>
                </div>
              </div>

              <TripSummary flight={selectedFlight} passengers={passengers} selectedSeat={selectedSeat} seatPrice={getSeatPrice(selectedSeat)} baseFare={baseFare} taxes={taxes} total={total} />
            </div>
          )}
        </>
      )}

      <style>{`
        @media (max-width: 900px) {
          .booking-layout-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}

function emptyPassengerObj() {
  return {
    _key: Date.now() + Math.random(),
    firstName: '', lastName: '', dateOfBirth: '', gender: '',
    nationality: '', email: '', phone: '',
    documentType: 'PASSPORT', documentNumber: '',
  };
}
