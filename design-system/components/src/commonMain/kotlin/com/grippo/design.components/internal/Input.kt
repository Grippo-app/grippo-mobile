package com.grippo.design.components.internal

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppTypography

@Immutable
internal sealed class InputStyle {
    data object Default : InputStyle()
    data class Clickable(val onClick: () -> Unit) : InputStyle()
}

@Immutable
internal sealed class InputError {
    data object Non : InputError()
    data class Error(val msg: String) : InputError()
}

@Immutable
internal sealed class PlaceHolder(open val value: String) {
    data class OverInput(override val value: String) : PlaceHolder(value = value)
    data object Empty : PlaceHolder(value = "")
}

@Composable
internal fun Input(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: PlaceHolder,
    enabled: Boolean = true,
    error: InputError = InputError.Non,
    inputStyle: InputStyle = InputStyle.Default,
    textStyle: TextStyle = AppTypography.b13Semi,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leading: (@Composable (color: Color) -> Unit)? = null,
    trailing: (@Composable (color: Color) -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val height = 50.dp
    val paddings = PaddingValues(horizontal = 12.dp)

    val backgroundShape = RoundedCornerShape(12.dp)

    val disabledBackgroundColor = Color.LightGray
    val activeBackgroundColor = Color.White
    val errorColor = Color.Red
    val focusedBorderColor = Color.Gray
    val unfocusedBorderColor = Color.White
    val disabledPlaceholderColor = Color.LightGray
    val activePlaceholderColor = Color.Gray

    val activeContentColor = Color.Gray
    val disabledContentColor = Color.Black

    val hasFocus = remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    if (source.collectIsPressedAsState().value && inputStyle is InputStyle.Clickable) {
        inputStyle.onClick.invoke()
    }

    val borderColor = when {
        error is InputError.Error -> errorColor
        enabled.not() -> Color.Transparent
        hasFocus.value -> focusedBorderColor
        else -> unfocusedBorderColor
    }

    val backgroundColor = when {
        enabled.not() -> disabledBackgroundColor
        else -> activeBackgroundColor
    }

    val placeholderColor = when {
        error is InputError.Error -> errorColor
        enabled.not() -> disabledPlaceholderColor
        else -> activePlaceholderColor
    }

    val contentColor = when {
        error is InputError.Error -> errorColor
        enabled.not() -> disabledContentColor
        else -> activeContentColor
    }

    val textStyleAnimateFraction = animateFloatAsState(
        targetValue = if (hasFocus.value || value.isNotBlank()) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
    )

    Column(modifier = modifier.animateContentSize()) {
        BasicTextField(
            modifier = Modifier
                .clip(backgroundShape)
                .background(shape = backgroundShape, color = backgroundColor)
                .border(shape = backgroundShape, width = 1.dp, color = borderColor)
                .heightIn(min = height)
                .padding(paddings)
                .onFocusChanged { hasFocus.value = it.hasFocus }
                .animateContentSize(),
            value = value,
            onValueChange = onValueChange,
            readOnly = inputStyle is InputStyle.Clickable,
            interactionSource = source,
            minLines = minLines,
            decorationBox = { innerTextField ->
                when (placeholder) {
                    PlaceHolder.Empty -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(intrinsicSize = IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        leading?.invoke(contentColor)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(),
                        ) {
                            innerTextField.invoke()
                        }

                        trailing?.invoke(contentColor)
                    }

                    is PlaceHolder.OverInput -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(intrinsicSize = IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        leading?.invoke(contentColor)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(),
                        ) {
                            Column {
                                Text(
                                    text = placeholder.value,
                                    style = lerp(
                                        start = AppTypography.b13Semi.copy(color = placeholderColor),
                                        stop = AppTypography.b11Semi.copy(color = placeholderColor),
                                        fraction = textStyleAnimateFraction.value,
                                    ),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = maxLines,
                                )

                                if (hasFocus.value || value.isNotBlank()) {
                                    innerTextField.invoke()
                                }
                            }
                        }

                        trailing?.invoke(contentColor)
                    }
                }
            },
            textStyle = textStyle.copy(color = contentColor),
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            maxLines = maxLines,
            singleLine = maxLines == 1,
        )

        if (error is InputError.Error) {
            Text(
                text = error.msg,
                style = AppTypography.b11Semi,
                color = errorColor,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}