package com.grippo.design.components.inputs.core

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.lerp
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.components.inputs.InputRepetitions
import com.grippo.design.components.inputs.InputSearch
import com.grippo.design.components.inputs.InputVolume
import com.grippo.design.components.inputs.InputWeight
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.input_invalid_value

@Stable
internal sealed class InputStyle {
    @Stable
    data class Default(
        val onValueChange: (String) -> Unit,
    ) : InputStyle()

    @Stable
    data class Clickable(val onClick: () -> Unit) : InputStyle()
}

@Immutable
internal sealed class InputError {
    data object None : InputError()
    data class Error(val msg: String) : InputError()
}

@Immutable
internal sealed class InputPlaceholder(open val value: String) {
    data class OverInput(override val value: String) : InputPlaceholder(value = value)
    data object Empty : InputPlaceholder(value = "")
}

@Composable
internal fun Input(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: InputPlaceholder,
    enabled: Boolean = true,
    error: InputError = InputError.None,
    inputStyle: InputStyle,
    textStyle: TextStyle = AppTokens.typography.b15Med(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leading: (@Composable (color: Color) -> Unit)? = null,
    trailing: (@Composable (color: Color) -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onFocused: () -> Unit = {},
) {
    val colors = AppTokens.colors
    val shape = RoundedCornerShape(AppTokens.dp.input.radius)
    val height = AppTokens.dp.input.height

    val isClickable = inputStyle is InputStyle.Clickable

    val hasFocus = remember { mutableStateOf(false) }

    // Internal TextFieldValue to control selection
    var tfv by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Sync internal TFV with external String, preserving selection when possible
    LaunchedEffect(value) {
        if (value != tfv.text) {
            val s = tfv.selection.start.coerceIn(0, value.length)
            val e = tfv.selection.end.coerceIn(0, value.length)
            tfv = tfv.copy(text = value, selection = TextRange(s, e))
        }
    }

    // Suppress error styling while the field is focused: we don't want to flash
    // the user red mid-typing (e.g. partial email doesn't match the regex yet).
    // Error becomes visible only after the user leaves the field.
    val showError = error is InputError.Error && !hasFocus.value
    val errorMessage = if (error is InputError.Error) {
        error.msg.ifBlank { AppTokens.strings.res(Res.string.input_invalid_value) }
    } else {
        ""
    }

    val borderColor = when {
        showError -> colors.semantic.error
        !enabled -> Color.Transparent
        hasFocus.value -> colors.border.focus
        else -> colors.border.default
    }
    val backgroundColor = when (enabled) {
        true -> colors.background.card
        else -> colors.input.backgroundDisabled
    }
    val placeholderColor = when {
        showError -> colors.input.placeholder
        !enabled -> colors.input.placeholderDisabled
        else -> colors.input.placeholder
    }
    val labelColor = when {
        showError -> colors.input.label
        !enabled -> colors.input.placeholderDisabled
        else -> colors.input.label
    }
    val contentColor = when {
        showError -> colors.semantic.error
        !enabled -> colors.input.textDisabled
        else -> colors.input.text
    }
    val leadingColor = when {
        showError -> colors.input.leading
        !enabled -> colors.input.textDisabled
        else -> colors.input.leading
    }
    val trailingColor = when {
        showError -> colors.input.trailing
        !enabled -> colors.input.textDisabled
        else -> colors.input.trailing
    }

    val textStyleAnimateFraction = animateFloatAsState(
        targetValue = if (hasFocus.value || tfv.text.isNotBlank()) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
    )

    Box(
        modifier = modifier
            .then(
                if (isClickable && enabled)
                    Modifier.scalableClick(
                        onClick = inputStyle.onClick
                    )
                else Modifier
            )
    ) {
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (showError) this.error(errorMessage)
                }
                .background(color = backgroundColor, shape = shape)
                .border(width = AppTokens.dp.input.borderWidth, color = borderColor, shape = shape)
                .heightIn(min = height)
                .onFocusChanged { focus ->
                    val wasFocused = hasFocus.value
                    hasFocus.value = focus.hasFocus
                    if (focus.hasFocus && !wasFocused) {
                        onFocused()
                    }
                }
                .animateContentSize(),
            value = tfv,
            cursorBrush = SolidColor(contentColor),
            onValueChange = { new ->
                tfv = new
                when (inputStyle) {
                    is InputStyle.Clickable -> Unit

                    is InputStyle.Default -> {
                        if (new.text != value) {
                            inputStyle.onValueChange(new.text)
                        }
                    }
                }
            },
            readOnly = isClickable,
            minLines = minLines,
            textStyle = textStyle.copy(color = contentColor),
            // iOS-critical: enabled = false disables the native UITextView's
            // userInteractionEnabled, so taps pass through to the outer
            // Box.clickable. readOnly = true alone is NOT enough — the native
            // field still swallows touches (especially inside LazyColumn,
            // where it also competes with the list's scroll gestures).
            enabled = enabled && !isClickable,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            decorationBox = { innerTextField ->

                val rowModifier = Modifier
                    .fillMaxWidth()
                    .height(intrinsicSize = IntrinsicSize.Min)
                    .padding(vertical = AppTokens.dp.input.verticalPadding)

                when (placeholder) {
                    InputPlaceholder.Empty -> Row(
                        modifier = rowModifier,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(AppTokens.dp.input.horizontalPadding))

                        if (leading != null) {
                            leading.invoke(leadingColor)
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.contentSpacing))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(),
                            content = { innerTextField() }
                        )

                        if (trailing != null) {
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.contentSpacing))
                            trailing.invoke(trailingColor)
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.trailingPadding))
                        } else {
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.horizontalPadding))
                        }
                    }

                    is InputPlaceholder.OverInput -> Row(
                        modifier = rowModifier,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(AppTokens.dp.input.horizontalPadding))

                        if (leading != null) {
                            leading.invoke(leadingColor)
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.contentSpacing))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize()
                        ) {
                            Column {
                                Text(
                                    text = placeholder.value,
                                    style = lerp(
                                        start = AppTokens.typography.b14Med()
                                            .copy(color = placeholderColor),
                                        stop = AppTokens.typography.b12Med()
                                            .copy(color = labelColor),
                                        fraction = textStyleAnimateFraction.value,
                                    ),
                                )
                                if (hasFocus.value || tfv.text.isNotBlank()) innerTextField()
                            }
                        }
                        if (trailing != null) {
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.contentSpacing))
                            trailing.invoke(trailingColor)
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.trailingPadding))
                        } else {
                            Spacer(modifier = Modifier.width(AppTokens.dp.input.horizontalPadding))
                        }
                    }
                }
            },
        )
    }
}

