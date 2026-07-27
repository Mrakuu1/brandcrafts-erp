# BrandCrafts ERP (MVP)

# Functional Requirements

Version: 1.0

---

# Purpose

This document defines all business requirements, workflows, validations, and user interactions for the BrandCrafts ERP Android application.

This document is the primary business reference for development.

No feature should be implemented unless it is defined here or added through a future revision.

---

# User Roles

The application supports two user roles.

## Administrator

Administrator has complete access to the application.

Responsibilities include:

- Manage Inventory
- Manage Customers
- Manage Suppliers
- Manage Employees
- Generate Business Documents
- Configure Application Settings
- View Dashboard Metrics

---

## Employee

Employees perform day-to-day operational activities.

Responsibilities include:

- View Inventory
- Record Stock In
- Record Stock Out
- Record Material Usage
- Create Quotations
- Generate Bills
- View Delivery Challans and use permitted PDF actions
- View Customers
- View Suppliers

Employees cannot access administrative modules.

---

# Authentication Module

## Login

Users authenticate using:

- Email Address
- Password

Requirements

- Validate email format
- Password is mandatory
- Display loading indicator
- Show meaningful error messages
- Remember authenticated session
- Support Forgot Password
- Force logout when account becomes inactive

---

## Forgot Password

Requirements

- User enters registered email
- Password reset email sent using Firebase Authentication
- Display confirmation message

---

## Logout

Requirements

- Clear local session
- Sign out from Firebase
- Navigate to Login Screen
- Clear navigation history

---

# Dashboard Module

Purpose

Provide a quick overview of business operations.

---

## Administrator Dashboard

Displays:

- Total Sales
- Outstanding Payments
- Low Stock Count
- Recent Activities
- Quick Actions

Quick Actions

- Add Stock
- Create Invoice
- Create Quotation
- Add Employee

---

## Employee Dashboard

Displays:

- Assigned Tasks
- Recent Activities
- Low Stock Alerts
- Quick Actions

Quick Actions

- Stock In
- Stock Out
- Material Usage

Employee dashboard must not display financial information.

---

# Inventory Module

Purpose

Maintain master list of materials.

Examples

- Flex
- Vinyl
- Ink
- Acrylic Sheet
- Foam Board
- PVC
- Banner Roll

Each material contains

- Material Name
- SKU
- Category
- Unit
- Available Quantity
- Minimum Quantity
- Notes

Administrator

Can

- Add Material
- Edit Material
- Delete Material

Employee

Can

- View Material
- Search Material

---

# Stock In

Purpose

Increase available inventory.

Input Fields

- Material
- Quantity
- Supplier
- Remarks

Validation

- Quantity > 0
- Material Required

System Actions

- Increase inventory quantity
- Create transaction record
- Update activity log

---

# Stock Out

Purpose

Decrease inventory manually.

Input Fields

- Material
- Quantity
- Reason
- Remarks

Validation

- Quantity > 0
- Quantity cannot exceed available stock

System Actions

- Decrease inventory
- Save transaction
- Update activity log

---

# Material Usage

Purpose

Deduct inventory against production work.

Input Fields

- Material
- Quantity
- Job Reference
- Remarks

Validation

- Quantity must be available
- Material required

System Actions

- Reduce inventory
- Save usage history

---

# Customer Management

Fields

- Name
- Company
- Mobile Number
- Email
- Address
- GST Number
- Notes

Administrator

CRUD

Employee

Create

View

Edit

Delete not allowed

Search required

---

# Supplier Management

Fields

- Supplier Name
- Company
- Phone
- Email
- Address
- GST Number

Administrator

CRUD

Employee

View Only

---

# Quotation Module

## Approved Calculation and Access Rules

Discount and tax are percentage values. Every monetary, percentage, and quantity value uses
decimal arithmetic. For each line, calculate subtotal, then discount, then taxable amount,
then tax; round each calculated monetary value to scale 2 with HALF_UP. Document totals are
the sums of rounded line values. New quotations are DRAFT; only DRAFT quotations may be edited.
Quotation creation and financial editing are Administrator-only.

