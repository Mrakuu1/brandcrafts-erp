# BrandCrafts ERP (MVP)

# 08_WORKFLOWS.md

Version: 1.0

---

# Purpose

This document defines the end-to-end business workflows of the BrandCrafts ERP application.

Every workflow describes:

- Trigger
- User Interaction
- Validation
- Firebase Operations
- Success Flow
- Error Handling

The application must follow these workflows consistently.

---

# Workflow 1 - User Login

Actor

Admin

Employee

Flow

Launch App

↓

Check Firebase Session

↓

Session Exists?

YES

↓

Load User Profile

↓

Account Active?

YES

↓

Dashboard

NO

↓

Logout

↓

Login Screen

NO SESSION

↓

Login Screen

Validation

- Email required
- Password required
- Account must be active

Success

Dashboard opens.

Failure

Display authentication error.

---

# Workflow 2 - Create Material

Actor

Admin

Trigger

Inventory FAB

↓

Universal Form Sheet

Fields

Material Name

SKU

Category

Unit

Purchase Price

Selling Price

Minimum Quantity

Notes

Validation

Name required

SKU unique

Minimum Quantity >= 0

Success

Create material document

Activity Log

Snackbar

---

# Workflow 3 - Stock In

Actor

Admin

Employee

Trigger

Inventory

↓

Material

↓

Stock In

Fields

Material

Quantity

Supplier

Remarks

Validation

Quantity > 0

Material exists

Transaction

Create

stock_transactions

Update

materials.availableQuantity

Insert

activity_logs

Success

Inventory refreshed

---

# Workflow 4 - Stock Out

Actor

Admin

Employee

Trigger

Inventory

↓

Material

↓

Stock Out

Validation

Quantity > 0

Available Quantity >= Requested Quantity

Transaction

Create

Stock Transaction

Update Material Quantity

Activity Log

Success

Updated inventory

---

# Workflow 5 - Material Usage

Actor

Admin

Employee

Trigger

Inventory

↓

Material Usage

Fields

Material

Quantity

Job Reference

Remarks

Validation

Quantity available

Transaction

Insert Stock Transaction

Update Inventory

Activity Log

---

# Workflow 6 - Create Customer

Actor

Admin

Employee

Fields

Name

Company

Phone

Email

Address

GST

Notes

Validation

Name required

Phone unique

Success

Customer created

---

# Workflow 7 - Create Supplier

Actor

Admin

Employee

Fields

Name

Company

Phone

Email

Address

GST

Validation

Name required

Phone unique

Success

Supplier created

---

# Workflow 8 - Create Quotation

Actor

Admin

Employee

Trigger

Orders

↓

New

↓

Quotation

Fields

Customer

Items

Quantity

Price

Discount

GST

Notes

System

Generate quotation number

Save document

Generate PDF

Share

Activity Log

---

# Workflow 9 - Create Purchase Order

Actor

Admin

Trigger

Orders

↓

Purchase Order

Fields

Supplier

Items

Quantity

Rate

Validation

Supplier required

Items required

Success

Generate PO

PDF

Activity Log

---

# Workflow 10 - Create Invoice

Actor

Admin

Employee

Trigger

Orders

↓

Invoice

Fields

Customer

Items

Quantity

Price

Discount

GST

Payment Status

Validation

Customer required

Minimum one item

Total calculated

Success

Generate Invoice

PDF

Activity Log

---

# Workflow 11 - Create Delivery Challan

Actor

Admin

Employee

Trigger

Orders

↓

Delivery Challan

Fields

Customer

Vehicle

Driver

Items

Notes

Validation

Customer required

Items required

Success

Generate PDF

Share

Activity Log

---

# Workflow 12 - Employee Management

Actor

Admin

Trigger

Profile

↓

Employee Management

Functions

Add Employee

Edit Employee

Deactivate

Activate

Reset Password

Change Role

Validation

Email unique

Phone unique

Role selected

System

Create Firebase Auth user

Create Firestore profile

Send password reset (optional)

Activity Log

---

# Workflow 13 - Share Document

Trigger

Document Details

↓

Share

System

Generate PDF

↓

Store in Firebase Storage

↓

Update Firestore

↓

Launch Android Share Sheet

Targets

WhatsApp

Email

Drive

Bluetooth

Nearby Share

---

# Workflow 14 - Search

Modules

Inventory

Orders

Contacts

Employees

Behavior

Realtime

Case insensitive

Local filtering after Firestore fetch

---

# Workflow 15 - Logout

Trigger

Profile

↓

Logout

System

Clear DataStore

Firebase SignOut

Clear Navigation

Navigate Login

---

# Activity Logging

Every workflow must create an activity log.

Fields

Module

Action

Reference Id

Performed By

Timestamp

Description

---

# Error Handling

Every workflow must handle

Network Failure

Firebase Exception

Validation Failure

Permission Denied

Unexpected Error

Show user-friendly messages.

---

# General Rules

- Use Firestore batched writes or transactions where multiple documents are updated.
- Never leave the database in a partially updated state.
- Every successful write must create an activity log.
- Every workflow must validate user permissions before execution.
- Long-running operations must display loading indicators.