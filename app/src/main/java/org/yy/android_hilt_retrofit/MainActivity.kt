package org.yy.android_hilt_retrofit

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


@Module
@InstallIn(ActivityComponent::class)
object HealthCheckModule {

    @Provides
    fun MyRetrofit():Retrofit{
        return Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    @Provides
    fun HealthCheckService(
            retrofit: Retrofit
    ):HealthCheckService{
        return  retrofit
                .create(HealthCheckService::class.java)
    }

    @Provides
    fun MyHealthCheckRepository(
            service:HealthCheckService
    ):HealthCheckRepository{
        return HealthCheckRepository(service)
    }

}


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.health.observe(this, Observer {
            Log.d("xxx", "viewModel: ${it.toString()}")
        })

    }

}


data class HealthCheckResponse (
        val current_user_url:String?
){
    override fun toString(): String {
        return "current_user_url $current_user_url"
    }
}


interface HealthCheckService {

    @GET("/")
    suspend fun healthCheck():HealthCheckResponse

}


class HealthCheckRepository(private val healthCheckService: HealthCheckService) {
    suspend fun getHealth() = healthCheckService.healthCheck()
}