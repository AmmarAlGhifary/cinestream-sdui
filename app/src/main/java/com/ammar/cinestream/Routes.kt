package com.ammar.cinestream

/**
 * The route to a movie list. `movie_id` is optional and left out when absent: interpolating a null
 * would send the text "null" to the server.
 */
internal fun movieListRoute(listType: String, movieId: String? = null): String {
    val route = "movie_list_screen/$listType"
    return if (movieId == null) route else "$route?movie_id=$movieId"
}
