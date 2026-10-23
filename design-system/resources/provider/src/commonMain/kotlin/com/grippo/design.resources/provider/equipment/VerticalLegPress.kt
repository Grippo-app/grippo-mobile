package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _VerticalLegPress: ImageVector? = null

public val VerticalLegPress: ImageVector
    get() {
        val cached = _VerticalLegPress
        if (cached != null) {
            return cached
        }
        val vector = ImageVector.Builder(
            name = "VerticalLegPress",
            defaultWidth = 330.dp,
            defaultHeight = 365.dp,
            viewportWidth = 330f,
            viewportHeight = 365f,
        ).apply {
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 163.4f, y = 197.56f)
                lineTo(x = 177.16f, y = 204.44f)
                lineTo(x = 177.16f, y = 197.44f)
                lineTo(x = 163.4f, y = 190.56f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 163.4f, y = 197.56f)
                lineTo(x = 147f, y = 206.16f)
                lineTo(x = 147f, y = 199.16f)
                lineTo(x = 163.4f, y = 190.56f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 147f, y = 206.16f)
                lineTo(x = 160.76f, y = 213.04f)
                lineTo(x = 160.76f, y = 206.04f)
                lineTo(x = 147f, y = 199.16f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 177.16f, y = 204.44f)
                lineTo(x = 160.76f, y = 213.04f)
                lineTo(x = 160.76f, y = 206.04f)
                lineTo(x = 177.16f, y = 197.44f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 163.4f, y = 190.56f)
                lineTo(x = 177.16f, y = 197.44f)
                lineTo(x = 160.76f, y = 206.04f)
                lineTo(x = 147f, y = 199.16f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 299.28f, y = 265.5f)
                lineTo(x = 313.04f, y = 272.38f)
                lineTo(x = 313.04f, y = 265.38f)
                lineTo(x = 299.28f, y = 258.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 299.28f, y = 265.5f)
                lineTo(x = 282.88f, y = 274.1f)
                lineTo(x = 282.88f, y = 267.1f)
                lineTo(x = 299.28f, y = 258.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 282.88f, y = 274.1f)
                lineTo(x = 296.64f, y = 280.98f)
                lineTo(x = 296.64f, y = 273.98f)
                lineTo(x = 282.88f, y = 267.1f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 313.04f, y = 272.38f)
                lineTo(x = 296.64f, y = 280.98f)
                lineTo(x = 296.64f, y = 273.98f)
                lineTo(x = 313.04f, y = 265.38f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 299.28f, y = 258.5f)
                lineTo(x = 313.04f, y = 265.38f)
                lineTo(x = 296.64f, y = 273.98f)
                lineTo(x = 282.88f, y = 267.1f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 164.38f, y = 194.57f)
                lineTo(x = 307.14f, y = 265.95f)
                lineTo(x = 307.14f, y = 252.95f)
                lineTo(x = 164.38f, y = 181.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 164.38f, y = 194.57f)
                lineTo(x = 152.9f, y = 200.59f)
                lineTo(x = 152.9f, y = 187.59f)
                lineTo(x = 164.38f, y = 181.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 152.9f, y = 200.59f)
                lineTo(x = 295.66f, y = 271.97f)
                lineTo(x = 295.66f, y = 258.97f)
                lineTo(x = 152.9f, y = 187.59f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 307.14f, y = 265.95f)
                lineTo(x = 295.66f, y = 271.97f)
                lineTo(x = 295.66f, y = 258.97f)
                lineTo(x = 307.14f, y = 252.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 164.38f, y = 181.57f)
                lineTo(x = 307.14f, y = 252.95f)
                lineTo(x = 295.66f, y = 258.97f)
                lineTo(x = 152.9f, y = 187.59f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 164.62f, y = 198.73f)
                lineTo(x = 176.66f, y = 204.75f)
                lineTo(x = 176.66f, y = 192.75f)
                lineTo(x = 164.62f, y = 186.73f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 164.62f, y = 198.73f)
                lineTo(x = 21.94f, y = 273.55f)
                lineTo(x = 21.94f, y = 261.55f)
                lineTo(x = 164.62f, y = 186.73f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 21.94f, y = 273.55f)
                lineTo(x = 33.98f, y = 279.57f)
                lineTo(x = 33.98f, y = 267.57f)
                lineTo(x = 21.94f, y = 261.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 176.66f, y = 204.75f)
                lineTo(x = 33.98f, y = 279.57f)
                lineTo(x = 33.98f, y = 267.57f)
                lineTo(x = 176.66f, y = 192.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 164.62f, y = 186.73f)
                lineTo(x = 176.66f, y = 192.75f)
                lineTo(x = 33.98f, y = 267.57f)
                lineTo(x = 21.94f, y = 261.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 285.02f, y = 258.93f)
                lineTo(x = 297.06f, y = 264.95f)
                lineTo(x = 297.06f, y = 252.95f)
                lineTo(x = 285.02f, y = 246.93f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 285.02f, y = 258.93f)
                lineTo(x = 142.34f, y = 333.75f)
                lineTo(x = 142.34f, y = 321.75f)
                lineTo(x = 285.02f, y = 246.93f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 142.34f, y = 333.75f)
                lineTo(x = 154.38f, y = 339.77f)
                lineTo(x = 154.38f, y = 327.77f)
                lineTo(x = 142.34f, y = 321.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 297.06f, y = 264.95f)
                lineTo(x = 154.38f, y = 339.77f)
                lineTo(x = 154.38f, y = 327.77f)
                lineTo(x = 297.06f, y = 252.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 285.02f, y = 246.93f)
                lineTo(x = 297.06f, y = 252.95f)
                lineTo(x = 154.38f, y = 327.77f)
                lineTo(x = 142.34f, y = 321.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 28.18f, y = 270.23f)
                lineTo(x = 41.94f, y = 277.11f)
                lineTo(x = 41.94f, y = 270.11f)
                lineTo(x = 28.18f, y = 263.23f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 28.18f, y = 270.23f)
                lineTo(x = 10.14f, y = 279.69f)
                lineTo(x = 10.14f, y = 272.69f)
                lineTo(x = 28.18f, y = 263.23f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 10.14f, y = 279.69f)
                lineTo(x = 23.9f, y = 286.57f)
                lineTo(x = 23.9f, y = 279.57f)
                lineTo(x = 10.14f, y = 272.69f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 41.94f, y = 277.11f)
                lineTo(x = 23.9f, y = 286.57f)
                lineTo(x = 23.9f, y = 279.57f)
                lineTo(x = 41.94f, y = 270.11f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 28.18f, y = 263.23f)
                lineTo(x = 41.94f, y = 270.11f)
                lineTo(x = 23.9f, y = 279.57f)
                lineTo(x = 10.14f, y = 272.69f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 160.62f, y = 336.45f)
                lineTo(x = 174.38f, y = 343.33f)
                lineTo(x = 174.38f, y = 336.33f)
                lineTo(x = 160.62f, y = 329.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 160.62f, y = 336.45f)
                lineTo(x = 142.58f, y = 345.91f)
                lineTo(x = 142.58f, y = 338.91f)
                lineTo(x = 160.62f, y = 329.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 142.58f, y = 345.91f)
                lineTo(x = 156.34f, y = 352.79f)
                lineTo(x = 156.34f, y = 345.79f)
                lineTo(x = 142.58f, y = 338.91f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 174.38f, y = 343.33f)
                lineTo(x = 156.34f, y = 352.79f)
                lineTo(x = 156.34f, y = 345.79f)
                lineTo(x = 174.38f, y = 336.33f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 160.62f, y = 329.45f)
                lineTo(x = 174.38f, y = 336.33f)
                lineTo(x = 156.34f, y = 345.79f)
                lineTo(x = 142.58f, y = 338.91f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 33.34f, y = 265.81f)
                lineTo(x = 169.22f, y = 333.75f)
                lineTo(x = 169.22f, y = 321.75f)
                lineTo(x = 33.34f, y = 253.81f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 33.34f, y = 265.81f)
                lineTo(x = 21.86f, y = 271.83f)
                lineTo(x = 21.86f, y = 259.83f)
                lineTo(x = 33.34f, y = 253.81f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 21.86f, y = 271.83f)
                lineTo(x = 157.74f, y = 339.77f)
                lineTo(x = 157.74f, y = 327.77f)
                lineTo(x = 21.86f, y = 259.83f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 169.22f, y = 333.75f)
                lineTo(x = 157.74f, y = 339.77f)
                lineTo(x = 157.74f, y = 327.77f)
                lineTo(x = 169.22f, y = 321.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 33.34f, y = 253.81f)
                lineTo(x = 169.22f, y = 321.75f)
                lineTo(x = 157.74f, y = 327.77f)
                lineTo(x = 21.86f, y = 259.83f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 147.9f, y = 226.57f)
                lineTo(x = 223.58f, y = 264.41f)
                lineTo(x = 223.58f, y = 256.41f)
                lineTo(x = 147.9f, y = 218.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 147.9f, y = 226.57f)
                lineTo(x = 137.24f, y = 232.16f)
                lineTo(x = 137.24f, y = 224.16f)
                lineTo(x = 147.9f, y = 218.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 137.24f, y = 232.16f)
                lineTo(x = 212.92f, y = 270f)
                lineTo(x = 212.92f, y = 262f)
                lineTo(x = 137.24f, y = 224.16f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 223.58f, y = 264.41f)
                lineTo(x = 212.92f, y = 270f)
                lineTo(x = 212.92f, y = 262f)
                lineTo(x = 223.58f, y = 256.41f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 147.9f, y = 218.57f)
                lineTo(x = 223.58f, y = 256.41f)
                lineTo(x = 212.92f, y = 262f)
                lineTo(x = 137.24f, y = 224.16f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 88.04f, y = 257.96f)
                lineTo(x = 163.72f, y = 295.8f)
                lineTo(x = 163.72f, y = 287.8f)
                lineTo(x = 88.04f, y = 249.96f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 88.04f, y = 257.96f)
                lineTo(x = 77.38f, y = 263.55f)
                lineTo(x = 77.38f, y = 255.55f)
                lineTo(x = 88.04f, y = 249.96f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 77.38f, y = 263.55f)
                lineTo(x = 153.06f, y = 301.39f)
                lineTo(x = 153.06f, y = 293.39f)
                lineTo(x = 77.38f, y = 255.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 163.72f, y = 295.8f)
                lineTo(x = 153.06f, y = 301.39f)
                lineTo(x = 153.06f, y = 293.39f)
                lineTo(x = 163.72f, y = 287.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 88.04f, y = 249.96f)
                lineTo(x = 163.72f, y = 287.8f)
                lineTo(x = 153.06f, y = 293.39f)
                lineTo(x = 77.38f, y = 255.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 158.38f, y = 232.17f)
                lineTo(x = 169.56f, y = 237.76f)
                lineTo(x = 169.56f, y = 226.76f)
                lineTo(x = 158.38f, y = 221.17f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 158.38f, y = 232.17f)
                lineTo(x = 147.72f, y = 237.76f)
                lineTo(x = 147.72f, y = 226.76f)
                lineTo(x = 158.38f, y = 221.17f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 147.72f, y = 237.76f)
                lineTo(x = 158.9f, y = 243.35f)
                lineTo(x = 158.9f, y = 232.35f)
                lineTo(x = 147.72f, y = 226.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 169.56f, y = 237.76f)
                lineTo(x = 158.9f, y = 243.35f)
                lineTo(x = 158.9f, y = 232.35f)
                lineTo(x = 169.56f, y = 226.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 158.38f, y = 221.17f)
                lineTo(x = 169.56f, y = 226.76f)
                lineTo(x = 158.9f, y = 232.35f)
                lineTo(x = 147.72f, y = 226.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 195.36f, y = 250.66f)
                lineTo(x = 206.54f, y = 256.25f)
                lineTo(x = 206.54f, y = 245.25f)
                lineTo(x = 195.36f, y = 239.66f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 195.36f, y = 250.66f)
                lineTo(x = 184.7f, y = 256.25f)
                lineTo(x = 184.7f, y = 245.25f)
                lineTo(x = 195.36f, y = 239.66f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 184.7f, y = 256.25f)
                lineTo(x = 195.88f, y = 261.84f)
                lineTo(x = 195.88f, y = 250.84f)
                lineTo(x = 184.7f, y = 245.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 206.54f, y = 256.25f)
                lineTo(x = 195.88f, y = 261.84f)
                lineTo(x = 195.88f, y = 250.84f)
                lineTo(x = 206.54f, y = 245.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 195.36f, y = 239.66f)
                lineTo(x = 206.54f, y = 245.25f)
                lineTo(x = 195.88f, y = 250.84f)
                lineTo(x = 184.7f, y = 245.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 104.26f, y = 260.55f)
                lineTo(x = 115.44f, y = 266.14f)
                lineTo(x = 115.44f, y = 255.14f)
                lineTo(x = 104.26f, y = 249.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 104.26f, y = 260.55f)
                lineTo(x = 93.6f, y = 266.14f)
                lineTo(x = 93.6f, y = 255.14f)
                lineTo(x = 104.26f, y = 249.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 93.6f, y = 266.14f)
                lineTo(x = 104.78f, y = 271.73f)
                lineTo(x = 104.78f, y = 260.73f)
                lineTo(x = 93.6f, y = 255.14f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 115.44f, y = 266.14f)
                lineTo(x = 104.78f, y = 271.73f)
                lineTo(x = 104.78f, y = 260.73f)
                lineTo(x = 115.44f, y = 255.14f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 104.26f, y = 249.55f)
                lineTo(x = 115.44f, y = 255.14f)
                lineTo(x = 104.78f, y = 260.73f)
                lineTo(x = 93.6f, y = 255.14f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 141.24f, y = 279.04f)
                lineTo(x = 152.42f, y = 284.63f)
                lineTo(x = 152.42f, y = 273.63f)
                lineTo(x = 141.24f, y = 268.04f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 141.24f, y = 279.04f)
                lineTo(x = 130.58f, y = 284.63f)
                lineTo(x = 130.58f, y = 273.63f)
                lineTo(x = 141.24f, y = 268.04f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 130.58f, y = 284.63f)
                lineTo(x = 141.76f, y = 290.22f)
                lineTo(x = 141.76f, y = 279.22f)
                lineTo(x = 130.58f, y = 273.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 152.42f, y = 284.63f)
                lineTo(x = 141.76f, y = 290.22f)
                lineTo(x = 141.76f, y = 279.22f)
                lineTo(x = 152.42f, y = 273.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 141.24f, y = 268.04f)
                lineTo(x = 152.42f, y = 273.63f)
                lineTo(x = 141.76f, y = 279.22f)
                lineTo(x = 130.58f, y = 273.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 94.12f, y = 277.32f)
                lineTo(x = 119.92f, y = 290.22f)
                lineTo(x = 119.92f, y = 277.22f)
                lineTo(x = 94.12f, y = 264.32f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 94.12f, y = 277.32f)
                lineTo(x = 83.46f, y = 282.91f)
                lineTo(x = 83.46f, y = 269.91f)
                lineTo(x = 94.12f, y = 264.32f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 83.46f, y = 282.91f)
                lineTo(x = 109.26f, y = 295.81f)
                lineTo(x = 109.26f, y = 282.81f)
                lineTo(x = 83.46f, y = 269.91f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 119.92f, y = 290.22f)
                lineTo(x = 109.26f, y = 295.81f)
                lineTo(x = 109.26f, y = 282.81f)
                lineTo(x = 119.92f, y = 277.22f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 94.12f, y = 264.32f)
                lineTo(x = 119.92f, y = 277.22f)
                lineTo(x = 109.26f, y = 282.81f)
                lineTo(x = 83.46f, y = 269.91f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 196.08f, y = 231.02f)
                lineTo(x = 209.84f, y = 237.9f)
                lineTo(x = 209.84f, y = 226.9f)
                lineTo(x = 196.08f, y = 220.02f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 196.08f, y = 231.02f)
                lineTo(x = 91.94f, y = 285.63f)
                lineTo(x = 91.94f, y = 274.63f)
                lineTo(x = 196.08f, y = 220.02f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 91.94f, y = 285.63f)
                lineTo(x = 105.7f, y = 292.51f)
                lineTo(x = 105.7f, y = 281.51f)
                lineTo(x = 91.94f, y = 274.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 209.84f, y = 237.9f)
                lineTo(x = 105.7f, y = 292.51f)
                lineTo(x = 105.7f, y = 281.51f)
                lineTo(x = 209.84f, y = 226.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 196.08f, y = 220.02f)
                lineTo(x = 209.84f, y = 226.9f)
                lineTo(x = 105.7f, y = 281.51f)
                lineTo(x = 91.94f, y = 274.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 163.84f, y = 188.02f)
                lineTo(x = 177.6f, y = 194.9f)
                lineTo(x = 177.6f, y = 95.9f)
                lineTo(x = 163.84f, y = 89.02f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 163.84f, y = 188.02f)
                lineTo(x = 148.26f, y = 196.19f)
                lineTo(x = 148.26f, y = 97.19f)
                lineTo(x = 163.84f, y = 89.02f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 148.26f, y = 196.19f)
                lineTo(x = 162.02f, y = 203.07f)
                lineTo(x = 162.02f, y = 104.07f)
                lineTo(x = 148.26f, y = 97.19f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 177.6f, y = 194.9f)
                lineTo(x = 162.02f, y = 203.07f)
                lineTo(x = 162.02f, y = 104.07f)
                lineTo(x = 177.6f, y = 95.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 163.84f, y = 89.02f)
                lineTo(x = 177.6f, y = 95.9f)
                lineTo(x = 162.02f, y = 104.07f)
                lineTo(x = 148.26f, y = 97.19f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 148.26f, y = 195.19f)
                lineTo(x = 126.94f, y = 206.37f)
                lineTo(x = 148.26f, y = 167.19f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 148.26f, y = 195.19f)
                lineTo(x = 153.42f, y = 197.77f)
                lineTo(x = 153.42f, y = 169.77f)
                lineTo(x = 148.26f, y = 167.19f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 163.18f, y = 92.89f)
                lineTo(x = 169.2f, y = 95.9f)
                lineTo(x = 169.2f, y = 29.9f)
                lineTo(x = 163.18f, y = 26.89f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 163.18f, y = 92.89f)
                lineTo(x = 155.8f, y = 96.76f)
                lineTo(x = 155.8f, y = 30.76f)
                lineTo(x = 163.18f, y = 26.89f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 155.8f, y = 96.76f)
                lineTo(x = 161.82f, y = 99.77f)
                lineTo(x = 161.82f, y = 33.77f)
                lineTo(x = 155.8f, y = 30.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 169.2f, y = 95.9f)
                lineTo(x = 161.82f, y = 99.77f)
                lineTo(x = 161.82f, y = 33.77f)
                lineTo(x = 169.2f, y = 29.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 163.18f, y = 26.89f)
                lineTo(x = 169.2f, y = 29.9f)
                lineTo(x = 161.82f, y = 33.77f)
                lineTo(x = 155.8f, y = 30.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 163.1f, y = 95.17f)
                lineTo(x = 172.56f, y = 99.9f)
                lineTo(x = 172.56f, y = 92.9f)
                lineTo(x = 163.1f, y = 88.17f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 163.1f, y = 95.17f)
                lineTo(x = 152.44f, y = 100.76f)
                lineTo(x = 152.44f, y = 93.76f)
                lineTo(x = 163.1f, y = 88.17f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 152.44f, y = 100.76f)
                lineTo(x = 161.9f, y = 105.49f)
                lineTo(x = 161.9f, y = 98.49f)
                lineTo(x = 152.44f, y = 93.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 172.56f, y = 99.9f)
                lineTo(x = 161.9f, y = 105.49f)
                lineTo(x = 161.9f, y = 98.49f)
                lineTo(x = 172.56f, y = 92.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 163.1f, y = 88.17f)
                lineTo(x = 172.56f, y = 92.9f)
                lineTo(x = 161.9f, y = 98.49f)
                lineTo(x = 152.44f, y = 93.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 151.73f, y = 168.61f)
                curveTo(x1 = 151.73f, y1 = 167.67f, x2 = 152.38f, y2 = 166.91f, x3 = 153.18f, y3 = 166.91f)
                curveTo(x1 = 153.98f, y1 = 166.91f, x2 = 154.63f, y2 = 167.67f, x3 = 154.63f, y3 = 168.61f)
                curveTo(x1 = 154.63f, y1 = 169.55f, x2 = 153.98f, y2 = 170.31f, x3 = 153.18f, y3 = 170.31f)
                curveTo(x1 = 152.38f, y1 = 170.31f, x2 = 151.73f, y2 = 169.55f, x3 = 151.73f, y3 = 168.61f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 151.73f, y = 155.61f)
                curveTo(x1 = 151.73f, y1 = 154.67f, x2 = 152.38f, y2 = 153.91f, x3 = 153.18f, y3 = 153.91f)
                curveTo(x1 = 153.98f, y1 = 153.91f, x2 = 154.63f, y2 = 154.67f, x3 = 154.63f, y3 = 155.61f)
                curveTo(x1 = 154.63f, y1 = 156.55f, x2 = 153.98f, y2 = 157.31f, x3 = 153.18f, y3 = 157.31f)
                curveTo(x1 = 152.38f, y1 = 157.31f, x2 = 151.73f, y2 = 156.55f, x3 = 151.73f, y3 = 155.61f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 151.73f, y = 142.61f)
                curveTo(x1 = 151.73f, y1 = 141.67f, x2 = 152.38f, y2 = 140.91f, x3 = 153.18f, y3 = 140.91f)
                curveTo(x1 = 153.98f, y1 = 140.91f, x2 = 154.63f, y2 = 141.67f, x3 = 154.63f, y3 = 142.61f)
                curveTo(x1 = 154.63f, y1 = 143.55f, x2 = 153.98f, y2 = 144.31f, x3 = 153.18f, y3 = 144.31f)
                curveTo(x1 = 152.38f, y1 = 144.31f, x2 = 151.73f, y2 = 143.55f, x3 = 151.73f, y3 = 142.61f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 151.73f, y = 129.61f)
                curveTo(x1 = 151.73f, y1 = 128.67f, x2 = 152.38f, y2 = 127.91f, x3 = 153.18f, y3 = 127.91f)
                curveTo(x1 = 153.98f, y1 = 127.91f, x2 = 154.63f, y2 = 128.67f, x3 = 154.63f, y3 = 129.61f)
                curveTo(x1 = 154.63f, y1 = 130.55f, x2 = 153.98f, y2 = 131.31f, x3 = 153.18f, y3 = 131.31f)
                curveTo(x1 = 152.38f, y1 = 131.31f, x2 = 151.73f, y2 = 130.55f, x3 = 151.73f, y3 = 129.61f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 151.73f, y = 116.61f)
                curveTo(x1 = 151.73f, y1 = 115.67f, x2 = 152.38f, y2 = 114.91f, x3 = 153.18f, y3 = 114.91f)
                curveTo(x1 = 153.98f, y1 = 114.91f, x2 = 154.63f, y2 = 115.67f, x3 = 154.63f, y3 = 116.61f)
                curveTo(x1 = 154.63f, y1 = 117.55f, x2 = 153.98f, y2 = 118.31f, x3 = 153.18f, y3 = 118.31f)
                curveTo(x1 = 152.38f, y1 = 118.31f, x2 = 151.73f, y2 = 117.55f, x3 = 151.73f, y3 = 116.61f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N400),
                strokeLineWidth = 3f,
            ) {
                moveTo(x = 149.82f, y = 122.61f)
                lineTo(x = 140.36f, y = 117.88f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N200),
                strokeLineWidth = 4f,
            ) {
                moveTo(x = 140.36f, y = 117.88f)
                lineTo(x = 134.62f, y = 120.89f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 173.18f, y = 216.29f)
                lineTo(x = 219.62f, y = 239.51f)
                curveTo(x1 = 224.37f, y1 = 241.88f, x2 = 224.55f, y2 = 245.74f, x3 = 220.02f, y3 = 248.11f)
                lineTo(x = 158.52f, y = 280.36f)
                curveTo(x1 = 153.99f, y1 = 282.73f, x2 = 146.47f, y2 = 282.73f, x3 = 141.72f, y3 = 280.36f)
                lineTo(x = 95.28f, y = 257.14f)
                curveTo(x1 = 90.53f, y1 = 254.77f, x2 = 90.35f, y2 = 250.91f, x3 = 94.88f, y3 = 248.54f)
                lineTo(x = 156.38f, y = 216.29f)
                curveTo(x1 = 160.91f, y1 = 213.92f, x2 = 168.43f, y2 = 213.92f, x3 = 173.18f, y3 = 216.29f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 220.02f, y = 238.11f)
                lineTo(x = 158.52f, y = 270.36f)
                lineTo(x = 158.52f, y = 280.36f)
                lineTo(x = 220.02f, y = 248.11f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 95.28f, y = 247.14f)
                lineTo(x = 141.72f, y = 270.36f)
                lineTo(x = 141.72f, y = 280.36f)
                lineTo(x = 95.28f, y = 257.14f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 173.18f, y = 206.29f)
                lineTo(x = 219.62f, y = 229.51f)
                curveTo(x1 = 224.37f, y1 = 231.88f, x2 = 224.55f, y2 = 235.74f, x3 = 220.02f, y3 = 238.11f)
                lineTo(x = 158.52f, y = 270.36f)
                curveTo(x1 = 153.99f, y1 = 272.73f, x2 = 146.47f, y2 = 272.73f, x3 = 141.72f, y3 = 270.36f)
                lineTo(x = 95.28f, y = 247.14f)
                curveTo(x1 = 90.53f, y1 = 244.77f, x2 = 90.35f, y2 = 240.91f, x3 = 94.88f, y3 = 238.54f)
                lineTo(x = 156.38f, y = 206.29f)
                curveTo(x1 = 160.91f, y1 = 203.92f, x2 = 168.43f, y2 = 203.92f, x3 = 173.18f, y3 = 206.29f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N450),
                strokeLineWidth = 0.9f,
            ) {
                moveTo(x = 160.76f, y = 209.66f)
                lineTo(x = 102.54f, y = 240.19f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N400),
                strokeLineWidth = 0.7f,
            ) {
                moveTo(x = 211.54f, y = 235.89f)
                lineTo(x = 154.14f, y = 265.99f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 95.48f, y = 258.44f)
                lineTo(x = 133.32f, y = 277.36f)
                curveTo(x1 = 138.07f, y1 = 279.73f, x2 = 138.25f, y2 = 283.59f, x3 = 133.72f, y3 = 285.96f)
                lineTo(x = 123.88f, y = 291.12f)
                curveTo(x1 = 119.35f, y1 = 293.49f, x2 = 111.83f, y2 = 293.49f, x3 = 107.08f, y3 = 291.12f)
                lineTo(x = 69.24f, y = 272.2f)
                curveTo(x1 = 64.49f, y1 = 269.83f, x2 = 64.31f, y2 = 265.97f, x3 = 68.84f, y3 = 263.6f)
                lineTo(x = 78.68f, y = 258.44f)
                curveTo(x1 = 83.21f, y1 = 256.07f, x2 = 90.73f, y2 = 256.07f, x3 = 95.48f, y3 = 258.44f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 133.72f, y = 271.96f)
                lineTo(x = 123.88f, y = 277.12f)
                lineTo(x = 123.88f, y = 291.12f)
                lineTo(x = 133.72f, y = 285.96f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 69.24f, y = 258.2f)
                lineTo(x = 107.08f, y = 277.12f)
                lineTo(x = 107.08f, y = 291.12f)
                lineTo(x = 69.24f, y = 272.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 95.48f, y = 244.44f)
                lineTo(x = 133.32f, y = 263.36f)
                curveTo(x1 = 138.07f, y1 = 265.73f, x2 = 138.25f, y2 = 269.59f, x3 = 133.72f, y3 = 271.96f)
                lineTo(x = 123.88f, y = 277.12f)
                curveTo(x1 = 119.35f, y1 = 279.49f, x2 = 111.83f, y2 = 279.49f, x3 = 107.08f, y3 = 277.12f)
                lineTo(x = 69.24f, y = 258.2f)
                curveTo(x1 = 64.49f, y1 = 255.83f, x2 = 64.31f, y2 = 251.97f, x3 = 68.84f, y3 = 249.6f)
                lineTo(x = 78.68f, y = 244.44f)
                curveTo(x1 = 83.21f, y1 = 242.07f, x2 = 90.73f, y2 = 242.07f, x3 = 95.48f, y3 = 244.44f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N450),
                strokeLineWidth = 0.8f,
            ) {
                moveTo(x = 89.7f, y = 246.29f)
                lineTo(x = 129.26f, y = 266.07f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeLineWidth = 4f,
            ) {
                moveTo(x = 98.74f, y = 243.23f)
                lineTo(x = 92.72f, y = 228.22f)
                lineTo(x = 72.22f, y = 238.97f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N200),
                strokeLineWidth = 5f,
            ) {
                moveTo(x = 85.34f, y = 232.09f)
                lineTo(x = 73.86f, y = 238.11f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N650),
                strokeLineWidth = 4f,
            ) {
                moveTo(x = 172.7f, y = 280.21f)
                lineTo(x = 178.72f, y = 271.22f)
                lineTo(x = 158.22f, y = 281.97f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N200),
                strokeLineWidth = 5f,
            ) {
                moveTo(x = 171.34f, y = 275.09f)
                lineTo(x = 159.86f, y = 281.11f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 280.8f, y = 246.5f)
                lineTo(x = 294.56f, y = 253.38f)
                lineTo(x = 294.56f, y = 154.38f)
                lineTo(x = 280.8f, y = 147.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 280.8f, y = 246.5f)
                lineTo(x = 265.22f, y = 254.67f)
                lineTo(x = 265.22f, y = 155.67f)
                lineTo(x = 280.8f, y = 147.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 265.22f, y = 254.67f)
                lineTo(x = 278.98f, y = 261.55f)
                lineTo(x = 278.98f, y = 162.55f)
                lineTo(x = 265.22f, y = 155.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 294.56f, y = 253.38f)
                lineTo(x = 278.98f, y = 261.55f)
                lineTo(x = 278.98f, y = 162.55f)
                lineTo(x = 294.56f, y = 154.38f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 280.8f, y = 147.5f)
                lineTo(x = 294.56f, y = 154.38f)
                lineTo(x = 278.98f, y = 162.55f)
                lineTo(x = 265.22f, y = 155.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 278.98f, y = 260.55f)
                lineTo(x = 257.66f, y = 271.73f)
                lineTo(x = 278.98f, y = 232.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 273.82f, y = 257.97f)
                lineTo(x = 278.98f, y = 260.55f)
                lineTo(x = 278.98f, y = 232.55f)
                lineTo(x = 273.82f, y = 229.97f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 281f, y = 151.8f)
                lineTo(x = 287.02f, y = 154.81f)
                lineTo(x = 287.02f, y = 88.81f)
                lineTo(x = 281f, y = 85.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 281f, y = 151.8f)
                lineTo(x = 273.62f, y = 155.67f)
                lineTo(x = 273.62f, y = 89.67f)
                lineTo(x = 281f, y = 85.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 273.62f, y = 155.67f)
                lineTo(x = 279.64f, y = 158.68f)
                lineTo(x = 279.64f, y = 92.68f)
                lineTo(x = 273.62f, y = 89.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 287.02f, y = 154.81f)
                lineTo(x = 279.64f, y = 158.68f)
                lineTo(x = 279.64f, y = 92.68f)
                lineTo(x = 287.02f, y = 88.81f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 281f, y = 85.8f)
                lineTo(x = 287.02f, y = 88.81f)
                lineTo(x = 279.64f, y = 92.68f)
                lineTo(x = 273.62f, y = 89.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 280.92f, y = 154.08f)
                lineTo(x = 290.38f, y = 158.81f)
                lineTo(x = 290.38f, y = 151.81f)
                lineTo(x = 280.92f, y = 147.08f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 280.92f, y = 154.08f)
                lineTo(x = 270.26f, y = 159.67f)
                lineTo(x = 270.26f, y = 152.67f)
                lineTo(x = 280.92f, y = 147.08f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 270.26f, y = 159.67f)
                lineTo(x = 279.72f, y = 164.4f)
                lineTo(x = 279.72f, y = 157.4f)
                lineTo(x = 270.26f, y = 152.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 290.38f, y = 158.81f)
                lineTo(x = 279.72f, y = 164.4f)
                lineTo(x = 279.72f, y = 157.4f)
                lineTo(x = 290.38f, y = 151.81f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 280.92f, y = 147.08f)
                lineTo(x = 290.38f, y = 151.81f)
                lineTo(x = 279.72f, y = 157.4f)
                lineTo(x = 270.26f, y = 152.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 282.5f, y = 233.97f)
                curveTo(x1 = 282.5f, y1 = 233.09f, x2 = 283.13f, y2 = 232.37f, x3 = 283.9f, y3 = 232.37f)
                curveTo(x1 = 284.67f, y1 = 232.37f, x2 = 285.3f, y2 = 233.09f, x3 = 285.3f, y3 = 233.97f)
                curveTo(x1 = 285.3f, y1 = 234.85f, x2 = 284.67f, y2 = 235.57f, x3 = 283.9f, y3 = 235.57f)
                curveTo(x1 = 283.13f, y1 = 235.57f, x2 = 282.5f, y2 = 234.85f, x3 = 282.5f, y3 = 233.97f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 282.5f, y = 220.97f)
                curveTo(x1 = 282.5f, y1 = 220.09f, x2 = 283.13f, y2 = 219.37f, x3 = 283.9f, y3 = 219.37f)
                curveTo(x1 = 284.67f, y1 = 219.37f, x2 = 285.3f, y2 = 220.09f, x3 = 285.3f, y3 = 220.97f)
                curveTo(x1 = 285.3f, y1 = 221.85f, x2 = 284.67f, y2 = 222.57f, x3 = 283.9f, y3 = 222.57f)
                curveTo(x1 = 283.13f, y1 = 222.57f, x2 = 282.5f, y2 = 221.85f, x3 = 282.5f, y3 = 220.97f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 282.5f, y = 207.97f)
                curveTo(x1 = 282.5f, y1 = 207.09f, x2 = 283.13f, y2 = 206.37f, x3 = 283.9f, y3 = 206.37f)
                curveTo(x1 = 284.67f, y1 = 206.37f, x2 = 285.3f, y2 = 207.09f, x3 = 285.3f, y3 = 207.97f)
                curveTo(x1 = 285.3f, y1 = 208.85f, x2 = 284.67f, y2 = 209.57f, x3 = 283.9f, y3 = 209.57f)
                curveTo(x1 = 283.13f, y1 = 209.57f, x2 = 282.5f, y2 = 208.85f, x3 = 282.5f, y3 = 207.97f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 282.5f, y = 194.97f)
                curveTo(x1 = 282.5f, y1 = 194.09f, x2 = 283.13f, y2 = 193.37f, x3 = 283.9f, y3 = 193.37f)
                curveTo(x1 = 284.67f, y1 = 193.37f, x2 = 285.3f, y2 = 194.09f, x3 = 285.3f, y3 = 194.97f)
                curveTo(x1 = 285.3f, y1 = 195.85f, x2 = 284.67f, y2 = 196.57f, x3 = 283.9f, y3 = 196.57f)
                curveTo(x1 = 283.13f, y1 = 196.57f, x2 = 282.5f, y2 = 195.85f, x3 = 282.5f, y3 = 194.97f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 282.5f, y = 181.97f)
                curveTo(x1 = 282.5f, y1 = 181.09f, x2 = 283.13f, y2 = 180.37f, x3 = 283.9f, y3 = 180.37f)
                curveTo(x1 = 284.67f, y1 = 180.37f, x2 = 285.3f, y2 = 181.09f, x3 = 285.3f, y3 = 181.97f)
                curveTo(x1 = 285.3f, y1 = 182.85f, x2 = 284.67f, y2 = 183.57f, x3 = 283.9f, y3 = 183.57f)
                curveTo(x1 = 283.13f, y1 = 183.57f, x2 = 282.5f, y2 = 182.85f, x3 = 282.5f, y3 = 181.97f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeLineWidth = 3f,
            ) {
                moveTo(x = 283.12f, y = 189.26f)
                lineTo(x = 295.16f, y = 195.28f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N200),
                strokeLineWidth = 5f,
            ) {
                moveTo(x = 295.16f, y = 195.28f)
                lineTo(x = 288.6f, y = 198.72f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 164.78f, y = 52.17f)
                lineTo(x = 290.34f, y = 114.95f)
                lineTo(x = 290.34f, y = 104.95f)
                lineTo(x = 164.78f, y = 42.17f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 164.78f, y = 52.17f)
                lineTo(x = 151.66f, y = 59.05f)
                lineTo(x = 151.66f, y = 49.05f)
                lineTo(x = 164.78f, y = 42.17f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 151.66f, y = 59.05f)
                lineTo(x = 277.22f, y = 121.83f)
                lineTo(x = 277.22f, y = 111.83f)
                lineTo(x = 151.66f, y = 49.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 290.34f, y = 114.95f)
                lineTo(x = 277.22f, y = 121.83f)
                lineTo(x = 277.22f, y = 111.83f)
                lineTo(x = 290.34f, y = 104.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 164.78f, y = 42.17f)
                lineTo(x = 290.34f, y = 104.95f)
                lineTo(x = 277.22f, y = 111.83f)
                lineTo(x = 151.66f, y = 49.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 163.02f, y = 54.45f)
                lineTo(x = 175.92f, y = 60.9f)
                lineTo(x = 175.92f, y = 33.9f)
                lineTo(x = 163.02f, y = 27.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 163.02f, y = 54.45f)
                lineTo(x = 149.08f, y = 61.76f)
                lineTo(x = 149.08f, y = 34.76f)
                lineTo(x = 163.02f, y = 27.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 149.08f, y = 61.76f)
                lineTo(x = 161.98f, y = 68.21f)
                lineTo(x = 161.98f, y = 41.21f)
                lineTo(x = 149.08f, y = 34.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 175.92f, y = 60.9f)
                lineTo(x = 161.98f, y = 68.21f)
                lineTo(x = 161.98f, y = 41.21f)
                lineTo(x = 175.92f, y = 33.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 163.02f, y = 27.45f)
                lineTo(x = 175.92f, y = 33.9f)
                lineTo(x = 161.98f, y = 41.21f)
                lineTo(x = 149.08f, y = 34.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 280.84f, y = 113.36f)
                lineTo(x = 293.74f, y = 119.81f)
                lineTo(x = 293.74f, y = 92.81f)
                lineTo(x = 280.84f, y = 86.36f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 280.84f, y = 113.36f)
                lineTo(x = 266.9f, y = 120.67f)
                lineTo(x = 266.9f, y = 93.67f)
                lineTo(x = 280.84f, y = 86.36f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 266.9f, y = 120.67f)
                lineTo(x = 279.8f, y = 127.12f)
                lineTo(x = 279.8f, y = 100.12f)
                lineTo(x = 266.9f, y = 93.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 293.74f, y = 119.81f)
                lineTo(x = 279.8f, y = 127.12f)
                lineTo(x = 279.8f, y = 100.12f)
                lineTo(x = 293.74f, y = 92.81f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 280.84f, y = 86.36f)
                lineTo(x = 293.74f, y = 92.81f)
                lineTo(x = 279.8f, y = 100.12f)
                lineTo(x = 266.9f, y = 93.67f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 188.92f, y = 41.44f)
                lineTo(x = 299f, y = 96.48f)
                lineTo(x = 299f, y = 89.48f)
                lineTo(x = 188.92f, y = 34.44f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 188.92f, y = 41.44f)
                lineTo(x = 139.72f, y = 67.24f)
                lineTo(x = 139.72f, y = 60.24f)
                lineTo(x = 188.92f, y = 34.44f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(x = 139.72f, y = 67.24f)
                lineTo(x = 249.8f, y = 122.28f)
                lineTo(x = 249.8f, y = 115.28f)
                lineTo(x = 139.72f, y = 60.24f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 299f, y = 96.48f)
                lineTo(x = 249.8f, y = 122.28f)
                lineTo(x = 249.8f, y = 115.28f)
                lineTo(x = 299f, y = 89.48f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 188.92f, y = 34.44f)
                lineTo(x = 299f, y = 89.48f)
                lineTo(x = 249.8f, y = 115.28f)
                lineTo(x = 139.72f, y = 60.24f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 141.36f, y = 59.08f)
                lineTo(x = 251.44f, y = 114.12f)
                lineTo(x = 249.8f, y = 115.28f)
                lineTo(x = 139.72f, y = 60.24f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N650),
            ) {
                moveTo(x = 188.92f, y = 34.44f)
                lineTo(x = 190.64f, y = 35f)
                lineTo(x = 143.08f, y = 59.94f)
                lineTo(x = 139.72f, y = 60.24f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeLineWidth = 1.1f,
            ) {
                moveTo(x = 198.58f, y = 43.17f)
                lineTo(x = 157.58f, y = 64.67f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeLineWidth = 1.1f,
            ) {
                moveTo(x = 212.34f, y = 50.05f)
                lineTo(x = 171.34f, y = 71.55f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeLineWidth = 1.1f,
            ) {
                moveTo(x = 226.1f, y = 56.93f)
                lineTo(x = 185.1f, y = 78.43f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeLineWidth = 1.1f,
            ) {
                moveTo(x = 239.86f, y = 63.81f)
                lineTo(x = 198.86f, y = 85.31f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeLineWidth = 1.1f,
            ) {
                moveTo(x = 253.62f, y = 70.69f)
                lineTo(x = 212.62f, y = 92.19f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeLineWidth = 1.1f,
            ) {
                moveTo(x = 267.38f, y = 77.57f)
                lineTo(x = 226.38f, y = 99.07f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeLineWidth = 1.1f,
            ) {
                moveTo(x = 281.14f, y = 84.45f)
                lineTo(x = 240.14f, y = 105.95f)
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 2f,
            ) {
                moveTo(x = 142.26f, y = 68.67f)
                lineTo(x = 248.9f, y = 121.99f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 155.95f, y = 54.51f)
                curveTo(x1 = 155.95f, y1 = 62.9f, x2 = 169.27f, y2 = 69.71f, x3 = 185.7f, y3 = 69.71f)
                curveTo(x1 = 202.13f, y1 = 69.71f, x2 = 215.45f, y2 = 62.9f, x3 = 215.45f, y3 = 54.51f)
                lineTo(x = 215.45f, y = 50.51f)
                curveTo(x1 = 215.45f, y1 = 58.9f, x2 = 202.13f, y2 = 65.71f, x3 = 185.7f, y3 = 65.71f)
                curveTo(x1 = 169.27f, y1 = 65.71f, x2 = 155.95f, y2 = 58.9f, x3 = 155.95f, y3 = 50.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 155.95f, y = 50.51f)
                curveTo(x1 = 155.95f, y1 = 42.12f, x2 = 169.27f, y2 = 35.31f, x3 = 185.7f, y3 = 35.31f)
                curveTo(x1 = 202.13f, y1 = 35.31f, x2 = 215.45f, y2 = 42.12f, x3 = 215.45f, y3 = 50.51f)
                curveTo(x1 = 215.45f, y1 = 58.9f, x2 = 202.13f, y2 = 65.71f, x3 = 185.7f, y3 = 65.71f)
                curveTo(x1 = 169.27f, y1 = 65.71f, x2 = 155.95f, y2 = 58.9f, x3 = 155.95f, y3 = 50.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 155.95f, y = 49.51f)
                curveTo(x1 = 155.95f, y1 = 57.9f, x2 = 169.27f, y2 = 64.71f, x3 = 185.7f, y3 = 64.71f)
                curveTo(x1 = 202.13f, y1 = 64.71f, x2 = 215.45f, y2 = 57.9f, x3 = 215.45f, y3 = 49.51f)
                lineTo(x = 215.45f, y = 45.51f)
                curveTo(x1 = 215.45f, y1 = 53.9f, x2 = 202.13f, y2 = 60.71f, x3 = 185.7f, y3 = 60.71f)
                curveTo(x1 = 169.27f, y1 = 60.71f, x2 = 155.95f, y2 = 53.9f, x3 = 155.95f, y3 = 45.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 155.95f, y = 45.51f)
                curveTo(x1 = 155.95f, y1 = 37.12f, x2 = 169.27f, y2 = 30.31f, x3 = 185.7f, y3 = 30.31f)
                curveTo(x1 = 202.13f, y1 = 30.31f, x2 = 215.45f, y2 = 37.12f, x3 = 215.45f, y3 = 45.51f)
                curveTo(x1 = 215.45f, y1 = 53.9f, x2 = 202.13f, y2 = 60.71f, x3 = 185.7f, y3 = 60.71f)
                curveTo(x1 = 169.27f, y1 = 60.71f, x2 = 155.95f, y2 = 53.9f, x3 = 155.95f, y3 = 45.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 155.95f, y = 44.51f)
                curveTo(x1 = 155.95f, y1 = 52.9f, x2 = 169.27f, y2 = 59.71f, x3 = 185.7f, y3 = 59.71f)
                curveTo(x1 = 202.13f, y1 = 59.71f, x2 = 215.45f, y2 = 52.9f, x3 = 215.45f, y3 = 44.51f)
                lineTo(x = 215.45f, y = 40.51f)
                curveTo(x1 = 215.45f, y1 = 48.9f, x2 = 202.13f, y2 = 55.71f, x3 = 185.7f, y3 = 55.71f)
                curveTo(x1 = 169.27f, y1 = 55.71f, x2 = 155.95f, y2 = 48.9f, x3 = 155.95f, y3 = 40.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 155.95f, y = 40.51f)
                curveTo(x1 = 155.95f, y1 = 32.12f, x2 = 169.27f, y2 = 25.31f, x3 = 185.7f, y3 = 25.31f)
                curveTo(x1 = 202.13f, y1 = 25.31f, x2 = 215.45f, y2 = 32.12f, x3 = 215.45f, y3 = 40.51f)
                curveTo(x1 = 215.45f, y1 = 48.9f, x2 = 202.13f, y2 = 55.71f, x3 = 185.7f, y3 = 55.71f)
                curveTo(x1 = 169.27f, y1 = 55.71f, x2 = 155.95f, y2 = 48.9f, x3 = 155.95f, y3 = 40.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 155.95f, y = 39.51f)
                curveTo(x1 = 155.95f, y1 = 47.9f, x2 = 169.27f, y2 = 54.71f, x3 = 185.7f, y3 = 54.71f)
                curveTo(x1 = 202.13f, y1 = 54.71f, x2 = 215.45f, y2 = 47.9f, x3 = 215.45f, y3 = 39.51f)
                lineTo(x = 215.45f, y = 35.51f)
                curveTo(x1 = 215.45f, y1 = 43.9f, x2 = 202.13f, y2 = 50.71f, x3 = 185.7f, y3 = 50.71f)
                curveTo(x1 = 169.27f, y1 = 50.71f, x2 = 155.95f, y2 = 43.9f, x3 = 155.95f, y3 = 35.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 155.95f, y = 35.51f)
                curveTo(x1 = 155.95f, y1 = 27.12f, x2 = 169.27f, y2 = 20.31f, x3 = 185.7f, y3 = 20.31f)
                curveTo(x1 = 202.13f, y1 = 20.31f, x2 = 215.45f, y2 = 27.12f, x3 = 215.45f, y3 = 35.51f)
                curveTo(x1 = 215.45f, y1 = 43.9f, x2 = 202.13f, y2 = 50.71f, x3 = 185.7f, y3 = 50.71f)
                curveTo(x1 = 169.27f, y1 = 50.71f, x2 = 155.95f, y2 = 43.9f, x3 = 155.95f, y3 = 35.51f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N500),
                strokeLineWidth = 0.8f,
            ) {
                moveTo(x = 161.7f, y = 35.51f)
                curveTo(x1 = 161.7f, y1 = 29.1f, x2 = 172.45f, y2 = 23.91f, x3 = 185.7f, y3 = 23.91f)
                curveTo(x1 = 198.95f, y1 = 23.91f, x2 = 209.7f, y2 = 29.1f, x3 = 209.7f, y3 = 35.51f)
                curveTo(x1 = 209.7f, y1 = 41.92f, x2 = 198.95f, y2 = 47.11f, x3 = 185.7f, y3 = 47.11f)
                curveTo(x1 = 172.45f, y1 = 47.11f, x2 = 161.7f, y2 = 41.92f, x3 = 161.7f, y3 = 35.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 180.1f, y = 35.51f)
                curveTo(x1 = 180.1f, y1 = 33.91f, x2 = 182.61f, y2 = 32.61f, x3 = 185.7f, y3 = 32.61f)
                curveTo(x1 = 188.79f, y1 = 32.61f, x2 = 191.3f, y2 = 33.91f, x3 = 191.3f, y3 = 35.51f)
                curveTo(x1 = 191.3f, y1 = 37.11f, x2 = 188.79f, y2 = 38.41f, x3 = 185.7f, y3 = 38.41f)
                curveTo(x1 = 182.61f, y1 = 38.41f, x2 = 180.1f, y2 = 37.11f, x3 = 180.1f, y3 = 35.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 185.62f, y = 33.79f)
                lineTo(x = 189.06f, y = 35.51f)
                lineTo(x = 189.06f, y = 12.51f)
                lineTo(x = 185.62f, y = 10.79f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 185.62f, y = 33.79f)
                lineTo(x = 182.34f, y = 35.51f)
                lineTo(x = 182.34f, y = 12.51f)
                lineTo(x = 185.62f, y = 10.79f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 182.34f, y = 35.51f)
                lineTo(x = 185.78f, y = 37.23f)
                lineTo(x = 185.78f, y = 14.23f)
                lineTo(x = 182.34f, y = 12.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 189.06f, y = 35.51f)
                lineTo(x = 185.78f, y = 37.23f)
                lineTo(x = 185.78f, y = 14.23f)
                lineTo(x = 189.06f, y = 12.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 185.62f, y = 10.79f)
                lineTo(x = 189.06f, y = 12.51f)
                lineTo(x = 185.78f, y = 14.23f)
                lineTo(x = 182.34f, y = 12.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 228.19f, y = 90.63f)
                curveTo(x1 = 228.19f, y1 = 99.02f, x2 = 241.51f, y2 = 105.83f, x3 = 257.94f, y3 = 105.83f)
                curveTo(x1 = 274.37f, y1 = 105.83f, x2 = 287.69f, y2 = 99.02f, x3 = 287.69f, y3 = 90.63f)
                lineTo(x = 287.69f, y = 86.63f)
                curveTo(x1 = 287.69f, y1 = 95.02f, x2 = 274.37f, y2 = 101.83f, x3 = 257.94f, y3 = 101.83f)
                curveTo(x1 = 241.51f, y1 = 101.83f, x2 = 228.19f, y2 = 95.02f, x3 = 228.19f, y3 = 86.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 228.19f, y = 86.63f)
                curveTo(x1 = 228.19f, y1 = 78.24f, x2 = 241.51f, y2 = 71.43f, x3 = 257.94f, y3 = 71.43f)
                curveTo(x1 = 274.37f, y1 = 71.43f, x2 = 287.69f, y2 = 78.24f, x3 = 287.69f, y3 = 86.63f)
                curveTo(x1 = 287.69f, y1 = 95.02f, x2 = 274.37f, y2 = 101.83f, x3 = 257.94f, y3 = 101.83f)
                curveTo(x1 = 241.51f, y1 = 101.83f, x2 = 228.19f, y2 = 95.02f, x3 = 228.19f, y3 = 86.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 228.19f, y = 85.63f)
                curveTo(x1 = 228.19f, y1 = 94.02f, x2 = 241.51f, y2 = 100.83f, x3 = 257.94f, y3 = 100.83f)
                curveTo(x1 = 274.37f, y1 = 100.83f, x2 = 287.69f, y2 = 94.02f, x3 = 287.69f, y3 = 85.63f)
                lineTo(x = 287.69f, y = 81.63f)
                curveTo(x1 = 287.69f, y1 = 90.02f, x2 = 274.37f, y2 = 96.83f, x3 = 257.94f, y3 = 96.83f)
                curveTo(x1 = 241.51f, y1 = 96.83f, x2 = 228.19f, y2 = 90.02f, x3 = 228.19f, y3 = 81.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 228.19f, y = 81.63f)
                curveTo(x1 = 228.19f, y1 = 73.24f, x2 = 241.51f, y2 = 66.43f, x3 = 257.94f, y3 = 66.43f)
                curveTo(x1 = 274.37f, y1 = 66.43f, x2 = 287.69f, y2 = 73.24f, x3 = 287.69f, y3 = 81.63f)
                curveTo(x1 = 287.69f, y1 = 90.02f, x2 = 274.37f, y2 = 96.83f, x3 = 257.94f, y3 = 96.83f)
                curveTo(x1 = 241.51f, y1 = 96.83f, x2 = 228.19f, y2 = 90.02f, x3 = 228.19f, y3 = 81.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(x = 228.19f, y = 80.63f)
                curveTo(x1 = 228.19f, y1 = 89.02f, x2 = 241.51f, y2 = 95.83f, x3 = 257.94f, y3 = 95.83f)
                curveTo(x1 = 274.37f, y1 = 95.83f, x2 = 287.69f, y2 = 89.02f, x3 = 287.69f, y3 = 80.63f)
                lineTo(x = 287.69f, y = 76.63f)
                curveTo(x1 = 287.69f, y1 = 85.02f, x2 = 274.37f, y2 = 91.83f, x3 = 257.94f, y3 = 91.83f)
                curveTo(x1 = 241.51f, y1 = 91.83f, x2 = 228.19f, y2 = 85.02f, x3 = 228.19f, y3 = 76.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(x = 228.19f, y = 76.63f)
                curveTo(x1 = 228.19f, y1 = 68.24f, x2 = 241.51f, y2 = 61.43f, x3 = 257.94f, y3 = 61.43f)
                curveTo(x1 = 274.37f, y1 = 61.43f, x2 = 287.69f, y2 = 68.24f, x3 = 287.69f, y3 = 76.63f)
                curveTo(x1 = 287.69f, y1 = 85.02f, x2 = 274.37f, y2 = 91.83f, x3 = 257.94f, y3 = 91.83f)
                curveTo(x1 = 241.51f, y1 = 91.83f, x2 = 228.19f, y2 = 85.02f, x3 = 228.19f, y3 = 76.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 228.19f, y = 75.63f)
                curveTo(x1 = 228.19f, y1 = 84.02f, x2 = 241.51f, y2 = 90.83f, x3 = 257.94f, y3 = 90.83f)
                curveTo(x1 = 274.37f, y1 = 90.83f, x2 = 287.69f, y2 = 84.02f, x3 = 287.69f, y3 = 75.63f)
                lineTo(x = 287.69f, y = 71.63f)
                curveTo(x1 = 287.69f, y1 = 80.02f, x2 = 274.37f, y2 = 86.83f, x3 = 257.94f, y3 = 86.83f)
                curveTo(x1 = 241.51f, y1 = 86.83f, x2 = 228.19f, y2 = 80.02f, x3 = 228.19f, y3 = 71.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 228.19f, y = 71.63f)
                curveTo(x1 = 228.19f, y1 = 63.24f, x2 = 241.51f, y2 = 56.43f, x3 = 257.94f, y3 = 56.43f)
                curveTo(x1 = 274.37f, y1 = 56.43f, x2 = 287.69f, y2 = 63.24f, x3 = 287.69f, y3 = 71.63f)
                curveTo(x1 = 287.69f, y1 = 80.02f, x2 = 274.37f, y2 = 86.83f, x3 = 257.94f, y3 = 86.83f)
                curveTo(x1 = 241.51f, y1 = 86.83f, x2 = 228.19f, y2 = 80.02f, x3 = 228.19f, y3 = 71.63f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(AppPalette.NeutralDark.N500),
                strokeLineWidth = 0.8f,
            ) {
                moveTo(x = 233.94f, y = 71.63f)
                curveTo(x1 = 233.94f, y1 = 65.22f, x2 = 244.69f, y2 = 60.03f, x3 = 257.94f, y3 = 60.03f)
                curveTo(x1 = 271.19f, y1 = 60.03f, x2 = 281.94f, y2 = 65.22f, x3 = 281.94f, y3 = 71.63f)
                curveTo(x1 = 281.94f, y1 = 78.04f, x2 = 271.19f, y2 = 83.23f, x3 = 257.94f, y3 = 83.23f)
                curveTo(x1 = 244.69f, y1 = 83.23f, x2 = 233.94f, y2 = 78.04f, x3 = 233.94f, y3 = 71.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N150),
            ) {
                moveTo(x = 252.34f, y = 71.63f)
                curveTo(x1 = 252.34f, y1 = 70.03f, x2 = 254.85f, y2 = 68.73f, x3 = 257.94f, y3 = 68.73f)
                curveTo(x1 = 261.03f, y1 = 68.73f, x2 = 263.54f, y2 = 70.03f, x3 = 263.54f, y3 = 71.63f)
                curveTo(x1 = 263.54f, y1 = 73.23f, x2 = 261.03f, y2 = 74.53f, x3 = 257.94f, y3 = 74.53f)
                curveTo(x1 = 254.85f, y1 = 74.53f, x2 = 252.34f, y2 = 73.23f, x3 = 252.34f, y3 = 71.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 257.86f, y = 69.91f)
                lineTo(x = 261.3f, y = 71.63f)
                lineTo(x = 261.3f, y = 48.63f)
                lineTo(x = 257.86f, y = 46.91f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 257.86f, y = 69.91f)
                lineTo(x = 254.58f, y = 71.63f)
                lineTo(x = 254.58f, y = 48.63f)
                lineTo(x = 257.86f, y = 46.91f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 254.58f, y = 71.63f)
                lineTo(x = 258.02f, y = 73.35f)
                lineTo(x = 258.02f, y = 50.35f)
                lineTo(x = 254.58f, y = 48.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 261.3f, y = 71.63f)
                lineTo(x = 258.02f, y = 73.35f)
                lineTo(x = 258.02f, y = 50.35f)
                lineTo(x = 261.3f, y = 48.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 257.86f, y = 46.91f)
                lineTo(x = 261.3f, y = 48.63f)
                lineTo(x = 258.02f, y = 50.35f)
                lineTo(x = 254.58f, y = 48.63f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(x = 214.68f, y = 72.57f)
                curveTo(x1 = 214.68f, y1 = 74.58f, x2 = 217.88f, y2 = 76.22f, x3 = 221.82f, y3 = 76.22f)
                curveTo(x1 = 225.76f, y1 = 76.22f, x2 = 228.96f, y2 = 74.58f, x3 = 228.96f, y3 = 72.57f)
                lineTo(x = 228.96f, y = 70.57f)
                curveTo(x1 = 228.96f, y1 = 72.58f, x2 = 225.76f, y2 = 74.22f, x3 = 221.82f, y3 = 74.22f)
                curveTo(x1 = 217.88f, y1 = 74.22f, x2 = 214.68f, y2 = 72.58f, x3 = 214.68f, y3 = 70.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(x = 214.68f, y = 70.57f)
                curveTo(x1 = 214.68f, y1 = 68.56f, x2 = 217.88f, y2 = 66.92f, x3 = 221.82f, y3 = 66.92f)
                curveTo(x1 = 225.76f, y1 = 66.92f, x2 = 228.96f, y2 = 68.56f, x3 = 228.96f, y3 = 70.57f)
                curveTo(x1 = 228.96f, y1 = 72.58f, x2 = 225.76f, y2 = 74.22f, x3 = 221.82f, y3 = 74.22f)
                curveTo(x1 = 217.88f, y1 = 74.22f, x2 = 214.68f, y2 = 72.58f, x3 = 214.68f, y3 = 70.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 221.74f, y = 68.85f)
                lineTo(x = 225.18f, y = 70.57f)
                lineTo(x = 225.18f, y = 39.57f)
                lineTo(x = 221.74f, y = 37.85f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 221.74f, y = 68.85f)
                lineTo(x = 218.46f, y = 70.57f)
                lineTo(x = 218.46f, y = 39.57f)
                lineTo(x = 221.74f, y = 37.85f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
            ) {
                moveTo(x = 218.46f, y = 70.57f)
                lineTo(x = 221.9f, y = 72.29f)
                lineTo(x = 221.9f, y = 41.29f)
                lineTo(x = 218.46f, y = 39.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(x = 225.18f, y = 70.57f)
                lineTo(x = 221.9f, y = 72.29f)
                lineTo(x = 221.9f, y = 41.29f)
                lineTo(x = 225.18f, y = 39.57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N800),
            ) {
                moveTo(x = 221.74f, y = 37.85f)
                lineTo(x = 225.18f, y = 39.57f)
                lineTo(x = 221.9f, y = 41.29f)
                lineTo(x = 218.46f, y = 39.57f)
                close()
            }
        }.build()
        _VerticalLegPress = vector
        return vector
    }