@AppPreview
@Composable
private fun InputDefaultEmptyPreview() {
    PreviewContainer {
        Input(
            value = "",
            placeholder = InputPlaceholder.Empty,
            inputStyle = InputStyle.Default(onValueChange = {}),
        )
    }
}

@AppPreview
@Composable
private fun InputDefaultWithPlaceholderPreview() {
    PreviewContainer {
        Input(
            value = "",
            placeholder = InputPlaceholder.OverInput("Label"),
            inputStyle = InputStyle.Default(onValueChange = {}),
        )
    }
}

@AppPreview
@Composable
private fun InputDefaultWithTextPreview() {
    PreviewContainer {
        Input(
            value = "Some text",
            placeholder = InputPlaceholder.OverInput("Label"),
            inputStyle = InputStyle.Default(onValueChange = {}),
        )
    }
}

@AppPreview
@Composable
private fun InputClickablePreview() {
    PreviewContainer {
        Input(
            value = "Tap me",
            placeholder = InputPlaceholder.OverInput("Action"),
            inputStyle = InputStyle.Clickable(onClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun InputErrorPreview() {
    PreviewContainer {
        Input(
            value = "Wrong",
            placeholder = InputPlaceholder.OverInput("Error label"),
            error = InputError.Error("Invalid input"),
            inputStyle = InputStyle.Default(onValueChange = {}),
        )
    }
}

@AppPreview
@Composable
private fun InputDisabledPreview() {
    PreviewContainer {
        Input(
            value = "Disabled",
            placeholder = InputPlaceholder.OverInput("Disabled"),
            inputStyle = InputStyle.Default(onValueChange = {}),
            enabled = false
        )
        Input(
            value = "",
            placeholder = InputPlaceholder.OverInput("Disabled empty"),
            inputStyle = InputStyle.Default(onValueChange = {}),
            enabled = false,
        )
        Input(
            value = "Invalid",
            placeholder = InputPlaceholder.OverInput("Disabled invalid"),
            error = InputError.Error("Invalid input"),
            inputStyle = InputStyle.Default(onValueChange = {}),
            enabled = false,
        )
    }
}

@AppPreview
@Composable
private fun InputFormPreview() {
    PreviewContainer {
        InputEmail(value = EmailFormatState.Empty(), onValueChange = {})
        InputEmail(value = EmailFormatState.of("athlete@example.com"), onValueChange = {})
        InputEmail(value = EmailFormatState.of("athlete@"), onValueChange = {})
        InputPassword(value = PasswordFormatState.of("Grippo123!"), onValueChange = {})
        InputSearch(value = "Жим гантелей", onValueChange = {})
    }
}

@AppPreview
@Composable
private fun InputCompactSheetPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.background(AppTokens.colors.background.dialog),
            verticalArrangement = Arrangement.spacedBy(
                AppTokens.dp.contentPadding.content
            ),
        ) {
            InputWeight(
                value = WeightFormatState.of(72.5f),
                placeholder = "Вага тіла · 100%",
                onClick = {},
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                InputVolume(
                    modifier = Modifier.weight(1f),
                    value = VolumeFormatState.of("123.5"),
                    placeholder = "Додаткова вага",
                    onValueChange = {},
                )
                InputRepetitions(
                    modifier = Modifier.weight(1f),
                    value = RepetitionsFormatState.of("12"),
                    onValueChange = {},
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun InputLongContentPreview() {
    PreviewContainer {
        InputEmail(
            value = EmailFormatState.of("oleksandr.kovalenko@example.com"),
            placeholder = "Адреса електронної пошти користувача",
            onValueChange = {},
        )
        Input(
            value = "Перший рядок\nДругий рядок\nТретій рядок",
            placeholder = InputPlaceholder.Empty,
            inputStyle = InputStyle.Default(onValueChange = {}),
            minLines = 2,
            maxLines = 3,
        )
    }
}
