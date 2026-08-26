/**
 * Format amount in Indian Rupees.
 * e.g. 42850 → "₹42,850"
 */
export function formatINR(amount) {
  if (amount == null || isNaN(amount)) return '₹0';
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  return '₹' + num.toLocaleString('en-IN', { maximumFractionDigits: 0 });
}

/**
 * Format amount with decimals in INR.
 * e.g. 42850.50 → "₹42,850.50"
 */
export function formatINRDecimal(amount) {
  if (amount == null || isNaN(amount)) return '₹0.00';
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  return '₹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/**
 * Format ISO instant to display date.
 */
export function formatDate(s) {
  if (!s) return '—';
  return new Date(s).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

/**
 * Format ISO instant to display time.
 */
export function formatTime(s) {
  if (!s) return '--:--';
  return new Date(s).toLocaleTimeString('en-IN', {
    hour: '2-digit', minute: '2-digit', hour12: true,
  });
}

/**
 * Format ISO instant to short display (e.g. "15 Aug, 1:30 PM").
 */
export function formatDateTime(s) {
  if (!s) return '—';
  return new Date(s).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit', hour12: true,
  });
}

/**
 * Calculate duration between two ISO instants.
 * e.g. "7h 30m"
 */
export function calculateDuration(departure, arrival) {
  if (!departure || !arrival) return '—';
  const diff = new Date(arrival) - new Date(departure);
  const hours = Math.floor(diff / 3600000);
  const mins = Math.floor((diff % 3600000) / 60000);
  return `${hours}h ${mins}m`;
}

/**
 * Calculate passenger age from date of birth.
 */
export function calculateAge(dob) {
  if (!dob) return null;
  const birth = new Date(dob);
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--;
  }
  return age;
}

/**
 * Get passenger type based on age.
 */
export function getPassengerType(dob) {
  const age = calculateAge(dob);
  if (age === null) return 'Adult';
  if (age < 2) return 'Infant';
  if (age < 12) return 'Child';
  return 'Adult';
}
