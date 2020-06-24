package org.yy.android_hilt_retrofit

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.runBlocking
import java.lang.RuntimeException

open class BaseViewModel : ViewModel(){
    fun showDialog(){
        Log.d("xxx","show error dialog.")
    }
}

class MainActivityViewModel @ViewModelInject constructor(
    private val repository: HealthCheckRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {
    val health: LiveData<HealthCheckResponse> = liveData {
        emit(
            try {
                repository.getHealth()
            }catch (e: UserRepositoryException) {
                Log.w("xxx","認証エラーキャッチ onMainActivityViewModel")
                return@liveData
            }
        )
    }
}