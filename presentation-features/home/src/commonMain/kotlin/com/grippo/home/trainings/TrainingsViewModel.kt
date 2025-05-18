package com.grippo.home.trainings

import com.grippo.core.BaseViewModel

internal class TrainingsViewModel :
    BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(TrainingsState),
    TrainingsContract