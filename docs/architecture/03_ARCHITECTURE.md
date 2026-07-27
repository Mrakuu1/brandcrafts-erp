# BrandCrafts ERP (MVP)

# 03_ARCHITECTURE.md

Version: 1.0

---

# Purpose

This document defines the complete software architecture for the BrandCrafts ERP Android application.

Every feature, screen, repository, ViewModel and data flow must follow this document.

No feature should introduce a new architecture pattern.

---

# Technology Stack

## Language

Kotlin

---

## UI Framework

Jetpack Compose

Material Design 3

---

## Minimum SDK

Android 9 (API 28)

---

## Architecture Pattern

MVVM

Repository Pattern

Single Activity Architecture

Navigation Compose

StateFlow

Hilt Dependency Injection

Firebase Backend

---

# High Level Architecture

                UI (Compose)
                     │
                     ▼
              ViewModel Layer
                     │
                     ▼
             Repository Layer
                     │
                     ▼
        Firebase Services Layer
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
 Cloud Firestore        Firebase Storage
         │
         ▼
 Firebase Authentication

Business logic must never exist inside composables.

---

# Application Structure

app/

core/

data/

domain/

feature/

navigation/

ui/

utils/

---

# Package Structure

com.brandcrafts.erp

│

├── core

│   ├── common

│   ├── constants

│   ├── extensions

│   ├── result

│   └── validation

│

├── data

│   ├── datasource

│   ├── firebase

│   ├── model

│   ├── repository

│   └── mapper

│

├── domain

│   ├── model

│   ├── repository

│   └── usecase

│

├── feature

│   ├── auth

│   ├── dashboard

│   ├── inventory

│   ├── contacts

│   ├── documents

│   ├── employee

│   └── settings

│

├── navigation

│

├── ui

│   ├── component

│   ├── theme

│   └── preview

│

└── utils

---

# Feature Structure

Each feature follows exactly the same structure.

Example

feature/

inventory/

│

├── InventoryScreen.kt

├── InventoryViewModel.kt

├── InventoryUiState.kt

├── InventoryEvent.kt

├── InventoryAction.kt

├── InventoryRepository.kt

├── InventoryRoute.kt

└── component/

Every feature must follow this structure.

---

# UI Layer

Responsibilities

Display UI

Collect StateFlow

Send User Events

Navigation

Animations

Nothing else.

Composable functions must never:

Access Firestore

Call Firebase APIs

Contain business logic

Modify data directly

---

# ViewModel Layer

Responsibilities

Receive UI events

Perform validation

Call Repository

Manage UI state

Handle loading

Handle errors

Expose immutable StateFlow

Every screen has exactly one ViewModel.

---

# Repository Layer

Responsibilities

Communicate with Firebase

Transform data

Handle exceptions

Return Result objects

Repositories are the only layer allowed to access Firebase.

---

# Firebase Layer

Responsibilities

Authentication

Firestore

Storage

Crashlytics

Analytics

No Compose dependency.

---

# State Management

Every screen exposes

StateFlow<UiState>

Example

InventoryUiState

contains

Loading

Data

Error

Empty

Selection

Search Query

Filter

No mutable state inside composables.

---

# UI Events

Every screen has

UiEvent

Example

InventoryEvent

SearchChanged

FilterChanged

StockIn

StockOut

Refresh

ItemClicked

---

# User Actions

Actions emitted from UI.

Example

InventoryAction

OpenDetails

Delete

Edit

Share

Navigate

Actions are one-time events.

---

# Navigation

Navigation Compose

Single Activity

No Fragments.

Navigation Graph

Splash

↓

Login

↓

Home

↓

Inventory

↓

Orders

↓

Contacts

↓

Employee Management

↓

Settings

Orders retains one shared NavHost and contains its existing document tabs. The Invoice tab routes
to the Invoice list; list actions route to Create Invoice, Edit Draft Invoice, and Invoice Details
using only the persisted invoice ID. Form success opens Invoice Details, Details back returns to
the list, and form back returns to its previous destination. No Invoice route accepts a role,
customer type, payment state, or other authorization value as a navigation argument.

The Delivery Challan tab routes to its list in the same NavHost. The list opens independent create,
Draft edit, and details using only persisted Challan IDs. Form success replaces the form with Details;
Details back returns to the prior destination. A parameterized Invoice-conversion destination accepts
only a persisted Invoice ID and resolves all source data internally. The current Orders list has no
Invoice ID context, so its create-from-Invoice action intentionally uses localized unavailable
feedback until Invoice Details supplies that ID. No Delivery Challan route accepts a role, status,
source type, or authorization value.

---

# Dependency Injection

Use Hilt.

Inject

Repositories

Firebase Services

Preferences

Utilities

Never manually instantiate dependencies.

---

# Data Flow

User

↓

Composable

↓

ViewModel

↓

Repository

↓

Firestore

↓

Repository

↓

ViewModel

↓

StateFlow

↓

Composable

One directional only.

---

# Error Handling

Repositories return

Success

Error

Loading

ViewModel converts Result into UiState.

UI only renders UiState.

---

# Coroutines

Use

viewModelScope

Dispatchers.IO

StateFlow

collectAsStateWithLifecycle()

Avoid GlobalScope.

Avoid runBlocking.

---

# Compose Guidelines

Use Material Design 3.

Stateless composables wherever possible.

State hoisting is mandatory.

Reusable composables before screen-specific composables.

Prefer:

Arrangement.spacedBy()

contentPadding

PaddingValues

instead of multiple Spacer composables.

Avoid deeply nested layouts.

Maximum nesting:

Column

↓

Card

↓

Column

Avoid nesting more than necessary.

---

# Component Reuse

Every common UI must become a reusable component.

Examples

AppButton

AppTextField

AppTopBar

AppCard

AppSearchBar

AppLoading

AppEmptyState

StatusChip

ConfirmationDialog

BottomSheetScaffold

Duplicate UI is not allowed.

---

# Validation

Validation belongs inside ViewModel.

Never inside composables.

Validation classes belong inside

core/validation

---

# Logging

Use Timber.

Never Log.d()

Never printStackTrace()

---

# Constants

Store inside

core/constants

No hardcoded strings.

---

# Resources

Strings

colors

icons

dimensions

must be centralized.

---

# Naming Convention

Screen

InventoryScreen

ViewModel

InventoryViewModel

UiState

InventoryUiState

Repository

InventoryRepository

Model

Inventory

Component

InventoryCard

---

# Coding Principles

SOLID

DRY

KISS

Single Responsibility

Immutable State

Composition over Inheritance

---

# Performance

LazyColumn

LazyVerticalGrid

remember()

derivedStateOf()

Stable data models

Avoid unnecessary recomposition.

---

# Security

Never trust client-side validation.

Every sensitive operation must also be validated through Firebase Security Rules.

---

# Testing Strategy

Unit Tests

Repositories

ViewModels

Validation

UI Tests

Critical Screens

Navigation

Authentication

Inventory

---

# Architecture Rules

Mandatory

✓ One ViewModel per screen

✓ One Repository per feature

✓ Stateless composables

✓ StateFlow

✓ Hilt

✓ Material 3

✓ Navigation Compose

✓ Firebase only through Repository

✓ No business logic inside UI

✓ No duplicated components

✓ Consistent package structure

✓ Reusable architecture

Violation of these rules requires explicit architectural approval.
