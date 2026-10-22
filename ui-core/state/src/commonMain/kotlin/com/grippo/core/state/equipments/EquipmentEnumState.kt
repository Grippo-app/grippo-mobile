package com.grippo.core.state.equipments

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.equipment.AbMachine
import com.grippo.design.resources.provider.equipment.AbductorMachine
import com.grippo.design.resources.provider.equipment.AdductorMachine
import com.grippo.design.resources.provider.equipment.AdjustableBench
import com.grippo.design.resources.provider.equipment.AssistedPullUpDip
import com.grippo.design.resources.provider.equipment.Barbell
import com.grippo.design.resources.provider.equipment.BicepsMachine
import com.grippo.design.resources.provider.equipment.Butterfly
import com.grippo.design.resources.provider.equipment.ButterflyForearm
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
import com.grippo.design.resources.provider.equipment.Kettlebells
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
import com.grippo.design.resources.provider.equipment_ab_machines
import com.grippo.design.resources.provider.equipment_abductor_machine
import com.grippo.design.resources.provider.equipment_adductor_machine
import com.grippo.design.resources.provider.equipment_adjustable_bench
import com.grippo.design.resources.provider.equipment_assisted_pull_up_dip
import com.grippo.design.resources.provider.equipment_barbell
import com.grippo.design.resources.provider.equipment_biceps_machines
import com.grippo.design.resources.provider.equipment_butterfly
import com.grippo.design.resources.provider.equipment_butterfly_forearm
import com.grippo.design.resources.provider.equipment_butterfly_reverse
import com.grippo.design.resources.provider.equipment_cable
import com.grippo.design.resources.provider.equipment_cable_crossover
import com.grippo.design.resources.provider.equipment_calf_raise_machines
import com.grippo.design.resources.provider.equipment_chest_press_machines
import com.grippo.design.resources.provider.equipment_close_grip_handle
import com.grippo.design.resources.provider.equipment_cord_handles
import com.grippo.design.resources.provider.equipment_deadlift_machines
import com.grippo.design.resources.provider.equipment_decline_bench
import com.grippo.design.resources.provider.equipment_decline_bench_with_rack
import com.grippo.design.resources.provider.equipment_dip_bars
import com.grippo.design.resources.provider.equipment_dumbbells
import com.grippo.design.resources.provider.equipment_ez_bar
import com.grippo.design.resources.provider.equipment_flat_bench
import com.grippo.design.resources.provider.equipment_flat_bench_with_rack
import com.grippo.design.resources.provider.equipment_glute_ham_raise_bench
import com.grippo.design.resources.provider.equipment_glute_machines
import com.grippo.design.resources.provider.equipment_hack_squat_machines
import com.grippo.design.resources.provider.equipment_hint_ab_machines
import com.grippo.design.resources.provider.equipment_hint_abductor_machine
import com.grippo.design.resources.provider.equipment_hint_adductor_machine
import com.grippo.design.resources.provider.equipment_hint_adjustable_bench
import com.grippo.design.resources.provider.equipment_hint_assisted_pull_up_dip
import com.grippo.design.resources.provider.equipment_hint_barbell
import com.grippo.design.resources.provider.equipment_hint_biceps_machines
import com.grippo.design.resources.provider.equipment_hint_butterfly
import com.grippo.design.resources.provider.equipment_hint_butterfly_forearm
import com.grippo.design.resources.provider.equipment_hint_butterfly_reverse
import com.grippo.design.resources.provider.equipment_hint_cable
import com.grippo.design.resources.provider.equipment_hint_cable_crossover
import com.grippo.design.resources.provider.equipment_hint_calf_raise_machines
import com.grippo.design.resources.provider.equipment_hint_chest_press_machines
import com.grippo.design.resources.provider.equipment_hint_close_grip_handle
import com.grippo.design.resources.provider.equipment_hint_cord_handles
import com.grippo.design.resources.provider.equipment_hint_deadlift_machines
import com.grippo.design.resources.provider.equipment_hint_decline_bench
import com.grippo.design.resources.provider.equipment_hint_decline_bench_with_rack
import com.grippo.design.resources.provider.equipment_hint_dip_bars
import com.grippo.design.resources.provider.equipment_hint_dumbbells
import com.grippo.design.resources.provider.equipment_hint_ez_bar
import com.grippo.design.resources.provider.equipment_hint_flat_bench
import com.grippo.design.resources.provider.equipment_hint_flat_bench_with_rack
import com.grippo.design.resources.provider.equipment_hint_glute_ham_raise_bench
import com.grippo.design.resources.provider.equipment_hint_glute_machines
import com.grippo.design.resources.provider.equipment_hint_hack_squat_machines
import com.grippo.design.resources.provider.equipment_hint_incline_bench_with_rack
import com.grippo.design.resources.provider.equipment_hint_kettlebells
import com.grippo.design.resources.provider.equipment_hint_lat_pulldown
import com.grippo.design.resources.provider.equipment_hint_lateral_raise_machines
import com.grippo.design.resources.provider.equipment_hint_leg_curl_machines
import com.grippo.design.resources.provider.equipment_hint_leg_extension_machines
import com.grippo.design.resources.provider.equipment_hint_leg_press_machine
import com.grippo.design.resources.provider.equipment_hint_preacher_curl_bench
import com.grippo.design.resources.provider.equipment_hint_pull_up_bar
import com.grippo.design.resources.provider.equipment_hint_romain_chair
import com.grippo.design.resources.provider.equipment_hint_rope
import com.grippo.design.resources.provider.equipment_hint_row_bench
import com.grippo.design.resources.provider.equipment_hint_row_cable
import com.grippo.design.resources.provider.equipment_hint_shoulder_press_machines
import com.grippo.design.resources.provider.equipment_hint_smith_machines
import com.grippo.design.resources.provider.equipment_hint_squat_rack
import com.grippo.design.resources.provider.equipment_hint_straight_bar
import com.grippo.design.resources.provider.equipment_hint_trap_bar
import com.grippo.design.resources.provider.equipment_hint_triceps_machines
import com.grippo.design.resources.provider.equipment_hint_v_bar
import com.grippo.design.resources.provider.equipment_hint_wide_grip_handle
import com.grippo.design.resources.provider.equipment_incline_bench_with_rack
import com.grippo.design.resources.provider.equipment_kettlebells
import com.grippo.design.resources.provider.equipment_lat_pulldown
import com.grippo.design.resources.provider.equipment_lateral_raise_machines
import com.grippo.design.resources.provider.equipment_leg_curl_machines
import com.grippo.design.resources.provider.equipment_leg_extension_machines
import com.grippo.design.resources.provider.equipment_leg_press_machine
import com.grippo.design.resources.provider.equipment_preacher_curl_bench
import com.grippo.design.resources.provider.equipment_pull_up_bar
import com.grippo.design.resources.provider.equipment_roman_chair
import com.grippo.design.resources.provider.equipment_rope
import com.grippo.design.resources.provider.equipment_row_bench
import com.grippo.design.resources.provider.equipment_row_cable
import com.grippo.design.resources.provider.equipment_shoulder_press_machines
import com.grippo.design.resources.provider.equipment_smith_machines
import com.grippo.design.resources.provider.equipment_squat_rack
import com.grippo.design.resources.provider.equipment_straight_bar
import com.grippo.design.resources.provider.equipment_trap_bar
import com.grippo.design.resources.provider.equipment_triceps_machines
import com.grippo.design.resources.provider.equipment_v_bar
import com.grippo.design.resources.provider.equipment_wide_grip_handle
import org.jetbrains.compose.resources.StringResource

