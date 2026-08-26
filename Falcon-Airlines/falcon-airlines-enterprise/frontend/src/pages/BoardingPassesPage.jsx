import { useState } from 'react';
import { Link } from 'react-router-dom';
import * as bpApi from '../api/boardingPasses';
import PageHeader from '../components/PageHeader';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import { colors, spacing, radius, fonts, input as inputStyle, buttons } from '../styles/theme';
import { SearchIcon, DownloadIcon, QrCodeIcon, CheckInIcon, PlaneTakeoffIcon, ShareIcon } from '../components/Icons';

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

export default function BoardingPassesPage() {
  const [tab, setTab] = useState('view');
  const [bpId, setBpId] = useState('');
  const [ticketId, setTicketId] = useState('');
  const [bp, setBp] = useState(null);
  const [qrBase64, setQrBase64] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const searchBp = async () => {
    setError('');
    setQrBase64(null);
    setLoading(true);
    setHasSearched(true);
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

  const formatTime = (s) => {
    if (!s) return '--:--';
    return new Date(s).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
  };

  const formatDate = (s) => {
    if (!s) return '—';
    return new Date(s).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  return (
    <div>
      <PageHeader title="Boarding Passes" description="View and manage your boarding passes." />

      {/* Tabs */}
      <div style={{
        display: 'flex',
        borderBottom: `1px solid ${colors.border}`,
        marginBottom: spacing.xxl,
      }}>
        <button onClick={() => setTab('view')} style={tabStyle(tab === 'view')}>
          My Boarding Passes
        </button>
        <button onClick={() => setTab('generate')} style={tabStyle(tab === 'generate')}>
          Generate from Ticket
        </button>
      </div>

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

      {/* VIEW TAB */}
      {tab === 'view' && (
        <>
          <form onSubmit={(e) => { e.preventDefault(); searchBp(); }} style={{
            display: 'flex',
            gap: spacing.sm,
            marginBottom: spacing.xl,
          }}>
            <div style={{ flex: 1, position: 'relative' }}>
              <input
                placeholder="Search by Boarding Pass ID..."
                type="number"
                value={bpId}
                onChange={(e) => setBpId(e.target.value)}
                style={{ ...inputStyle, paddingLeft: 40 }}
              />
              <SearchIcon
                size={16}
                color={colors.textMuted}
                style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)' }}
              />
            </div>
            <button type="submit" disabled={loading || !bpId} style={{ ...buttons.primary, minWidth: 100 }}>
              {loading ? 'Searching...' : 'Search'}
            </button>
          </form>

          {!hasSearched && !bp && (
            <EmptyState
              icon="🛫"
              heading="No boarding passes yet"
              text="Generate a boarding pass from an eligible ticket, or search by ID."
              ctaLabel="View Tickets"
              ctaTo="/tickets"
            />
          )}
        </>
      )}

      {/* GENERATE TAB */}
      {tab === 'generate' && !bp && (
        <div style={{
          background: colors.bgCard,
          borderRadius: radius.lg,
          border: `1px solid ${colors.border}`,
          padding: spacing.xxxl,
          textAlign: 'center',
          maxWidth: 480,
          margin: '0 auto',
        }}>
          <div style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            background: colors.bg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: `0 auto ${spacing.xl}px`,
            fontSize: 36,
          }}>🛫</div>
          <h3 style={{ fontSize: fonts.xl, fontWeight: 600, color: colors.text, marginBottom: spacing.sm }}>
            Generate your boarding pass
          </h3>
          <p style={{ fontSize: fonts.base, color: colors.textSecondary, marginBottom: spacing.xl, lineHeight: 1.5 }}>
            Enter a valid ticket ID to create your boarding pass.
          </p>
          <form onSubmit={(e) => { e.preventDefault(); generate(); }} style={{
            display: 'flex',
            gap: spacing.sm,
            justifyContent: 'center',
          }}>
            <input
              placeholder="Ticket ID"
              type="number"
              value={ticketId}
              onChange={(e) => setTicketId(e.target.value)}
              style={{ ...inputStyle, width: 240 }}
            />
            <button type="submit" disabled={loading || !ticketId} style={{ ...buttons.success }}>
              {loading ? 'Generating...' : 'Generate'}
            </button>
          </form>
        </div>
      )}

      {/* Boarding pass card */}
      {bp && (
        <div style={{
          background: colors.bgCard,
          borderRadius: radius.lg,
          border: `1px solid ${colors.border}`,
          overflow: 'hidden',
        }}>
          {/* Header */}
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
                background: colors.primary + '0D',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 20,
              }}>🛫</div>
              <div>
                <h3 style={{ fontSize: fonts.xl, fontWeight: 600, color: colors.text, margin: 0 }}>
                  {bp.flightNumber}
                </h3>
                <span style={{ fontSize: fonts.sm, color: colors.textSecondary }}>
                  Pass {bp.boardingPassNumber}
                </span>
              </div>
            </div>
            <StatusBadge status={bp.status} size="lg" />
          </div>

          {/* Body */}
          <div style={{ padding: spacing.xl }}>
            {/* Route visualization */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: `${spacing.xl}px`,
              background: colors.bg,
              borderRadius: radius.lg,
              marginBottom: spacing.xl,
            }}>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 32, fontWeight: 700, color: colors.text, letterSpacing: 1 }}>
                  {bp.originAirportCode}
                </div>
                <div style={{ fontSize: fonts.sm, color: colors.textSecondary, marginTop: 4 }}>
                  {bp.originAirportName || 'Origin'}
                </div>
              </div>
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 16px' }}>
                <div style={{ flex: 1, height: 2, background: colors.border }} />
                <div style={{ padding: '0 12px' }}>
                  <PlaneTakeoffIcon size={24} color={colors.primary} />
                </div>
                <div style={{ flex: 1, height: 2, background: colors.border }} />
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 32, fontWeight: 700, color: colors.text, letterSpacing: 1 }}>
                  {bp.destinationAirportCode}
                </div>
                <div style={{ fontSize: fonts.sm, color: colors.textSecondary, marginTop: 4 }}>
                  {bp.destinationAirportName || 'Destination'}
                </div>
              </div>
            </div>

            {/* Details grid */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
              gap: spacing.xl,
              marginBottom: spacing.xl,
            }}>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Passenger</div>
                <div style={{ fontWeight: 600, color: colors.text }}>{bp.passengerName}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Seat</div>
                <div style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.text }}>{bp.seatNumber || '—'}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Date</div>
                <div style={{ fontWeight: 500, color: colors.text }}>{formatDate(bp.scheduledDeparture)}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Gate</div>
                <div style={{ fontSize: fonts.lg, fontWeight: 700, color: colors.text }}>{bp.gate || 'TBA'}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Boarding Time</div>
                <div style={{ fontWeight: 600, color: colors.text }}>{formatTime(bp.boardingTime)}</div>
              </div>
              <div>
                <div style={{ fontSize: fonts.sm, color: colors.textMuted, marginBottom: 4 }}>Group</div>
                <div style={{ fontWeight: 600, color: colors.text }}>{bp.boardingGroup || '—'}</div>
              </div>
            </div>

            {/* QR Code + Actions row */}
            <div style={{
              display: 'flex',
              gap: spacing.xl,
              alignItems: 'flex-start',
              flexWrap: 'wrap',
            }}>
              {/* QR Code */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                padding: spacing.xl,
                background: colors.bg,
                borderRadius: radius.lg,
                border: `1px solid ${colors.borderLight}`,
                minWidth: 180,
              }}>
                {qrBase64 ? (
                  <>
                    <img
                      src={`data:image/png;base64,${qrBase64}`}
                      alt="Boarding Pass QR Code"
                      style={{
                        width: 140,
                        height: 140,
                        borderRadius: radius.md,
                        marginBottom: spacing.sm,
                      }}
                    />
                    <span style={{ fontSize: fonts.xs, color: colors.textMuted }}>Scan at the gate</span>
                  </>
                ) : (
                  <button
                    onClick={loadQr}
                    style={{
                      ...buttons.secondary,
                      flexDirection: 'column',
                      padding: `${spacing.xxl}px ${spacing.xl}px`,
                      gap: spacing.sm,
                      background: 'transparent',
                      borderStyle: 'dashed',
                    }}
                  >
                    <QrCodeIcon size={32} color={colors.textMuted} />
                    <span style={{ fontSize: fonts.sm, color: colors.textMuted }}>Show QR Code</span>
                  </button>
                )}
              </div>

              {/* Actions */}
              <div style={{ flex: 1, minWidth: 200 }}>
                <h4 style={{ fontSize: fonts.base, fontWeight: 600, color: colors.text, marginBottom: spacing.lg }}>
                  Actions
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: spacing.sm }}>
                  <button onClick={downloadPdf} style={{ ...buttons.primary, width: '100%', justifyContent: 'center' }}>
                    <DownloadIcon size={16} color="white" />
                    Download PDF
                  </button>
                  <button
                    onClick={() => {
                      if (navigator.share) {
                        navigator.share({ title: `Boarding Pass ${bp.boardingPassNumber}`, text: `${bp.flightNumber} ${bp.originAirportCode} → ${bp.destinationAirportCode} Seat ${bp.seatNumber}` });
                      }
                    }}
                    style={{ ...buttons.secondary, width: '100%', justifyContent: 'center' }}
                  >
                    <ShareIcon size={16} />
                    Share
                  </button>
                  {bp.status === 'GENERATED' && (
                    <button onClick={handleCheckIn} disabled={loading} style={{ ...buttons.success, width: '100%', justifyContent: 'center' }}>
                      <CheckInIcon size={16} color="white" />
                      Check In
                    </button>
                  )}
                  {bp.status === 'CHECKED_IN' && (
                    <button onClick={handleBoard} disabled={loading} style={{
                      ...buttons.secondary,
                      width: '100%',
                      justifyContent: 'center',
                      background: colors.warning,
                      color: 'white',
                      borderColor: colors.warning,
                    }}>
                      Board Passenger
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {tab === 'view' && hasSearched && !bp && !loading && !error && (
        <EmptyState
          icon="🔍"
          heading="No boarding pass found"
          text="No boarding pass matches that ID. Please check and try again."
        />
      )}
    </div>
  );
}
