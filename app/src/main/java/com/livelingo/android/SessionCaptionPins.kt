package com.livelingo.android

/**
 * Opt-in, in-memory bookmarks for the current live conversation only.
 * IDs are never saved, uploaded or included in the personal word backup.
 */
internal object SessionCaptionPins {
    const val MAX_PINS = 80

    fun toggle(pins: Set<Long>, captionId: Long, liveIds: Set<Long>): Set<Long> {
        if (captionId !in liveIds) return pins
        if (captionId in pins) return pins - captionId
        if (pins.size >= MAX_PINS) return pins
        return pins + captionId
    }

    fun prune(pins: Set<Long>, liveIds: Set<Long>): Set<Long> =
        pins.intersect(liveIds)
}
