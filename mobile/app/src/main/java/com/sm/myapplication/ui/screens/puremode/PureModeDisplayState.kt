package com.sm.myapplication.ui.screens.puremode

import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import com.sm.myapplication.ui.screens.timer.TimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Camera chrome changes only for status, never for every 200 ms timer tick. */
internal data class PureModeDisplayState(
    val isRunning: Boolean,
    val pauseCause: TimerPauseCause,
    val message: String,
)

internal fun pureModeDisplayState(state: TimerState) =
    PureModeDisplayState(state.isRunning, state.pauseCause, state.message)

internal fun pureModeDisplayStates(states: Flow<TimerState>): Flow<PureModeDisplayState> =
    states.map(::pureModeDisplayState).distinctUntilChanged()

/** HH:mm:ss displays whole seconds; the underlying timer keeps its precision. */
internal fun pureModeElapsedSeconds(states: Flow<TimerState>): Flow<Long> =
    states.map { it.elapsedMs / 1_000L }.distinctUntilChanged()
