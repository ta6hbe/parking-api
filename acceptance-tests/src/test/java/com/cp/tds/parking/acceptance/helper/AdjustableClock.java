package com.cp.tds.parking.acceptance.helper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Supplies adjustable time for acceptance tests.
 * Zone-specific copies share the same thread-safe instant.
 */
public class AdjustableClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zoneId;

    public AdjustableClock(String formattedTime) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(formattedTime);
        this.zoneId = zonedDateTime.getZone();
        this.instant = new AtomicReference<>(zonedDateTime.toInstant());
    }

    private AdjustableClock(AtomicReference<Instant> instant, ZoneId zoneId) {
        this.instant = instant;
        this.zoneId = Objects.requireNonNull(zoneId);
    }

    @Override
    public ZoneId getZone() {
        return this.zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new AdjustableClock(this.instant, zone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }

    public void setInstant(Instant instant) {
        this.instant.set(Objects.requireNonNull(instant));
    }
}
