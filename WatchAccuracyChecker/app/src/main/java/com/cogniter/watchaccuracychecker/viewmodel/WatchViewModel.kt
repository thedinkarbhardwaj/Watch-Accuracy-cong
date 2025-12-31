package com.cogniter.watchaccuracychecker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cogniter.watchaccuracychecker.database.entity.SubItemEntity
import com.cogniter.watchaccuracychecker.database.entity.WatchEntity
import com.cogniter.watchaccuracychecker.repository.WatchRepository
import kotlinx.coroutines.launch

class WatchViewModel(private val repository: WatchRepository) : ViewModel() {

    fun startWatch(watchId: Long) = viewModelScope.launch {
        repository.updateRunningState(watchId, true, System.currentTimeMillis())
    }

    fun stopWatch(watchId: Long) = viewModelScope.launch {
        val watch = repository.getWatchById(watchId) ?: return@launch
        val elapsed = watch.elapsedTimeMillis +
                (System.currentTimeMillis() - (watch.startTimeMillis ?: 0))

        repository.updateWatch(
            watch.copy(
                isWatchRunning = false,
                startTimeMillis = null,
                elapsedTimeMillis = elapsed
            )
        )
    }

    fun addSubItem(subItem: SubItemEntity) = viewModelScope.launch {
        repository.insertSubItem(subItem)
    }

    val allWatches = repository.getAllWatches().asLiveData()

    fun insertWatch(watch: WatchEntity) = viewModelScope.launch {
        repository.insertWatch(watch)
    }

    fun updateWatch(watch: WatchEntity) = viewModelScope.launch {
        repository.updateWatch(watch)
    }

    fun deleteWatch(watchId: Long) = viewModelScope.launch {
        repository.deleteWatch(watchId)
    }

    fun isWatchRunning(watchId: Long): LiveData<Boolean> {
        return repository.isWatchRunning(watchId)
    }

    fun updateElapsedTime(id: Int, elapsed: Long, running: Boolean) {
        viewModelScope.launch {
            repository.updateTimer(id.toLong(), elapsed, running)
        }
    }

    fun updateRunningState(
        watchId: Long,
        isRunning: Boolean,
        startTime: Long?
    ) {
        viewModelScope.launch {
            repository.updateRunningState(
                watchId = watchId,
                isRunning = isRunning,
                startTime = startTime
            )
        }
    }


}
