package com.tassiolima.regavaranda.util

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `epochDayOf matches the local date derived from the timestamp`() {
        val millis = 1_700_000_000_000L
        val expected = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        assertEquals(expected, DateUtils.epochDayOf(millis))
    }

    @Test
    fun `epochDayOf increases by one after a full day`() {
        val millis = 1_700_000_000_000L
        val nextDay = millis + 86_400_000L

        assertEquals(DateUtils.epochDayOf(millis) + 1, DateUtils.epochDayOf(nextDay))
    }

    @Test
    fun `todayEpochDay matches the current local date`() {
        val expected = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        assertEquals(expected, DateUtils.todayEpochDay())
    }
}
