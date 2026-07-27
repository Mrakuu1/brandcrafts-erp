# BrandCrafts ERP (MVP)

# 12_FIREBASE_SECURITY_RULES.md

Version: 1.0

---

# Purpose

This document defines the Firebase Security Rules, Cloud Firestore access policies, Firebase Storage rules, and indexing strategy for the BrandCrafts ERP application.

Security must always be enforced at Firebase.

UI restrictions are for user experience only.

Firestore Security Rules are the source of truth.

---

# Security Principles

- Every request must be authenticated.
- Every authenticated user must have an active account.
- Every request is validated against the user's role.
- Employees cannot elevate privileges.
- Only Admins may perform destructive operations.
- Client applications must never bypass Firebase Security Rules.

---

# User Roles

Supported roles

ADMIN

EMPLOYEE

Stored in

/users/{uid}

Fields

role

active

firstLogin

---

# Helper Functions

Required helper functions

isSignedIn()

isActiveUser()

isAdmin()

currentUser()

These helper functions should be reused throughout the rules file.

---

# Firestore Collections

/users

/inventory

/stockTransactions

/customers

/suppliers

/quotations

/purchaseOrders

/invoices

/delivery_challans

/activityLogs

/settings

---

# Users Collection

Read

Authenticated active users

Create

Admin only

Update

Admin only

Delete

Admin only

Employees cannot

- Create users
- Change roles
- Activate users
- Deactivate users
- Delete users

---

# Inventory Collection

Read

Admin

Employee

Create

Admin

Update

Admin

Delete

Admin only

Restricted Fields

purchasePrice

visible only to Admin in UI.

Security rules protect modification, while UI hides the field.

---

# Stock Transactions

Read

Admin

Employee

Create

Admin

Employee

Update

No updates after creation

Delete

Admin only

Stock history must remain immutable.

---

# Customers

Read

Admin

Employee

Create

Admin

Employee

Update

Admin

Employee

Delete

Admin only

---

# Suppliers

Read

Admin

Employee

Create

Admin

Employee

Update

Admin

Employee

Delete

Admin only

---

# Quotations

Read

Admin

Employee

Create

Admin

Employee

Update

Admin

Employee

Delete

Admin only

---

# Purchase Orders

Read

Admin

Create

Admin

Update

Admin

Delete

Admin

Employees cannot access Purchase Orders.

---

# Invoices

Read

Admin

Employee

Create

Admin

Employee

Update

Admin

Delete

Admin

Employees cannot modify issued invoices.

---

# Delivery Challans

Read

Admin

Employee

Create

Admin

Update

Admin

Delete

Never

Delivery Challan parent and `items` writes must be restricted to an authenticated active Admin.
Rules must reject changes to immutable number, source, and creation-audit fields after creation;
only Draft parents may be edited. Dispatch is a coordinated privileged transaction that changes the
parent, Inventory, Stock Out records, and activity log together. Security rules must not allow an
Employee to invoke those writes directly. Activity and Stock Out records remain append-only.

---

# Activity Logs

Read

Admin

Employee

Create

System generated only

Update

Never

Delete

Never

Activity history must remain immutable.

---

# Settings

Read

Admin

Create

Admin

Update

Admin

Delete

Never

Employees cannot access settings.

---

# Firebase Storage

Folders

quotations/

invoices/

purchaseOrders/

delivery_challans/

company/

profileImages/

---

# Storage Permissions

Quotations

Read

Authenticated users

Write

Admin

Employee

Invoices

Read

Authenticated users

Write

Admin

Employee

Purchase Orders

Read

Admin

Write

Admin

Company Logo

Read

Authenticated users

Write

Admin

Profile Images

Read

Authenticated users

Write

Current authenticated user only

---

# Firestore Validation

Every document must contain

createdAt

updatedAt

createdBy

updatedBy

Never allow clients to remove required fields.

---

# Immutable Fields

Document ID

createdAt

createdBy

must never be modified.

---

# Server Timestamp

Always use

FieldValue.serverTimestamp()

Never trust device time.

---

# Authentication Rules

No anonymous authentication.

Email + Password only.

Only Admin creates user accounts.

---

# Deactivated Users

If

active == false

Immediately deny

Firestore

Storage

Realtime listeners

Application signs out user.

---

# Security Against Privilege Escalation

Employees cannot

Change role

Create Admin

Modify another user

Deactivate users

Modify settings

Delete business records

Even if requests are sent manually.

---

# Composite Indexes

Create indexes for

Inventory

category

name

Customers

company

name

Orders

status

createdAt

Invoices

status

createdAt

Activity Logs

createdAt

module

---

# Offline Behavior

Firestore Offline Persistence enabled.

Security rules apply when synchronization occurs.

Offline cache never bypasses server-side validation.

---

# Audit Strategy

Every successful write

↓

Activity Log

↓

User

↓

Timestamp

↓

Module

↓

Action

---

# Security Testing Checklist

Verify

- Employee cannot delete inventory.
- Employee cannot delete customer.
- Employee cannot access Purchase Orders.
- Employee cannot edit settings.
- Employee cannot change user roles.
- Inactive users are immediately blocked.
- Unauthenticated users cannot access Firestore.
- PDF files respect Storage Rules.
- Profile image upload restricted to owner.
- Activity Logs cannot be modified.

Every release must pass this checklist before deployment.

---

# Future Enhancements

Potential future additions

- Custom Claims for Admin role.
- Firebase App Check.
- Multi-company (tenant) isolation.
- Fine-grained department permissions.
- Role hierarchy (Manager, Supervisor, Operator).
- Audit export and compliance reporting.

Current MVP supports only

ADMIN

EMPLOYEE

as defined in the RBAC specification.
