package org.yy.android_hilt_retrofit

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData

class MainActivityViewModel(
    private val healthRepository: HealthCheckRepository
) : ViewModel() {

    val health: LiveData<HealthCheckResponse> = liveData {
        emit(healthRepository.getHealth())
    }
}