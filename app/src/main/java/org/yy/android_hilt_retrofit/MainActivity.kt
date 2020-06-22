package org.yy.android_hilt_retrofit

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Qualifier


@Module
@InstallIn(ActivityComponent::class)
object HealthCheckModule {

    @Provides
    fun provideRetrofit():Retrofit{
        return Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    @Provides
    fun provideHealthCheckService(
            retrofit: Retrofit
    ):HealthCheckService{
        return  retrofit
                .create(HealthCheckService::class.java)
    }

    @Provides
    fun provideHealthCheckRepository(
            service:HealthCheckService
    ):HealthCheckRepository{
        return HealthCheckRepository(service)
    }

}

/**
 * モック用のアノテーション
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class MockRepository


@Module
@InstallIn(ActivityComponent::class)
object HealthCheckMockModule {
    @MockRepository
    @Provides
    fun provideMockHealthCheckService():HealthCheckService{
        return  MockHealthCheckService()
    }

    @MockRepository
    @Provides
    fun provideHealthCheckRepository(
            service:HealthCheckService
    ):HealthCheckRepository{
        return HealthCheckRepository(service)
    }
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val viewModel: MainActivityViewModel by viewModels()

    @MockRepository
    @Inject
    lateinit var healthCheckRepository: HealthCheckRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            Log.d("xxx","${healthCheckRepository.getHealth()}")
        }

//        viewModel.health.observe(this, Observer {
//            Log.d("xxx", "viewModel: ${it.toString()}")
//        })

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

class MockHealthCheckService : HealthCheckService {

    override suspend fun healthCheck(): HealthCheckResponse {
        return HealthCheckResponse("this is mock response")
    }

}

//fun provideMockHealthApi(retrofit: Retrofit): MockHealthCheckApi()
