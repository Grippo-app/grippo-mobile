package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color

internal object AppColorTokens {

    object Common {
        val White = Color(0xFFFAFAFA)
        val Black = Color(0xFF121212)
        val Accent = Color(0xFF3366FF)
    }

    object Semantic {
        val Error = Color(0xFFFF4D4F)
        val Warning = Color(0xFFFFAA2C)
        val Success = Color(0xFF3AC86B)
    }

    object Primary {
        val P100 = Color(0xFFE6ECFF)
        val P150 = Color(0xFFD4DFFF)
        val P200 = Color(0xFFC2D1FF)
        val P250 = Color(0xFFB0C4FF)
        val P300 = Color(0xFF99B2FF)
        val P400 = Color(0xFF7090FF)
        val P500 = Color(0xFF3366FF)
        val P600 = Color(0xFF2B57E6)
        val P700 = Color(0xFF244ACC)
        val P800 = Color(0xFF1C3DAF)
        val P900 = Color(0xFF152F94)
    }

    object NeutralLight {
        val N050 = Color(0xFFFAFBFC)
        val N100 = Color(0xFFEDEFF1)
        val N150 = Color(0xFFE7E9ED)
        val N200 = Color(0xFFD9DDE4)
        val N250 = Color(0xFFCBD0D8)
        val N300 = Color(0xFFADB3BF)
        val N400 = Color(0xFF939AAA)
        val N500 = Color(0xFF798292)
        val N550 = Color(0xFF6B7484)
        val N600 = Color(0xFF606A7B)
        val N700 = Color(0xFF2F353F)
        val N800 = Color(0xFF15181F)
    }

    object NeutralDark {
        val N050 = Color(0xFF0F1115)
        val N100 = Color(0xFF181A1F)
        val N150 = Color(0xFF1F2228)
        val N200 = Color(0xFF262A31)
        val N250 = Color(0xFF2E333B)
        val N300 = Color(0xFF373D47)
        val N400 = Color(0xFF4C5566)
        val N500 = Color(0xFF6A768C)
        val N550 = Color(0xFF7F8CA3)
        val N600 = Color(0xFFA1ADC3)
        val N700 = Color(0xFFBECADC)
        val N800 = Color(0xFFF4F7FA)
    }

    object Chart {

        object SurfaceDark {
            val Grid = Color(0x22FFFFFF)
            val Axis = Color(0x33FFFFFF)
            val LabelPrimary = Color(0x77FFFFFF)
            val LabelSecondary = Color(0x66FFFFFF)
            val ExtremaLabel = Color(0xCCFFFFFF)
        }

        object SurfaceLight {
            val Grid = Color(0x22000000)
            val Axis = Color(0x33000000)
            val LabelPrimary = Color(0x77000000)
            val LabelSecondary = Color(0x66000000)
            val ExtremaLabel = Color(0xCC000000)
        }

        object Area {
            val LineA = Color(0xFF00E6A7)
            val LineB = Color(0xFF6AA9FF)
            val FillBase = LineA
            val Glow = LineA
            val Dot = LineA
        }

        object Pie {
            val LabelTextOnDark = Color(0xCCFFFFFF)
            val LabelTextOnLight = Color(0xCC000000)
            val LeaderOnDark: Color? = null
            val LeaderOnLight: Color? = null
        }

        object Progress {
            val TrackOnDark = Color(0x14FFFFFF)
            val StrokeOnDark = Color(0x22FFFFFF)
            val TargetOnDark = Color(0x44FFFFFF)

            val TrackOnLight = Color(0x14000000)
            val StrokeOnLight = Color(0x22000000)
            val TargetOnLight = Color(0x44000000)

            val Palette: List<Color> = listOf(
                Color(0xFF6AA9FF), Color(0xFF00E6A7), Color(0xFFFF7A33), Color(0xFFB049F8),
                Color(0xFFFFC53D), Color(0xFF3A86FF), Color(0xFFFF5E8A), Color(0xFF2B7FFF),
                Color(0xFF33D2FF), Color(0xFFA56FFF), Color(0xFFAF5CF7), Color(0xFF64B5F6)
            )
        }

