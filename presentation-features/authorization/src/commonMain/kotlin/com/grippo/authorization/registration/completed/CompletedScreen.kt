package com.grippo.authorization.registration.completed

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.get_started_btn
import com.grippo.design.resources.icons.Check
import com.grippo.design.resources.registration_completed_description
import com.grippo.design.resources.registration_completed_title
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun CompletedScreen(
    state: CompletedState,
    loaders: ImmutableSet<CompletedLoader>,
    contract: CompletedContract
) {
    Crossfade(
        targetState = loaders.contains(CompletedLoader.Registration),
        modifier = Modifier.fillMaxSize(),
    ) { isLoading ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (isLoading) {
                true -> {
                    Loader(modifier = Modifier.fillMaxSize())
                }

                false -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = AppTokens.dp.paddings.screenHorizontal,
                                vertical = AppTokens.dp.paddings.screenVertical
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .shadowDefault(
                                    shape = CircleShape,
                                    elevation = ShadowElevation.Component
                                )
                                .clip(CircleShape)
                                .background(AppTokens.colors.background.secondary)
                                .fillMaxWidth(0.5f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .aspectRatio(1f),
                                imageVector = AppTokens.icons.Check,
                                tint = AppTokens.colors.semantic.success,
                                contentDescription = null
                            )
                        }

                        Spacer(modifier = Modifier.size(24.dp))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = AppTokens.strings.res(
                                Res.string.registration_completed_title,
                                state.name
                            ),
                            style = AppTokens.typography.h2(),
                            color = AppTokens.colors.text.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.size(12.dp))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = AppTokens.strings.res(Res.string.registration_completed_description),
                            style = AppTokens.typography.b14Med(),
                            color = AppTokens.colors.text.secondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            text = AppTokens.strings.res(Res.string.get_started_btn),
                            style = ButtonStyle.Primary,
                            onClick = contract::complete
                        )
                    }
                }
            }
        }
    }
}