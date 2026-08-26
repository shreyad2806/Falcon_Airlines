import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardCard from '../components/DashboardCard';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import { colors, spacing, radius, fonts, card, buttons, shadows } from '../styles/theme';
import { PlaneIcon, CalendarIcon, TicketIcon, BoardingPassIcon, PlaneTakeoffIcon } from '../components/Icons';
import * as bookingsApi from '../api/bookings';
import * as ticketsApi from '../api/tickets';
import * as flightsApi from '../api/flights';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const name = user?.username || 'Traveler';

  const [stats, setStats] = useState({ flights: 0, bookings: 0, tickets: 0, boardingPasses: 0 });
  const [upcomingFlight, setUpcomingFlight] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const [flightsRes, bookingsRes] = await Promise.allSettled([
        flightsApi.searchFlights({ size: 100, status: 'SCHEDULED' }),
        bookingsApi.getBookingByRef(''), // We'll use a different approach
      ]);

      let flightCount = 0;
      if (flightsRes.status === 'fulfilled') {
        const content = flightsRes.value.data?.data?.content || [];
        flightCount = content.length;
        // Find upcoming flight (first scheduled one)
        if (content.length > 0) {
          setUpcomingFlight(content[0]);
        }
      }

      setStats({
        flights: flightCount,
        bookings: 0,
        tickets: 0,
        boardingPasses: 0,
      });
    } catch {
      // Silent fail for dashboard stats
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
    return new Date(s).toLocaleDateString('en-US', { day: 'numeric', month: 'short' });
  };

  return (
    <div>
      {/* Welcome header */}
      <div style={{ marginBottom: spacing.xxxl }}>
        <h1 style={{
          fontSize: fonts.hero,
          fontWeight: 700,
          color: colors.text,
          marginBottom: spacing.xs,
        }}>
          Welcome back, {name} 👋
        </h1>
        <p style={{
          fontSize: fonts.lg,
          color: colors.textSecondary,
          lineHeight: 1.5,
        }}>
          Here's what's happening with your journeys.
        </p>
      </div>

      {/* Stats + Upcoming Flight row */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 320px',
        gap: spacing.xl,
        marginBottom: spacing.xxl,
      }}
      className="dashboard-main-grid"
      >
        {/* Stats cards */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: spacing.lg,
        }}
        className="dashboard-stats-grid"
        >
          <StatCard
            icon="✈"
            label="Upcoming Flights"
            count={stats.flights}
            subtext={upcomingFlight ? `Next: ${upcomingFlight.flightNumber}` : 'No flights scheduled'}
            to="/flights"
            iconBg={colors.primary + '0D'}
            iconColor={colors.primary}
          />
          <StatCard
            icon="📅"
            label="Active Bookings"
            count={stats.bookings}
            subtext={stats.bookings > 0 ? `${stats.bookings} confirmed` : 'No bookings yet'}
            to="/bookings"
            iconBg={colors.statusActiveBg}
            iconColor={colors.success}
          />
          <StatCard
            icon="🎫"
            label="Your Tickets"
            count={stats.tickets}
            subtext={stats.tickets > 0 ? `${stats.tickets} issued` : 'No tickets yet'}
            to="/tickets"
            iconBg="#EDE9FE"
            iconColor="#7C3AED"
          />
          <StatCard
            icon="🛫"
            label="Boarding Passes"
            count={stats.boardingPasses}
            subtext={stats.boardingPasses > 0 ? `${stats.boardingPasses} passes` : 'No boarding passes'}
            to="/boarding-passes"
            iconBg={colors.warningLight}
            iconColor="#B45309"
          />
        </div>

        {/* Upcoming Flight sidebar */}
        <div style={{
          background: colors.bgCard,
          borderRadius: radius.lg,
          border: `1px solid ${colors.border}`,
          padding: spacing.xl,
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: spacing.lg,
          }}>
            <h3 style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text, margin: 0 }}>
              Upcoming Flight
            </h3>
            <button
              onClick={() => navigate('/flights')}
              style={{
                ...buttons.ghost,
                color: colors.primary,
                fontSize: fonts.sm,
                padding: '4px 8px',
                height: 'auto',
              }}
            >
              View All
            </button>
          </div>

          {upcomingFlight ? (
            <div>
              {/* Status + flight number + date */}
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: spacing.lg,
              }}>
                <StatusBadge status={upcomingFlight.status} size="sm" />
                <span style={{
                  fontSize: fonts.sm,
                  color: colors.textSecondary,
                  fontWeight: 500,
                }}>{upcomingFlight.flightNumber}</span>
                <span style={{
                  fontSize: fonts.sm,
                  color: colors.textMuted,
                }}>{formatDate(upcomingFlight.scheduledDeparture)}</span>
              </div>

              {/* Route */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: spacing.xl,
                padding: `${spacing.lg}px 0`,
              }}>
                {/* Origin */}
                <div style={{ textAlign: 'center' }}>
                  <div style={{
                    fontSize: 28,
                    fontWeight: 700,
                    color: colors.text,
                    letterSpacing: 1,
                  }}>{upcomingFlight.originAirportIataCode}</div>
                  <div style={{
                    fontSize: fonts.sm,
                    color: colors.textSecondary,
                    marginTop: 2,
                  }}>{upcomingFlight.originAirportName || 'Origin'}</div>
                </div>

                {/* Plane icon */}
                <div style={{
                  color: colors.textMuted,
                  display: 'flex',
                  alignItems: 'center',
                  padding: '0 8px',
                }}>
                  <PlaneTakeoffIcon size={22} color={colors.textMuted} />
                </div>

                {/* Destination */}
                <div style={{ textAlign: 'center' }}>
                  <div style={{
                    fontSize: 28,
                    fontWeight: 700,
                    color: colors.text,
                    letterSpacing: 1,
                  }}>{upcomingFlight.destinationAirportIataCode}</div>
                  <div style={{
                    fontSize: fonts.sm,
                    color: colors.textSecondary,
                    marginTop: 2,
                  }}>{upcomingFlight.destinationAirportName || 'Destination'}</div>
                </div>
              </div>

              {/* Times */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: spacing.md,
                marginBottom: spacing.xl,
              }}>
                <div>
                  <div style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text }}>
                    {formatTime(upcomingFlight.scheduledDeparture)}
                  </div>
                  <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>
                    {formatDate(upcomingFlight.scheduledDeparture)}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text }}>
                    {formatTime(upcomingFlight.scheduledArrival)}
                  </div>
                  <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>
                    {formatDate(upcomingFlight.scheduledArrival)}
                  </div>
                </div>
              </div>

              {/* Gate + Seat */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: spacing.md,
                marginBottom: spacing.xl,
                padding: `${spacing.lg}px`,
                background: colors.bg,
                borderRadius: radius.md,
              }}>
                <div>
                  <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>Gate</div>
                  <div style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text }}>{upcomingFlight.gate || 'TBA'}</div>
                </div>
                <div>
                  <div style={{ fontSize: fonts.sm, color: colors.textMuted }}>Seat</div>
                  <div style={{ fontSize: fonts.lg, fontWeight: 600, color: colors.text }}>{upcomingFlight.seatNumber || '—'}</div>
                </div>
              </div>

              <button
                onClick={() => navigate('/flights')}
                style={{
                  ...buttons.primary,
                  width: '100%',
                  justifyContent: 'center',
                }}
              >
                View Details ›
              </button>
            </div>
          ) : (
            <div style={{
              textAlign: 'center',
              padding: `${spacing.xxl}px ${spacing.lg}`,
            }}>
              <div style={{
                width: 56,
                height: 56,
                borderRadius: '50%',
                background: colors.bg,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: `0 auto ${spacing.lg}px`,
                fontSize: 28,
              }}>✈</div>
              <p style={{
                fontSize: fonts.base,
                color: colors.textSecondary,
                marginBottom: spacing.lg,
                lineHeight: 1.5,
              }}>No upcoming flights</p>
              <button
                onClick={() => navigate('/flights')}
                style={{
                  ...buttons.secondary,
                  fontSize: fonts.sm,
                }}
              >
                Browse Flights
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div>
        <h2 style={{
          fontSize: fonts.xl,
          fontWeight: 600,
          color: colors.text,
          marginBottom: spacing.lg,
        }}>
          Quick Actions
        </h2>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: spacing.lg,
        }}
        className="quick-actions-grid"
        >
          <DashboardCard
            icon="✈"
            title="Browse Flights"
            description="Search and explore flights"
            to="/flights"
          />
          <DashboardCard
            icon="📅"
            title="My Bookings"
            description="View and manage bookings"
            to="/bookings"
          />
          <DashboardCard
            icon="🎫"
            title="My Tickets"
            description="View tickets and download"
            to="/tickets"
          />
          <DashboardCard
            icon="🛫"
            title="Boarding Passes"
            description="Generate and download"
            to="/boarding-passes"
          />
        </div>
      </div>

      {/* Responsive styles */}
      <style>{`
        @media (max-width: 1100px) {
          .dashboard-main-grid {
            grid-template-columns: 1fr !important;
          }
        }
        @media (max-width: 900px) {
          .dashboard-stats-grid {
            grid-template-columns: repeat(2, 1fr) !important;
          }
          .quick-actions-grid {
            grid-template-columns: 1fr !important;
          }
        }
        @media (max-width: 500px) {
          .dashboard-stats-grid {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
}
