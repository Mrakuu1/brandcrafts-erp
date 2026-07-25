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

# Future Collections

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