Quotation line items persist percentage inputs and calculated decimal-string values using
`discountPercent`, `taxPercent`, `lineSubtotal`, `lineDiscount`, `taxableAmount`, `lineTax`,
`lineTotal`, and integer `sortOrder`. Legacy `discount`, `tax`, and `total` item fields are
obsolete for new quotation documents.

Purpose

Generate customer quotations.

Fields

- Customer
- Date
- Valid Until
- Items
- Quantity
- Unit Price
- Discount
- Tax
- Total
- Notes

Functions

- Create
- Edit
- View
- Share PDF

Status

Draft

Approved

Rejected

Expired

---

# Purchase Order Module

Purpose

Request or place orders with suppliers.

Administrator-only purchasing document issued to an active Supplier. It uses existing
Inventory materials but never changes inventory quantity; Stock In remains the only
receipt workflow that increases stock.

Fields: Supplier, date, optional expected delivery date, optional supplier reference
number, items, quantity, unit price, line total, total, optional remarks, and status.

Statuses are DRAFT, APPROVED, and CANCELLED. Allowed transitions are DRAFT to APPROVED,
DRAFT to CANCELLED, and APPROVED to CANCELLED. Only DRAFT orders are editable. Purchase
Orders do not support discount, tax, or GST calculations in this MVP.

Functions: Create, edit Draft, view details, approve, cancel, generate/share PDF.

Implementation note: Orders uses the documented unified tabbed container. Quotations remain one
tab and Purchase Orders are an Admin-only tab. Purchase Order details and forms display the
authoritative grand total only; subtotal, tax, and discount are not represented in this MVP
presentation contract.

Purchase Order PDFs are generated in `cacheDir/pdf/` and previewed/shared through FileProvider.
Required CompanyConfig identity fields must be present. A configured remote-only logo is omitted
when no supported local bitmap resolver exists, and `quotationTerms` is not reused because
Purchase Order terms are not configured.

Draft updates preserve retained line IDs and discover stale line IDs with an external pre-read
before the update transaction; the transaction then synchronizes the parent, retained/new lines,
stale-line deletions, and activity entry together.

---

# Billing Module

Purpose

Generate customer invoices, record cumulative payments, and produce a secure PDF from real
Invoice, Customer, and company configuration data.

Invoices are Customer-only financial documents and do not change Inventory. They use Draft,
Issued, and Cancelled document states. Payment state is derived from stored `paidAmount` and
`grandTotal` as Unpaid, Partially Paid, or Paid; outstanding amount and overdue state are derived
and are not persisted. Due date is optional and cannot precede the required invoice date.

Fields

- Customer
- Invoice Date
- Invoice Number
- Items
- Quantity
- Price
- Discount
- Tax
- Grand Total
- Payment Status

Payment Status

Unpaid

Partially Paid

Paid

Functions

- Create a Draft invoice
- Edit a Draft invoice
- Issue a Draft invoice
- Cancel an unpaid Draft or Issued invoice
- Record an Issued-invoice payment without exceeding the outstanding amount
- Preview or share the generated Invoice PDF

Invoice numbering is generated atomically as `INV-000001` from the Invoice counter. Monetary
values use `BigDecimal` and plain-decimal persistence; the line calculation applies percentage
discount before percentage tax, and totals use two-decimal HALF_UP rounding. `paidAmount` is
authoritative; outstanding and overdue values are derived rather than stored. Creating, editing,
issuing, cancelling, and recording payment never mutate Inventory.

Current authoritative financial mutations are Admin-only. Employees can view available Invoice
information and use PDF actions where permitted, but cannot create, edit, issue, cancel, record
payments, change prices, or apply discounts.

---

# Delivery Challan

Purpose

Generate non-financial delivery documents for a Customer.

Fields

- Customer and delivery address
- Required delivery date
- Optional Invoice reference
- Optional vehicle number and driver name
- Item description, positive quantity, unit, and optional material reference
- Notes
- Status and audit information

