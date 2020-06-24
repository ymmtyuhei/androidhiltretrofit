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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.internal.http2.Http2Reader.Companion.logger
import retrofit2.HttpException
import retrofit2.Response.error
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException
import java.lang.Exception
import javax.inject.Inject


enum class UserReposFailedCause{
    NotAuthorized,
    InvalidToken,
    Unknown
}
open class UserRepositoryException(msg:String,cause:UserReposFailedCause):IOException(msg)// OkHttp3 InterceptorがキャッチできるのはIOException
class UserRepositoryFailed(cause:UserReposFailedCause) : UserRepositoryException(cause.toString(),cause)

interface UserRepository{
    suspend fun getUserToken():String
}
class UserRepositoryImpl:UserRepository{
    override suspend fun getUserToken(): String {
        delay(500)
        throw UserRepositoryFailed(UserReposFailedCause.NotAuthorized) // エラースローテスト
        return "Bearer mocktokenxxx"
    }
}

class TokenInterceptor constructor(val userRepository: UserRepository) :Interceptor{

    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()

        if (original.url.encodedPath.contains("/login")&& original.method == "post") {
            return  chain.proceed(original)
        }

        // OkHttpのスレッドで実行されているのでブロックしても問題ない
        val token = runBlocking { userRepository.getUserToken() }

        val request = original
            .newBuilder()
            .addHeader("Authorization",token)
            .url(original.url)
            .build()

        return chain.proceed(request)
    }
}


@Module
@InstallIn(ActivityComponent::class)
object HealthCheckModule {

    @Provides
    fun userRepository():UserRepository{
        return  UserRepositoryImpl()
    }

    @Provides
    fun provideOkHttpClient(
        userRepository: UserRepository
    ):OkHttpClient{
        return OkHttpClient()
            .newBuilder()
            .addInterceptor(
                TokenInterceptor(userRepository)
            )
            .build()
    }

    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient):Retrofit{
        return Retrofit.Builder()
                .baseUrl("https://03874f53bf488fa409f0d35f558f683a.m.pipedream.net")
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
            try {
                Log.d("xxx","${healthCheckRepository.getHealth()}")
            }catch (e: UserRepositoryException) {
                Log.w("xxx", "認証エラーキャッチ on Activity")
            }
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