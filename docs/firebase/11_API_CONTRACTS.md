# BrandCrafts ERP (MVP)

# 11_API_CONTRACTS.md

Version: 1.0

---

# Purpose

This document defines the repository contracts, Firestore data operations, response handling, and service interfaces used throughout the BrandCrafts ERP application.

Firebase is the only backend.

No REST APIs are used.

Repositories are the only layer allowed to communicate with Firebase.

---

# Standard Repository Pattern

Every feature follows

UI

↓

ViewModel

↓

Repository Interface

↓

Repository Implementation

↓

Firebase

Never bypass the repository layer.

---

# Result Wrapper

All repository methods return

Result<T>

or

Flow<Result<T>>

Possible states

Loading

Success

Error

Never throw Firebase exceptions to the UI layer.

---

# Repository List

AuthenticationRepository

UserRepository

InventoryRepository

StockRepository

ContactRepository

QuotationRepository

PurchaseOrderRepository

InvoiceRepository

DeliveryChallanRepository

DashboardRepository

ActivityRepository

SettingsRepository

---

# AuthenticationRepository

Functions

login()

logout()

resetPassword()

getCurrentUser()

observeAuthState()

Responsibilities

Firebase Authentication only.

Never read Firestore directly except user lookup after login.

---

# UserRepository

Responsibilities

Employee Management

Admin Management

User Profile

Functions

createUser()

updateUser()

deleteUser()

activateUser()

deactivateUser()

changeRole()

getUser()

getAllUsers()

observeCurrentUser()

---

# InventoryRepository

Functions

createMaterial()

updateMaterial()

deleteMaterial()

getMaterial()

getMaterials()

searchMaterials()

observeMaterials()

---

# StockRepository

Functions

stockIn()

stockOut()

recordUsage()

getTransactions()

observeTransactions()

Every operation updates

Inventory

+

Stock Transaction

+

Activity Log

inside one Firestore transaction.

---

# ContactRepository

Functions

createCustomer()

updateCustomer()

deleteCustomer()

createSupplier()

updateSupplier()

deleteSupplier()

getCustomers()

getSuppliers()

searchContacts()

---

# QuotationRepository

Functions

createQuotation()

updateQuotation()

deleteQuotation()

getQuotation()

getQuotations()

generateQuotationPdf()

shareQuotation()

---

# PurchaseOrderRepository

Functions

createPurchaseOrder()

updatePurchaseOrder()

deletePurchaseOrder()

getPurchaseOrders()

generatePurchaseOrderPdf()

---

# InvoiceRepository

Functions

createInvoice()

updateInvoice()

deleteInvoice()

getInvoices()

generateInvoicePdf()

shareInvoice()

---

# DeliveryChallanRepository

Functions

createDeliveryChallan()

updateDeliveryChallan()

deleteDeliveryChallan()

getDeliveryChallans()

generateDeliveryChallanPdf()

shareDeliveryChallan()

---

# DashboardRepository

Functions

getDashboardSummary()

getLowStock()

getRecentActivities()

Admin

Revenue

Outstanding

Employees

Low Stock

Employee

Assigned Work

Recent Activity

Low Stock

---

# ActivityRepository

Functions

createActivity()

getActivities()

observeActivities()

Every successful write operation creates an activity log.

---

# SettingsRepository

Functions

getBusinessSettings()

updateBusinessSettings()

observeSettings()

Admin only.

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

/deliveryChallans

/activityLogs

/settings

---

# CRUD Rules

Every module follows

Create

Read

Update

Delete

Delete operations

Admin only.

Employees never permanently delete records.

---

# Firestore Transactions

Must be used for

Stock In

Stock Out

Material Usage

Invoice Generation

Purchase Orders

Multiple document updates

Never perform multiple related writes independently.

---

# Server Timestamp

Every document contains

createdAt

updatedAt

Updated using

FieldValue.serverTimestamp()

Never use device time.

---

# Document IDs

Use Firestore auto-generated IDs.

Store readable document numbers separately.

Examples

Quotation Number

QT-2026-0001

Invoice Number

INV-2026-0001

Purchase Order

PO-2026-0001

Delivery Challan

DC-2026-0001

---

# Search Strategy

Firestore fetch

↓

Client-side filtering

↓

Displayed List

Future

Firestore indexing

---

# Pagination

Initial MVP

Load latest documents.

Pagination can be added later.

---

# Offline Support

Use Firestore Offline Persistence.

Do not implement custom offline storage.

---

# Error Mapping

Repository converts Firebase exceptions into

ValidationError

PermissionError

NetworkError

UnknownError

UI never displays raw Firebase exceptions.

---

# Logging

Every successful repository operation

↓

Create Activity Log

↓

Return Success

---

# Dependency Injection

Repositories injected using Hilt.

Never manually instantiate repositories.

---

# Testing

Every repository should support

Fake Repository

Mock Repository

Unit Testing

No repository should directly depend on UI components.

---

# Future Expansion

## Secure Employee Management Callables

Employee account creation and profile updates use Firebase Callable Cloud Functions. Android
clients must never create Firebase Authentication users directly.

`createEmployee` accepts only `name`, `email`, `phone`, `role` (`ADMIN` or `EMPLOYEE`),
`active`, and a temporary password. It derives the caller UID and audit identity from the
authenticated callable context, verifies an active Admin `/users/{uid}` profile, validates
required data and uniqueness, creates the Auth user, creates the documented profile with
`firstLogin`, server timestamps, and audit fields, then creates an immutable activity log.
If the profile/audit transaction fails after Auth creation, the function deletes the new Auth
account. Its response contains only `uid`, `name`, `email`, `phone`, `role`, `active`, and
`firstLogin`.

`updateEmployee` accepts only `uid`, `name`, `email`, `phone`, `role`, and `active`. It
requires an active Admin, loads the target profile, rejects self-deactivation and
self-demotion, preserves immutable creation fields, validates uniqueness, updates the
Firebase Auth profile and documented Firestore fields, and writes an immutable audit event.
It attempts to restore a changed Auth email if the profile update fails.

Both functions use safe callable error codes: `unauthenticated`, `permission-denied`,
`invalid-argument`, `already-exists`, `not-found`, and `internal`. Android calls them only
through the Employee repository and maps errors before presentation. Deploy from
`functions/` with `firebase deploy --only functions`; local verification uses the Auth,
Firestore, and Functions emulators defined in `firebase.json`.

Repository interfaces should remain stable.

Changing implementations must not affect ViewModels.

New data sources (REST API, local database, etc.) should only require replacing the repository implementation, not the UI.
