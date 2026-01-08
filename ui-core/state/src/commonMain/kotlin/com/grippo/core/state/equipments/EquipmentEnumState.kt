package com.grippo.core.state.equipments

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.equipment_ab_machines
import com.grippo.design.resources.provider.equipment_abductor_machine
import com.grippo.design.resources.provider.equipment_adductor_machine
import com.grippo.design.resources.provider.equipment_adjustable_bench
import com.grippo.design.resources.provider.equipment_barbell
import com.grippo.design.resources.provider.equipment_biceps_machines
import com.grippo.design.resources.provider.equipment_butterfly
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
import com.grippo.design.resources.provider.equipment_hint_barbell
import com.grippo.design.resources.provider.equipment_hint_biceps_machines
import com.grippo.design.resources.provider.equipment_hint_butterfly
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

@Immutable
public enum class EquipmentEnumState {
    DUMBBELLS,
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

    public fun title(): UiText {
        val r = when (this) {
            DUMBBELLS -> Res.string.equipment_dumbbells
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
        return UiText.Res(r)
    }

    public fun hint(): UiText {
        val r = when (this) {
            DUMBBELLS -> Res.string.equipment_hint_dumbbells
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
        return UiText.Res(r)
    }
}