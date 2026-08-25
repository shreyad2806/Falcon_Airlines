import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import FlightsPage from './pages/FlightsPage';
import BookingsPage from './pages/BookingsPage';
import TicketsPage from './pages/TicketsPage';
import BoardingPassesPage from './pages/BoardingPassesPage';

function HomePage() {
  return (
    <div style={{ textAlign: 'center', marginTop: 60 }}>
      <h1 style={{ fontSize: 36, color: '#0a2744' }}>✈ Falcon Airlines</h1>
      <p style={{ fontSize: 18, color: '#666', marginTop: 12 }}>Enterprise Reservation & Management Platform</p>
      <p style={{ color: '#999', marginTop: 24 }}>Use the navigation above to access flights, bookings, tickets, and boarding passes.</p>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/flights" element={<ProtectedRoute><FlightsPage /></ProtectedRoute>} />
            <Route path="/bookings" element={<ProtectedRoute><BookingsPage /></ProtectedRoute>} />
            <Route path="/tickets" element={<ProtectedRoute><TicketsPage /></ProtectedRoute>} />
            <Route path="/boarding-passes" element={<ProtectedRoute><BoardingPassesPage /></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
