import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const { register, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await register(form);
      setSuccess('Account created! Redirecting to login...');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: '80px auto' }}>
      <h2 style={{ textAlign: 'center', marginBottom: 24 }}>Create Account</h2>
      {error && <div style={{ color: '#c0392b', background: '#fdecea', padding: 10, borderRadius: 6, marginBottom: 16 }}>{error}</div>}
      {success && <div style={{ color: '#27ae60', background: '#eafaf1', padding: 10, borderRadius: 6, marginBottom: 16 }}>{success}</div>}
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <input placeholder="Username" value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })} required minLength={3}
          style={{ padding: '10px 12px', borderRadius: 6, border: '1px solid #ddd', fontSize: 14 }} />
        <input type="email" placeholder="Email" value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })} required
          style={{ padding: '10px 12px', borderRadius: 6, border: '1px solid #ddd', fontSize: 14 }} />
        <input type="password" placeholder="Password (8+ chars, upper, lower, digit)" value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8}
          style={{ padding: '10px 12px', borderRadius: 6, border: '1px solid #ddd', fontSize: 14 }} />
        <button type="submit" disabled={loading}
          style={{ padding: '10px', background: '#0a2744', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer' }}>
          {loading ? 'Creating account...' : 'Register'}
        </button>
      </form>
      <p style={{ textAlign: 'center', marginTop: 16, fontSize: 14 }}>
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </div>
  );
}
