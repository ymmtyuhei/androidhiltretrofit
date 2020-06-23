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
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Timeout
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.inject.Inject


sealed class Result<out T> {
    data class Success<T>(val data: T?) : Result<T>()
    data class Failure(val statusCode: Int?) : Result<Nothing>()
    object NetworkError : Result<Nothing>()
}


abstract class CallDelegate<TIn, TOut>(
    protected val proxy: Call<TIn>
) : Call<TOut> {
    override fun execute(): Response<TOut> = throw NotImplementedError()
    final override fun enqueue(callback: Callback<TOut>) = enqueueImpl(callback)
    final override fun clone(): Call<TOut> = cloneImpl()

    override fun cancel() = proxy.cancel()
    override fun request(): Request = proxy.request()
    override fun isExecuted() = proxy.isExecuted
    override fun isCanceled() = proxy.isCanceled

    abstract fun enqueueImpl(callback: Callback<TOut>)
    abstract fun cloneImpl(): Call<TOut>
}

class ResultCall<T>(proxy: Call<T>) : CallDelegate<T, Result<T>>(proxy) {
    override fun enqueueImpl(callback: Callback<Result<T>>) = proxy.enqueue(object: Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val code = response.code()
            val result = if (code in 200 until 300) {
                val body = response.body()
                val successResult:Result<T> = Result.Success(body)
                successResult
            } else {
                Result.Failure(code)
            }

            callback.onResponse(this@ResultCall, Response.success(result))
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            val result = if (t is IOException) {
                Result.NetworkError
            } else {
                Result.Failure(null)
            }

            callback.onResponse(this@ResultCall, Response.success(result))
        }
    })

    override fun cloneImpl() = ResultCall(proxy.clone())
    override fun timeout(): Timeout {
        TODO("Not yet implemented")
    }
}

class ResultAdapter(
    private val type: Type
): CallAdapter<Type, Call<Result<Type>>> {
    override fun responseType() = type
    override fun adapt(call: Call<Type>): Call<Result<Type>> = ResultCall(call)
}

class MyCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ) = when (getRawType(returnType)) {
        Call::class.java -> {
            val callType = getParameterUpperBound(0, returnType as ParameterizedType)
            when (getRawType(callType)) {
                Result::class.java -> {
                    val resultType = getParameterUpperBound(0, callType as ParameterizedType)
                    ResultAdapter(resultType)
                }
                else -> null
            }
        }
        else -> null
    }
}


interface UserRepository{
    suspend fun getUserToken():String
}
class UserRepositoryImpl:UserRepository{
    override suspend fun getUserToken(): String {
        delay(500)
        throw RuntimeException("failed to get user token.")
        return "Bearer mocktokenxxx"
    }
}

//class TokenInterceptor constructor(val userRepository: UserRepository) :Interceptor{
//
//    override fun intercept(chain: Interceptor.Chain): Response {
//
//        val original = chain.request()
//
//        if (original.url.encodedPath.contains("/login")&& original.method == "post") {
//            return  chain.proceed(original)
//        }
//
//        // OkHttpのスレッドで実行されているのでブロックしても問題ない
//        val token = runBlocking { userRepository.getUserToken() }
//
//        val request = original
//            .newBuilder()
//            .addHeader("Authorization",token)
//            .url(original.url)
//            .build()
//
//        return chain.proceed(request)
//    }
//}


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
            //.addInterceptor(TokenInterceptor(userRepository))
            .build()
    }

    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient):Retrofit{
        return Retrofit.Builder()
                .baseUrl("https://03874f53bf488fa409f0d35f558f683a.m.pipedream.net")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(MyCallAdapterFactory())
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
            when(it){
                is Result.Success -> {
                    val healthCheckResponse = it.data
                    Log.d("xxx","success ${healthCheckResponse?.toString()}")
                }
                is Result.Failure -> {
                    Log.d("xxx","failure")
                }
                Result.NetworkError -> {
                    Log.d("xxx","network error")
                }
            }
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
    suspend fun healthCheck():Result<HealthCheckResponse>

}


class HealthCheckRepository(private val healthCheckService: HealthCheckService) {
    suspend fun getHealth() = healthCheckService.healthCheck()
}