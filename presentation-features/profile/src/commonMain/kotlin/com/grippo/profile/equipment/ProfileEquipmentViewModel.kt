package com.grippo.profile.equipment

import com.grippo.core.BaseViewModel

internal class ProfileEquipmentViewModel :
    BaseViewModel<ProfileEquipmentState, ProfileEquipmentDirection, ProfileEquipmentLoader>(
        ProfileEquipmentState
    ), ProfileEquipmentContract