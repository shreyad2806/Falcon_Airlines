package com.falcon.airlines.enums;

/**
 * Ticket lifecycle states.
 * 
 * Valid transitions according to business rules:
 * - ACTIVE → CANCELLED
 * - ACTIVE → REFUNDED
 * - ACTIVE → USED
 * - REFUNDED → CANCELLED
 * 
 * Invalid transitions:
 * - CANCELLED → any other state (terminal state)
 * - USED → any other state (terminal state)
 * - REFUNDED → ACTIVE (cannot reactivate refunded ticket)
 * - REFUNDED → USED (cannot use refunded ticket)
 */
public enum TicketStatus {
    /**
     * Ticket is active and valid for travel.
     * This is the initial state when a booking is confirmed.
     */
    ACTIVE,
    
    /**
     * Ticket has been cancelled.
     * This is a terminal state - no further transitions allowed.
     */
    CANCELLED,
    
    /**
     * Ticket has been refunded.
     * Can transition to CANCELLED but not back to ACTIVE or USED.
     */
    REFUNDED,
    
    /**
     * Ticket has been used for travel.
     * This is a terminal state - no further transitions allowed.
     */
    USED,
    
    /**
     * Legacy state for backward compatibility.
     * Maps to ACTIVE for new implementations.
     * @deprecated Use ACTIVE instead
     */
    @Deprecated
    ISSUED,
    
    /**
     * Legacy state for backward compatibility.
     * Maps to CANCELLED for new implementations.
     * @deprecated Use CANCELLED instead
     */
    @Deprecated
    VOID
}
