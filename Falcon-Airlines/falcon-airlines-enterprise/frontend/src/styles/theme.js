/**
 * Falcon Airlines Design System
 * Modern airline/travel SaaS dashboard tokens
 */

export const colors = {
  // Primary palette
  primary: '#16324F',
  primaryHover: '#1e4068',
  primaryLight: '#1e4a7a',

  // Accent / success
  success: '#1F9D67',
  successHover: '#1a8a5a',
  successLight: '#e6f9f0',

  // Semantic
  danger: '#DC3545',
  dangerHover: '#c82333',
  dangerLight: '#fdecea',
  warning: '#F59E0B',
  warningLight: '#FEF3C7',
  info: '#3B82F6',
  infoLight: '#EFF6FF',

  // Neutrals
  white: '#FFFFFF',
  bg: '#F7F8FA',
  bgCard: '#FFFFFF',
  border: '#E5E7EB',
  borderLight: '#F3F4F6',
  borderFocus: '#16324F',

  // Text
  text: '#1F2937',
  textSecondary: '#6B7280',
  textMuted: '#9CA3AF',
  textLight: '#D1D5DB',

  // Status colors
  statusScheduled: '#16324F',
  statusScheduledBg: '#E8EDF2',
  statusActive: '#1F9D67',
  statusActiveBg: '#E6F9F0',
  statusDelayed: '#F59E0B',
  statusDelayedBg: '#FEF3C7',
  statusCancelled: '#DC3545',
  statusCancelledBg: '#FDECEA',
  statusCompleted: '#6B7280',
  statusCompletedBg: '#F3F4F6',
};

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  xxxl: 32,
  page: 32,
};

export const radius = {
  sm: 6,
  md: 8,
  lg: 12,
  xl: 16,
  full: 9999,
};

export const fonts = {
  xs: 11,
  sm: 12,
  md: 13,
  base: 14,
  lg: 16,
  xl: 18,
  xxl: 20,
  hero: 28,
  display: 32,
};

export const shadows = {
  sm: '0 1px 2px rgba(0,0,0,0.04)',
  md: '0 2px 8px rgba(0,0,0,0.06)',
  lg: '0 4px 16px rgba(0,0,0,0.08)',
  xl: '0 8px 24px rgba(0,0,0,0.12)',
  cardHover: '0 4px 20px rgba(22,50,79,0.10)',
};

/** Button style factories */
export const btnBase = {
  border: 'none',
  borderRadius: radius.md,
  cursor: 'pointer',
  fontSize: fonts.base,
  fontWeight: 500,
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  transition: 'all 0.15s ease',
  lineHeight: 1.4,
  whiteSpace: 'nowrap',
};

export const buttons = {
  primary: {
    ...btnBase,
    background: colors.primary,
    color: 'white',
    padding: '10px 20px',
    height: 40,
  },
  success: {
    ...btnBase,
    background: colors.success,
    color: 'white',
    padding: '10px 20px',
    height: 40,
  },
  danger: {
    ...btnBase,
    background: colors.danger,
    color: 'white',
    padding: '10px 20px',
    height: 40,
  },
  secondary: {
    ...btnBase,
    background: colors.white,
    color: colors.text,
    border: `1px solid ${colors.border}`,
    padding: '10px 20px',
    height: 40,
  },
  ghost: {
    ...btnBase,
    background: 'transparent',
    color: colors.textSecondary,
    padding: '8px 12px',
    height: 36,
  },
  icon: {
    ...btnBase,
    background: 'transparent',
    color: colors.textSecondary,
    padding: 8,
    height: 36,
    width: 36,
  },
  sm: {
    padding: '6px 12px',
    fontSize: fonts.sm,
    height: 32,
  },
};

/** Common input style */
export const input = {
  padding: '10px 14px',
  borderRadius: radius.md,
  border: `1px solid ${colors.border}`,
  fontSize: fonts.base,
  width: '100%',
  outline: 'none',
  transition: 'border-color 0.15s, box-shadow 0.15s',
  background: colors.white,
  color: colors.text,
  height: 40,
  boxSizing: 'border-box',
};

/** Card base style */
export const card = {
  background: colors.bgCard,
  borderRadius: radius.lg,
  border: `1px solid ${colors.border}`,
  padding: spacing.xl,
};

/** Badge base */
export const badge = {
  display: 'inline-flex',
  alignItems: 'center',
  padding: '3px 10px',
  borderRadius: radius.full,
  fontSize: fonts.sm,
  fontWeight: 600,
  lineHeight: 1.4,
  whiteSpace: 'nowrap',
};

/** Status color map */
export const statusColors = {
  SCHEDULED: { bg: colors.statusScheduledBg, text: colors.statusScheduled },
  ON_TIME: { bg: colors.statusActiveBg, text: colors.statusActive },
  ACTIVE: { bg: colors.statusActiveBg, text: colors.statusActive },
  CONFIRMED: { bg: colors.statusActiveBg, text: colors.statusActive },
  GENERATED: { bg: colors.warningLight, text: '#92600A' },
  ISSUED: { bg: colors.statusActiveBg, text: colors.statusActive },
  CHECKED_IN: { bg: colors.infoLight, text: colors.info },
  BOARDED: { bg: colors.statusScheduledBg, text: colors.statusScheduled },
  DELAYED: { bg: colors.statusDelayedBg, text: colors.statusDelayed },
  CANCELLED: { bg: colors.statusCancelledBg, text: colors.statusCancelled },
  VOID: { bg: colors.statusCancelledBg, text: colors.statusCancelled },
  REFUNDED: { bg: colors.statusCancelledBg, text: colors.statusCancelled },
  COMPLETED: { bg: colors.statusCompletedBg, text: colors.statusCompleted },
  BOARDING: { bg: '#EDE9FE', text: '#7C3AED' },
};
