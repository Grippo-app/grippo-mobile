package com.grippo.design.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview()
public annotation class AppPreview

@Composable
public fun PreviewContainer(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        AppTheme(darkTheme = false, localeTag = "ua") {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(12.dp),
                content = content,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            )
        }

        AppTheme(darkTheme = true, localeTag = "en") {
            Column(
                modifier = Modifier
                    .background(Color.Black)
                    .padding(12.dp),
                content = content,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            )
        }
    }
}