package com.grippo.presentation.api.equipment.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.resources.icons.equipment.AbMachine
import com.grippo.design.resources.icons.equipment.AbductorMachine
import com.grippo.design.resources.icons.equipment.AdductorMachine
import com.grippo.design.resources.icons.equipment.AdjustableBench
import com.grippo.design.resources.icons.equipment.Cable
import com.grippo.design.resources.icons.equipment.CalfRaiseMachines
import com.grippo.design.resources.icons.equipment.ChestPressMachines
import com.grippo.design.resources.icons.equipment.CloseGripHandle
import com.grippo.design.resources.icons.equipment.CordHandles
import com.grippo.design.resources.icons.equipment.Crossower
import com.grippo.design.resources.icons.equipment.DeadliftMachines
import com.grippo.design.resources.icons.equipment.DeclineBench
import com.grippo.design.resources.icons.equipment.DeclineBenchWithRack
import com.grippo.design.resources.icons.equipment.DipBar
import com.grippo.design.resources.icons.equipment.Dumbbell
import com.grippo.design.resources.icons.equipment.EzBar
import com.grippo.design.resources.icons.equipment.FlatBench
import com.grippo.design.resources.icons.equipment.FlatBenchWithRack
import com.grippo.design.resources.icons.equipment.GluteHamRaiseBench
import com.grippo.design.resources.icons.equipment.GluteMachines
import com.grippo.design.resources.icons.equipment.HackSquatMachines
import com.grippo.design.resources.icons.equipment.InclineBenchWithRack
import com.grippo.design.resources.icons.equipment.LatPulldown
import com.grippo.design.resources.icons.equipment.LateralRaiseMachines
import com.grippo.design.resources.icons.equipment.LegCurlMachine
import com.grippo.design.resources.icons.equipment.LegExtensionMachine
import com.grippo.design.resources.icons.equipment.LegPressMachine
import com.grippo.design.resources.icons.equipment.PreacherCurlBench
import com.grippo.design.resources.icons.equipment.PullUpBar
import com.grippo.design.resources.icons.equipment.RomainChair
import com.grippo.design.resources.icons.equipment.Rope
import com.grippo.design.resources.icons.equipment.RowBench
import com.grippo.design.resources.icons.equipment.RowCable
import com.grippo.design.resources.icons.equipment.ShoulderPressMachines
import com.grippo.design.resources.icons.equipment.SmithMachine
import com.grippo.design.resources.icons.equipment.SquatRack
import com.grippo.design.resources.icons.equipment.StraightBar
import com.grippo.design.resources.icons.equipment.TrapBar
import com.grippo.design.resources.icons.equipment.TricepsMachines
import com.grippo.design.resources.icons.equipment.VBar
import com.grippo.design.resources.icons.equipment.WideGripHandle
import equipments.Barbell
import equipments.BicepsMachine
import equipments.Butterfly

@Immutable
public data class EquipmentState(
    val id: String,
    val name: String,
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
            EquipmentEnumState.BUTTERFLY_REVERSE -> Butterfly
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
            EquipmentEnumState.CABLE_CROSSOVER -> Crossower
            EquipmentEnumState.ROW_CABLE -> RowCable
            EquipmentEnumState.PULL_UP_BAR -> PullUpBar
            EquipmentEnumState.DIP_BARS -> DipBar
            EquipmentEnumState.ROMAIN_CHAIR -> RomainChair
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