# BrandCrafts ERP (MVP)

# 04_FIRESTORE_SCHEMA.md

Version: 1.0

---

# Purpose

This document defines the Cloud Firestore database structure for the BrandCrafts ERP application.

Every collection, document, field, naming convention, relationship, and data validation rule must follow this specification.

This document is the single source of truth for all Firestore implementation.

---

# Database Design Principles

The database must be:

- Simple
- Scalable
- Normalized
- Easy to maintain
- Cost efficient
- Firebase friendly

Avoid duplicate data wherever possible.

---

# Collections

The MVP contains the following collections.

users

materials

stock_transactions

contacts

documents

settings

activity_logs

counters

---

# Collection: users

Document ID

Firebase Authentication UID

Example

users/{uid}

Fields

uid : String

name : String

email : String

phone : String

role : ADMIN | EMPLOYEE

active : Boolean

firstLogin : Boolean

designation : String

profileImage : String

createdAt : Timestamp

updatedAt : Timestamp

createdBy : UID

updatedBy : UID

Purpose

Stores application users.

---

# Collection: materials

Document ID

Firestore Auto ID

Example

materials/{materialId}

Fields

id : String

name : String

sku : String

category : String

unit : String

availableQuantity : Double

minimumQuantity : Double

purchasePrice : Double

sellingPrice : Double

description : String

active : Boolean

createdAt : Timestamp

updatedAt : Timestamp

createdBy : UID

updatedBy : UID

Example Categories

Flex

Vinyl

Ink

Foam Board

PVC

Acrylic

Banner

Accessories

---

# Collection: stock_transactions

Purpose

Stores every inventory movement.

Never edit or delete records.

Document ID

Firestore Auto ID

Fields

id : String

materialId : String

transactionType :

STOCK_IN

STOCK_OUT

MATERIAL_USAGE

quantity : Double

unit : String

referenceId : String

referenceType :

PURCHASE

INVOICE

JOB

MANUAL

supplierId : String

remarks : String

performedBy : UID

createdAt : Timestamp

Purpose

Inventory history.

Stock reports.

Audit.

---

# Collection: contacts

Stores both customers and suppliers.

Document ID

Firestore Auto ID

Fields

id : String

type :

CUSTOMER

SUPPLIER

name : String

company : String

phone : String

email : String

address : String

gstNumber : String

city : String

state : String

pincode : String

notes : String

active : Boolean

createdAt : Timestamp

updatedAt : Timestamp

createdBy : UID

updatedBy : UID

Reason

Single collection reduces duplicate code.

Filter by

type

---

# Collection: documents

Quotation decimal fields (`subTotal`, `discount`, `tax`, `grandTotal`, and item quantity,
unitPrice, discount, tax, and total) are stored as canonical plain decimal strings for
QUOTATION documents. They are mapped to BigDecimal only in the data layer; never through Double.

Stores all business documents.

Supported Types

QUOTATION

PURCHASE_ORDER

INVOICE

DELIVERY_CHALLAN

Document ID

Firestore Auto ID

Fields

id : String

documentNumber : String

type : String

contactId : String

date : Timestamp

status : String

subtotal : Decimal String

discountTotal : Decimal String

taxableTotal : Decimal String

taxTotal : Decimal String

grandTotal : Decimal String

remarks : String

paymentStatus :

PENDING

PARTIAL

PAID

pdfUrl : String

createdBy : UID

updatedBy : UID

createdAt : Timestamp

updatedAt : Timestamp

---

Sub Collection

documents/{documentId}/items

Fields

itemId : String

materialId : String

description : String

quantity : Decimal String

unit : String

unitPrice : Decimal String

discountPercent : Decimal String

taxPercent : Decimal String

lineSubtotal : Decimal String

lineDiscount : Decimal String

taxableAmount : Decimal String

lineTax : Decimal String

lineTotal : Decimal String

sortOrder : Integer

---

