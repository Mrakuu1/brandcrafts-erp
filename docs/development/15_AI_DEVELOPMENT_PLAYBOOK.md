# BrandCrafts ERP (MVP)

# 15_AI_DEVELOPMENT_PLAYBOOK.md

Version: 1.0

---

# Purpose

This document defines how AI assistants are used throughout the BrandCrafts ERP project.

Every AI must follow the project documentation before generating any code.

No AI should introduce new architecture, dependencies, UI patterns, or workflows unless explicitly approved.

Project documentation is the source of truth.

---

# AI Responsibilities

## Codex

Primary Responsibility

Application development

Tasks

- Write production-ready Kotlin code
- Implement Jetpack Compose screens
- Implement ViewModels
- Implement Use Cases
- Implement Repositories
- Implement Firebase integration
- Fix bugs
- Refactor code
- Maintain project architecture

Codex must never redesign the UI without an approved UI specification.

---

## Gemini

Primary Responsibility

UI and UX

Tasks

- Generate Compose screen previews
- Improve layouts
- Suggest Material Design 3 improvements
- Review accessibility
- Suggest animations
- Optimize user flows

Gemini should not change business logic.

---

## ChatGPT

Primary Responsibility

Architecture

Planning

Documentation

Code Reviews

Tasks

- Create documentation
- Review implementation
- Review architecture
- Explain code
- Suggest optimizations
- Create implementation plans
- Debug complex issues

ChatGPT should not introduce undocumented functionality.

---

# AI Development Workflow

Every feature follows this workflow.

Step 1

Read project documentation.

↓

Step 2

Review existing implementation.

↓

Step 3

Identify reusable components.

↓

Step 4

Implement feature.

↓

Step 5

Run self-review.

↓

Step 6

Fix issues.

↓

Step 7

Mark phase complete.

Never skip steps.

---

# Documentation Priority

If multiple documents exist, priority is

1

Development Guidelines

2

Architecture

3

Workflows

4

Design System

5

UI Specifications

6

API Contracts

7

Remaining documentation

Generated code must always match documentation.

---

# Before Writing Code

AI must verify

- Current phase
- Required module
- Existing components
- Existing models
- Existing repositories
- Existing navigation
- Existing Firebase collections

Never create duplicates.

---

# Before Creating Files

AI must check

- Does this file already exist?

If yes

Update it.

Do not create duplicate files.

---

# Before Creating Components

Check

ui/components/

If a reusable component exists

Reuse it.

Never duplicate reusable UI.

---

# Before Creating Models

Check

domain/model

data/model

Never create duplicate models.

---

# Before Creating Repositories

Check

domain/repository

data/repository

Never duplicate repositories.

---

# Before Creating Navigation

Check

navigation/

Never create multiple NavHosts.

---

# Before Creating Firebase Code

Check

Repository

Only repositories communicate with Firebase.

Composable functions never access Firebase.

---

# Prompt Template

Every AI implementation prompt should begin with

Read the complete project documentation before making changes.

Do not make assumptions.

Implement only the requested phase.

Follow Clean Architecture, MVVM, Material Design 3, and Firebase.

Reuse existing components.

Do not duplicate code.

Stop if documentation is insufficient.

---

# UI Generation Prompt

Generate only Jetpack Compose UI.

Requirements

- Material Design 3
- Mobile first
- Reusable components
- Stateless composables
- No business logic
- Preview included
- No XML

---

# Feature Implementation Prompt

Implement only the requested feature.

Do not modify unrelated files.

Do not redesign existing UI.

Follow the repository pattern.

Update only the required navigation.

---

# Bug Fix Prompt

Analyze the root cause.

Do not apply temporary fixes.

Do not rewrite unrelated modules.

Maintain architecture.

Explain the root cause before applying the fix.

---

# Refactoring Prompt

Improve readability.

Reduce duplication.

Preserve functionality.

Preserve architecture.

Do not change public interfaces unless documented.

---

# Firebase Prompt

Follow

API Contracts

Firestore Schema

Security Rules

Use transactions where required.

Never bypass repositories.

---

# Code Review Checklist

Review

Architecture

Naming

Compose

StateFlow

Repositories

Firebase

Error handling

Performance

Accessibility

Material Design 3

Security

RBAC

No duplicated code

No unused imports

No TODOs

---

# UI Review Checklist

Verify

Spacing

Typography

Colors

Icons

Accessibility

Dark mode readiness

Responsive layouts

Material Design 3

Preview availability

---

# Performance Review Checklist

Verify

Recomposition

Lazy lists

Stable models

remember usage

derivedStateOf usage

Coroutine scope

Memory usage

---

# Security Review Checklist

Verify

Firebase Rules

Role enforcement

No client-side trust

Authentication

Storage Rules

No exposed credentials

---

# Git Commit Format

Examples

feat(auth): implement Firebase login

feat(stock): add stock in workflow

feat(invoice): generate PDF invoice

fix(login): handle inactive users

refactor(inventory): simplify repository

docs(rbac): update permission matrix

---

# AI Completion Checklist

Before marking work complete

- Documentation followed
- Builds successfully
- No compiler errors
- No runtime crashes
- Uses existing architecture
- Uses reusable components
- Uses Material Design 3
- Uses ViewModel correctly
- Uses repositories correctly
- Handles Loading
- Handles Empty
- Handles Error
- RBAC respected
- Firebase integrated
- Compose Preview added
- No duplicated code
- No unused resources
- Acceptance criteria satisfied

Only after every item passes should the feature be considered complete.

---

# Golden Rules

- Documentation is the source of truth.
- Never make assumptions.
- Reuse before creating.
- One responsibility per class.
- One phase at a time.
- Keep the UI simple.
- Keep business logic out of Compose.
- Never bypass the repository layer.
- Never compromise security for convenience.
- Deliver working, testable code at the end of every phase.