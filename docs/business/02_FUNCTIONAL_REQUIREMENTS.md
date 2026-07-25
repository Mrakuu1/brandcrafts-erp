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
- Create Delivery Challans
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

Fields

- Supplier
- Date
- Items
- Quantity
- Unit Price
- Total

Functions

- Create
- View
- Share PDF

Administrator Only

---

# Billing Module

Purpose

Generate customer invoices.

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

Pending

Partial

Paid

Functions

- Create
- Edit
- Share PDF

Employees cannot modify prices.

---

# Delivery Challan

Purpose

Generate delivery documents.

Fields

- Customer
- Invoice Reference
- Delivery Date
- Vehicle Number
- Driver Name
- Items
- Notes

Functions

- Create
- View
- Share PDF

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