## Invoice document rules

Invoices use `invoices/{invoiceId}` and `invoices/{invoiceId}/items/{itemId}`. They are financial
documents for an active Customer contact and never mutate Inventory in this phase. Invoice parent
documents contain `id`, `invoiceNumber`, `customerId`, required `invoiceDate : Timestamp`, optional
`dueDate : Timestamp`, `status : DRAFT | ISSUED | CANCELLED`, canonical plain-decimal `subtotal`,
`taxTotal`, `discountTotal`, `grandTotal`, and `paidAmount`, plus `paymentStatus : UNPAID |
PARTIALLY_PAID | PAID`, optional `remarks`, and the audit fields `createdAt`, `createdBy`,
`updatedAt`, `updatedBy`, `issuedAt`, `issuedBy`, `cancelledAt`, and `cancelledBy`.

`paidAmount` starts at `0`, must be non-negative, and must not exceed `grandTotal`.
`outstandingAmount` and `isOverdue` are never stored: outstanding is `grandTotal - paidAmount`;
overdue is a presentation condition only when an ISSUED invoice has a due date before the current
date and a positive outstanding amount. Payment status is derived from the two authoritative
amounts. New invoice numbers are generated atomically from `counters/invoice` using `INV-000001`
format. Due dates may not precede invoice dates.

Invoice items preserve stable `itemId` values and store `materialId`, `description`, `quantity`,
`unit`, `unitPrice`, `discountPercent`, `taxPercent`, `lineSubtotal`, `lineDiscount`,
`taxableAmount`, `lineTax`, `lineTotal`, and integer `sortOrder`. All decimal values are canonical
plain decimal strings. Invoice calculations follow the existing Quotation policy: quantity times
unit price, percentage discount, taxable amount, percentage tax, then line total. Monetary values
are rounded to two decimal places using HALF_UP; parent totals are sums of the rounded line values.

Allowed document transitions are `DRAFT -> ISSUED`, `DRAFT -> CANCELLED`, and
`ISSUED -> CANCELLED` only where `paidAmount == 0`. Only Draft invoices may be edited. Payments
may be recorded only against Issued invoices and do not alter Inventory.

Invoice list observation listens to parent documents only, ordered by `invoiceDate` descending;
it does not create item-subcollection listeners. Complete retrieval reads the parent and its
`items` subcollection ordered by `sortOrder`. Snapshot listeners are removed when their Flow
collector is cancelled. Malformed parent or item data fails the read through typed errors rather
than producing fabricated invoices.

---

Invoice write infrastructure reserves 25 writes below Firestore's 500-write limit, giving a safe
limit of 475. Create is `itemCount + 3` (counter, parent, activity) and therefore permits at most
472 lines. Draft update is `submittedItemCount + staleDeleteCount + 2`; Issue, Cancellation, and
Payment are two writes each (parent plus activity). Requests that exceed the safe limit fail before
any mutation. The missing `counters/invoice` document represents zero issued numbers, so the first
transaction formats `INV-000001`.

Create atomically reads and increments the counter, verifies the generated parent document does not
already exist, writes the parent and every item, and writes `INVOICE_CREATED`. Draft update performs
a pre-read to discover stale item IDs, then atomically rechecks that the parent remains Draft,
synchronizes parent and retained/new items, deletes stale items, and writes `INVOICE_UPDATED`.
The stale-ID pre-read is a documented concurrency limitation; the parent status and all writes are
still revalidated together in the transaction. Issue, cancellation, and payment each atomically
update the parent and write their corresponding activity.

Invoice IDs, item IDs, and activity IDs are generated before transaction execution and reused for
every transaction retry. The transaction derives the invoice number from the counter. New and
updated writes require a validated, authenticated, active Admin actor; caller-supplied actor IDs
are never trusted. Create, Draft update, and Issue validate that the referenced Contact exists,
is active, and has type `CUSTOMER`; they never create a Customer implicitly.

