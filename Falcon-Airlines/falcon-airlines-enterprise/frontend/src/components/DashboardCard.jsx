import { Link } from 'react-router-dom';
import { colors, spacing, radius, fonts, shadows } from '../styles/theme';

/**
 * Clickable dashboard card for quick actions — matches reference image style.
 */
export default function DashboardCard({ icon, title, description, to }) {
  return (
    <Link to={to} style={{
      display: 'flex',
      alignItems: 'center',
      gap: spacing.lg,
      padding: `${spacing.xl}px ${spacing.xl}px`,
      background: colors.bgCard,
      border: `1px solid ${colors.border}`,
      borderRadius: radius.lg,
      textDecoration: 'none',
      color: colors.text,
      transition: 'border-color 0.2s, box-shadow 0.2s, transform 0.15s',
      cursor: 'pointer',
      minHeight: 72,
    }}
    onMouseEnter={(e) => {
      e.currentTarget.style.borderColor = colors.primary;
      e.currentTarget.style.boxShadow = shadows.cardHover;
      e.currentTarget.style.transform = 'translateY(-1px)';
    }}
    onMouseLeave={(e) => {
      e.currentTarget.style.borderColor = colors.border;
      e.currentTarget.style.boxShadow = 'none';
      e.currentTarget.style.transform = 'translateY(0)';
    }}
    >
      {/* Icon container */}
      <div style={{
        width: 44,
        height: 44,
        borderRadius: radius.lg,
        background: colors.primary + '0D',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: 22,
        flexShrink: 0,
      }}>
        {icon}
      </div>
      
      {/* Text */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: fonts.lg,
          fontWeight: 600,
          color: colors.text,
          marginBottom: 2,
        }}>{title}</div>
        {description && (
          <div style={{
            fontSize: fonts.sm,
            color: colors.textSecondary,
            lineHeight: 1.4,
          }}>{description}</div>
        )}
      </div>

      {/* Arrow */}
      <div style={{
        color: colors.textMuted,
        flexShrink: 0,
        fontSize: 18,
        lineHeight: 1,
      }}>›</div>
    </Link>
  );
}
