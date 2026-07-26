package org.eu.john007.hdmikvm.network

import retrofit2.Response
import retrofit2.http.*

interface EspHomeApi {
    @Headers(
        "Accept: */*",
        "Content-Type: application/x-www-form-urlencoded",
        "User-Agent: Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36",
        "Connection: keep-alive"
    )
    @POST
    suspend fun setInput(
        @Url url: String,
        @Header("Origin") origin: String,
        @Header("Referer") referer: String
    ): Response<Unit>

    @GET("select/{entity}")
    suspend fun getState(
        @Path(value = "entity", encoded = true) entity: String
    ): Response<EspHomeState>

    @GET
    suspend fun getString(@Url url: String): Response<String>
}
