import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import FlightsPage from './pages/FlightsPage';
import BookingsPage from './pages/BookingsPage';
import TicketsPage from './pages/TicketsPage';
import BoardingPassesPage from './pages/BoardingPassesPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
            <Route path="/flights" element={<ProtectedRoute><FlightsPage /></ProtectedRoute>} />
            <Route path="/bookings" element={<ProtectedRoute><BookingsPage /></ProtectedRoute>} />
            <Route path="/tickets" element={<ProtectedRoute><TicketsPage /></ProtectedRoute>} />
            <Route path="/boarding-passes" element={<ProtectedRoute><BoardingPassesPage /></ProtectedRoute>} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
