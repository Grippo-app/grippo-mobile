# Module Descriptions

This document provides an overview of all modules in the Grippo Mobile project, explaining their
purpose and relationships.

## Core Modules

### shared

The main shared KMM module that contains the core application logic and serves as the entry point
for both Android and iOS platforms.

### androidApp

The Android-specific launcher application that integrates with the shared module.

### iosApp

The iOS-specific SwiftUI wrapper that integrates with the shared module.

## Design System Modules

### design-system:core

Contains the core design system elements like themes, tokens, and basic styling utilities.

### design-system:resources

Provides resources like colors, typography, dimensions, and icons used throughout the application.

### design-system:components

Reusable UI components built with Jetpack Compose that implement the design system.

### design-system:preview

Utilities for creating Compose previews of UI components.

## Data Layer Modules

### data-services:network

Network communication layer built with Ktor, handling API requests, responses, and serialization.

### data-services:database

Local database implementation using Room, providing data persistence.

### data-mappers:database-mapper

Mappers for converting between database entities and domain models.

### data-mappers:network-mapper

Mappers for converting between network DTOs and domain models.

### data-mappers:domain-mapper

Mappers for transforming domain models between different layers of the application.

## Feature Modules

### data-features:feature-api

Common interfaces and models for all feature modules.

### Feature Module Structure

Each feature module follows a similar pattern:

- Defines domain interfaces and models
- Implements repositories for data access
- Provides feature implementation that connects to data services

Examples of feature modules include:

- Authentication and user management
- Training and exercise management
- User preferences and settings

## Presentation Modules

### presentation-features:presentation-api

Common interfaces and models for presentation layer.

### Presentation Module Structure

Each presentation module follows a similar pattern:

- Contains UI components built with Jetpack Compose
- Implements ViewModels that connect to data features
- Defines navigation and state management
- Handles user interactions and UI state updates

Presentation modules typically correspond to major sections of the application, such as
authentication flows, main content screens, and user profile management.

## Dialog Modules

### dialog-features:dialog-api

Common interfaces and models for dialog components.

### Dialog Module Structure

Dialog modules provide reusable dialog components that can be used across the application:

- Implement self-contained UI components for specific interactions
- Handle user input and validation
- Provide consistent look and feel through the design system
- Return results to calling components
- Support stack-based navigation for nested dialogs

The dialog content is managed as a stack, allowing for nested dialogs and back navigation:

- Solo dialogs (like error displays) replace the entire stack
- Regular dialogs are pushed onto the stack when shown
- Back navigation pops from the stack or releases the dialog if only one item remains
- Dialog state tracks the current process (SHOW, DISMISS, RELEASE) and manages the stack

Examples include selection dialogs (date, weight, height), information displays, and error messages.

## Common Utility Modules

### common:platform-core

Platform-specific utilities and abstractions.

### common:logger

Logging infrastructure.

### common:core

Core utilities and base classes used throughout the application.

### common:error:error-provider

Error handling interfaces.

### common:error:error-provider-impl

Implementation of error handling.

### common:validation

Input validation utilities.

### common:date-utils

Date and time manipulation utilities.

## Compose Libraries

### compose-libs:wheel-picker

Custom wheel picker component for Compose.

### compose-libs:segment-control

Segmented control component for Compose.

### compose-libs:konfetti

Confetti animation library for Compose.

### compose-libs:chart

Chart and graph components for Compose.

## Module Dependencies

The modules follow a clear dependency hierarchy:

1. **Common Utilities**: Base modules that have no dependencies on other project modules
2. **Data Services**: Depend only on common utilities
3. **Data Mappers**: Depend on data services and common utilities
4. **Data Features**: Depend on data services, data mappers, and common utilities
5. **Design System**: Depends on common utilities
6. **Presentation Features**: Depend on data features, design system, and common utilities
7. **Dialog Features**: Depend on design system, data features, and common utilities
8. **Shared**: Depends on all other modules
9. **Platform Apps**: Depend on the shared module

This hierarchy ensures a clean separation of concerns and prevents circular dependencies.
