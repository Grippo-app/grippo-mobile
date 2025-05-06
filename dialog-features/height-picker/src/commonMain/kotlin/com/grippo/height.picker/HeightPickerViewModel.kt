package com.grippo.height.picker

import com.grippo.core.BaseViewModel

public class HeightPickerViewModel :
    BaseViewModel<HeightPickerState, HeightPickerDirection, HeightPickerLoader>(HeightPickerState),
    HeightPickerContract {

    override fun dismiss() {
        navigateTo(HeightPickerDirection.Dismiss)
    }

    override fun submit() {
        navigateTo(HeightPickerDirection.DismissWithResult(123))
    }
}