## Purchase Order document rules

Purchase Orders use `documents/{purchaseOrderId}` with `type : PURCHASE_ORDER` and
`documents/{purchaseOrderId}/items/{itemId}`. A Purchase Order parent document contains:

- `supplierId : String`
- `date : Timestamp` (required; legacy epoch values are temporarily readable)
- `expectedDeliveryDate : Timestamp` (optional; legacy epoch values are temporarily readable)
- `supplierReferenceNumber : String` (optional)
- `remarks : String` (optional)
- `status : DRAFT | APPROVED | CANCELLED`
- `total : Decimal String`
- `approvedAt : Timestamp` and `approvedBy : UID` when approved
- `cancelledAt : Timestamp` and `cancelledBy : UID` when cancelled
- the common document audit fields.

Purchase Order items contain only `itemId`, `materialId`, `description`, `quantity`,
`unit`, `unitPrice`, `lineTotal`, and `sortOrder`. Decimal values are canonical plain
decimal strings. Purchase Orders do not store discount or tax values in this MVP.
Purchase Order writes use a 475-write safe transaction limit (25 writes reserved below
Firestore's 500-write limit). Create uses `itemCount + 3`; update uses submitted-item
writes plus stale-item deletions plus parent and activity writes. Excessive requests fail
before mutation; atomicity review remains deferred.
Create commits the counter increment, parent, item documents, and activity log in one
transaction after reading the counter. Update discovers item IDs before its transaction,
then transactionally revalidates the Draft parent status and commits the parent, item
writes, stale-item deletions, and activity log together. This pre-read means the stale
set is not independently revalidated in the transaction. Approval and Draft cancellation
each atomically commit their parent update and activity log. Approved cancellation remains
blocked until Stock In stores a verifiable Purchase Order reference.

---

# Collection: settings

Single document

settings/business

Fields

companyName

address

phone

email

gstNumber

website

invoicePrefix

quotationPrefix

purchaseOrderPrefix

deliveryChallanPrefix

currency

logoUrl

updatedAt

updatedBy

---

# Collection: activity_logs

Stores application history.

Fields

id

module

action

referenceId

referenceType

description

performedBy

performedByName

createdAt

Example

Inventory Updated

Customer Added

Invoice Generated

Employee Created

Quotation Shared

---

# Collection: counters

Purpose

Generate running document numbers.

Documents

quotation

invoice

purchaseOrder

deliveryChallan

Example

counters/invoice

Fields

nextNumber : 1025

prefix : INV

Result

INV-0001025

---

# Relationships

users

↓

documents.createdBy

↓

stock_transactions.performedBy

↓

activity_logs.performedBy

--------------------------------------------------

contacts

↓

documents.contactId

--------------------------------------------------

materials

↓

stock_transactions.materialId

↓

documents/items/materialId

---

# Naming Convention

Collections

lowercase

Plural

Examples

users

materials

documents

Fields

camelCase

Examples

createdAt

updatedBy

availableQuantity

minimumQuantity

Never use spaces.

---

# Timestamps

Every document must include

createdAt

updatedAt

Use Firebase Server Timestamp.

Never use device time.

---

# Audit Fields

Every editable document must include

createdBy

updatedBy

createdAt

updatedAt

Never remove these fields.

---

# Soft Delete Policy

Do not delete business records.

Instead

active = false

Applies to

Users

Materials

Contacts

Settings

Documents

Stock Transactions

Never delete.

Immutable history.

---

# Document Number Format

Quotation

QT-000001

Invoice

INV-000001

Purchase Order

PO-000001

Delivery Challan

DC-000001

Generated using

counters

collection.

---

# Dashboard Calculations

Dashboard values are calculated using

documents

stock_transactions

Examples

Today's Sales

↓

INVOICE

↓

paymentStatus

↓

grandTotal

Low Stock

↓

materials

↓

availableQuantity

↓

minimumQuantity

Recent Activities

↓

activity_logs

---

# Firebase Storage

Folder Structure

storage/

logos/

documents/

invoices/

quotations/

purchase_orders/

delivery_challans/

profile_images/

---

# Required Firestore Indexes

documents

type

createdAt

documents

contactId

date

materials

category

name

contacts

type

name

stock_transactions

materialId

createdAt

activity_logs

createdAt

module

---

# Security Rules Mapping

users

Admin CRUD

Employee Read Self

materials

Admin CRUD

Employee Read

stock_transactions

Admin Create

Employee Create

No Update

No Delete

contacts

Admin CRUD

Employee Read/Create/Edit

documents

Admin CRUD

Employee Create/View

settings

Admin Only

activity_logs

System Generated

Read Only

---

# Data Validation Rules

Quantity > 0

Price >= 0

GST Number Optional

Phone must be unique

Email should be unique

SKU should be unique

Document Number unique

Material Name required

Customer Name required

Supplier Name required

---

## Delivery Challan document rules

Delivery Challans use `delivery_challans/{deliveryChallanId}` and
`delivery_challans/{deliveryChallanId}/items/{itemId}`. The counter is `counters/deliveryChallan`;
numbers are generated atomically as `DC-000001`. A parent contains `id`, `dcNumber`, `customerId`,
`deliveryAddress`, required `date : Timestamp`, `sourceType : INDEPENDENT | INVOICE`, nullable
`sourceInvoiceId` and `sourceInvoiceNumber`, optional `vehicleNumber`, `driverName`, and `notes`,
`status : DRAFT | DISPATCHED | CANCELLED`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, and
the nullable dispatch/cancellation audit pairs `dispatchedAt`/`dispatchedBy` and
`cancelledAt`/`cancelledBy`. Audit timestamps use `FieldValue.serverTimestamp()`; the delivery date
is the user-selected business date and is stored as a Firestore `Timestamp`.

Item documents contain stable `itemId` (also the item-document ID), optional `materialId`,
`description`, positive decimal-string `quantity`, `unit`, and integer `sortOrder`. They contain no
prices, discounts, tax, totals, payments, or other financial fields. A material-less free-text line
is permitted only when it has a description and unit. `sortOrder` is recalculated sequentially when
a Draft is edited; it is never used as the persistent line identity.

Only a Draft may be edited. The only status transitions are `DRAFT -> DISPATCHED` and
`DRAFT -> CANCELLED`; both terminal states are immutable. Independent creation stores no source
Invoice reference. Invoice conversion accepts only an `ISSUED` Invoice, copies only the Customer
reference plus material ID, description, quantity, and unit, preserves the immutable Invoice ID and
number, limits requested quantity to the matching Invoice line, and refuses a second active
(`DRAFT` or `DISPATCHED`) Delivery Challan for the same Invoice. It does not modify the source
Invoice. Quotation conversion is unsupported.

Create and Draft edit never mutate Inventory. Dispatch is the only Inventory-changing operation. In
one Firestore transaction it rereads the Draft parent and persisted lines, checks for an existing
`stock_transactions` document with `referenceType = DELIVERY_CHALLAN` and the Challan's
`referenceId`, validates active material stock, decreases each material quantity, creates the Stock
Out records, changes the parent to `DISPATCHED`, and writes the dispatch activity. Free-text lines
do not create a stock movement. This parent-status recheck and reference check prevent duplicate
stock deduction. Stock reversal after dispatch is not implemented.

Draft updates discover stale item IDs with an external pre-read before the update transaction. The
transaction re-reads the parent and requires `DRAFT`, then atomically writes the parent, retained or
new lines, stale-line deletions, and update activity. This is the documented stale-line-discovery
limitation; it does not permit source or creation-audit changes.

attendance

payroll

production_jobs

machines

notifications

branches

roles

permissions

reports

These collections are intentionally excluded from the MVP.