@Immutable
public enum class EquipmentEnumState {
    DUMBBELLS,
    KETTLEBELLS,
    BARBELL,
    V_BAR,
    WIDE_GRIP_HANDLE,
    CLOSE_GRIP_HANDLE,
    EZ_BAR,
    TRAP_BAR,
    ROPE,
    STRAIGHT_BAR,
    CORD_HANDLES,

    AB_MACHINES,
    BUTTERFLY,
    BUTTERFLY_FOREARM,
    BUTTERFLY_REVERSE,
    LEG_EXTENSION_MACHINES,
    LEG_CURL_MACHINES,
    CHEST_PRESS_MACHINES,
    BICEPS_MACHINES,
    SMITH_MACHINES,
    HACK_SQUAT_MACHINES,
    LEG_PRESS_MACHINE,
    DEADLIFT_MACHINES,
    SHOULDER_PRESS_MACHINES,
    LATERAL_RAISE_MACHINES,
    TRICEPS_MACHINES,
    CALF_RAISE_MACHINES,
    GLUTE_MACHINES,
    ASSISTED_PULL_UP_DIP,

    LAT_PULLDOWN,
    CABLE,
    CABLE_CROSSOVER,
    ROW_CABLE,

    PULL_UP_BAR,
    DIP_BARS,
    ROMAIN_CHAIR,
    GLUTE_HAM_RAISE_BENCH,

