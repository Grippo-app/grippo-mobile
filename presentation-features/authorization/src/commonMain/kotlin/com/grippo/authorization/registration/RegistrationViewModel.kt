package com.grippo.authorization.registration

import com.grippo.core.BaseViewModel

internal class RegistrationViewModel :
    BaseViewModel<RegistrationState, RegistrationDirection, RegistrationLoader>(RegistrationState),
    RegistrationContract