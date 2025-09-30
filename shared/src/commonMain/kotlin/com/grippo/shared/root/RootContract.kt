package com.grippo.shared.root

public interface RootContract {
    public fun onClose()
    public fun toHome()
    public fun toProfile()
    public fun toDebug()
    public fun toTraining(id: String?)
    public fun toWeightHistory()
    public fun toMissingEquipment()
    public fun toExcludedMuscles()
    public fun onBack()

    public companion object Empty : RootContract {
        override fun onClose() {}
        override fun toHome() {}
        override fun toProfile() {}
        override fun toDebug() {}
        override fun toTraining(id: String?) {}
        override fun toWeightHistory() {}
        override fun toMissingEquipment() {}
        override fun toExcludedMuscles() {}
        override fun onBack() {}
    }
}