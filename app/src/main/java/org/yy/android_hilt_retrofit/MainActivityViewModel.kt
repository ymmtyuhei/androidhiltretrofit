package org.yy.android_hilt_retrofit

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData


class MainActivityViewModel @ViewModelInject constructor(
    private val repository: HealthCheckRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val health: LiveData<HealthCheckResponse> = liveData {
        emit(repository.getHealth())
    }
}