package com.grippo.state.equipments

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class EquipmentGroupState(
    val id: String,
    val name: String,
    val equipments: ImmutableList<EquipmentState>
)

public fun stubEquipments(): ImmutableList<EquipmentGroupState> = persistentListOf(
    EquipmentGroupState(
        id = "56550c17-dd77-40c2-8737-fde011dcbbaa",
        name = "Free Weight",
        equipments = persistentListOf(
            EquipmentState(
                "9d66ac93-3a48-429d-aeaa-54302856e204",
                "Dumbbells",
                EquipmentEnumState.DUMBBELLS
            ),
            EquipmentState(
                "b17ae8af-2d78-4e77-b45b-39253c28247a",
                "Barbell",
                EquipmentEnumState.BARBELL
            ),
            EquipmentState(
                "ad130932-4b2f-4e7b-b3a4-c20b4a6b85ae",
                "EZ Bar",
                EquipmentEnumState.EZ_BAR
            ),
            EquipmentState(
                "21aad68b-b21b-4452-9ebf-7407be8e613d",
                "Trap Bar",
                EquipmentEnumState.TRAP_BAR
            ),
            EquipmentState(
                "15495639-2adb-41b8-899c-493ac0172f57",
                "Straight Bar",
                EquipmentEnumState.STRAIGHT_BAR
            ),
            EquipmentState(
                "331a0c35-f5a5-478d-ba7c-9f14ba2ee0fa",
                "Cord Handles",
                EquipmentEnumState.CORD_HANDLES
            ),
            EquipmentState("af38ec0a-1465-45a8-99ba-a394224530dc", "Rope", EquipmentEnumState.ROPE),
            EquipmentState(
                "524da8cf-0303-4c53-8761-832a5fdb54ed",
                "V Bar",
                EquipmentEnumState.V_BAR
            ),
            EquipmentState(
                "c7c51826-c595-4ae8-9ac4-4421b2afc4ad",
                "Close Grip Handle",
                EquipmentEnumState.CLOSE_GRIP_HANDLE
            ),
            EquipmentState(
                "dec9f53a-7dac-4199-b4ff-ab0624090b8b",
                "Wide Grip Handle",
                EquipmentEnumState.WIDE_GRIP_HANDLE
            )
        )
    ),
    EquipmentGroupState(
        id = "fcfa00b0-820c-494a-ac9e-ff4cf4e69489",
        name = "Machines",
        equipments = persistentListOf(
            EquipmentState(
                "527227fe-8182-4aec-949a-66335c5ce25e",
                "AB Machines",
                EquipmentEnumState.AB_MACHINES
            ),
            EquipmentState(
                "3f2fb6e0-df68-4881-a735-f07ea083aaa7",
                "Butterfly",
                EquipmentEnumState.BUTTERFLY
            ),
            EquipmentState(
                "526347a3-ee32-473d-9b5d-049f526ae48e",
                "Butterfly Reverse",
                EquipmentEnumState.BUTTERFLY_REVERSE
            ),
            EquipmentState(
                "c74a2236-739f-476b-96d9-a11487d4049f",
                "Leg Extension Machines",
                EquipmentEnumState.LEG_EXTENSION_MACHINES
            ),
            EquipmentState(
                "a8a80e95-9165-4200-af80-cd7608099307",
                "Leg Curl Machines",
                EquipmentEnumState.LEG_CURL_MACHINES
            ),
            EquipmentState(
                "79e4532a-afda-421f-9b5f-8c2de5f63ec0",
                "Chest Press Machines",
                EquipmentEnumState.CHEST_PRESS_MACHINES
            ),
            EquipmentState(
                "0d0f8242-be68-4086-b665-0a11ff6a0dcd",
                "Biceps Machines",
                EquipmentEnumState.BICEPS_MACHINES
            ),
            EquipmentState(
                "623e0be7-870a-4bca-b053-76e99c9ea7e0",
                "Smith Machines",
                EquipmentEnumState.SMITH_MACHINES
            ),
            EquipmentState(
                "20e225dd-68d7-409b-9b7d-5ef6d4224d02",
                "Hack Squat Machines",
                EquipmentEnumState.HACK_SQUAT_MACHINES
            )
        )
    ),
    EquipmentGroupState(
        id = "7f343be4-0ec8-458a-a45b-d5ab7c1973de",
        name = "Cable Machines",
        equipments = persistentListOf(
            EquipmentState(
                "18995b62-6971-4750-84fe-0c2bc712f352",
                "Lat Pulldown",
                EquipmentEnumState.LAT_PULLDOWN
            ),
            EquipmentState(
                "752ee7ba-ae88-46f0-95fb-e0a316212f16",
                "Cable",
                EquipmentEnumState.CABLE
            ),
            EquipmentState(
                "a6628e7c-1488-4268-82ee-5174f3a5a2a5",
                "Cable Crossover",
                EquipmentEnumState.CABLE_CROSSOVER
            ),
            EquipmentState(
                "373d04ea-8079-439a-82a3-d118da6253b1",
                "Row Cable",
                EquipmentEnumState.ROW_CABLE
            )
        )
    )
)