package org.yy.android_hilt_retrofit

import android.view.ViewTreeObserver
import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HealthCheckServiceTest {

    @Test
    fun healthCheckServiceTest(){
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(HealthCheckService::class.java)
        runBlocking {
            val res = service.healthCheck().toString()
            assertNotNull(res)
        }
    }

}