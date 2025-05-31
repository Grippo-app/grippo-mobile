package com.grippo.design.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

private typealias AppFont = Res.font

public data object AppTypography {

    @Composable
    public fun h1(): TextStyle = TextStyle(
        fontSize = 32.sp,
        fontFamily = manrope(),
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun h2(): TextStyle = TextStyle(
        fontSize = 24.sp,
        fontFamily = manrope(),
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun h3(): TextStyle = TextStyle(
        fontSize = 18.sp,
        fontFamily = manrope(),
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b16Bold(): TextStyle = TextStyle(
        fontSize = 16.sp,
        fontFamily = manrope(),
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b15Med(): TextStyle = TextStyle(
        fontSize = 15.sp,
        fontFamily = manrope(),
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b14Bold(): TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b14Semi(): TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun b14Med(): TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b14Reg(): TextStyle = TextStyle(
        fontSize = 14.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    )

    @Composable
    public fun b13Bold(): TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable
    public fun b13Semi(): TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    public fun b13Med(): TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    )

    @Composable
    public fun b13Reg(): TextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = manrope(),
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    )

    @Composable
    public fun b12Bold(): TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope(),
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
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
    public fun b12Reg(): TextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = manrope(),
        lineHeight = 16.sp,
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