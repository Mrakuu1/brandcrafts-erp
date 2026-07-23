# BrandCrafts ERP (MVP)

# 13_FOLDER_STRUCTURE.md

Version: 1.0

---

# Purpose

This document defines the complete project folder structure for the BrandCrafts ERP application.

Every feature must follow this structure.

No new package or module should be introduced without updating this document.

---

# Technology Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- MVVM
- Clean Architecture
- Firebase Auth
- Cloud Firestore
- Firebase Storage
- Hilt
- Navigation Compose
- Kotlin Coroutines
- StateFlow
- Coil
- DataStore

---

# Project Structure

```
app/
│
├── di/
│
├── navigation/
│
├── core/
│   ├── common/
│   ├── constants/
│   ├── datastore/
│   ├── dispatcher/
│   ├── exception/
│   ├── extension/
│   ├── model/
│   ├── network/
│   ├── pdf/
│   ├── result/
│   ├── util/
│   └── validation/
│
├── data/
│   ├── datasource/
│   │   ├── firestore/
│   │   ├── storage/
│   │   └── auth/
│   │
│   ├── mapper/
│   │
│   ├── model/
│   │
│   └── repository/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── ui/
│   ├── components/
│   ├── dialogs/
│   ├── bottomsheet/
│   ├── theme/
│   └── preview/
│
├── feature/
│
│   ├── auth/
│   │
│   ├── dashboard/
│   │
│   ├── inventory/
│   │
│   ├── contacts/
│   │
│   ├── quotation/
│   │
│   ├── invoice/
│   │
│   ├── purchaseorder/
│   │
│   ├── deliverychallan/
│   │
│   ├── employee/
│   │
│   ├── settings/
│   │
│   └── profile/
│
└── MainActivity.kt
```

---

# Feature Folder Structure

Every feature must follow the same layout.

Example

```
feature/

inventory/

components/

navigation/

InventoryScreen.kt

InventoryViewModel.kt

InventoryUiState.kt

InventoryEvent.kt

InventoryAction.kt
```

No feature should have a different structure.

---

# UI Components

Reusable UI belongs only in

```
ui/components/
```

Examples

```
AppButton.kt

AppTopBar.kt

AppTextField.kt

AppCard.kt

StatusChip.kt

AppSearchBar.kt

LoadingView.kt

EmptyState.kt

ErrorState.kt

ConfirmationDialog.kt

UniversalFormSheet.kt
```

Never duplicate reusable UI inside feature folders.

---

# Dialogs

```
ui/dialogs/
```

Examples

```
DeleteDialog

LogoutDialog

ConfirmationDialog
```

---

# Bottom Sheets

```
ui/bottomsheet/
```

Examples

```
UniversalFormSheet

ProfileBottomSheet

FilterBottomSheet
```

---

# Theme

```
ui/theme/
```

Contains

```
Color.kt

Theme.kt

Typography.kt

Shape.kt
```

No colors outside this package.

---

# Navigation

```
navigation/
```

Contains

```
AppNavigation.kt

NavGraph.kt

Routes.kt
```

Navigation must remain centralized.

---

# Dependency Injection

```
di/
```

Modules

```
FirebaseModule

RepositoryModule

DataStoreModule

DispatcherModule
```

---

# Core Package

Contains application-wide reusable code.

Never place feature-specific code here.

---

# Core/Common

```
core/common/
```

Examples

```
UiState

UiEvent

UiAction

BaseViewModel
```

---

# Core/Constants

```
AppConstants

FirestoreCollections

Roles

DocumentTypes

Routes
```

---

# Core/Validation

Validators

```
EmailValidator

PhoneValidator

QuantityValidator

GSTValidator
```

---

# Core/Result

Contains

```
Result.kt

Resource.kt
```

Used across repositories.

---

# Data Layer

Contains Firebase implementation only.

No UI code.

No Compose code.

---

# Domain Layer

Contains

Models

Repository Interfaces

Use Cases

No Firebase SDK imports.

---

# Repository Naming

Example

```
InventoryRepository

InventoryRepositoryImpl
```

Keep implementation separate from interface.

---

# Firestore Models

Located in

```
data/model/
```

Examples

```
InventoryEntity

CustomerEntity

InvoiceEntity
```

---

# Domain Models

Located in

```
domain/model/
```

Examples

```
Inventory

Customer

Invoice
```

---

# Mappers

Located in

```
data/mapper/
```

Responsible for converting

Firestore Entity

↓

Domain Model

Never expose Firestore entities to UI.

---

# Use Cases

Located in

```
domain/usecase/
```

Examples

```
CreateInvoiceUseCase

GetInventoryUseCase

LoginUseCase

StockInUseCase
```

Business logic belongs here.

---

# ViewModels

One ViewModel per screen.

Responsibilities

- Handle UI Events
- Call Use Cases
- Expose StateFlow
- Emit Actions

Never access Firebase directly.

---

# Screen Files

Every screen contains only UI logic.

No business logic.

No Firestore operations.

---

# Assets

```
res/

drawable/

font/

mipmap/

values/
```

Follow Android standards.

---

# Strings

All user-facing text must be stored in

```
strings.xml
```

Never hardcode strings.

---

# Images

Load images using Coil.

Do not use BitmapFactory directly.

---

# PDF Files

Temporary PDFs

```
cacheDir/pdf/
```

Shared using

```
FileProvider
```

Never expose internal storage paths.

---

# Naming Conventions

Composable

```
InventoryScreen
```

ViewModel

```
InventoryViewModel
```

Repository

```
InventoryRepository
```

UseCase

```
CreateInvoiceUseCase
```

Entity

```
InvoiceEntity
```

Domain Model

```
Invoice
```

---

# Maximum File Size

Composable

~300 lines

ViewModel

~250 lines

Repository

~300 lines

Split files when they grow beyond these limits.

---

# Package Rules

Feature code stays inside its feature package.

Shared code stays inside ui/ or core/.

Do not import feature packages into other features.

Use domain interfaces for communication.

---

# AI Development Rules

When generating code:

- Follow this folder structure exactly.
- Do not create additional root packages.
- Do not duplicate components.
- Do not place Firebase code in UI.
- Do not bypass the domain layer.
- Keep the architecture clean and consistent.
- Reuse existing packages before creating new ones.