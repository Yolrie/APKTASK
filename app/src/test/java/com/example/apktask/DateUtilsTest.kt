package com.example.apktask.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    // ── dayOfWeekFor ──────────────────────────────────────────────────────────

    @Test
    fun `dayOfWeekFor returns MONDAY for 2026-03-09`() {
        // 2026-03-09 is a Monday
        assertEquals(Calendar.MONDAY, DateUtils.dayOfWeekFor("2026-03-09"))
    }

    @Test
    fun `dayOfWeekFor returns FRIDAY for 2026-03-13`() {
        assertEquals(Calendar.FRIDAY, DateUtils.dayOfWeekFor("2026-03-13"))
    }

    @Test
    fun `dayOfWeekFor returns SATURDAY for 2026-03-14`() {
        assertEquals(Calendar.SATURDAY, DateUtils.dayOfWeekFor("2026-03-14"))
    }

    @Test
    fun `dayOfWeekFor returns SUNDAY for 2026-03-15`() {
        assertEquals(Calendar.SUNDAY, DateUtils.dayOfWeekFor("2026-03-15"))
    }

    @Test
    fun `dayOfWeekFor returns null for blank string`() {
        assertNull(DateUtils.dayOfWeekFor(""))
    }

    @Test
    fun `dayOfWeekFor returns null for invalid format`() {
        assertNull(DateUtils.dayOfWeekFor("not-a-date"))
    }

    // ── areConsecutiveDays ────────────────────────────────────────────────────

    @Test
    fun `areConsecutiveDays true for adjacent dates`() {
        assertTrue(DateUtils.areConsecutiveDays("2026-03-09", "2026-03-10"))
    }

    @Test
    fun `areConsecutiveDays true across month boundary`() {
        assertTrue(DateUtils.areConsecutiveDays("2026-03-31", "2026-04-01"))
    }

    @Test
    fun `areConsecutiveDays true across year boundary`() {
        assertTrue(DateUtils.areConsecutiveDays("2025-12-31", "2026-01-01"))
    }

    @Test
    fun `areConsecutiveDays false for same day`() {
        assertFalse(DateUtils.areConsecutiveDays("2026-03-09", "2026-03-09"))
    }

    @Test
    fun `areConsecutiveDays false for gap of two days`() {
        assertFalse(DateUtils.areConsecutiveDays("2026-03-09", "2026-03-11"))
    }

    @Test
    fun `areConsecutiveDays false for blank earlier`() {
        assertFalse(DateUtils.areConsecutiveDays("", "2026-03-10"))
    }

    @Test
    fun `areConsecutiveDays false for blank later`() {
        assertFalse(DateUtils.areConsecutiveDays("2026-03-09", ""))
    }
}