Functions

- Admin-only independent create, Invoice conversion, Draft edit, Dispatch, and Draft cancellation
- View permitted Delivery Challans
- Preview or share a generated PDF where permitted

Delivery Challans are non-financial documents: they never contain prices, discounts, tax, totals,
payments, or payment status. They may be created independently or from an Issued Invoice. Invoice
conversion preserves immutable Invoice ID and number references, copies only the Customer and
permitted line snapshots, allows quantities no greater than the matching Invoice lines, and never
modifies the Invoice. A second active Challan for the same Invoice is rejected. Quotation conversion
is not implemented.

The only lifecycle transitions are `DRAFT -> DISPATCHED` and `DRAFT -> CANCELLED`. Only Drafts are
editable. Create and Draft edit never change Inventory. Dispatch is Admin-only and atomically
checks stock, creates the corresponding `STOCK_OUT` records for material-linked lines, reduces
Inventory, updates the Challan status, and records activity. Free-text lines do not affect Inventory.
There is no dispatched-stock reversal in this phase.

The PDF is non-financial and contains company identity, Challan number/status/date, Customer and
delivery address, Invoice reference when present, vehicle/driver details, item descriptions,
quantities, units, notes, dispatched-by text when applicable, received-by/signature blanks,
authorized-signature area, generation date, and page numbers. It is generated in the app cache and
previewed/shared through a FileProvider `content://` URI; no storage permission or filesystem URI is
exposed.

Known limitations: the Invoice-to-Delivery-Challan route exists but the current Invoice Details flow
does not yet hand an Invoice ID to it, so the Orders list's create-from-Invoice action reports the
localized unavailable-feature message. There is no Quotation conversion, delivery reversal, or
remote-logo download for the PDF.

---

# Employee Management

Administrator Only

Functions

- View Employees
- Add Employee
- Edit Employee
- Activate Employee
- Deactivate Employee
- Change Role

Employee Fields

- Name
- Mobile
- Email
- Role
- Status

No HR functionality.

---

# Settings

Administrator Only

Functions

Business Profile

Company Name

Address

GST Number

Phone

Email

Invoice Prefix

Quotation Prefix

PO Prefix

Delivery Challan Prefix

Default Currency

Theme

Logout

---

# Search

The following modules must support searching.

Inventory

Customers

Suppliers

Documents

Employees

Search should be real-time.

---

# Filtering

Inventory

- All
- Low Stock
- Category

Orders

- Quotation
- Invoice
- Purchase Order
- Delivery Challan

Contacts

- Customers
- Suppliers

---

# Activity History

Every important action should generate a history entry.

Examples

- Material Added
- Stock Increased
- Stock Reduced
- Invoice Created
- Employee Added
- Customer Updated

Each activity stores

- User
- Date
- Action
- Module

---

# Notifications

The MVP supports only in-app notifications.

Examples

- Low Stock
- Employee Disabled
- Invoice Generated
- Quotation Shared

Push notifications are outside MVP scope.

---

# Error Handling

Every operation must handle

- No Internet
- Firebase Failure
- Validation Errors
- Unauthorized Access
- Session Expired

Display user-friendly messages.

---

# Loading States

Every screen must display loading states during data fetch.

Use Material Design 3 loading indicators.

No blank screens.

---

# Empty States

Every list screen must provide an empty state.

Examples

"No Customers Found"

"No Inventory Available"

"No Quotations Created"

Provide a primary action where applicable.

---

# Success Messages

Display confirmation after successful operations.

Examples

Material Added Successfully

Invoice Generated

Quotation Shared

Employee Created

Use Snackbar for transient messages.

---

# Future Enhancements

The following features are intentionally deferred.

- Attendance
- Payroll
- Barcode Printing
- Bluetooth Printing
- GST Filing
- WhatsApp API
- Analytics Dashboard
- Multi Branch
- Offline Sync Engine
- Production Planning
- AI Reports
