# BrandCrafts ERP (MVP)

# 06_UI_UX_GUIDELINES.md

Version: 1.0

---

# Purpose

This document defines the complete UI/UX standards for the BrandCrafts ERP application.

Every screen, component, animation, spacing rule, typography rule, and interaction pattern must follow this document.

Consistency is more important than creativity.

---

# Design Philosophy

The application should feel like a modern business application.

Keywords

• Clean
• Professional
• Fast
• Minimal
• Enterprise
• Mobile First
• Easy to Learn
• Touch Friendly
• Material Design 3

Users should complete common tasks within three taps whenever possible.

---

# Design References

The UI should be inspired by

Material Design 3

Google Workspace

Stripe Dashboard

Notion

Linear

Shopify POS

Avoid outdated ERP designs.

---

# Design System

Material Design 3

Do not mix Material 2 components.

Do not use XML layouts.

Jetpack Compose only.

---

# Color Palette

Primary

#2563EB

Secondary

#475569

Background

#F8FAFC

Surface

#FFFFFF

Error

#DC2626

Success

#16A34A

Warning

#D97706

Info

#0284C7

Use MaterialTheme.colorScheme throughout the application.

Never hardcode colors in composables.

---

# Typography

Use MaterialTheme.typography.

Do not create custom fonts in MVP.

Hierarchy

Display

Headline

Title

Body

Label

Maintain consistent typography.

---

# Corner Radius

Cards

12.dp

Buttons

12.dp

Text Fields

12.dp

Bottom Sheets

20.dp

Dialogs

16.dp

Keep the entire application visually consistent.

---

# Elevation

Default Cards

1.dp

Dialogs

4.dp

FAB

6.dp

Avoid excessive shadows.

---

# Spacing System

Use only these spacing values

4.dp

8.dp

12.dp

16.dp

20.dp

24.dp

32.dp

Do not use random spacing values.

---

# Layout Rules

Prefer

Arrangement.spacedBy()

contentPadding

PaddingValues

Modifier.padding()

instead of multiple Spacer composables.

Spacer should only be used when no better layout alternative exists.

---

# Navigation

Single Activity

Navigation Compose

Bottom Navigation

Four tabs

Home

Stock

Orders

Contacts

Profile actions belong in the Top App Bar.

---

# Screen Structure

Every screen follows the same structure.

Top App Bar

↓

Search / Filter (optional)

↓

Content

↓

Floating Action Button (optional)

↓

Bottom Navigation

Avoid unnecessary nesting.

---

# Top App Bar

Contains

Title

Optional Search

Optional Filter

Profile

Use LargeTopAppBar only where appropriate.

Otherwise use CenterAlignedTopAppBar.

---

# Cards

Use ElevatedCard or Card from Material 3.

Cards should contain

Title

Subtitle

Status

Actions

Avoid placing too many actions inside one card.

---

# Lists

Use LazyColumn.

Never use Column with verticalScroll for large datasets.

Every list should support

Loading

Empty State

Search

Pull to Refresh (optional)

---

# Search

Search should appear at the top of list screens.

Use OutlinedTextField with leading search icon.

Debounce search if required.

---

# Filters

Use Material 3 Filter Chips.

Avoid custom filter widgets.

---

# Forms

Use OutlinedTextField.

Fields should appear in a logical order.

Group related fields.

Use keyboard types appropriately.

Show validation messages below fields.

---

# Buttons

Primary

FilledButton

Secondary

OutlinedButton

Danger

FilledTonalButton with Error colors

Never mix different button styles unnecessarily.

---

# Floating Action Button

Use ExtendedFloatingActionButton where text improves clarity.

Example

+ Add Material

+ New Invoice

Avoid FABs on screens where the action is not primary.

---

# Bottom Sheets

Use ModalBottomSheet for

Stock In

Stock Out

Material Usage

Create Contact

Create Employee

Quick Edit

Avoid creating separate screens for simple forms.

---

# Dialogs

Use AlertDialog for

Delete confirmation

Logout confirmation

Unsaved changes

Never create custom dialog implementations.

---

# Empty States

Every list must provide an empty state.

Include

Illustration/Icon

Title

Description

Primary Action

Example

No Customers Found

Tap "Add Customer" to create one.

---

# Loading States

Every network request must show loading.

Use CircularProgressIndicator.

Avoid blank screens.

---

# Error States

Display friendly messages.

Provide Retry button where appropriate.

Avoid exposing technical errors to users.

---

# Status Chips

Use AssistChip or FilterChip style.

Examples

Paid

Pending

Approved

Low Stock

Draft

Rejected

Completed

Colors should follow Material 3 guidelines.

---

# Dashboard

Dashboard should contain

Summary Cards

Quick Actions

Recent Activity

Avoid excessive charts in MVP.

---

# Tables

Do not use desktop-style tables.

Display records as cards.

Cards are more mobile friendly.

---

# Icons

Use Material Icons.

Avoid downloading random icon packs.

Icons should always have semantic meaning.

---

# Images

Use Coil.

Support placeholder images.

Avoid large bitmap loading.

---

# PDF Actions

Display Share PDF button.

Display View PDF button when available.

Avoid exposing file paths.

---

# Animations

Keep animations subtle.

Allowed

Crossfade

AnimatedVisibility

animateContentSize

Avoid heavy motion.

Application speed is more important.

---

# Responsiveness

Support

Phones

Foldables

Tablets

Avoid hardcoded widths.

Prefer

fillMaxWidth()

weight()

WindowSizeClass (future enhancement)

---

# Accessibility

Minimum touch target

48.dp

Provide content descriptions.

Maintain color contrast.

Avoid tiny text.

---

# Theme

Support

Light Theme

Dark Theme (Future)

Follow MaterialTheme.

---

# Component Library

Reusable Components

AppTopBar

AppSearchBar

AppStatCard

AppListCard

StatusChip

PrimaryButton

SecondaryButton

AppTextField

EmptyState

LoadingView

ConfirmationDialog

AppSectionHeader

UniversalFormSheet

No duplicate components.

---

# Preview Policy

Every reusable component must include Compose Preview.

Every screen must include Preview using sample data.

Preview code must never contain business logic.

---

# Naming Convention

InventoryCard

CustomerCard

EmployeeCard

InvoiceCard

MaterialCard

Keep names predictable.

---

# Performance

Avoid unnecessary recomposition.

Remember expensive calculations.

Use immutable models.

Keep composables small.

Maximum recommended size

250 lines

Split into reusable components when larger.

---

# AI Development Rules

Any AI generating UI must follow these rules.

- Material Design 3 only
- Compose only
- No XML
- Reusable components first
- Stateless composables
- Consistent spacing system
- No hardcoded colors
- No hardcoded strings
- No duplicated layouts
- Avoid unnecessary Spacer usage
- Use Arrangement.spacedBy() where possible
- Every screen must have Loading, Empty, and Error states
- Every screen must have Preview
- Follow this document exactly

Deviation from these guidelines requires updating this document first.