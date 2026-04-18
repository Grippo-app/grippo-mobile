package com.grippo.design.components.inputs.core

import com.grippo.core.state.formatters.FormatState

internal fun FormatState<*>.toInputError(): InputError =
    if (this is FormatState.Invalid<*>) InputError.Error("") else InputError.Non
