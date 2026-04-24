package com.csms.csms.entity;

public enum UserRole {
    /** Full system access (legacy; prefer MANAGER + ACCOUNTANT for new deployments). */
    ADMIN,
    /** Shed manager: operations, flocks, inventory, user management, etc. */
    MANAGER,
    /** Accountant: financial transactions, purchases, sales, payroll, reports. */
    ACCOUNTANT
}