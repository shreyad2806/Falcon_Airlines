import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' });
  const [error, setError] = useState('');
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(form.usernameOrEmail, form.password);
      navigate('/flights');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: '80px auto' }}>
      <h2 style={{ textAlign: 'center', marginBottom: 24 }}>Sign In</h2>
      {error && <div style={{ color: '#c0392b', background: '#fdecea', padding: 10, borderRadius: 6, marginBottom: 16 }}>{error}</div>}
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <input placeholder="Username or Email" value={form.usernameOrEmail}
          onChange={(e) => setForm({ ...form, usernameOrEmail: e.target.value })} required
          style={{ padding: '10px 12px', borderRadius: 6, border: '1px solid #ddd', fontSize: 14 }} />
        <input type="password" placeholder="Password" value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })} required
          style={{ padding: '10px 12px', borderRadius: 6, border: '1px solid #ddd', fontSize: 14 }} />
        <button type="submit" disabled={loading}
          style={{ padding: '10px', background: '#0a2744', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer' }}>
          {loading ? 'Signing in...' : 'Sign In'}
        </button>
      </form>
      <p style={{ textAlign: 'center', marginTop: 16, fontSize: 14 }}>
        Don't have an account? <Link to="/register">Register</Link>
      </p>
    </div>
  );
}
