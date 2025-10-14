package com.grippo.shared.dialog.content

import com.grippo.core.foundation.BaseViewModel

internal class DialogContentViewModel :
    BaseViewModel<DialogContentState, DialogContentDirection, DialogContentLoader>(
        DialogContentState
    ), DialogContentContract {

    override fun onBack(pendingResult: (() -> Unit)?) {
        navigateTo(DialogContentDirection.Back(pendingResult))
    }
}