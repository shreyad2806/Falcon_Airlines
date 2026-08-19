package com.falcon.airlines.enums;

/**
 * Boarding pass lifecycle states.
 * 
 * Valid transitions according to business rules:
 * - GENERATED → CHECKED_IN
 * - GENERATED → VOID
 * - CHECKED_IN → BOARDING
 * - CHECKED_IN → VOID
 * - BOARDING → USED
 * - BOARDING → VOID
 * 
 * Invalid transitions:
 * - VOID → any other state (terminal state)
 * - USED → any other state (terminal state)
 * - CHECKED_IN → GENERATED (cannot go back)
 * - BOARDING → CHECKED_IN (cannot go back)
 */
public enum BoardingPassStatus {
    /**
     * Boarding pass has been generated but passenger has not checked in yet.
     * This is the initial state when a boarding pass is created.
     */
    GENERATED,
    
    /**
     * Passenger has checked in for the flight.
     */
    CHECKED_IN,
    
    /**
     * Passenger is in the boarding process.
     */
    BOARDING,
    
    /**
     * Boarding pass has been used for boarding.
     * This is a terminal state - no further transitions allowed.
     */
    USED,
    
    /**
     * Boarding pass has been voided (e.g., flight cancelled, passenger rebooked).
     * This is a terminal state - no further transitions allowed.
     */
    VOID
}
