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

Atomically generate `PO-000001` style numbering, persist the Draft and line items,
and record an activity log. No inventory transaction or material quantity update occurs.
Draft may be approved or cancelled by an active Admin; Approved may only be cancelled
when it has no Stock In reference. PDF generation uses company configuration and the
Supplier, items, total, and status.

Purchase Order PDF flow loads the real Purchase Order, Supplier profile, and `config/company`;
it renders a temporary file in `cacheDir/pdf/` and previews or shares it through FileProvider.
Missing required CompanyConfig blocks generation. A remote-only logo is omitted safely, no terms
section is rendered because no Purchase Order terms field exists, and approved cancellation stays
blocked until Stock In contains a verifiable Purchase Order reference.

Orders is a unified tabbed destination: Quotations remains its existing tab, Purchase Orders is
the Admin-only tab, Invoices opens the implemented Invoice list, and Delivery Challans remains in
its existing availability state. A Draft update discovers stale line IDs before the transaction,
then atomically synchronizes the parent, item writes/deletions, and activity entry.

---

# Workflow 10 - Create Invoice

Actor

Admin for Invoice financial mutations; an Employee may only view permitted Invoice information
and use permitted PDF actions.

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

Atomically generate `INV-000001`-style numbering, persist a Draft and its line items, and write
an `INVOICE_CREATED` activity. Drafts may be edited, issued, or cancelled. Issued invoices accept
positive payments up to their outstanding amount; status becomes Unpaid, Partially Paid, or Paid
from the cumulative paid amount. Cancellation is blocked after any payment. Invoice creation,
issue, cancellation, and payment do not modify Inventory.

PDF generation loads the real Invoice, Customer, and `config/company`, creates a paginated cache
file, and previews or shares it through the existing FileProvider. Required company identity
fields are mandatory; remote-only logos are omitted safely and quotation terms are not reused.

---

# Workflow 11 - Create Delivery Challan

Actor

Admin

Trigger

Orders

↓

Delivery Challan

Independent Create

Validate active Customer, delivery address/date, and at least one positive-quantity line. In one
transaction increment `counters/deliveryChallan`, generate `DC-000001`, write the Draft parent and
stable item IDs, and write `DELIVERY_CHALLAN_CREATED`. This operation does not change Inventory.

Invoice Conversion

The source Invoice must be `ISSUED`. Copy the Customer reference and only material ID, description,
quantity, and unit from selected Invoice lines; preserve the Invoice ID and number, enforce each
requested quantity against its matching source line, and reject another active Challan for that
Invoice. The transaction writes the counter, Draft parent, lines, and activity without changing the
Invoice or Inventory. Quotation conversion is unsupported.

Draft Edit

Read stale item IDs before the transaction, then reread the parent and require `DRAFT`. Atomically
write retained/new items, delete stale items, preserve number/source/creation audit fields, and add
`DELIVERY_CHALLAN_UPDATED`. The external stale-ID read is a documented concurrency limitation.

Dispatch

Admin confirms Dispatch → transaction rereads the Draft parent and persisted items → verifies no
existing `STOCK_OUT` record references the Challan → validates active material stock → reduces each
material-linked inventory quantity and writes its Stock Out record → marks the Challan
`DISPATCHED` and writes `DELIVERY_CHALLAN_DISPATCHED`. Free-text lines have no stock movement. Any
failure aborts the whole transaction; no negative quantity or duplicate deduction is allowed.

Cancellation

Admin may cancel only a Draft. The transaction changes status to `CANCELLED`, sets cancellation and
update audit fields, and writes `DELIVERY_CHALLAN_CANCELLED`. It does not mutate Inventory.
Dispatched or already cancelled Challans cannot be cancelled, and stock reversal is not implemented.

PDF

The details action generates a paginated, non-financial PDF in app cache from the real Challan,
Customer, and company configuration. It repeats item headers on later pages and is previewed/shared
only through the existing FileProvider `content://` URI.

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
