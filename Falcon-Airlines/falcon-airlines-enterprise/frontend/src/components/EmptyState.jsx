import { Link } from 'react-router-dom';
import { colors, spacing, radius, fonts } from '../styles/theme';

/**
 * Consistent empty state display for pages with no data.
 */
export default function EmptyState({ icon, heading, text, ctaLabel, ctaTo }) {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: `${spacing.xxxl * 2}px ${spacing.xl}px`,
      textAlign: 'center',
      minHeight: 280,
    }}>
      <div style={{
        width: 72,
        height: 72,
        borderRadius: '50%',
        background: colors.bg,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: spacing.xl,
      }}>
        <span style={{ fontSize: 36, lineHeight: 1 }}>{icon}</span>
      </div>
      <h3 style={{
        fontSize: fonts.xl,
        color: colors.text,
        fontWeight: 600,
        marginBottom: spacing.sm,
      }}>{heading}</h3>
      <p style={{
        fontSize: fonts.base,
        color: colors.textSecondary,
        maxWidth: 400,
        lineHeight: 1.6,
        marginBottom: ctaTo ? spacing.xl : 0,
      }}>{text}</p>
      {ctaTo && (
        <Link to={ctaTo} style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 6,
          padding: '10px 20px',
          background: colors.primary,
          color: 'white',
          borderRadius: radius.md,
          textDecoration: 'none',
          fontSize: fonts.base,
          fontWeight: 500,
          marginTop: spacing.sm,
        }}>
          {ctaLabel}
        </Link>
      )}
    </div>
  );
}
