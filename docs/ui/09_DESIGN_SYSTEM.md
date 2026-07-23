# BrandCrafts ERP (MVP)

# 09_DESIGN_SYSTEM.md

Version: 1.0

---

# Purpose

This document defines the reusable Design System for the BrandCrafts ERP application.

Every screen must be built using the components defined here.

New reusable components should only be added when absolutely necessary.

Consistency takes priority over customization.

---

# Design Principles

The design system follows:

- Material Design 3
- Mobile First
- Accessibility First
- Reusable Components
- Consistent Layouts
- Minimal Visual Noise
- Enterprise Grade UI

---

# Component Hierarchy

Application

↓

Scaffold

↓

TopAppBar

↓

Content

↓

Reusable Components

↓

Material Components

---

# Component Rules

- Every reusable component lives in

ui/components/

- Components must be stateless.
- Components never access ViewModels.
- Components never access Firebase.
- Components receive everything through parameters.
- Components must support Preview.

---

# Core Components

## AppScaffold

Purpose

Common layout for every screen.

Contains

- Top App Bar
- Content
- FAB (optional)
- Bottom Navigation
- Snackbar Host

Used By

Every screen except Login.

---

## AppTopBar

Purpose

Common top app bar.

Supports

- Title
- Back Button
- Search Action
- Filter Action
- Profile Action

Variants

- CenterAligned
- Small
- Large

---

## AppSearchBar

Purpose

Reusable search field.

Features

- Search icon
- Clear button
- Placeholder
- Debounced input
- Keyboard Search action

Used By

Inventory

Contacts

Orders

Employees

---

## AppTextField

Purpose

Standard text input.

Supports

- Label
- Placeholder
- Leading Icon
- Trailing Icon
- Validation Error
- Read Only
- Single Line
- Multi Line

Variants

- Text
- Number
- Email
- Phone
- Password

---

## AppDropdown

Purpose

Reusable selection field.

Supports

- Searchable
- Non-searchable
- Read only display
- Validation

---

## AppDateField

Purpose

Reusable date selector.

Uses

Material Date Picker.

---

## AppButton

Variants

Primary

Secondary

Outlined

Danger

Disabled

Loading

Loading button must show CircularProgressIndicator.

---

## AppIconButton

Used for

Call

WhatsApp

Edit

Delete

Share

Print

Search

Filter

Refresh

---

## AppCard

Purpose

Base card component.

Properties

- Title
- Subtitle
- Content
- Actions
- Status

Every list item is based on AppCard.

---

## AppSectionHeader

Purpose

Reusable section heading.

Supports

- Title
- Optional Action
- Divider

---

## StatusChip

Purpose

Display status.

Supported

Draft

Pending

Paid

Completed

Cancelled

Low Stock

Approved

Rejected

---

## MetricCard

Dashboard component.

Shows

Title

Value

Icon

Color

Trend (optional)

---

## EmptyState

Purpose

Display empty data.

Contains

Illustration

Title

Description

Primary Action

---

## ErrorState

Purpose

Display recoverable errors.

Contains

Icon

Title

Description

Retry Button

---

## LoadingView

Purpose

Display loading state.

Uses

Material CircularProgressIndicator

Optional message.

---

## ConfirmationDialog

Purpose

Confirmation before destructive actions.

Used For

Delete

Logout

Deactivate Employee

Reset Data

---

## UniversalFormSheet

Purpose

Reusable data entry sheet.

Supports

Dynamic title

Dynamic fields

Validation

Primary button

Cancel

Used For

Stock In

Stock Out

Material Usage

Contact

Supplier

Employee

Invoice

Quotation

Purchase Order

Delivery Challan

---

# Business Cards

## InventoryCard

Displays

Material Name

SKU

Category

Available Quantity

Status

Actions

---

## ContactCard

Displays

Name

Company

Phone

Outstanding

Actions

Call

WhatsApp

---

## DocumentCard

Displays

Document Number

Customer

Amount

Status

Date

Actions

Share

View

---

## EmployeeCard

Displays

Avatar

Name

Role

Phone

Status

Actions

Edit

Activate

Deactivate

---

## ActivityCard

Displays

Action

Module

User

Time

---

# Navigation Components

Bottom Navigation

Contains

Home

Stock

Orders

Contacts

Profile handled through Top App Bar.

---

# FAB Rules

Inventory

Admin

Add Material

Employee

Stock Action

Orders

New Document

Contacts

Add Contact

Employee Management

Add Employee

No other screens use FAB unless approved.

---

# Icons

Use Material Symbols only.

Do not import third-party icon libraries.

---

# Colors

Always use

MaterialTheme.colorScheme

Never hardcode colors.

---

# Typography

Always use

MaterialTheme.typography

Never hardcode font sizes.

---

# Shapes

Use

MaterialTheme.shapes

Avoid custom corner radii in individual components.

---

# Dimensions

Spacing

4dp

8dp

12dp

16dp

20dp

24dp

32dp

Corner Radius

12dp

Bottom Sheet

20dp

Touch Target

Minimum

48dp

---

# Lists

Always use

LazyColumn

LazyVerticalGrid

Never use Column + verticalScroll for datasets.

---

# Forms

Use

OutlinedTextField

KeyboardOptions

Validation below field

Logical field grouping

---

# Accessibility

Every clickable component must have

contentDescription

Touch target >= 48dp

Readable text contrast

---

# Preview Rules

Every reusable component must include

Compose Preview

Light Theme

Sample Data

No ViewModel

No Firebase

---

# AI Development Rules

Every AI generating UI must:

- Reuse existing components.
- Never duplicate components.
- Never create screen-specific buttons if a generic button exists.
- Never hardcode colors.
- Never hardcode typography.
- Never access business logic from UI components.
- Prefer composition over inheritance.
- Keep components under approximately 200 lines.
- Split large components into smaller reusable ones.
- Follow Material Design 3.