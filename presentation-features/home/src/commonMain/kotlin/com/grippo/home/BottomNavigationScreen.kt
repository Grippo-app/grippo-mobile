package com.grippo.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.segment.ThumbPosition
import com.grippo.design.core.AppTokens
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun BottomNavigationScreen(
    pages: Value<ChildPages<BottomNavigationRouter, BottomNavigationComponent.Child>>,
    state: BottomNavigationState,
    loaders: ImmutableSet<BottomNavigationLoader>,
    contract: BottomNavigationContract
) = BaseComposeScreen {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        ChildPages(
            modifier = Modifier.fillMaxWidth().weight(1f),
            pages = pages,
            onPageSelected = contract::selectPage,
            scrollAnimation = PagesScrollAnimation.Default,
            pageContent = { _, page -> page.component.Render() }
        )

        Segment(
            modifier = Modifier
                .shadowDefault(
                    shape = RoundedCornerShape(0.dp),
                    elevation = ShadowElevation.Container
                )
                .background(AppTokens.colors.background.secondary)
                .navigationBarsPadding()
                .fillMaxWidth(),
            items = state.segments,
            selected = state.selectedIndex,
            onSelect = contract::selectPage,
            segmentWidth = SegmentWidth.EqualFill,
            thumbPosition = ThumbPosition.Top
        )
    }
}