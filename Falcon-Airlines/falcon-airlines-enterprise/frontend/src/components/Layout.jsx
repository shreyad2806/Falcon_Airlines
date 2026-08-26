import { useState } from 'react';
import { Link, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { colors, spacing, radius, fonts, shadows } from '../styles/theme';
import { BellIcon, LogoutIcon, PlaneIcon } from './Icons';

const navItems = [
  { path: '/dashboard', label: 'Dashboard' },
  { path: '/flights', label: 'Flights' },
  { path: '/bookings', label: 'Bookings' },
  { path: '/tickets', label: 'Tickets' },
  { path: '/boarding-passes', label: 'Boarding Passes' },
];

export default function Layout() {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  // Don't show nav on login/register
  const hideNav = ['/login', '/register'].includes(location.pathname);

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: colors.bg }}>
      {/* Navbar */}
      <nav style={{
        background: colors.primary,
        color: 'white',
        padding: '0 32px',
        display: 'flex',
        alignItems: 'center',
        height: 56,
        position: 'sticky',
        top: 0,
        zIndex: 100,
        boxShadow: '0 1px 4px rgba(0,0,0,0.15)',
      }}>
        {/* Logo */}
        <Link to="/dashboard" style={{
          color: 'white',
          textDecoration: 'none',
          fontWeight: 700,
          fontSize: 17,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginRight: 40,
          whiteSpace: 'nowrap',
        }}>
          <PlaneIcon size={18} color="white" />
          Falcon Airlines
        </Link>

        {/* Desktop nav links */}
        {isAuthenticated && !hideNav && (
          <div style={{
            display: 'flex',
            gap: 2,
            flex: 1,
          }}
          className="desktop-nav"
          >
            {navItems.map(({ path, label }) => {
              const active = isActive(path);
              return (
                <Link key={path} to={path} style={{
                  color: active ? 'white' : 'rgba(255,255,255,0.65)',
                  textDecoration: 'none',
                  fontSize: 13.5,
                  fontWeight: active ? 600 : 400,
                  padding: '8px 14px',
                  borderRadius: radius.sm,
                  transition: 'all 0.15s',
                  borderBottom: active ? '2px solid white' : '2px solid transparent',
                  background: active ? 'rgba(255,255,255,0.1)' : 'transparent',
                }}
                onMouseEnter={(e) => {
                  if (!active) {
                    e.currentTarget.style.color = 'white';
                    e.currentTarget.style.background = 'rgba(255,255,255,0.06)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!active) {
                    e.currentTarget.style.color = 'rgba(255,255,255,0.65)';
                    e.currentTarget.style.background = 'transparent';
                  }
                }}
                >
                  {label}
                </Link>
              );
            })}
          </div>
        )}

        {/* Right side */}
        <div style={{
          marginLeft: 'auto',
          display: 'flex',
          alignItems: 'center',
          gap: spacing.md,
        }}>
          {isAuthenticated ? (
            <>
              {/* Notification bell */}
              <button style={{
                background: 'transparent',
                border: 'none',
                color: 'rgba(255,255,255,0.65)',
                cursor: 'pointer',
                padding: 6,
                borderRadius: radius.sm,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                transition: 'color 0.15s',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.color = 'white'; }}
              onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.65)'; }}
              >
                <BellIcon size={18} />
                {/* Notification dot */}
                <span style={{
                  position: 'absolute',
                  top: 4,
                  right: 4,
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  background: '#EF4444',
                  border: '2px solid ' + colors.primary,
                }} />
              </button>

              {/* User avatar + name */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: spacing.sm,
              }}>
                <span style={{
                  fontSize: 13,
                  color: 'rgba(255,255,255,0.8)',
                  fontWeight: 500,
                }}
                className="desktop-only"
                >{user?.username}</span>
                <div style={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  background: 'rgba(255,255,255,0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 14,
                  fontWeight: 600,
                  color: 'white',
                  border: '2px solid rgba(255,255,255,0.25)',
                  textTransform: 'uppercase',
                }}>
                  {(user?.username || 'U')[0]}
                </div>
              </div>

              {/* Logout button */}
              <button onClick={handleLogout} style={{
                background: colors.danger,
                color: 'white',
                border: 'none',
                padding: '6px 14px',
                borderRadius: radius.sm,
                cursor: 'pointer',
                fontSize: 13,
                fontWeight: 500,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                transition: 'background 0.15s',
                height: 32,
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = colors.dangerHover; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = colors.danger; }}
              >
                Logout
              </button>
            </>
          ) : (
            <Link to="/login" style={{
              color: 'white',
              textDecoration: 'none',
              fontSize: 14,
              fontWeight: 500,
            }}>Login</Link>
          )}

          {/* Mobile menu toggle */}
          {isAuthenticated && !hideNav && (
            <button
              className="mobile-menu-btn"
              onClick={() => setMobileOpen(!mobileOpen)}
              style={{
                display: 'none',
                background: 'transparent',
                border: 'none',
                color: 'white',
                cursor: 'pointer',
                padding: 8,
                fontSize: 20,
              }}
            >
              {mobileOpen ? '✕' : '☰'}
            </button>
          )}
        </div>
      </nav>

      {/* Mobile nav dropdown */}
      {mobileOpen && isAuthenticated && !hideNav && (
        <div className="mobile-nav-dropdown" style={{
          background: colors.primary,
          borderBottom: `1px solid rgba(255,255,255,0.1)`,
          padding: '8px 16px 16px',
        }}>
          {navItems.map(({ path, label }) => (
            <Link
              key={path}
              to={path}
              onClick={() => setMobileOpen(false)}
              style={{
                display: 'block',
                padding: '10px 16px',
                color: isActive(path) ? 'white' : 'rgba(255,255,255,0.7)',
                textDecoration: 'none',
                fontSize: 14,
                fontWeight: isActive(path) ? 600 : 400,
                borderRadius: radius.sm,
                background: isActive(path) ? 'rgba(255,255,255,0.1)' : 'transparent',
              }}
            >
              {label}
            </Link>
          ))}
        </div>
      )}

      {/* Main content */}
      <main style={{
        flex: 1,
        padding: spacing.xxxl,
        maxWidth: 1280,
        margin: '0 auto',
        width: '100%',
      }}>
        <Outlet />
      </main>

      {/* Responsive CSS via inline style tag */}
      <style>{`
        @media (max-width: 900px) {
          .desktop-nav { display: none !important; }
          .desktop-only { display: none !important; }
          .mobile-menu-btn { display: flex !important; }
        }
        @media (min-width: 901px) {
          .mobile-nav-dropdown { display: none !important; }
        }
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
