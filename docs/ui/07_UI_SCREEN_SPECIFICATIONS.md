# BrandCrafts ERP (MVP)

# 07_UI_SCREEN_SPECIFICATIONS.md

Version: 1.0

---

# Purpose

This document defines the UI specification for every screen in the BrandCrafts ERP application.

It includes

- Screen purpose
- Role visibility
- Layout hierarchy
- Components
- User actions
- Navigation
- Loading state
- Empty state
- Error state

Every screen implementation must follow this specification.

---

# Global Screen Structure

Every screen follows the same hierarchy.

Scaffold

├── Top App Bar

├── Screen Content

├── Floating Action Button (Optional)

├── Modal Bottom Sheet (Optional)

└── Bottom Navigation

---

# 1 Login Screen

Visible To

Everyone

Purpose

Authenticate users.

Layout

Logo

↓

Welcome Text

↓

Email Field

↓

Password Field

↓

Forgot Password

↓

Login Button

↓

Version

Components

AppLogo

AppTextField

PrimaryButton

LoadingButton

Actions

Login

Forgot Password

Loading

Disable login button

Show progress indicator

Error

Snackbar

---

# 2 Dashboard Screen

Visible To

Admin

Employee

Purpose

Business overview.

Layout

Top App Bar

↓

Summary Cards

↓

Quick Actions

↓

Recent Activity

↓

Bottom Navigation

Components

AppTopBar

AppStatCard

QuickActionGrid

ActivityCard

FAB

None

Admin Dashboard

Revenue

Outstanding

Low Stock

Recent Activities

Employee Dashboard

Low Stock

Recent Activities

Assigned Tasks

Quick Actions

---

# 3 Inventory Screen

Visible To

Admin

Employee

Purpose

Manage inventory.

Layout

Search

↓

Filter Chips

↓

Inventory List

↓

FAB

Components

SearchBar

FilterChipRow

MaterialCard

Extended FAB

Actions

Search

Filter

Open Material

Stock In

Stock Out

Material Usage

Admin Only

Edit Material

Delete Material

Loading

LoadingView

Empty

No Materials

Error

Retry

Bottom Sheet

UniversalFormSheet

---

# 4 Material Details Screen

Visible To

Admin

Employee

Purpose

View material details.

Layout

Material Header

↓

Information Card

↓

Recent Transactions

↓

Action Buttons

Admin

Edit

Delete

Employee

Stock In

Stock Out

Material Usage

---

# 5 Contacts Screen

Visible To

Admin

Employee

Purpose

Customers & Suppliers.

Layout

Segmented Button

↓

Search

↓

Contact List

↓

FAB

Tabs

Customers

Suppliers

Card

Name

Phone

Company

Outstanding

Actions

Call

WhatsApp

View

Edit

Delete (Admin)

---

# 6 Contact Details Screen

Purpose

Display customer/supplier details.

Sections

Information

Documents

Recent Transactions

Actions

Call

WhatsApp

Create Quote

Create Invoice

---

# 7 Orders Screen

Purpose

Unified documents.

Tabs

Quotation

Invoice

Purchase Order

Delivery Challan

Layout

Search

↓

Filter

↓

Document Cards

↓

FAB

Actions

Create

Share

Edit

Delete (Admin)

Open Details

---

# 8 Document Details Screen

Purpose

View document.

Sections

Header

Customer

Items

Totals

Status

Actions

Share PDF

Print

Edit

Delete (Admin)

Convert

Quotation → Invoice

Delivery Challan details are non-financial: they show Challan number, status, Customer, delivery
address/date, optional source Invoice, vehicle/driver, descriptions, quantities, units, notes, and
available audit information. Prices, tax, discounts, totals, and payments are never rendered.
Draft-only Edit, Dispatch, and Cancel are displayed only for an authorized Admin. PDF preview/share
uses the existing secure FileProvider flow and reports recoverable errors through the screen's
snackbar.

---

# 9 Employee Management

Visible

Admin

Purpose

Manage application users.

Layout

Employee List

↓

FAB

Employee Card

Avatar

Name

Role

Phone

Status

Actions

View

Edit

Deactivate

Reset Password

FAB

Add Employee

Bottom Sheet

UniversalFormSheet

---

# 10 Settings

Visible

Admin

Sections

Business Information

Document Prefix

Theme

About

Logout

Employee

Only

Profile

Change Password

Logout

---

# 11 Profile Bottom Sheet

Visible

Everyone

Contents

Avatar

Name

Email

Role

Admin

Employee Management

Settings

Logout

Employee

Change Password

Logout

---

# Universal Form Sheet

Single reusable bottom sheet.

Used For

Stock In

Stock Out

Material Usage

Customer

Supplier

Employee

Quotation

Invoice

Purchase Order

Delivery Challan

Behavior

Dynamic title

Dynamic fields

Primary button

Validation

Cancel

---

# Universal Search

Supported Screens

Inventory

Contacts

Orders

Employees

Behavior

Real-time search

Case insensitive

Debounce input

---

# Floating Action Button Rules

Dashboard

No FAB

Inventory

Add Material (Admin)

Stock Action (Employee)

Contacts

Add Contact

Orders

New Document

Employee

Add Employee

Settings

None

---

# Loading State

Every screen

Loading indicator

Disable actions

Prevent duplicate requests

---

# Empty State

Every list screen

Icon

Title

Description

Primary Action

---

# Error State

Every screen

Friendly message

Retry button

Snackbar

---

# Pull To Refresh

Supported

Dashboard

Inventory

Contacts

Orders

Employee List

---

# Navigation Flow

Splash

↓

Login

↓

Dashboard

↓

Inventory

↓

Material Details

↓

Orders

↓

Document Details

↓

Contacts

↓

Contact Details

↓

Employee

↓

Settings

---

# Screen Naming Convention

LoginScreen

DashboardScreen

InventoryScreen

MaterialDetailScreen

ContactsScreen

ContactDetailScreen

OrdersScreen

DocumentDetailScreen

EmployeeManagementScreen

SettingsScreen

---

# Preview Requirement

Every screen must include

Compose Preview

Sample Data

Light Theme

Dark Theme (future)

No screen should be merged until its preview matches the approved UI specification.
