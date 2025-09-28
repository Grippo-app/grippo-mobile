package com.grippo.design.resources.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

private typealias AppFont = Res.font

@Stable
public data object AppTypography {

    @Composable
    public fun h1(): TextStyle = TextStyle(
        fontSize = 30.sp,
        fontFamily = manrope(),
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun h2(): TextStyle = TextStyle(
        fontSize = 25.sp,
        fontFamily = manrope(),
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun h3(): TextStyle = TextStyle(
        fontSize = 22.sp,
        fontFamily = manrope(),
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun h4(): TextStyle = TextStyle(
        fontSize = 18.sp,
        fontFamily = manrope(),
        lineHeight = 24.sp, // ×1.33
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun h5(): TextStyle = TextStyle(
        fontSize = 16.sp,
        fontFamily = manrope(),
        lineHeight = 24.sp, // ×1.5
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun h6(): TextStyle = TextStyle(
        fontSize = 15.sp,
        fontFamily = manrope(),
        lineHeight = 22.sp, // ×1.47
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b15Med(): TextStyle = TextStyle(
        fontSize = 15.sp,
        fontFamily = manrope(),
        lineHeight = 22.sp, // ×1.47
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b14Bold(): TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope(),
        lineHeight = 20.sp, // ×1.43
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b14Semi(): TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope(),
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun b14Med(): TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope(),
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b13Semi(): TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope(),
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun b13Bold(): TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope(),
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b13Med(): TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope(),
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b12Semi(): TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope(),
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun b12Med(): TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope(),
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b11Bold(): TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope(),
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b11Semi(): TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope(),
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun b11Med(): TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope(),
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b11Reg(): TextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = manrope(),
        lineHeight = 15.sp,
        fontWeight = FontWeight.Normal,
    )

    @Composable
    public fun b10Bold(): TextStyle = TextStyle(
        fontSize = 10.sp,
        fontFamily = manrope(),
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b10Semi(): TextStyle = TextStyle(
        fontSize = 10.sp,
        fontFamily = manrope(),
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun b10Med(): TextStyle = TextStyle(
        fontSize = 10.sp,
        fontFamily = manrope(),
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b10Reg(): TextStyle = TextStyle(
        fontSize = 10.sp,
        fontFamily = manrope(),
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal,
    )
}

@Composable
internal fun manrope(): FontFamily = FontFamily(
    Font(AppFont.manrope_bold, weight = FontWeight.Bold),
    Font(AppFont.manrope_extra_bold, weight = FontWeight.ExtraBold),
    Font(AppFont.manrope_extra_light, weight = FontWeight.ExtraLight),
    Font(AppFont.manrope_light, weight = FontWeight.Light),
    Font(AppFont.manrope_medium, weight = FontWeight.Medium),
    Font(AppFont.manrope_regular, weight = FontWeight.Normal),
    Font(AppFont.manrope_semi_bold, weight = FontWeight.SemiBold),
)