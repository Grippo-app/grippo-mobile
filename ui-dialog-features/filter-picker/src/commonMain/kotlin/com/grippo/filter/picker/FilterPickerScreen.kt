package com.grippo.filter.picker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.filters
import com.grippo.design.resources.provider.submit_btn
import com.grippo.state.filters.FilterValue
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun FilterPickerScreen(
    state: FilterPickerState,
    loaders: ImmutableSet<FilterPickerLoader>,
    contract: FilterPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.filters),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            items(
                items = state.list,
                key = { it.id },
                contentType = { it::class }
            ) { item ->
                val content = remember(item) { item }

                Text(
                    modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                    text = content.title().text(),
                    style = AppTokens.typography.b14Bold(),
                    color = AppTokens.colors.text.primary,
                )

                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

                when (content) {
                    is FilterValue.Experience -> {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                        ) {
                            content.available.onEach { value ->
                                key(value.ordinal) {
                                    val clickProvider = remember(value.ordinal) {
                                        { contract.onItemClick(content.copy(value = value)) }
                                    }

                                    SelectableCard(
                                        style = SelectableCardStyle.Small(
                                            title = value.title().text()
                                        ),
                                        isSelected = value == content.value,
                                        onSelect = clickProvider
                                    )
                                }
                            }
                        }
                    }

                    is FilterValue.Category -> {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                        ) {
                            content.available.onEach { value ->
                                key(value.ordinal) {
                                    val clickProvider = remember(value.ordinal) {
                                        { contract.onItemClick(content.copy(value = value)) }
                                    }

                                    SelectableCard(
                                        style = SelectableCardStyle.Small(
                                            title = value.title().text()
                                        ),
                                        isSelected = value == content.value,
                                        onSelect = clickProvider
                                    )
                                }
                            }
                        }
                    }

                    is FilterValue.ForceType -> {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                        ) {
                            content.available.onEach { value ->
                                key(value.ordinal) {
                                    val clickProvider = remember(value.ordinal) {
                                        { contract.onItemClick(content.copy(value = value)) }
                                    }

                                    SelectableCard(
                                        style = SelectableCardStyle.Small(
                                            title = value.title().text()
                                        ),
                                        isSelected = value == content.value,
                                        onSelect = clickProvider
                                    )
                                }
                            }
                        }
                    }

                    is FilterValue.WeightType -> {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                        ) {
                            content.available.onEach { value ->
                                key(value.ordinal) {
                                    val clickProvider = remember(value.ordinal) {
                                        { contract.onItemClick(content.copy(value = value)) }
                                    }

                                    SelectableCard(
                                        style = SelectableCardStyle.Small(
                                            title = value.title().text()
                                        ),
                                        isSelected = value == content.value,
                                        onSelect = clickProvider
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.submit_btn),
            ),
            style = ButtonStyle.Primary,
            onClick = contract::onSubmitClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        FilterPickerScreen(
            state = FilterPickerState(
                list = persistentListOf()
            ),
            loaders = persistentSetOf(),
            contract = FilterPickerContract.Empty
        )
    }
}