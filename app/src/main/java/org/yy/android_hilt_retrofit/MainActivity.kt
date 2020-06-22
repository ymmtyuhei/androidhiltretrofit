package org.yy.android_hilt_retrofit

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton


//@Module
//@InstallIn(ActivityComponent::class)
//class ActivityModule {
//
//    @Provides
//    fun provideHash(): String {
//        return hashCode().toString()
//    }
//
//    @Inject
//    lateinit var retrofit: MyRetrofit
//
//    @Provides
//    fun service(): HealthCheckService {
//        retrofit.retrofit.create(HealthCheckService::class.java)
//    }
//}

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
}

//@Qualifier
//@Retention(AnnotationRetention.RUNTIME)
//internal annotation class ActivityHash


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

//    @Inject
//    lateinit var retrofit: MyRetrofit
//
//    @Inject
//    lateinit var HealthCheckRepository:HealthCheckRepository
//
//    @Inject
//    lateinit var activityHash: String
//
//    @Inject
//    lateinit var repos: HealthCheckRepository

    @Inject
    lateinit var mService: HealthCheckService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            Log.d("xxx",mService.healthCheck().toString())
        }

        //val service = retrofit.retrofit.create(HealthCheckService::class.java)
        //lifecycleScope.launch {
        //    val res = service.healthCheck()
        //    Log.d("xxx",res.toString())
        //}


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
//
//
//@Singleton
//class MyRetrofit @Inject constructor(){
//
//    val retrofit = Retrofit.Builder()
//            .baseUrl("https://api.github.com/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//}
//
//@Singleton
//class HealthCheckRepository @Inject constructor(private val healthCheckService: HealthCheckService) {
//    suspend fun getHealth() = healthCheckService.healthCheck()
//}