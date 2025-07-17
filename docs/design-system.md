# Design System

This document describes the design system used in the Grippo Mobile application.

## Overview

The design system is organized into several modules:

- **design-system:core**: Core design system elements like themes and tokens
- **design-system:resources**: Resources like colors, typography, dimensions, and icons
- **design-system:components**: Reusable UI components built with Jetpack Compose
- **design-system:preview**: Utilities for creating Compose previews

## Design Tokens

Design tokens are the foundation of the design system, providing consistent values for colors, typography, spacing, and other visual elements.

### AppTokens

The `AppTokens` object provides access to all design tokens:

```kotlin
object AppTokens {
    val colors: AppColor
        get() = AppTheme.colors

    val typography: AppTypography
        get() = AppTheme.typography

    val dp: AppDp
        get() = AppTheme.dp

    val icons: AppIcon
        get() = AppTheme.icons
}
```

### Colors

Colors are defined in `AppColor`:

```kotlin
interface AppColor {
    val background: BackgroundColors
    val text: TextColors
    val icon: IconColors
    val button: ButtonColors
    val divider: DividerColors
    // ...
}

interface BackgroundColors {
    val primary: Color
    val secondary: Color
    val tertiary: Color
    // ...
}
```

### Typography

Typography is defined in `AppTypography`:

```kotlin
interface AppTypography {
    fun h1(): TextStyle
    fun h2(): TextStyle
    fun h3(): TextStyle
    fun h4(): TextStyle
    fun body1(): TextStyle
    fun body2(): TextStyle
    fun caption(): TextStyle
    // ...
}
```

### Dimensions

Dimensions are defined in `AppDp`:

```kotlin
interface AppDp {
    val contentPadding: ContentPadding
    val button: ButtonDp
    val card: CardDp
    val exerciseCard: ExerciseCardDp
    // ...
}

interface ContentPadding {
    val screen: Dp
    val content: Dp
    // ...
}
```

### Icons

Icons are defined in `AppIcon`:

```kotlin
interface AppIcon {
    val Back: ImageVector
    val Close: ImageVector
    val Add: ImageVector
    val NavArrowRight: ImageVector
    val InfoEmpty: ImageVector
    // ...
}
```

## Theme

The `AppTheme` composable function applies the design system theme to a composable hierarchy:

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
```

## Components

The design system provides a set of reusable components that implement the design system. Here are some examples:

### Common Components

The design system provides a variety of reusable components:

#### Buttons
- `PrimaryButton`: Main call-to-action button with loading state support
- `SecondaryButton`: Alternative action button
- `TextButton`: Text-only button for less prominent actions

#### Cards
- `ContentCard`: General-purpose card for content display
- `ExerciseCard`: Specialized card for exercise information
- `UserCard`: Card displaying user information

#### Text Fields
- `AppTextField`: Standard text input with validation support
- `PasswordField`: Secure text input with visibility toggle
- `SearchField`: Text field optimized for search functionality

Each component:
- Uses design tokens for consistent styling
- Supports common modifiers for customization
- Handles states like loading, error, and disabled
- Follows accessibility best practices

## Previews

The design system includes preview utilities to help with component development:

### Preview Annotation

The `@AppPreview` annotation simplifies creating standardized previews:
- Applies consistent preview settings
- Works with the standard Compose Preview panel
- Supports both light and dark themes

### Preview Container

The `PreviewContainer` composable provides a consistent environment for previews:
- Applies the app theme
- Sets up the correct background color
- Handles both light and dark mode

### Usage Pattern

```kotlin
@AppPreview
@Composable
private fun ComponentPreview() {
    PreviewContainer {
        // Component to preview
    }
}
```

## Using the Design System

### Integration Pattern

The design system is designed to be used throughout the application:

1. **Apply the Theme**
   - Wrap your UI with the `AppTheme` composable
   - This ensures consistent styling across the application

2. **Use Design Tokens**
   - Access tokens through the `AppTokens` object
   - Use tokens for colors, typography, spacing, and other visual properties
   - This ensures consistency and supports theme changes

3. **Leverage Components**
   - Use pre-built components for common UI elements
   - Customize through parameters rather than modifying the components
   - This ensures consistent behavior and appearance

4. **Follow Patterns**
   - Study existing screens for patterns and conventions
   - Maintain consistency in layout, spacing, and interaction patterns

## Extending the Design System

The design system is designed to be extensible. When adding new components:

### Guidelines

1. **Follow Design Principles**
   - Maintain consistency with existing components
   - Use design tokens for all visual properties
   - Support light and dark themes

2. **Component Structure**
   - Place components in the appropriate package
   - Use clear, descriptive parameter names
   - Provide sensible defaults
   - Support common modifiers

3. **Documentation and Testing**
   - Add preview functions for visual testing
   - Document the component's purpose and usage
   - Include examples of common configurations