    FLAT_BENCH,
    ADJUSTABLE_BENCH,
    ADDUCTOR_MACHINE,
    ABDUCTOR_MACHINE,
    DECLINE_BENCH,
    FLAT_BENCH_WITH_RACK,
    INCLINE_BENCH_WITH_RACK,
    DECLINE_BENCH_WITH_RACK,
    SQUAT_RACK,
    PREACHER_CURL_BENCH,
    ROW_BENCH;

    public fun title(): UiText = TITLES.getValue(this)
    public fun hint(): UiText = HINTS.getValue(this)
    public fun hintRes(): StringResource = HINT_RES.getValue(this)

    public fun icon(): ImageVector {
        return when (this) {
            DUMBBELLS -> Dumbbell
            KETTLEBELLS -> Kettlebells
            BARBELL -> Barbell
            V_BAR -> VBar
            WIDE_GRIP_HANDLE -> WideGripHandle
            CLOSE_GRIP_HANDLE -> CloseGripHandle
            EZ_BAR -> EzBar
            TRAP_BAR -> TrapBar
            ROPE -> Rope
            STRAIGHT_BAR -> StraightBar
            CORD_HANDLES -> CordHandles

            AB_MACHINES -> AbMachine
            BUTTERFLY -> Butterfly
            BUTTERFLY_FOREARM -> ButterflyForearm
            BUTTERFLY_REVERSE -> ButterflyReverse
            LEG_EXTENSION_MACHINES -> LegExtensionMachine
            LEG_CURL_MACHINES -> LegCurlMachine
            CHEST_PRESS_MACHINES -> ChestPressMachines
            BICEPS_MACHINES -> BicepsMachine
            SMITH_MACHINES -> SmithMachine
            HACK_SQUAT_MACHINES -> HackSquatMachines
            LEG_PRESS_MACHINE -> LegPressMachine
            DEADLIFT_MACHINES -> DeadliftMachines
            SHOULDER_PRESS_MACHINES -> ShoulderPressMachines
            LATERAL_RAISE_MACHINES -> LateralRaiseMachines
            TRICEPS_MACHINES -> TricepsMachines
            CALF_RAISE_MACHINES -> CalfRaiseMachines
            GLUTE_MACHINES -> GluteMachines
            ASSISTED_PULL_UP_DIP -> AssistedPullUpDip

            LAT_PULLDOWN -> LatPulldown
            CABLE -> Cable
            CABLE_CROSSOVER -> Crossover
            ROW_CABLE -> RowCable

            PULL_UP_BAR -> PullUpBar
            DIP_BARS -> DipBar
            ROMAIN_CHAIR -> RomanChair
            GLUTE_HAM_RAISE_BENCH -> GluteHamRaiseBench

            FLAT_BENCH -> FlatBench
            ADJUSTABLE_BENCH -> AdjustableBench
            ADDUCTOR_MACHINE -> AdductorMachine
            ABDUCTOR_MACHINE -> AbductorMachine
            DECLINE_BENCH -> DeclineBench
            FLAT_BENCH_WITH_RACK -> FlatBenchWithRack
            INCLINE_BENCH_WITH_RACK -> InclineBenchWithRack
            DECLINE_BENCH_WITH_RACK -> DeclineBenchWithRack
            SQUAT_RACK -> SquatRack
            PREACHER_CURL_BENCH -> PreacherCurlBench
            ROW_BENCH -> RowBench
        }
    }

