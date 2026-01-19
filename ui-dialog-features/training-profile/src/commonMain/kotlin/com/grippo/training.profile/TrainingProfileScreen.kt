package com.grippo.training.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.stubTrainingLoadProfile
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.TrainingLoadProfileCard
import com.grippo.design.components.metrics.TrainingLoadProfileDetailsCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingProfileScreen(
    state: TrainingProfileDialogState,
    loaders: ImmutableSet<TrainingProfileLoader>,
    contract: TrainingProfileContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        val baseTitle = AppTokens.strings.res(Res.string.training_profile)

        val title = state.range.label()?.let { "$baseTitle • $it" } ?: baseTitle

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.range.formatted(),
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        if (loaders.contains(TrainingProfileLoader.Content)) {
            Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = AppTokens.dp.dialog.horizontalPadding,
                    end = AppTokens.dp.dialog.horizontalPadding,
                    top = AppTokens.dp.contentPadding.content,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
            ) {
                state.profile?.let { profile ->
                    item(key = "summary") {
                        TrainingLoadProfileCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = profile,
                        )
                    }

                    item(key = "details") {
                        TrainingLoadProfileDetailsCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = profile,
                        )
                    }
                }

                item("bottom_space") {
                    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingProfileScreen(
            state = TrainingProfileDialogState(
                range = DateRange.Range.Last7Days().range,
                profile = stubTrainingLoadProfile()
            ),
            loaders = persistentSetOf(),
            contract = TrainingProfileContract.Empty
        )
    }
}
