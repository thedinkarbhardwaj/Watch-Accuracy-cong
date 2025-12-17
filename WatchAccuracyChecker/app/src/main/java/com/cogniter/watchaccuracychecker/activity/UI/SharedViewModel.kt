package com.cogniter.watchaccuracychecker.activity.UI

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _clickEvent = MutableLiveData<Boolean>()
    val clickEvent: LiveData<Boolean>
        get() = _clickEvent

    fun onFragmentClick() {
        _clickEvent.value = true
    }
}
