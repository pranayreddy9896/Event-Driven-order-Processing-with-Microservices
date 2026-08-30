package com.system.inventory.model;

public enum ReservationStatus {
    RESERVED,
    RELEASED, // Released by compensating transaction
    CONFIRMED
}
