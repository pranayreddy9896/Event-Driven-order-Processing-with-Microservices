package com.system.common.model;

public final class Topics {

    private Topics() {}

    public static final String ORDER_CREATED = "order.created";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_FAILED = "inventory.failed";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String NOTIFICATION_SENT = "notification.sent";

    // Dead Letter Topics (DLT)
    public static final String ORDER_CREATED_DLT = "order.created.DLT";
    public static final String INVENTORY_RESERVED_DLT = "inventory.reserved.DLT";
    public static final String PAYMENT_COMPLETED_DLT = "payment.completed.DLT";
}
