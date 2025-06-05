package com.grippo.profile.equipments

import com.grippo.core.BaseViewModel

internal class ProfileEquipmentsViewModel :
    BaseViewModel<ProfileEquipmentsState, ProfileEquipmentsDirection, ProfileEquipmentsLoader>(
        ProfileEquipmentsState
    ), ProfileEquipmentsContract