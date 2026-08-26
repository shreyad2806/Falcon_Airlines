import { colors, spacing, radius, fonts } from '../styles/theme';

/**
 * Horizontal step indicator for multi-step flows.
 * Steps: [{ number, label }]
 */
export default function StepIndicator({ steps, currentStep }) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 0,
      marginBottom: spacing.xxl,
      padding: `${spacing.lg}px 0`,
      overflowX: 'auto',
    }}>
      {steps.map((step, idx) => {
        const isActive = idx === currentStep;
        const isCompleted = idx < currentStep;
        const isLast = idx === steps.length - 1;

        return (
          <div key={idx} style={{
            display: 'flex',
            alignItems: 'center',
            flex: isLast ? 'none' : 1,
          }}>
            {/* Step circle + label */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: spacing.sm,
              whiteSpace: 'nowrap',
            }}>
              <div style={{
                width: 28,
                height: 28,
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: fonts.sm,
                fontWeight: 600,
                background: isActive || isCompleted ? colors.primary : colors.borderLight,
                color: isActive || isCompleted ? 'white' : colors.textMuted,
                border: `2px solid ${isActive ? colors.primary : isCompleted ? colors.primary : colors.border}`,
                flexShrink: 0,
              }}>
                {isCompleted ? '✓' : step.number}
              </div>
              <span style={{
                fontSize: fonts.base,
                fontWeight: isActive ? 600 : 400,
                color: isActive ? colors.text : isCompleted ? colors.primary : colors.textMuted,
              }}>{step.label}</span>
            </div>

            {/* Connector line */}
            {!isLast && (
              <div style={{
                flex: 1,
                height: 2,
                background: isCompleted ? colors.primary : colors.border,
                margin: `0 ${spacing.md}px`,
                minWidth: 20,
              }} />
            )}
          </div>
        );
      })}
    </div>
  );
}
