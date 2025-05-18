package com.grippo.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.segment.ThumbPosition
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun BottomNavigationScreen(
    pages: Value<ChildPages<BottomNavigationRouter, BottomNavigationComponent.Child>>,
    state: BottomNavigationState,
    loaders: ImmutableSet<BottomNavigationLoader>,
    contract: BottomNavigationContract
) = BaseComposeScreen {
    Column(modifier = Modifier.fillMaxSize()) {
        ChildPages(
            modifier = Modifier.fillMaxWidth().weight(1f),
            pages = pages,
            onPageSelected = contract::selectPage,
            scrollAnimation = PagesScrollAnimation.Default,
            pageContent = { _, page -> page.component.Render() }
        )

        BottomNavigationBar()
    }
}

@Composable
private fun BottomNavigationBar() {
    Row {
        val segmentItems = remember {
            BottomBarMenu.entries
                .map { it.ordinal to it.name }
                .toPersistentList()
        }

        Segment(
            modifier = Modifier
                .fillMaxWidth(),
            items = segmentItems,
            selected = 0,
            onSelect = {},
            segmentWidth = SegmentWidth.EqualFill,
            thumbPosition = ThumbPosition.Top
        )
    }
}