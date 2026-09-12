package io.gitlab.icestom.eschaton.kinematics;

import static io.gitlab.icestom.eschaton.kinematics.AngleUtil.normalizeYaw;

public interface D0 {
    double x();
    double y();
    double z();
    float yaw();

    D0 ZERO = new D0Record(0, 0, 0, 0);

    default boolean d0is(D0 other) {
        return x() == other.x() && y() == other.y() && z() == other.z();
    }

    default boolean d0isAlmost(D0 other) {
        return Math.abs(other.x() - x()) < 1e-5 && Math.abs(other.y() - y()) < 1e-5 && Math.abs(other.z() - z()) < 1e-5;
    }

    default boolean d0xzIsAlmost(D0 other) {
        return Math.abs(other.x() - x()) < 1e-5 && Math.abs(other.z() - z()) < 1e-5;
    }

    default D1 d0sub(D0 other) {
        return new D1.D1Record(
                x() - other.x(),
                y() - other.y(),
                z() - other.z(),
                normalizeYaw(yaw() - other.yaw())
        );
    }

    default double d0err() {
        return Math.sqrt(
                x() * x() +
                y() * y() +
                z() * z()
        );
    }

    private static float angleDifference(float a, float b) {
        float diff = (a - b) % 360.0F;

        if (diff <= -180.0F) {
            diff += 360.0F;
        } else if (diff > 180.0F) {
            diff -= 360.0F;
        }

        return diff;
    }

    record D0Record(
            double x,
            double y,
            double z,
            float yaw
    ) implements D0 {}
}
