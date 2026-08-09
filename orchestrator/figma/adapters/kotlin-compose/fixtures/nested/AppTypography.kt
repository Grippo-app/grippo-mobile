package com.example.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

object AppType {
    val body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    val display = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.W700, fontFamily = Inter)
    val broken = TextStyle(16.sp)
}
