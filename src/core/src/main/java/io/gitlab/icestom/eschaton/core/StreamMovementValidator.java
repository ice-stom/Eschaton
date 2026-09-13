package io.gitlab.icestom.eschaton.core;

import io.gitlab.icestom.eschaton.kinematics.*;

public class StreamMovementValidator {
    private final BoatModel model;
    private final WorldLike world;

    private final RegardSpeed regardSpeed;
    private final RegardSlime regardSlime;
    private final RegardGravity regardGravity;

    private int countWalltapTime = 0;

    StreamMovementValidator(D0 d0, WorldLike world,
                            RegardSpeed regardSpeed,
                            RegardSlime regardSlime,
                            RegardGravity regardGravity) {
        this.model = new BoatModel(d0, D1.ZERO, world);
        this.world = world;
        this.regardSpeed = regardSpeed;
        this.regardSlime = regardSlime;
        this.regardGravity = regardGravity;
    }

    public BruteForceReverseSolver.Result update(D0 now) {
        D1 delta = now.d0sub(model);


        if (regardGravity == RegardGravity.SKIP && model.dy() != 0) {
            model.goTo(now, delta);
            return null;
        }

        if (regardGravity == RegardGravity.SKIP && delta.dy() != 0) {
            model.goTo(now, delta);
            return null;
        }

        if (regardSlime == RegardSlime.SKIP && world.isSlime((int) model.x(), (int) Math.floor(model.y() - 0.1), (int) model.z())) {
            model.goTo(now, delta);
            return null;
        }

        boolean flagAllowWalls = false;

        if (regardSpeed == RegardSpeed.IGNORE_WALL) {
            flagAllowWalls =
                    (Math.abs(delta.dx()) < 1e-6 && Math.abs(delta.dz()) > 1e-6 && Math.abs((now.yaw()) % 180) > 1e-6) ||
                    (Math.abs(delta.dz()) < 1e-6 && Math.abs(delta.dx()) > 1e-6 && Math.abs((now.yaw() - 90) % 180) > 1e-6);
        }

        boolean wasSlower = delta.d1err() <  model.d1err();
        boolean couldBeWalltap = wasSlower;

        if (!couldBeWalltap) couldBeWalltap = delta.dx() * model.dx() <= 1e-6;
        if (!couldBeWalltap) couldBeWalltap = delta.dz() * model.dz() <= 1e-6;

        if (regardSpeed == RegardSpeed.FASTER && wasSlower) {
            model.goTo(now, delta);
            return null;
        }

        BruteForceReverseSolver.Result res = BruteForceReverseSolver.findInput(model, now);

        float oldYaw = model.yaw();
        float newYaw = now.yaw();

        boolean crossesWrap = (oldYaw > 0.0F && newYaw < 0.0F) || (oldYaw < 0.0F && newYaw > 0.0F);

        if (!res.exact() && flagAllowWalls) {
            model.goTo(now, delta);
            return null;
        }

        if (crossesWrap) {
            model.goTo(now, delta);
            return null;
        }

        // Allow 1 tick to collide x/z (it's very hard to predict wall taps unless we start solving against hitboxes)
        // 2nd tick included for cases where you immediately accelerate away from the wall after tapping, d1 is still cooked from last tick
        if (regardSpeed == RegardSpeed.IGNORE_WALL && !res.exact() && couldBeWalltap) {
            if (countWalltapTime < 2) {
                countWalltapTime++;

                model.goTo(now, delta);
                return null;
            }
        } else {
            countWalltapTime = 0;
        }

        model.goTo(now, delta);

        return res;
    }

    public enum RegardSpeed {
        ALWAYS,
        IGNORE_WALL,
        FASTER
    }

    public enum RegardSlime {
        ALWAYS,
        SKIP
    }

    public enum RegardGravity {
        ENABLE_THIS_IS_BUGGY,
        SKIP
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private RegardSpeed regardSpeed = RegardSpeed.IGNORE_WALL;
        private RegardSlime regardSlime = RegardSlime.SKIP;
        private RegardGravity regardGravity = RegardGravity.SKIP;

        public Builder regardSpeed(RegardSpeed speed) {
            this.regardSpeed = speed;
            return this;
        }

        public Builder regardSlime(RegardSlime slime) {
            this.regardSlime = slime;
            return this;
        }

        public Builder regardGravity(RegardGravity gravity) {
            this.regardGravity = gravity;
            return this;
        }

        public StreamMovementValidator build(D0 now, WorldLike world) {
            return new StreamMovementValidator(
                    now,
                    world,
                    regardSpeed,
                    regardSlime,
                    regardGravity
            );
        }
    }
}
