package com.grippo.design.components.inputs.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
public class InputController internal constructor() {
    internal var moveToEndOnNextFocus by mutableStateOf(false)
    internal var selectAllOnNextFocus by mutableStateOf(false)

    /** Move caret to end once when the field next gains focus. */
    public fun moveCaretToEndOnFocusOnce() {
        selectAllOnNextFocus = false
        moveToEndOnNextFocus = true
    }

    /** Select all text once when the field next gains focus. */
    public fun selectAllOnFocusOnce() {
        moveToEndOnNextFocus = false
        selectAllOnNextFocus = true
    }
}

@Composable
public fun rememberInputController(): InputController = remember { InputController() }