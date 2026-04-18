package com.grippo.design.components.inputs.core

import com.grippo.core.state.formatters.FormatState

/**
 * Single source of truth for turning a FormatState into the Input's visual error.
 *
 * Convention: only the `Invalid` variant renders as an error. `Empty` and `Valid`
 * are visually neutral. If a specific call-site needs a real message, pass it via
 * `InputError.Error(msg)` directly instead of this helper.
 */
internal fun FormatState<*>.toInputError(): InputError =
    if (this is FormatState.Invalid<*>) InputError.Error("") else InputError.Non
