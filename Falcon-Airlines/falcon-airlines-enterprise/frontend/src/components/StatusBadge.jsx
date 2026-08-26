import { colors, badge, statusColors } from '../styles/theme';

/**
 * Consistent status badge for flights, bookings, tickets, and boarding passes.
 * Falls back gracefully for unknown statuses.
 */
export default function StatusBadge({ status, size = 'default' }) {
  const key = (status || '').toUpperCase();
  const scheme = statusColors[key] || { bg: colors.borderLight, text: colors.textSecondary };
  
  const sizeStyles = size === 'sm' 
    ? { padding: '1px 6px', fontSize: 11 }
    : size === 'lg' 
      ? { padding: '5px 14px', fontSize: 13 }
      : {};

  return (
    <span style={{
      ...badge,
      ...sizeStyles,
      background: scheme.bg,
      color: scheme.text,
    }}>
      {status || 'UNKNOWN'}
    </span>
  );
}
