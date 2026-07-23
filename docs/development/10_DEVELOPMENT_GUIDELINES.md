# BrandCrafts ERP (MVP)

# 10_DEVELOPMENT_GUIDELINES.md

Version: 1.0

---

# Purpose

This document defines the coding standards, implementation rules, architecture constraints, and development workflow for the BrandCrafts ERP application.

Every developer and AI assistant must follow these guidelines.

If a conflict exists between this document and generated code, this document takes precedence.

---

# General Principles

Follow

- SOLID
- DRY
- KISS
- Clean Architecture
- MVVM
- Material Design 3

Prioritize

- Readability
- Maintainability
- Reusability
- Performance
- Simplicity

---

# AI Development Rules

Before implementing any feature

1. Read all relevant project documentation.
2. Do not make assumptions.
3. Follow the documented architecture.
4. Reuse existing components.
5. Do not create duplicate classes.
6. Complete one phase before starting another.
7. If documentation is unclear, stop and request clarification.

Never invent functionality that is not documented.

---

# Project Structure

Every feature must follow the standard project structure.

feature/

featureName/

Screen.kt

ViewModel.kt

UiState.kt

Event.kt

Action.kt

Repository.kt

Components/

Navigation.kt

---

# Kotlin Guidelines

Use

- Kotlin Coroutines
- StateFlow
- Immutable data classes
- Extension functions where appropriate

Avoid

- Global variables
- Singleton business objects
- Static mutable state
- Reflection

---

# Jetpack Compose Guidelines

Use

- Material Design 3
- Stateless composables
- State hoisting
- remember only for UI state
- collectAsStateWithLifecycle()

Avoid

- Business logic inside composables
- Direct Firebase access
- Mutable state spread across screens

---

# Layout Guidelines

Prefer

- Arrangement.spacedBy()
- contentPadding
- PaddingValues

Avoid excessive Spacer usage.

Do not create deeply nested layouts.

Maximum recommended nesting

Scaffold

↓

Column

↓

Card

↓

Column

---

# Navigation

Use

Navigation Compose

Single Activity

Single NavHost

Never create multiple navigation graphs unless documented.

---

# ViewModel Guidelines

Every screen has exactly one ViewModel.

Responsibilities

- Handle UI events
- Validate input
- Manage loading state
- Expose immutable UI state
- Call repositories

Do not perform navigation directly.

Navigation events should be emitted to the UI.

---

# Repository Guidelines

Repositories are the only layer allowed to communicate with Firebase.

Responsibilities

- Read Firestore
- Write Firestore
- Handle exceptions
- Return Result objects

Never expose Firebase SDK classes to the UI.

---

# Firestore Guidelines

Use

- Transactions where required
- Batched writes where appropriate
- Server timestamps
- Snapshot listeners only when necessary

Never duplicate data unnecessarily.

---

# Dependency Injection

Use Hilt.

Inject

- Repositories
- Firebase services
- DataStore
- Utility classes

Never manually instantiate dependencies that should be injected.

---

# Error Handling

Every repository operation must return a Result.

Handle

- Network failures
- Firebase exceptions
- Validation errors
- Permission errors

Do not expose raw exception messages to users.

---

# State Management

Every screen exposes

StateFlow<UiState>

Every UI action is represented by

UiEvent

One-time events use

UiAction

Do not expose mutable state to composables.

---

# Component Development

Before creating a new component

1. Check if one already exists.
2. Extend existing components if possible.
3. Create new reusable components only when necessary.

Avoid screen-specific reusable components unless justified.

---

# Naming Conventions

Classes

PascalCase

Functions

camelCase

Variables

camelCase

Constants

UPPER_SNAKE_CASE

Packages

lowercase

Files

Match primary class or composable name.

---

# Strings

All user-visible text must be stored in

strings.xml

Do not hardcode strings in composables.

---

# Colors

Use

MaterialTheme.colorScheme

Never hardcode colors.

---

# Typography

Use

MaterialTheme.typography

Never hardcode font sizes.

---

# Icons

Use Material Symbols.

Do not introduce third-party icon libraries.

---

# Images

Use Coil.

Provide placeholders.

Handle loading and error states.

---

# Logging

Use Timber.

Avoid

Log.d()

System.out.println()

printStackTrace()

Remove unnecessary debug logging before release.

---

# Performance Guidelines

Use

LazyColumn

LazyVerticalGrid

remember()

derivedStateOf()

Immutable models

Stable collections

Avoid unnecessary recomposition.

---

# Security Guidelines

Never trust client-side validation.

Enforce authorization through Firebase Security Rules.

Never expose sensitive information to unauthorized users.

---

# Code Quality

Functions should perform one responsibility.

Prefer small, focused composables.

Split files that become difficult to maintain.

Avoid excessive nesting and duplication.

---

# Git Workflow

Use feature-based branches.

Recommended branch names

feature/auth

feature/inventory

feature/orders

feature/contacts

feature/employees

fix/login-crash

refactor/dashboard

Commit frequently with meaningful messages.

---

# Testing Guidelines

Unit Test

- ViewModels
- Repositories
- Validators

UI Test

- Login
- Navigation
- Inventory
- Orders

Test every critical workflow before merging.

---

# Documentation

Whenever architecture or behavior changes

Update the corresponding .md file first.

Documentation is the source of truth.

---

# Phase-Based Development

Development order

Phase 1

Project setup

Architecture

Theme

Navigation

Firebase

Authentication

Phase 2

Inventory

Stock

Contacts

Employee Management

Phase 3

Orders

Quotation

Invoice

Purchase Order

Delivery Challan

PDF Generation

Phase 4

Dashboard

Settings

Reports

Testing

Optimization

Release

Do not begin a new phase until the current phase is complete and reviewed.

---

# AI Completion Checklist

Before considering any feature complete

- Follows project architecture
- Uses existing reusable components
- Uses Material Design 3
- No duplicated code
- No business logic in UI
- Uses ViewModel and Repository correctly
- Handles Loading state
- Handles Empty state
- Handles Error state
- Includes Compose Preview
- Includes input validation
- Uses Firestore through Repository only
- Follows RBAC rules
- Matches UI specification
- Builds successfully
- No TODOs left in production code

Only after every checklist item passes should the feature be considered complete.