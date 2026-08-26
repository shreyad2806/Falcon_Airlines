import { Link } from 'react-router-dom';
import { colors, spacing, radius, fonts, shadows } from '../styles/theme';

/**
 * Dashboard statistics card with icon, count, and supporting text.
 */
export default function StatCard({ icon, label, count, subtext, to, iconBg = colors.primary + '0D', iconColor = colors.primary }) {
  const content = (
    <div style={{
      background: colors.bgCard,
      border: `1px solid ${colors.border}`,
      borderRadius: radius.lg,
      padding: `${spacing.xl}px`,
      display: 'flex',
      alignItems: 'flex-start',
      gap: spacing.lg,
      transition: 'box-shadow 0.2s, transform 0.15s',
      cursor: to ? 'pointer' : 'default',
      minHeight: 90,
    }}
    onMouseEnter={(e) => {
      if (to) {
        e.currentTarget.style.boxShadow = shadows.cardHover;
        e.currentTarget.style.transform = 'translateY(-1px)';
      }
    }}
    onMouseLeave={(e) => {
      e.currentTarget.style.boxShadow = 'none';
      e.currentTarget.style.transform = 'translateY(0)';
    }}
    >
      {/* Icon */}
      <div style={{
        width: 44,
        height: 44,
        borderRadius: radius.lg,
        background: iconBg,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        fontSize: 20,
      }}>
        {icon}
      </div>

      {/* Content */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: fonts.sm,
          color: colors.textSecondary,
          fontWeight: 500,
          marginBottom: spacing.xs,
        }}>{label}</div>
        <div style={{
          fontSize: 28,
          fontWeight: 700,
          color: colors.text,
          lineHeight: 1.1,
          marginBottom: 2,
        }}>{count}</div>
        {subtext && (
          <div style={{
            fontSize: fonts.sm,
            color: colors.textMuted,
            lineHeight: 1.3,
          }}>{subtext}</div>
        )}
      </div>
    </div>
  );

  if (to) {
    return <Link to={to} style={{ textDecoration: 'none' }}>{content}</Link>;
  }
  return content;
}
