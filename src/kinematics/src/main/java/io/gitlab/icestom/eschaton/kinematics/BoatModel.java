package io.gitlab.icestom.eschaton.kinematics;

import static io.gitlab.icestom.eschaton.kinematics.AngleUtil.normalizeYaw;

public class BoatModel implements D0, D1 {

    private static final double BOAT_HALF_WIDTH = 1.375 / 2.0;
    private static final double BOAT_HEIGHT = 0.5625;

    private static final float DEFAULT_SLIPPERINESS = 0.6F;

    private double x;
    private double y;
    private double z;
    private float yaw;
    private double dx;
    private double dy;
    private double dz;
    private float dYaw;
    private final WorldLike world;

    public BoatModel(D0 position, D1 velocity, WorldLike world) {
        this.x = position.x();
        this.y = position.y();
        this.z = position.z();
        this.yaw = position.yaw();
        this.dx = velocity.dx();
        this.dy = velocity.dy();
        this.dz = velocity.dz();
        this.dYaw = velocity.dYaw();
        this.world = world;
    }

    private Float getMu() {
        double halfWidth = BOAT_HALF_WIDTH;

        double boxMinX = x - halfWidth;
        double boxMaxX = x + halfWidth;
        double boxMinY = y;
        double boxMinZ = z - halfWidth;
        double boxMaxZ = z + halfWidth;

        double sliceMinX = boxMinX;
        double sliceMaxX = boxMaxX;
        double sliceMinY = boxMinY - 0.001;
        double sliceMaxY = boxMinY;
        double sliceMinZ = boxMinZ;
        double sliceMaxZ = boxMaxZ;

        int i = (int) Math.floor(sliceMinX) - 1;
        int j = (int) Math.ceil(sliceMaxX) + 1;
        int k = (int) Math.floor(sliceMinY) - 1;
        int l = (int) Math.ceil(sliceMaxY) + 1;
        int m = (int) Math.floor(sliceMinZ) - 1;
        int n = (int) Math.ceil(sliceMaxZ) + 1;

        float sum = 0.0F;
        int count = 0;

        for (int p = i; p < j; ++p) {
            for (int q = m; q < n; ++q) {
                int r = (p != i && p != j - 1 ? 0 : 1) + (q != m && q != n - 1 ? 0 : 1);
                if (r != 2) {
                    for (int s = k; s < l; ++s) {
                        if (r <= 0 || (s != k && s != l - 1)) {
                            boolean overlaps = p < sliceMaxX && (p + 1) > sliceMinX
                                    && s < sliceMaxY && (s + 1) > sliceMinY
                                    && q < sliceMaxZ && (q + 1) > sliceMinZ;

                            if (overlaps) {
                                if (world.isWater(p, s, q)) {
                                    return 0.9f;
                                }

                                Float slipperiness = world.getBlockSlipperiness(p, s, q);
                                if (slipperiness != null) {
                                    sum += slipperiness;
                                    ++count;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (count == 0) return null;

        return sum / (float) count;
    }

    public void applyLocal(D2 accel) {
        Float mu = getMu();

        if (mu == null) mu = world.getAirSlipperiness();

        dYaw *= mu;
        dYaw += accel.aYaw();

        yaw += dYaw;
        yaw = normalizeYaw(yaw);

        double yawR = Math.toRadians(yaw);
        double cos = Math.cos(yawR);
        double sin = Math.sin(yawR);

        double ax = accel.ax();
        double az = accel.az();

        double rotatedX = ax * cos - az * sin;
        double rotatedZ = ax * sin + az * cos;

        applyGlobal(new D2.D2Record(
                rotatedX,
                accel.ay(),
                rotatedZ,
                0
        ), mu);
    }

    private void applyGlobal(D2 accel, float mu) {
        dx *= mu;
        dz *= mu;
        applyD2(accel);
        applyD1(this);

        applyEffectsFromBlocks();
    }

    private void applyEffectsFromBlocks() {
        if (world.isSlime((int) x, (int) Math.floor(y - 0.1), (int) z)) {
            double absDy = Math.abs(dy);
            if (absDy < 0.1) {
                double factor = 0.4 + absDy * 0.2;
                dx *= factor;
                dz *= factor;
            }
        }
    }

    public void goTo(D0 d0, D1 d1) {
        this.x = d0.x();
        this.y = d0.y();
        this.z = d0.z();
        this.yaw = d0.yaw();
        this.dx = d1.dx();
        this.dy = d1.dy();
        this.dz = d1.dz();
        this.dYaw = d1.dYaw();
    }

    private void applyD2(D2 d2) {
        this.dx += d2.ax();
        this.dy += d2.ay();
        this.dz += d2.az();
        this.dYaw += d2.aYaw();
    }

    private void applyD1(D1 d1) {
        this.x += d1.dx();
        this.y += d1.dy();
        this.z += d1.dz();
    }

    public BoatModel copy() {
        return new BoatModel(
                new D0Record(x, y, z, yaw),
                new D1Record(dx, dy, dz, dYaw),
                world
        );
    }

    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public double z() { return z; }
    @Override public float yaw() { return yaw; }

    @Override public double dx() { return dx; }
    @Override public double dy() { return dy; }
    @Override public double dz() { return dz; }
    @Override public float dYaw() { return dYaw; }
}