package com.grippo.state.muscles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.resources.provider.muscles.bodyBack
import com.grippo.design.resources.provider.muscles.bodyFront
import com.grippo.design.resources.provider.muscles.bodySplit
import com.grippo.design.resources.provider.muscles.legsSplit
import com.grippo.state.muscles.factory.MuscleColorStrategy
import com.grippo.state.muscles.factory.MuscleEngine
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class MuscleGroupState<T : MuscleRepresentationState>(
    val id: String,
    val name: String,
    val muscles: ImmutableList<T>,
    val type: MuscleGroupEnumState,
) {
    @Composable
    public fun image(
        selectedIds: ImmutableList<String>
    ): ImageVector {
        val preset = MuscleEngine.generatePreset(
            MuscleColorStrategy.BySelection(
                group = this,
                selectedIds = selectedIds.toImmutableSet()
            )
        )

        return when (type) {
            MuscleGroupEnumState.CHEST_MUSCLES -> bodyFront(preset)
            MuscleGroupEnumState.BACK_MUSCLES -> bodyBack(preset)
            MuscleGroupEnumState.ABDOMINAL_MUSCLES -> bodyFront(preset)
            MuscleGroupEnumState.LEGS -> legsSplit(preset)
            MuscleGroupEnumState.ARMS_AND_FOREARMS -> bodySplit(preset)
            MuscleGroupEnumState.SHOULDER_MUSCLES -> bodySplit(preset)
        }
    }
}

public fun stubMuscles(): PersistentList<MuscleGroupState<MuscleRepresentationState.Plain>> {
    return persistentListOf(
        MuscleGroupState(
            id = "4289bf91-51d8-40b0-9aca-66780584a4eb",
            name = "Back Muscles",
            type = MuscleGroupEnumState.BACK_MUSCLES,
            muscles = listOf(
                MuscleState(
                    id = "2e0faf2b-31a5-4c63-ac15-454be132796f",
                    type = MuscleEnumState.TRAPEZIUS
                ),
                MuscleState(
                    id = "af854064-078a-4f50-af1d-8744e866751e",
                    type = MuscleEnumState.RHOMBOIDS
                ),
                MuscleState(
                    id = "831f39bd-80a8-4d11-9964-bde1788abae1",
                    type = MuscleEnumState.LATISSIMUS_DORSI
                ),
                MuscleState(
                    id = "be38dcef-1bc8-487b-a44f-96df1ab9e68c",
                    type = MuscleEnumState.TERES_MAJOR
                ),
            ).map { MuscleRepresentationState.Plain(it) }.toPersistentList()
        ),
        MuscleGroupState(
            id = "e1f1e456-3f8d-46f6-b7ba-75bb4b8c3802",
            name = "Abdominal Muscles",
            type = MuscleGroupEnumState.ABDOMINAL_MUSCLES,
            muscles = listOf(
                MuscleState(
                    id = "1ddbb748-37a6-4d66-a7d4-4957bdbc647f",
                    type = MuscleEnumState.OBLIQUES
                ),
                MuscleState(
                    id = "9e69205f-6c6e-44a7-8ee6-89215e28a28e",
                    type = MuscleEnumState.RECTUS_ABDOMINIS
                ),
            ).map { MuscleRepresentationState.Plain(it) }.toPersistentList()
        ),
        MuscleGroupState(
            id = "255efc07-6c7e-42ab-97e5-01c06d60b5a3",
            name = "Legs",
            type = MuscleGroupEnumState.LEGS,
            muscles = listOf(
                MuscleState(
                    id = "57559b71-b757-468a-983d-a1b3cec4acef",
                    type = MuscleEnumState.QUADRICEPS
                ),
                MuscleState(
                    id = "bba5b66d-9a9c-4b44-8dd6-9574760038ee",
                    type = MuscleEnumState.CALF
                ),
                MuscleState(
                    id = "f6e65bfe-0746-4a8f-8210-0e9bf88d9886",
                    type = MuscleEnumState.GLUTEAL
                ),
                MuscleState(
                    id = "3eeaa9fa-0847-4780-9d01-185f91252794",
                    type = MuscleEnumState.HAMSTRINGS
                ),
                MuscleState(
                    id = "fa8025e6-e106-475c-8b9d-77831132fb47",
                    type = MuscleEnumState.ADDUCTORS
                ),
                MuscleState(
                    id = "ab1dbd50-83a4-42c7-a3cd-da1784818ec8",
                    type = MuscleEnumState.ABDUCTORS
                ),
            ).map { MuscleRepresentationState.Plain(it) }.toPersistentList()
        ),
        MuscleGroupState(
            id = "e1117068-06cd-4330-a4f9-93b485165805",
            name = "Shoulder Muscles",
            type = MuscleGroupEnumState.SHOULDER_MUSCLES,
            muscles = listOf(
                MuscleState(
                    id = "2da3d8f2-6a28-45ff-90a2-ea3a6bb2afe8",
                    type = MuscleEnumState.LATERAL_DELTOID
                ),
                MuscleState(
                    id = "d736a513-9d73-47a3-bffc-c14911662ea2",
                    type = MuscleEnumState.ANTERIOR_DELTOID
                ),
                MuscleState(
                    id = "97136fa7-622a-49d6-9d09-403a631d253d",
                    type = MuscleEnumState.POSTERIOR_DELTOID
                ),
            ).map { MuscleRepresentationState.Plain(it) }.toPersistentList()
        ),
        MuscleGroupState(
            id = "2043a22c-c547-42c2-81bb-81f85693d9cd",
            name = "Arms and Forearms",
            type = MuscleGroupEnumState.ARMS_AND_FOREARMS,
            muscles = listOf(
                MuscleState(
                    id = "9a8024fe-c721-4bea-969c-db88674b5ece",
                    type = MuscleEnumState.FOREARM
                ),
                MuscleState(
                    id = "97a87b01-35e8-490a-94b9-9bdae9c2f965",
                    type = MuscleEnumState.BICEPS
                ),
                MuscleState(
                    id = "0fd0be35-f933-43b8-a0d7-a4b6adaa9c1a",
                    type = MuscleEnumState.TRICEPS
                ),
            ).map { MuscleRepresentationState.Plain(it) }.toPersistentList()
        ),
        MuscleGroupState(
            id = "5fd8ccc9-8630-4357-a234-c2f278d905db",
            name = "Chest Muscles",
            type = MuscleGroupEnumState.CHEST_MUSCLES,
            muscles = listOf(
                MuscleState(
                    id = "b4658891-9713-48c4-864c-8dd907da19b0",
                    type = MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
                ),
                MuscleState(
                    id = "c57aa60c-61ea-4498-b01f-fedcafe8a32a",
                    type = MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR
                ),
                MuscleState(
                    id = "a3a8eae0-6315-4435-8974-f2c07ec3567f",
                    type = MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL
                ),
            ).map { MuscleRepresentationState.Plain(it) }.toPersistentList()
        ),
    )
}