        object Bar {
            val StrokeOnDark = SurfaceDark.Axis
            val StrokeOnLight = SurfaceLight.Axis
        }

        object Heatmap {
            val StopsCoolWarm: List<Pair<Float, Color>> = listOf(
                0.00f to Color(0xFF1E66F5),
                0.08f to Color(0xFF2B7FFF),
                0.16f to Color(0xFF4592FF),
                0.24f to Color(0xFF62A8FF),
                0.32f to Color(0xFF82BEFF),
                0.40f to Color(0xFFA2D0FF),
                0.48f to Color(0xFFC2E0FF),
                0.56f to Color(0xFFFFE1C2),
                0.64f to Color(0xFFFFD0A0),
                0.72f to Color(0xFFFFBA7D),
                0.80f to Color(0xFFFFA15E),
                0.88f to Color(0xFFFF8A45),
                0.96f to Color(0xFFFF7539),
                1.00f to Color(0xFFFF6A33)
            )

            object Dark {
                val Missing = Color(0x11FFFFFF)
                val Border = Color(0x22FFFFFF)
                val ValueText = Common.White
            }

            object Light {
                val Missing = Color(0x11000000)
                val Border = Color(0x22000000)
                val ValueText = Common.Black
            }
        }

        object Categorical {
            val Palette12 = listOf(
                Color(0xFF6AA9FF), Color(0xFF00E6A7), Color(0xFFFF7A33), Color(0xFFB049F8),
                Color(0xFFFFC53D), Color(0xFF3A86FF), Color(0xFFFF5E8A), Color(0xFF58D68D),
                Color(0xFF45B39D), Color(0xFFAF7AC5), Color(0xFFF1948A), Color(0xFF5DADE2)
            )

            val Palette24 = listOf(
                Color(0xFF6AA9FF), Color(0xFF00E6A7), Color(0xFFFF7A33), Color(0xFFB049F8),
                Color(0xFFFFC53D), Color(0xFF3A86FF), Color(0xFFFF5E8A), Color(0xFF58D68D),
                Color(0xFF45B39D), Color(0xFFAF7AC5), Color(0xFFF1948A), Color(0xFF5DADE2),
                Color(0xFF52BE80), Color(0xFF48C9B0), Color(0xFFA569BD), Color(0xFFEB984E),
                Color(0xFFF7DC6F), Color(0xFF85C1E9), Color(0xFFD98880), Color(0xFF7FB3D5),
                Color(0xFF73C6B6), Color(0xFFD2B4DE), Color(0xFFA3E4D7), Color(0xFFF8C471)
            )

            val Palette32 = listOf(
                Color(0xFF6AA9FF), Color(0xFF00E6A7), Color(0xFFFF7A33), Color(0xFFB049F8),
                Color(0xFFFFC53D), Color(0xFF3A86FF), Color(0xFFFF5E8A), Color(0xFF58D68D),
                Color(0xFF45B39D), Color(0xFFAF7AC5), Color(0xFFF1948A), Color(0xFF5DADE2),
                Color(0xFF52BE80), Color(0xFF48C9B0), Color(0xFFA569BD), Color(0xFFEB984E),
                Color(0xFFF7DC6F), Color(0xFF85C1E9), Color(0xFFD98880), Color(0xFF7FB3D5),
                Color(0xFF73C6B6), Color(0xFFD2B4DE), Color(0xFFA3E4D7), Color(0xFFF8C471),
                Color(0xFF2ECC71), Color(0xFF1ABC9C), Color(0xFF9B59B6), Color(0xFFE67E22),
                Color(0xFFF1C40F), Color(0xFF3498DB), Color(0xFFE84393), Color(0xFF16A085)
            )
        }
    }
}