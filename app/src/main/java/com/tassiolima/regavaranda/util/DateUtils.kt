package com.tassiolima.regavaranda.util

import java.time.Instant
import java.time.ZoneId

object DateUtils {
    fun epochDayOf(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun todayEpochDay(): Long =
        Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
}
