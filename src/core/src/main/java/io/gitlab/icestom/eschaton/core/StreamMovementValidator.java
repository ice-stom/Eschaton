package io.gitlab.icestom.eschaton.core;

import io.gitlab.icestom.eschaton.kinematics.*;

public class StreamMovementValidator {
    private final BoatModel model;

    public StreamMovementValidator(D0 d0, WorldLike world) {
        this.model = new BoatModel(d0, D1.ZERO, world);
    }

    public BruteForceReverseSolver.Result update(D0 now) {
        BruteForceReverseSolver.Result res = BruteForceReverseSolver.findInput(model, now);

        float oldYaw = model.yaw();
        float newYaw = now.yaw();

        boolean crossesWrap = (oldYaw > 0.0F && newYaw < 0.0F) || (oldYaw < 0.0F && newYaw > 0.0F);

        model.goTo(now, now.d0sub(model));

        if (model.dy() != 0) {
            return null;
        }

        if (crossesWrap) {
            return null;
        }

        return res;
    }
}
