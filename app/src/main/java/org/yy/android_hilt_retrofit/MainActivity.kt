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
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.http2.Http2Reader.Companion.logger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException
import javax.inject.Inject


internal class LoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val t1 = System.nanoTime()
        logger.info(
            java.lang.String.format(
                "Sending request %s on %s%n%s",
                request.url, chain.connection(), request.headers
            )
        )
        val response: Response = chain.proceed(request)
        val t2 = System.nanoTime()
        logger.info(
            java.lang.String.format(
                "Received response for %s in %.1fms%n%s",
                response.request.url, (t2 - t1) / 1e6, response.headers
            )
        )
        return response
    }
}

@Module
@InstallIn(ActivityComponent::class)
object HealthCheckModule {

    @Provides
    fun provideOkHttpClient():OkHttpClient{
        return OkHttpClient()
            .newBuilder()
            .addInterceptor(LoggingInterceptor())
            .build()
    }

    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient):Retrofit{
        return Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(okHttpClient)
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


@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var healthCheckRepository: HealthCheckRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            Log.d("xxx","${healthCheckRepository.getHealth()}")
        }

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