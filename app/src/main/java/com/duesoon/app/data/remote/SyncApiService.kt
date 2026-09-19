package com.duesoon.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SyncApiService {
    
    @GET("salt/{email}")
    suspend fun getSalt(@Path("email") email: String): SaltResponse

    @POST("sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @GET("sync/pull")
    suspend fun pull(@Query("sinceRevision") sinceRevision: Long): SyncPullResponse
}
