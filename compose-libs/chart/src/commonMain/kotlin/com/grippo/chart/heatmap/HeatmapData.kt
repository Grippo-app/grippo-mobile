package com.grippo.chart.heatmap

import androidx.compose.runtime.Immutable

@Immutable
public data class Matrix01 private constructor(
    val rows: Int,
    val cols: Int,
    val values: List<Float>,
) {
    init {
        require(rows > 0) { "rows must be > 0" }
        require(cols > 0) { "cols must be > 0" }
        require(values.size == rows * cols) { "values size must be rows*cols" }
        require(values.all { it in 0f..1f }) { "All values must be in [0,1] range" }
    }

    public operator fun get(r: Int, c: Int): Float {
        require(r in 0 until rows && c in 0 until cols) { "Index out of bounds ($r,$c)" }
        return values[r * cols + c]
    }

    public companion object {
        public fun fromRows(values01: List<List<Float>>): Matrix01 {
            require(values01.isNotEmpty()) { "rows must not be empty" }
            val cols = values01.first().size
            require(cols > 0) { "cols must be > 0" }
            require(values01.all { it.size == cols }) { "All rows must have the same length" }
            val flat = values01.flatten()
            require(flat.all { it in 0f..1f }) { "All values must be in [0,1] range" }
            return Matrix01(values01.size, cols, flat)
        }

        public fun fromFlat(rows: Int, cols: Int, values01: List<Float>): Matrix01 =
            Matrix01(rows, cols, values01.toList())
    }
}

@Immutable
public data class HeatmapData(
    val matrix: Matrix01,
    val rowLabels: List<String> = emptyList(),
    val colLabels: List<String> = emptyList(),
    val rowDim: String? = null,
    val colDim: String? = null,
    val valueUnit: String? = null,
) {
    init {
        require(rowLabels.isEmpty() || rowLabels.size == matrix.rows) {
            "rowLabels must be empty or have size equal to matrix.rows"
        }
        require(colLabels.isEmpty() || colLabels.size == matrix.cols) {
            "colLabels must be empty or have size equal to matrix.cols"
        }
    }

    public companion object {
        public fun fromRows(
            values01: List<List<Float>>,
            rowLabels: List<String> = emptyList(),
            colLabels: List<String> = emptyList(),
            rowDim: String? = null,
            colDim: String? = null,
            valueUnit: String? = null,
        ): HeatmapData {
            val m = Matrix01.fromRows(values01)
            require(rowLabels.isEmpty() || rowLabels.size == m.rows) {
                "rowLabels must be empty or have size ${'$'}{m.rows}"
            }
            require(colLabels.isEmpty() || colLabels.size == m.cols) {
                "colLabels must be empty or have size ${'$'}{m.cols}"
            }
            return HeatmapData(
                matrix = m,
                rowLabels = rowLabels,
                colLabels = colLabels,
                rowDim = rowDim,
                colDim = colDim,
                valueUnit = valueUnit,
            )
        }
    }
}