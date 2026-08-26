import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { colors, spacing, radius, fonts, buttons } from '../styles/theme';
import { PlaneIcon } from '../components/Icons';

export default function LoginPage() {
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    try {
      await login(form.usernameOrEmail, form.password);
      navigate('/dashboard');
    } catch (err) {
      const data = err.response?.data;
      if (data?.details && data.details.length > 0) {
        const mapped = {};
        data.details.forEach(d => { mapped[d.field] = d.message; });
        setFieldErrors(mapped);
        setError(data.message || 'Please fix the errors below.');
      } else {
        setError(data?.message || 'Login failed');
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
          }}>Sign In</h2>
          <p style={{ fontSize: fonts.base, color: colors.textSecondary, marginTop: spacing.xs }}>
            Welcome back to Falcon Airlines
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

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: spacing.lg }}>
          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.sm }}>
              Username or Email
            </label>
            <input
              placeholder="Enter your username or email"
              value={form.usernameOrEmail}
              onChange={(e) => setForm({ ...form, usernameOrEmail: e.target.value })}
              required
              style={inputStyle(fieldErrors.usernameOrEmail)}
              onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
              onBlur={(e) => { e.target.style.borderColor = fieldErrors.usernameOrEmail ? colors.danger : colors.border; }}
            />
            {fieldErrors.usernameOrEmail && (
              <div style={{ color: colors.danger, fontSize: fonts.sm, marginTop: spacing.xs }}>{fieldErrors.usernameOrEmail}</div>
            )}
          </div>

          <div>
            <label style={{ fontSize: fonts.sm, fontWeight: 500, color: colors.text, display: 'block', marginBottom: spacing.sm }}>
              Password
            </label>
            <input
              type="password"
              placeholder="Enter your password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
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
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <p style={{
          textAlign: 'center',
          marginTop: spacing.xl,
          fontSize: fonts.base,
          color: colors.textSecondary,
        }}>
          Don't have an account?{' '}
          <Link to="/register" style={{ color: colors.primary, fontWeight: 500 }}>Register</Link>
        </p>
      </div>
    </div>
  );
}
