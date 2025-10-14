package com.grippo.core.state.equipments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.resources.provider.equipment.AbMachine
import com.grippo.design.resources.provider.equipment.AbductorMachine
import com.grippo.design.resources.provider.equipment.AdductorMachine
import com.grippo.design.resources.provider.equipment.AdjustableBench
import com.grippo.design.resources.provider.equipment.Barbell
import com.grippo.design.resources.provider.equipment.BicepsMachine
import com.grippo.design.resources.provider.equipment.Butterfly
import com.grippo.design.resources.provider.equipment.ButterflyReverse
import com.grippo.design.resources.provider.equipment.Cable
import com.grippo.design.resources.provider.equipment.CalfRaiseMachines
import com.grippo.design.resources.provider.equipment.ChestPressMachines
import com.grippo.design.resources.provider.equipment.CloseGripHandle
import com.grippo.design.resources.provider.equipment.CordHandles
import com.grippo.design.resources.provider.equipment.Crossover
import com.grippo.design.resources.provider.equipment.DeadliftMachines
import com.grippo.design.resources.provider.equipment.DeclineBench
import com.grippo.design.resources.provider.equipment.DeclineBenchWithRack
import com.grippo.design.resources.provider.equipment.DipBar
import com.grippo.design.resources.provider.equipment.Dumbbell
import com.grippo.design.resources.provider.equipment.EzBar
import com.grippo.design.resources.provider.equipment.FlatBench
import com.grippo.design.resources.provider.equipment.FlatBenchWithRack
import com.grippo.design.resources.provider.equipment.GluteHamRaiseBench
import com.grippo.design.resources.provider.equipment.GluteMachines
import com.grippo.design.resources.provider.equipment.HackSquatMachines
import com.grippo.design.resources.provider.equipment.InclineBenchWithRack
import com.grippo.design.resources.provider.equipment.LatPulldown
import com.grippo.design.resources.provider.equipment.LateralRaiseMachines
import com.grippo.design.resources.provider.equipment.LegCurlMachine
import com.grippo.design.resources.provider.equipment.LegExtensionMachine
import com.grippo.design.resources.provider.equipment.LegPressMachine
import com.grippo.design.resources.provider.equipment.PreacherCurlBench
import com.grippo.design.resources.provider.equipment.PullUpBar
import com.grippo.design.resources.provider.equipment.RomanChair
import com.grippo.design.resources.provider.equipment.Rope
import com.grippo.design.resources.provider.equipment.RowBench
import com.grippo.design.resources.provider.equipment.RowCable
import com.grippo.design.resources.provider.equipment.ShoulderPressMachines
import com.grippo.design.resources.provider.equipment.SmithMachine
import com.grippo.design.resources.provider.equipment.SquatRack
import com.grippo.design.resources.provider.equipment.StraightBar
import com.grippo.design.resources.provider.equipment.TrapBar
import com.grippo.design.resources.provider.equipment.TricepsMachines
import com.grippo.design.resources.provider.equipment.VBar
import com.grippo.design.resources.provider.equipment.WideGripHandle
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public data class EquipmentState(
    val id: String,
    val type: EquipmentEnumState,
) {

    @Composable
    public fun image(): ImageVector {
        return when (type) {
            EquipmentEnumState.DUMBBELLS -> Dumbbell
            EquipmentEnumState.ROPE -> Rope
            EquipmentEnumState.CORD_HANDLES -> CordHandles
            EquipmentEnumState.STRAIGHT_BAR -> StraightBar
            EquipmentEnumState.BARBELL -> Barbell
            EquipmentEnumState.EZ_BAR -> EzBar
            EquipmentEnumState.V_BAR -> VBar
            EquipmentEnumState.CLOSE_GRIP_HANDLE -> CloseGripHandle
            EquipmentEnumState.WIDE_GRIP_HANDLE -> WideGripHandle
            EquipmentEnumState.TRAP_BAR -> TrapBar
            EquipmentEnumState.AB_MACHINES -> AbMachine
            EquipmentEnumState.BUTTERFLY -> Butterfly
            EquipmentEnumState.BUTTERFLY_REVERSE -> ButterflyReverse
            EquipmentEnumState.LEG_EXTENSION_MACHINES -> LegExtensionMachine
            EquipmentEnumState.LEG_CURL_MACHINES -> LegCurlMachine
            EquipmentEnumState.CHEST_PRESS_MACHINES -> ChestPressMachines
            EquipmentEnumState.BICEPS_MACHINES -> BicepsMachine
            EquipmentEnumState.SMITH_MACHINES -> SmithMachine
            EquipmentEnumState.HACK_SQUAT_MACHINES -> HackSquatMachines
            EquipmentEnumState.DEADLIFT_MACHINES -> DeadliftMachines
            EquipmentEnumState.SHOULDER_PRESS_MACHINES -> ShoulderPressMachines
            EquipmentEnumState.LATERAL_RAISE_MACHINES -> LateralRaiseMachines
            EquipmentEnumState.TRICEPS_MACHINES -> TricepsMachines
            EquipmentEnumState.CALF_RAISE_MACHINES -> CalfRaiseMachines
            EquipmentEnumState.GLUTE_MACHINES -> GluteMachines
            EquipmentEnumState.LAT_PULLDOWN -> LatPulldown
            EquipmentEnumState.CABLE -> Cable
            EquipmentEnumState.CABLE_CROSSOVER -> Crossover
            EquipmentEnumState.ROW_CABLE -> RowCable
            EquipmentEnumState.PULL_UP_BAR -> PullUpBar
            EquipmentEnumState.DIP_BARS -> DipBar
            EquipmentEnumState.ROMAIN_CHAIR -> RomanChair
            EquipmentEnumState.GLUTE_HAM_RAISE_BENCH -> GluteHamRaiseBench
            EquipmentEnumState.FLAT_BENCH -> FlatBench
            EquipmentEnumState.ADJUSTABLE_BENCH -> AdjustableBench
            EquipmentEnumState.ADDUCTOR_MACHINE -> AdductorMachine
            EquipmentEnumState.ABDUCTOR_MACHINE -> AbductorMachine
            EquipmentEnumState.DECLINE_BENCH -> DeclineBench
            EquipmentEnumState.FLAT_BENCH_WITH_RACK -> FlatBenchWithRack
            EquipmentEnumState.INCLINE_BENCH_WITH_RACK -> InclineBenchWithRack
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK -> DeclineBenchWithRack
            EquipmentEnumState.SQUAT_RACK -> SquatRack
            EquipmentEnumState.PREACHER_CURL_BENCH -> PreacherCurlBench
            EquipmentEnumState.ROW_BENCH -> RowBench
            EquipmentEnumState.LEG_PRESS_MACHINE -> LegPressMachine
        }
    }
}