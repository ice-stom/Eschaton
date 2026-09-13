package io.gitlab.icestom.eschaton.paper;

public final class HeatTracker {

    private static final double MIN_GRACE_TICKS = 20.0;
    private static final double MAX_GRACE_TICKS = 40.0;
    private static final double GRACE_SCALE = 5.0;

    private long storedHeat = 0;
    private long lastChangeTick = 0L;

    public long gain(long currentTick) {
        storedHeat = get(currentTick) + 1;
        lastChangeTick = currentTick;
        return storedHeat;
    }

    public long get(long currentTick) {
        long elapsed = Math.max(0L, currentTick - lastChangeTick);
        double grace = graceTicksFor(storedHeat);

        if (elapsed <= grace) {
            return storedHeat;
        }

        long decayedTicks = elapsed - (long) Math.ceil(grace);
        return Math.max(0L, storedHeat - decayedTicks);
    }

    public boolean isExpired(long currentTick) {
        return get(currentTick) <= 0;
    }

    public void reset() {
        storedHeat = 0;
        lastChangeTick = 0L;
    }

    private double graceTicksFor(long heat) {
        if (heat <= 0) return MIN_GRACE_TICKS;
        double t = 1.0 - Math.exp(-heat / GRACE_SCALE);
        return MIN_GRACE_TICKS + t * (MAX_GRACE_TICKS - MIN_GRACE_TICKS);
    }
}