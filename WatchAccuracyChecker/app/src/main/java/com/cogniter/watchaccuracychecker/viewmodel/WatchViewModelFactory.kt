package com.cogniter.watchaccuracychecker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cogniter.watchaccuracychecker.repository.WatchRepository

class WatchViewModelFactory(
    private val repository: WatchRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
