package com.grippo.design.resources

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private typealias AppFont = Res.font

public data object AppTypography {

    val h1: TextStyle = TextStyle(
        fontSize = 32.sp,
        fontFamily = manrope,
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val h2: TextStyle = TextStyle(
        fontSize = 24.sp,
        fontFamily = manrope,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    )

    val h3: TextStyle = TextStyle(
        fontSize = 20.sp,
        fontFamily = manrope,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    )

    val h4: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontFamily = manrope,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    )

    val h5: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontFamily = manrope,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val h6: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontFamily = manrope,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    )

    val b48Reg: TextStyle = TextStyle(
        fontSize = 48.sp,
        fontFamily = manrope,
        lineHeight = 50.sp,
        fontWeight = FontWeight.Normal,
    )

    val b48Semi: TextStyle = TextStyle(
        fontSize = 48.sp,
        fontFamily = manrope,
        lineHeight = 50.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val b15Bold: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontFamily = manrope,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    )

    val b15Semi: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontFamily = manrope,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val b14Bold: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )

    val b14Semi: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val b14Med: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    )

    val b14Reg: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    )

    val b13Bold: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )

    val b13Semi: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val b13Med: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    )

    val b13Reg: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    )

    val b12Bold: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
    )

    val b12Semi: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val b12Med: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )

    val b12Reg: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    )

    val b11Bold: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
    )

    val b11Semi: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope,
        lineHeight = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val b11SEMI: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope,
        lineHeight = 13.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val b11Med: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Medium,
    )

    val b10Med: TextStyle = TextStyle(
        fontSize = 10.sp,
        fontFamily = manrope,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )

    val b9Bold: TextStyle = TextStyle(
        fontSize = 9.sp,
        fontFamily = manrope,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Bold,
    )

    val b9Med: TextStyle = TextStyle(
        fontSize = 9.sp,
        fontFamily = manrope,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Medium,
    )

    val p16Med: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontFamily = manrope,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    )

    val p14Reg: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    )

    val p11Med: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )
}

internal val manrope = FontFamily(
    Font(AppFont.manrope_bold, weight = FontWeight.Bold),
    Font(AppFont.manrope_extra_bold, weight = FontWeight.ExtraBold),
    Font(AppFont.manrope_extra_light, weight = FontWeight.ExtraLight),
    Font(AppFont.manrope_light, weight = FontWeight.Light),
    Font(AppFont.manrope_medium, weight = FontWeight.Medium),
    Font(AppFont.manrope_regular, weight = FontWeight.Normal),
    Font(AppFont.manrope_semi_bold, weight = FontWeight.SemiBold),
)