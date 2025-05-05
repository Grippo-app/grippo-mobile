package com.grippo.design.components.internal

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
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
import com.grippo.design.core.AppTokens

@Immutable
internal sealed class InputStyle {
    data class Default(
        val onValueChange: (String) -> Unit,
    ) : InputStyle()

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
    placeholder: PlaceHolder,
    enabled: Boolean = true,
    error: InputError = InputError.Non,
    inputStyle: InputStyle,
    textStyle: TextStyle = AppTokens.typography.b13Semi(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leading: (@Composable (color: Color) -> Unit)? = null,
    trailing: (@Composable (color: Color) -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val colors = AppTokens.colors
    val shape = RoundedCornerShape(AppTokens.dp.shape.component)
    val height = AppTokens.dp.size.componentHeight

    val hasFocus = remember { mutableStateOf(false) }

    val borderColor = when {
        error is InputError.Error -> colors.semantic.error
        !enabled -> Color.Transparent
        hasFocus.value -> colors.border.focus
        else -> colors.border.default
    }

    val backgroundColor = when (enabled) {
        true -> colors.input.background
        else -> colors.input.backgroundDisabled
    }

    val placeholderColor = when {
        error is InputError.Error -> colors.semantic.error
        !enabled -> colors.input.placeholderDisabled
        else -> colors.input.placeholder
    }

    val contentColor = when {
        error is InputError.Error -> colors.semantic.error
        !enabled -> colors.input.textDisabled
        else -> colors.input.text
    }

    val textStyleAnimateFraction = animateFloatAsState(
        targetValue = if (hasFocus.value || value.isNotBlank()) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
    )

    Column(modifier = modifier.animateContentSize()) {

        val interactionSource = remember { MutableInteractionSource() }

        if (interactionSource.collectIsPressedAsState().value) {
            (inputStyle as? InputStyle.Clickable)?.onClick?.invoke()
        }

        BasicTextField(
            modifier = Modifier
                .clip(shape)
                .background(color = backgroundColor, shape = shape)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .heightIn(min = height)
                .onFocusChanged { hasFocus.value = it.hasFocus }
                .animateContentSize()
                .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
            value = value,
            onValueChange = when (inputStyle) {
                is InputStyle.Clickable -> {
                    {}
                }

                is InputStyle.Default -> {
                    inputStyle.onValueChange
                }
            },
            readOnly = inputStyle is InputStyle.Clickable,
            minLines = minLines,
            textStyle = textStyle.copy(color = contentColor),
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            decorationBox = { innerTextField ->

                val rowModifier = Modifier
                    .fillMaxWidth()
                    .height(intrinsicSize = IntrinsicSize.Min)

                when (placeholder) {
                    PlaceHolder.Empty -> Row(
                        modifier = rowModifier,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(AppTokens.dp.paddings.componentHorizontal))

                        leading?.invoke(contentColor)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize()
                        ) {
                            innerTextField()
                        }

                        if (trailing != null) {
                            trailing.invoke(contentColor)
                            Spacer(modifier = Modifier.width(AppTokens.dp.paddings.componentHorizontal / 3))
                        } else {
                            Spacer(modifier = Modifier.width(AppTokens.dp.paddings.componentHorizontal))
                        }
                    }

                    is PlaceHolder.OverInput -> Row(
                        modifier = rowModifier,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(AppTokens.dp.paddings.componentHorizontal))

                        leading?.invoke(contentColor)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize()
                        ) {
                            Column {
                                Text(
                                    text = placeholder.value,
                                    style = lerp(
                                        start = AppTokens.typography.b13Semi()
                                            .copy(color = placeholderColor),
                                        stop = AppTokens.typography.b12Semi()
                                            .copy(color = placeholderColor),
                                        fraction = textStyleAnimateFraction.value,
                                    ),
                                    maxLines = maxLines,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (hasFocus.value || value.isNotBlank()) innerTextField()
                            }
                        }


                        if (trailing != null) {
                            trailing.invoke(contentColor)
                            Spacer(modifier = Modifier.width(AppTokens.dp.paddings.componentHorizontal / 3))
                        } else {
                            Spacer(modifier = Modifier.width(AppTokens.dp.paddings.componentHorizontal))
                        }
                    }
                }
            },
        )

        if (error is InputError.Error) {
            Text(
                text = error.msg,
                style = AppTokens.typography.b11Semi(),
                color = colors.semantic.error,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}