package com.example.design

import androidx.compose.ui.graphics.Color

interface AppColor {
    val primary: Color
    val background: Color
    val outline: Color
    val error: Error
    val warning: Warning

    interface Error {
        val content: Color
        val container: Color
    }

    interface Warning {
        val content: Color
    }
}
