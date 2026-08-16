package io.gitlab.icestom.eschaton.kinematics;

public interface D1 {
    double dx();
    double dy();
    double dz();
    float dYaw();

    D1 ZERO = new D1Record(0, 0, 0, 0);

    default boolean d1is(D0 other) {
        return dx() == other.x() && dy() == other.y() && dz() == other.z();
    }

    default double d1err() {
        return Math.sqrt(
                dx() * dx() +
                dy() * dy() +
                dz() * dz()
        );
    }

    record D1Record(
            double dx,
            double dy,
            double dz,
            float dYaw
    ) implements D1 {}
}
