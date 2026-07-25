# BrandCrafts ERP (MVP)

# 05_RBAC.md

Version: 1.0

---

# Purpose

This document defines the Role-Based Access Control (RBAC) strategy for the BrandCrafts ERP application.

RBAC controls:

- Screen visibility
- Navigation visibility
- Menu visibility
- CRUD permissions
- Firebase authorization
- UI rendering
- Session management

This document is the single source of truth for all authorization decisions.

---

# Supported Roles

The application supports two roles.

ADMIN

EMPLOYEE

No other roles exist in the MVP.

---

# Authentication Flow

Launch App

↓

Check FirebaseAuth.currentUser

↓

Authenticated?

YES

↓

Load Firestore user document

/users/{uid}

↓

Is account active?

YES

↓

Store user in SessionManager

↓

Navigate to Dashboard

NO

↓

Sign Out

↓

Navigate to Login

---

# User Session

A logged-in user contains:

uid

name

email

phone

role

active

firstLogin

The session is cached using DataStore.

---

# First Login

When

firstLogin == true

Display mandatory password change screen.

User cannot access the application until password is updated.

After success

firstLogin = false

---

# Profile Menu

Visible to all users.

Contains

My Profile

Change Password

Logout

Admin additionally sees

Employee Management

Business Settings

---

# Bottom Navigation

Both roles use the same navigation structure.

Home

Stock

Orders

Contacts

Navigation must never change based on role.

Only screen content changes.

---

# Dashboard Permissions

## Administrator

Visible

Revenue

Outstanding Payments

Low Stock

Recent Activities

Quick Actions

Add Employee

Create Invoice

Create Quotation

Stock In

---

## Employee

Visible

Low Stock

Recent Activities

Assigned Tasks

Quick Actions

Stock In

Stock Out

Material Usage

Hidden

Revenue

Profit

Receivables

Business Metrics

---

# Inventory Permissions

## Admin

View Materials

Create Material

Edit Material

Delete Material

View Purchase Price

View Selling Price

Stock In

Stock Out

Material Usage

---

## Employee

View Materials

Stock In

Stock Out

Material Usage

Hidden

Delete

Purchase Price

Profit Margin

---

# Customer Permissions

Admin

Create

View

Edit

Delete

Employee

Create

View

Edit

Delete Hidden

---

# Supplier Permissions

Admin

Create

View

Edit

Delete

Employee

View

Supplier actions are view-only. Create, edit, and delete actions are hidden in the UI and
must be rejected outside the UI boundary.

---

# Quotation Permissions

Quotation creation and editing, including unit price, discount percentage, tax percentage,
and financial totals, are Administrator-only. Employees may view quotation lists and details
only; they cannot access quotation create/edit routes or trigger quotation writes.

Admin

Create

View

Edit

Delete

Share PDF

Employee

Create

View

Edit

Share PDF

Delete Hidden

---

# Purchase Order Permissions

Admin

Create

View

Edit

Delete

Share PDF

Employee

Hidden

Purchase Orders are not visible to employees.

---

# Billing Permissions

Admin

Create

View

Edit

Delete

Apply Discount

Modify Prices

Share PDF

Employee

Create

View

Share PDF

Locked

Price

Discount

Delete

---

# Delivery Challan Permissions

Admin

Create

View

Edit

Delete

Share PDF

Employee

Create

View

Share PDF

Delete Hidden

---

# Employee Management

Visible only to Administrator.

Functions

View Employees

Add Employee

Edit Employee

Deactivate Employee

Activate Employee

Change Role

Reset Password

Employees never see this module.

---

# Business Settings

Visible only to Administrator.

Contains

Company Information

Invoice Prefix

Quotation Prefix

PO Prefix

Delivery Challan Prefix

Logo

Theme

Employees cannot access Settings.

---

# Delete Policy

Only Administrator may delete.

Employees never see delete actions.

Delete actions require confirmation.

Business records should preferably be soft deleted.

---

# UI Visibility Policy

Restricted UI must be hidden.

Never show disabled buttons.

Correct

Employee

Stock In

Material Usage

View Inventory

Wrong

Delete (Disabled)

Settings (Disabled)

Admin Only (Disabled)

Hide completely.

---

# Permission Matrix

| Module | Action | Admin | Employee |
|---------|--------|:-----:|:--------:|
| Dashboard | View Financial Cards | ✅ | ❌ |
| Dashboard | View Activity Feed | ✅ | ✅ |
| Inventory | View Materials | ✅ | ✅ |
| Inventory | Add Material | ✅ | ✅ |
| Inventory | Edit Material | ✅ | ❌ |
| Inventory | Delete Material | ✅ | ❌ |
| Inventory | Stock In | ✅ | ✅ |
| Inventory | Stock Out | ✅ | ✅ |
| Inventory | Material Usage | ✅ | ✅ |
| Customers | Create | ✅ | ✅ |
| Customers | Edit | ✅ | ✅ |
| Customers | Delete | ✅ | ❌ |
| Suppliers | Create | ✅ | ❌ |
| Suppliers | Edit | ✅ | ❌ |
| Suppliers | Delete | ✅ | ❌ |
| Quotations | Create | ✅ | ✅ |
| Quotations | Edit | ✅ | ✅ |
| Quotations | Delete | ✅ | ❌ |
| Quotations | Share PDF | ✅ | ✅ |
| Purchase Orders | View | ✅ | ❌ |
| Purchase Orders | Create | ✅ | ❌ |
| Purchase Orders | Edit | ✅ | ❌ |
| Purchase Orders | Delete | ✅ | ❌ |
| Billing | Create Invoice | ✅ | ✅ |
| Billing | Edit Prices | ✅ | ❌ |
| Billing | Discount | ✅ | ❌ |
| Billing | Delete Invoice | ✅ | ❌ |
| Billing | Share Invoice | ✅ | ✅ |
| Delivery Challan | Create | ✅ | ✅ |
| Delivery Challan | Share | ✅ | ✅ |
| Employee Management | Full Access | ✅ | ❌ |
| Settings | Full Access | ✅ | ❌ |

---

# Firestore Mapping

Role values

ADMIN

EMPLOYEE

Account Status

active = true

active = false

Every request must validate

Authenticated

Account Active

Role

UI restrictions alone are not security.

Firestore Security Rules are mandatory.

---

# Session Management

User signs in

↓

Load Firestore profile

↓

Cache locally

↓

Observe Firestore user document

↓

If

active == false

Immediately

Sign Out

Navigate Login

Clear Local Cache

---

# Navigation Policy

Single Activity

Single NavHost

Single Bottom Navigation

Single App Theme

Role differences are implemented only through

Visibility

Permissions

Data filtering

Never maintain separate Admin and Employee navigation graphs.

---

# UX Guidelines

Hide restricted actions.

Do not disable them.

Show meaningful empty states.

Never expose unauthorized data.

Avoid role-specific layouts.

Maintain a consistent user experience across roles.

---

# Security Principles

Client-side RBAC improves UX.

Server-side Firestore Security Rules enforce security.

Every sensitive operation must be validated by Firestore rules.

Client checks must never be trusted as the only protection.
