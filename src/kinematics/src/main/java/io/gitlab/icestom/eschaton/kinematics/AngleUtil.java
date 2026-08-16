package io.gitlab.icestom.eschaton.kinematics;

public interface AngleUtil {
    static float normalizeYaw(float yaw) {
        yaw %= 360.0F;

        if (yaw <= -180.0F) {
            yaw += 360.0F;
        } else if (yaw > 180.0F) {
            yaw -= 360.0F;
        }

        return yaw;
    }
}
