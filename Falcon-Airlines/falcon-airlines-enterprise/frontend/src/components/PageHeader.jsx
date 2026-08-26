import { spacing, fonts, colors } from '../styles/theme';

/**
 * Consistent page header with title and optional description.
 */
export default function PageHeader({ title, description, children }) {
  return (
    <div style={{
      marginBottom: spacing.xxl,
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'flex-start',
      flexWrap: 'wrap',
      gap: spacing.md,
    }}>
      <div>
        <h1 style={{
          fontSize: fonts.xxl,
          color: colors.text,
          fontWeight: 700,
          lineHeight: 1.3,
          margin: 0,
        }}>{title}</h1>
        {description && (
          <p style={{
            fontSize: fonts.base,
            color: colors.textSecondary,
            marginTop: spacing.xs,
            lineHeight: 1.5,
          }}>{description}</p>
        )}
      </div>
      {children && (
        <div style={{ display: 'flex', gap: spacing.sm, flexWrap: 'wrap' }}>{children}</div>
      )}
    </div>
  );
}
