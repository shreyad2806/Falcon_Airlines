import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { colors, spacing, radius, fonts, buttons } from '../styles/theme';
import { PlaneIcon } from '../components/Icons';

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [success, setSuccess] = useState('');
  const { register, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setSuccess('');
    try {
      await register(form);
      setSuccess('Account created! Redirecting to login...');
      setTimeout(() => navigate('/login'), 1200);
    } catch (err) {
      const data = err.response?.data;
      if (data?.details && data.details.length > 0) {
        const mapped = {};
        data.details.forEach(d => { mapped[d.field] = d.message; });
        setFieldErrors(mapped);
        setError(data.message || 'Please fix the errors below.');
      } else {
        setError(data?.message || 'Registration failed');
      }
    }
  };

  const inputStyle = (hasError) => ({
    padding: '12px 14px',
    borderRadius: radius.md,
    border: `1px solid ${hasError ? colors.danger : colors.border}`,
    fontSize: fonts.base,
    width: '100%',
    boxSizing: 'border-box',
    outline: 'none',
    transition: 'border-color 0.15s',
    height: 44,
  });

  return (
    <div style={{
      minHeight: 'calc(100vh - 56px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: spacing.xxxl,
    }}>
      <div style={{
        width: '100%',
        maxWidth: 420,
        background: colors.bgCard,
        borderRadius: radius.xl,
        border: `1px solid ${colors.border}`,
        padding: `${spacing.xxxl}px ${spacing.xxxl}px`,
        boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
      }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: spacing.xxl }}>
          <div style={{
            width: 48,
            height: 48,
            borderRadius: '50%',
            background: colors.primary,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: `0 auto ${spacing.lg}px`,
          }}>
            <PlaneIcon size={22} color="white" />
          </div>
          <h2 style={{
            fontSize: fonts.xxl,
            fontWeight: 700,
            color: colors.text,
            margin: 0,
          }}>Create Account</h2>
          <p style={{ fontSize: fonts.base, color: colors.textSecondary, marginTop: spacing.xs }}>
            Join Falcon Airlines today
          </p>
        </div>

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

        {success && (
          <div style={{
            color: colors.success,
            background: colors.successLight,
            padding: `${spacing.md}px ${spacing.lg}px`,
            borderRadius: radius.md,
            marginBottom: spacing.lg,
            fontSize: fonts.base,
          }}>
            {success}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: spacing.lg }}>
          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.sm }}>
              Username
            </label>
            <input
              placeholder="Choose a username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
              minLength={3}
              style={inputStyle(fieldErrors.username)}
              onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
              onBlur={(e) => { e.target.style.borderColor = fieldErrors.username ? colors.danger : colors.border; }}
            />
            {fieldErrors.username && (
              <div style={{ color: colors.danger, fontSize: fonts.sm, marginTop: spacing.xs }}>{fieldErrors.username}</div>
            )}
          </div>

          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.sm }}>
              Email
            </label>
            <input
              type="email"
              placeholder="Enter your email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
              style={inputStyle(fieldErrors.email)}
              onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
              onBlur={(e) => { e.target.style.borderColor = fieldErrors.email ? colors.danger : colors.border; }}
            />
            {fieldErrors.email && (
              <div style={{ color: colors.danger, fontSize: fonts.sm, marginTop: spacing.xs }}>{fieldErrors.email}</div>
            )}
          </div>

          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.sm }}>
              Password
            </label>
            <input
              type="password"
              placeholder="Min 8 chars, upper, lower, digit"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
              minLength={8}
              style={inputStyle(fieldErrors.password)}
              onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
              onBlur={(e) => { e.target.style.borderColor = fieldErrors.password ? colors.danger : colors.border; }}
            />
            {fieldErrors.password && (
              <div style={{ color: colors.danger, fontSize: fonts.sm, marginTop: spacing.xs }}>{fieldErrors.password}</div>
            )}
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              ...buttons.primary,
              width: '100%',
              justifyContent: 'center',
              height: 44,
              marginTop: spacing.sm,
            }}
          >
            {loading ? 'Creating account...' : 'Register'}
          </button>
        </form>

        <p style={{
          textAlign: 'center',
          marginTop: spacing.xl,
          fontSize: fonts.base,
          color: colors.textSecondary,
        }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: colors.primary, fontWeight: 500 }}>Sign in</Link>
        </p>
      </div>
    </div>
  );
}
