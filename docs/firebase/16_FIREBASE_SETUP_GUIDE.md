# BrandCrafts ERP (MVP)

# 16_FIREBASE_SETUP_GUIDE.md

Version: 1.0

---

# Purpose

This guide explains how to configure Firebase for the BrandCrafts ERP Android application.

Every environment (Development, Testing, Production) must follow this guide.

---

# Firebase Services Used

The MVP uses the following Firebase products.

Required

- Firebase Authentication
- Cloud Firestore
- Firebase Storage

Optional (Future)

- Firebase Cloud Messaging
- Crashlytics
- Analytics
- App Check
- Remote Config

---

# Firebase Project

Project Name

BrandCrafts ERP

Project ID

brandcrafts-erp

Android Package

com.brandcrafts.erp

Minimum SDK

Android 8.0 (API 26)

Target SDK

Latest Stable

---

# Android Registration

Register Android App

Package Name

com.brandcrafts.erp

Download

google-services.json

Place file inside

app/

---

# SHA Certificates

Generate

Debug SHA-1

Release SHA-1

Debug SHA-256

Release SHA-256

Add all certificates to Firebase Console.

---

# Gradle Plugins

Project

Google Services

Android Application

Kotlin Android

Kotlin KAPT

Hilt

---

# Required Dependencies

Authentication

Firestore

Storage

Hilt

Navigation Compose

Material Design 3

Coil

Timber

DataStore

Coroutines

Lifecycle Compose

Do not add unnecessary libraries.

---

# Firebase Authentication

Enable

Email / Password

Disable

Anonymous

Phone

Google

Facebook

GitHub

Apple

Email verification is optional for MVP.

---

# Authentication Flow

Admin

Creates Employee

↓

Employee Receives Credentials

↓

Employee Logs In

↓

Must Change Password

↓

Access Dashboard

---

# Firestore Database

Mode

Production Mode

Region

Nearest production region

Enable

Offline Persistence

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

# Storage

Folders

company/

profileImages/

quotation/

invoice/

purchaseOrder/

deliveryChallan/

Only PDFs and images should be stored.

---

# Firestore Indexes

Create composite indexes for

Inventory

- category
- materialName

Customers

- company
- name

Suppliers

- company
- name

Invoices

- status
- createdAt

Quotations

- status
- createdAt

Purchase Orders

- status
- createdAt

Delivery Challans

- status
- createdAt

Activity Logs

- createdAt
- module

---

# Firestore Rules

Deploy

12_FIREBASE_SECURITY_RULES.md

No development shortcuts.

Never use

allow read, write: if true;

---

# Firebase Storage Rules

Protect

Company Logo

Invoices

Quotations

Purchase Orders

Delivery Challans

Profile Images

Use role-based access.

---

# Default Admin Account

Created manually.

Example

Email

admin@brandcrafts.com

Password

Change immediately after first login.

The application should not hardcode credentials.

---

# User Document

/users/{uid}

Required fields

uid

name

email

phone

role

active

firstLogin

createdAt

updatedAt

---

# Security Checklist

Verify

Authentication enabled

Firestore rules deployed

Storage rules deployed

Indexes created

Admin account created

Employee login tested

Inactive account blocked

Delete permissions verified

---

# Offline Support

Enable Firestore offline persistence.

No custom offline database for MVP.

Sync automatically when network becomes available.

---

# Backup Strategy

Export Firestore weekly.

Keep Storage backups.

Never delete production data directly.

---

# Development Environment

Firebase Project

brandcrafts-erp-dev

Used by developers.

---

# Production Environment

Firebase Project

brandcrafts-erp-prod

Used by clients.

Never connect development builds to production accidentally.

---

# Build Variants

debug

Uses Development Firebase

release

Uses Production Firebase

---

# Logging

Development

Enable Timber DebugTree

Production

Disable debug logs

No sensitive information in logs.

---

# PDF Storage

Generated PDFs

↓

Temporary Cache

↓

Share Intent

↓

Optional Upload to Firebase Storage

↓

Store download URL in Firestore

Never expose local file paths.

---

# Deployment Checklist

Before first release

- Firebase project created
- Android app registered
- google-services.json added
- SHA fingerprints configured
- Authentication enabled
- Firestore created
- Storage enabled
- Security Rules deployed
- Storage Rules deployed
- Indexes created
- Admin account created
- Test employee created
- Login verified
- CRUD operations verified
- PDF generation verified
- File upload verified
- Offline mode tested

Only after every checklist item passes should production deployment begin.

---

# Future Enhancements

- Firebase Cloud Messaging
- Crashlytics
- Analytics
- App Check
- Remote Config
- Cloud Functions
- Scheduled Firestore backups
- Multi-company support
- Custom Claims