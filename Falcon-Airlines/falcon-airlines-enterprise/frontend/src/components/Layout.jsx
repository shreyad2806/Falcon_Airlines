import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout() {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <nav style={{ background: '#0a2744', color: 'white', padding: '0 24px', display: 'flex', alignItems: 'center', height: 56, gap: 24 }}>
        <Link to="/" style={{ color: 'white', textDecoration: 'none', fontWeight: 700, fontSize: 18 }}>
          ✈ Falcon Airlines
        </Link>
        {isAuthenticated && (
          <div style={{ display: 'flex', gap: 16, flex: 1 }}>
            <Link to="/flights" style={{ color: '#ccc', textDecoration: 'none', fontSize: 14 }}>Flights</Link>
            <Link to="/bookings" style={{ color: '#ccc', textDecoration: 'none', fontSize: 14 }}>Bookings</Link>
            <Link to="/tickets" style={{ color: '#ccc', textDecoration: 'none', fontSize: 14 }}>Tickets</Link>
            <Link to="/boarding-passes" style={{ color: '#ccc', textDecoration: 'none', fontSize: 14 }}>Boarding Passes</Link>
          </div>
        )}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 12 }}>
          {isAuthenticated ? (
            <>
              <span style={{ fontSize: 13, color: '#aaa' }}>{user?.username}</span>
              <button onClick={handleLogout} style={{ background: '#c0392b', color: 'white', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}>
                Logout
              </button>
            </>
          ) : (
            <Link to="/login" style={{ color: 'white', textDecoration: 'none', fontSize: 14 }}>Login</Link>
          )}
        </div>
      </nav>
      <main style={{ flex: 1, padding: 24, maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </main>
    </div>
  );
}
