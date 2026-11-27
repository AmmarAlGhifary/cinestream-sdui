package com.ammar.cinestream

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `a list route carries the movie id when there is one`() {
        assertEquals("movie_list_screen/similar?movie_id=42", movieListRoute("similar", "42"))
    }

    @Test
    fun `a list route leaves the movie id out when there is none`() {
        assertEquals("movie_list_screen/trending", movieListRoute("trending", null))
        assertEquals("movie_list_screen/trending", movieListRoute("trending"))
    }
}
