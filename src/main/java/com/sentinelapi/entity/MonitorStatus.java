package com.sentinelapi.entity;

/**
 * Represents the current operational status of a monitor.
 *
 * <p>This enum maps to the {@code status} VARCHAR column in the {@code monitors} table.
 * It is stored as a STRING in the database via {@code @Enumerated(EnumType.STRING)}.
 *
 * <h3>State Transitions</h3>
 * <pre>
 *   UNKNOWN ──(first successful check)──→ UP
 *   UNKNOWN ──(failures ≥ threshold)──→ DOWN
 *   UP ──────(failures ≥ threshold)──→ DOWN
 *   DOWN ────(successful check)──────→ UP
 *   Any ─────(monitor.active = false)─→ PAUSED
 *   PAUSED ──(monitor.active = true)──→ UNKNOWN (reset, re-evaluates on next check)
 * </pre>
 *
 * <h3>Why use an enum instead of a String?</h3>
 * <ul>
 *   <li>Type safety — the compiler prevents typos like "up" or "Active"</li>
 *   <li>IDE auto-complete — you see all valid options</li>
 *   <li>Refactoring safety — renaming a status updates all references</li>
 *   <li>Switch exhaustiveness — the compiler can warn if you forget a case</li>
 * </ul>
 */
public enum MonitorStatus {

    /** The monitored endpoint is responding correctly. */
    UP,

    /** The monitored endpoint has failed consecutive checks above the failure threshold. */
    DOWN,

    /**
     * The monitor has never been checked yet, or was just re-activated.
     * This is the initial state for all new monitors.
     */
    UNKNOWN,

    /**
     * The monitor is manually deactivated ({@code active = false}).
     * The scheduler will not check paused monitors.
     */
    PAUSED
}