    public companion object {
        private val TITLE_RES: Map<EquipmentEnumState, StringResource> =
            entries.associateWith {
                when (it) {
                    DUMBBELLS -> Res.string.equipment_dumbbells
                    KETTLEBELLS -> Res.string.equipment_kettlebells
                    BARBELL -> Res.string.equipment_barbell
                    V_BAR -> Res.string.equipment_v_bar
                    WIDE_GRIP_HANDLE -> Res.string.equipment_wide_grip_handle
                    CLOSE_GRIP_HANDLE -> Res.string.equipment_close_grip_handle
                    EZ_BAR -> Res.string.equipment_ez_bar
                    TRAP_BAR -> Res.string.equipment_trap_bar
                    ROPE -> Res.string.equipment_rope
                    STRAIGHT_BAR -> Res.string.equipment_straight_bar
                    CORD_HANDLES -> Res.string.equipment_cord_handles

                    AB_MACHINES -> Res.string.equipment_ab_machines
                    BUTTERFLY -> Res.string.equipment_butterfly
                    BUTTERFLY_FOREARM -> Res.string.equipment_butterfly_forearm
                    BUTTERFLY_REVERSE -> Res.string.equipment_butterfly_reverse
                    LEG_EXTENSION_MACHINES -> Res.string.equipment_leg_extension_machines
                    LEG_CURL_MACHINES -> Res.string.equipment_leg_curl_machines
                    CHEST_PRESS_MACHINES -> Res.string.equipment_chest_press_machines
                    BICEPS_MACHINES -> Res.string.equipment_biceps_machines
                    SMITH_MACHINES -> Res.string.equipment_smith_machines
                    HACK_SQUAT_MACHINES -> Res.string.equipment_hack_squat_machines
                    LEG_PRESS_MACHINE -> Res.string.equipment_leg_press_machine
                    DEADLIFT_MACHINES -> Res.string.equipment_deadlift_machines
                    SHOULDER_PRESS_MACHINES -> Res.string.equipment_shoulder_press_machines
                    LATERAL_RAISE_MACHINES -> Res.string.equipment_lateral_raise_machines
                    TRICEPS_MACHINES -> Res.string.equipment_triceps_machines
                    CALF_RAISE_MACHINES -> Res.string.equipment_calf_raise_machines
                    GLUTE_MACHINES -> Res.string.equipment_glute_machines
                    ASSISTED_PULL_UP_DIP -> Res.string.equipment_assisted_pull_up_dip

                    LAT_PULLDOWN -> Res.string.equipment_lat_pulldown
                    CABLE -> Res.string.equipment_cable
                    CABLE_CROSSOVER -> Res.string.equipment_cable_crossover
                    ROW_CABLE -> Res.string.equipment_row_cable

                    PULL_UP_BAR -> Res.string.equipment_pull_up_bar
                    DIP_BARS -> Res.string.equipment_dip_bars
                    ROMAIN_CHAIR -> Res.string.equipment_roman_chair
                    GLUTE_HAM_RAISE_BENCH -> Res.string.equipment_glute_ham_raise_bench

                    FLAT_BENCH -> Res.string.equipment_flat_bench
                    ADJUSTABLE_BENCH -> Res.string.equipment_adjustable_bench
                    ADDUCTOR_MACHINE -> Res.string.equipment_adductor_machine
                    ABDUCTOR_MACHINE -> Res.string.equipment_abductor_machine
                    DECLINE_BENCH -> Res.string.equipment_decline_bench
                    FLAT_BENCH_WITH_RACK -> Res.string.equipment_flat_bench_with_rack
                    INCLINE_BENCH_WITH_RACK -> Res.string.equipment_incline_bench_with_rack
                    DECLINE_BENCH_WITH_RACK -> Res.string.equipment_decline_bench_with_rack
                    SQUAT_RACK -> Res.string.equipment_squat_rack
                    PREACHER_CURL_BENCH -> Res.string.equipment_preacher_curl_bench
                    ROW_BENCH -> Res.string.equipment_row_bench
                }
            }

        private val HINT_RES: Map<EquipmentEnumState, StringResource> =
            entries.associateWith {
                when (it) {
                    DUMBBELLS -> Res.string.equipment_hint_dumbbells
                    KETTLEBELLS -> Res.string.equipment_hint_kettlebells
                    BARBELL -> Res.string.equipment_hint_barbell
                    V_BAR -> Res.string.equipment_hint_v_bar
                    WIDE_GRIP_HANDLE -> Res.string.equipment_hint_wide_grip_handle
                    CLOSE_GRIP_HANDLE -> Res.string.equipment_hint_close_grip_handle
                    EZ_BAR -> Res.string.equipment_hint_ez_bar
                    TRAP_BAR -> Res.string.equipment_hint_trap_bar
                    ROPE -> Res.string.equipment_hint_rope
                    STRAIGHT_BAR -> Res.string.equipment_hint_straight_bar
                    CORD_HANDLES -> Res.string.equipment_hint_cord_handles

                    AB_MACHINES -> Res.string.equipment_hint_ab_machines
                    BUTTERFLY -> Res.string.equipment_hint_butterfly
                    BUTTERFLY_FOREARM -> Res.string.equipment_hint_butterfly_forearm
                    BUTTERFLY_REVERSE -> Res.string.equipment_hint_butterfly_reverse
                    LEG_EXTENSION_MACHINES -> Res.string.equipment_hint_leg_extension_machines
                    LEG_CURL_MACHINES -> Res.string.equipment_hint_leg_curl_machines
                    CHEST_PRESS_MACHINES -> Res.string.equipment_hint_chest_press_machines
                    BICEPS_MACHINES -> Res.string.equipment_hint_biceps_machines
                    SMITH_MACHINES -> Res.string.equipment_hint_smith_machines
                    HACK_SQUAT_MACHINES -> Res.string.equipment_hint_hack_squat_machines
                    LEG_PRESS_MACHINE -> Res.string.equipment_hint_leg_press_machine
                    DEADLIFT_MACHINES -> Res.string.equipment_hint_deadlift_machines
                    SHOULDER_PRESS_MACHINES -> Res.string.equipment_hint_shoulder_press_machines
                    LATERAL_RAISE_MACHINES -> Res.string.equipment_hint_lateral_raise_machines
                    TRICEPS_MACHINES -> Res.string.equipment_hint_triceps_machines
                    CALF_RAISE_MACHINES -> Res.string.equipment_hint_calf_raise_machines
                    GLUTE_MACHINES -> Res.string.equipment_hint_glute_machines
                    ASSISTED_PULL_UP_DIP -> Res.string.equipment_hint_assisted_pull_up_dip

                    LAT_PULLDOWN -> Res.string.equipment_hint_lat_pulldown
                    CABLE -> Res.string.equipment_hint_cable
                    CABLE_CROSSOVER -> Res.string.equipment_hint_cable_crossover
                    ROW_CABLE -> Res.string.equipment_hint_row_cable

                    PULL_UP_BAR -> Res.string.equipment_hint_pull_up_bar
                    DIP_BARS -> Res.string.equipment_hint_dip_bars
                    ROMAIN_CHAIR -> Res.string.equipment_hint_romain_chair
                    GLUTE_HAM_RAISE_BENCH -> Res.string.equipment_hint_glute_ham_raise_bench

                    FLAT_BENCH -> Res.string.equipment_hint_flat_bench
                    ADJUSTABLE_BENCH -> Res.string.equipment_hint_adjustable_bench
                    ADDUCTOR_MACHINE -> Res.string.equipment_hint_adductor_machine
                    ABDUCTOR_MACHINE -> Res.string.equipment_hint_abductor_machine
                    DECLINE_BENCH -> Res.string.equipment_hint_decline_bench
                    FLAT_BENCH_WITH_RACK -> Res.string.equipment_hint_flat_bench_with_rack
                    INCLINE_BENCH_WITH_RACK -> Res.string.equipment_hint_incline_bench_with_rack
                    DECLINE_BENCH_WITH_RACK -> Res.string.equipment_hint_decline_bench_with_rack
                    SQUAT_RACK -> Res.string.equipment_hint_squat_rack
                    PREACHER_CURL_BENCH -> Res.string.equipment_hint_preacher_curl_bench
                    ROW_BENCH -> Res.string.equipment_hint_row_bench
                }
            }

        private val TITLES: Map<EquipmentEnumState, UiText> =
            TITLE_RES.mapValues { (_, res) -> UiText.Res(res) }

        private val HINTS: Map<EquipmentEnumState, UiText> =
            HINT_RES.mapValues { (_, res) -> UiText.Res(res) }
    }
}
