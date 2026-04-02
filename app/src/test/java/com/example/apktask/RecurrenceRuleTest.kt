package com.example.apktask.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [RecurrenceRule.isDueOn].
 *
 * Reference dates (verified against a perpetual calendar):
 *   2026-03-09 = Monday
 *   2026-03-10 = Tuesday
 *   2026-03-11 = Wednesday
 *   2026-03-12 = Thursday
 *   2026-03-13 = Friday
 *   2026-03-14 = Saturday
 *   2026-03-15 = Sunday
 */
class RecurrenceRuleTest {

    // ── DAILY ─────────────────────────────────────────────────────────────────

    @Test
    fun `DAILY is due on a weekday`() {
        assertTrue(RecurrenceRule.daily().isDueOn("2026-03-09")) // Monday
    }

    @Test
    fun `DAILY is due on a Saturday`() {
        assertTrue(RecurrenceRule.daily().isDueOn("2026-03-14")) // Saturday
    }

    @Test
    fun `DAILY is due on a Sunday`() {
        assertTrue(RecurrenceRule.daily().isDueOn("2026-03-15")) // Sunday
    }

    // ── WEEKDAYS ──────────────────────────────────────────────────────────────

    @Test
    fun `WEEKDAYS is due on Monday`() {
        assertTrue(RecurrenceRule.weekdays().isDueOn("2026-03-09"))
    }

    @Test
    fun `WEEKDAYS is due on Friday`() {
        assertTrue(RecurrenceRule.weekdays().isDueOn("2026-03-13"))
    }

    @Test
    fun `WEEKDAYS is not due on Saturday`() {
        assertFalse(RecurrenceRule.weekdays().isDueOn("2026-03-14"))
    }

    @Test
    fun `WEEKDAYS is not due on Sunday`() {
        assertFalse(RecurrenceRule.weekdays().isDueOn("2026-03-15"))
    }

    // ── WEEKLY ────────────────────────────────────────────────────────────────

    @Test
    fun `WEEKLY Wednesday is due on Wednesday`() {
        val rule = RecurrenceRule.weekly(Calendar.WEDNESDAY)
        assertTrue(rule.isDueOn("2026-03-11")) // Wednesday
    }

    @Test
    fun `WEEKLY Wednesday is not due on Thursday`() {
        val rule = RecurrenceRule.weekly(Calendar.WEDNESDAY)
        assertFalse(rule.isDueOn("2026-03-12")) // Thursday
    }

    @Test
    fun `WEEKLY Monday is due every Monday`() {
        val rule = RecurrenceRule.weekly(Calendar.MONDAY)
        assertTrue(rule.isDueOn("2026-03-09"))  // Monday week 1
        assertTrue(rule.isDueOn("2026-03-16"))  // Monday week 2
        assertFalse(rule.isDueOn("2026-03-10")) // Tuesday
    }

    // ── CUSTOM ────────────────────────────────────────────────────────────────

    @Test
    fun `CUSTOM Monday+Wednesday is due on Monday`() {
        val rule = RecurrenceRule.custom(Calendar.MONDAY, Calendar.WEDNESDAY)
        assertTrue(rule.isDueOn("2026-03-09")) // Monday
    }

    @Test
    fun `CUSTOM Monday+Wednesday is due on Wednesday`() {
        val rule = RecurrenceRule.custom(Calendar.MONDAY, Calendar.WEDNESDAY)
        assertTrue(rule.isDueOn("2026-03-11")) // Wednesday
    }

    @Test
    fun `CUSTOM Monday+Wednesday is not due on Tuesday`() {
        val rule = RecurrenceRule.custom(Calendar.MONDAY, Calendar.WEDNESDAY)
        assertFalse(rule.isDueOn("2026-03-10")) // Tuesday
    }

    @Test
    fun `CUSTOM Monday+Wednesday is not due on Saturday`() {
        val rule = RecurrenceRule.custom(Calendar.MONDAY, Calendar.WEDNESDAY)
        assertFalse(rule.isDueOn("2026-03-14")) // Saturday
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `isDueOn returns false for invalid date`() {
        assertFalse(RecurrenceRule.daily().isDueOn("not-a-date"))
    }

    @Test
    fun `isDueOn returns false for blank date`() {
        assertFalse(RecurrenceRule.daily().isDueOn(""))
    }

    @Test
    fun `CUSTOM with empty bitmask is never due`() {
        // custom() with no days → bitmask = 0 → no bit ever matches
        val rule = RecurrenceRule(Frequency.CUSTOM, 0)
        assertFalse(rule.isDueOn("2026-03-09"))
        assertFalse(rule.isDueOn("2026-03-15"))
    }
}
