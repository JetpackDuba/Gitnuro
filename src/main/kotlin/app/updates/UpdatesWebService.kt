package app.updates

import retrofit2.http.GET
import retrofit2.http.Url

interface UpdatesService {
    @GET
    suspend fun release(@Url url: String): String
}