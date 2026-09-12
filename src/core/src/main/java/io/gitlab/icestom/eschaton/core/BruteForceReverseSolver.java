package io.gitlab.icestom.eschaton.core;

import io.gitlab.icestom.eschaton.kinematics.*;

import java.util.EnumMap;
import java.util.Map;

public class BruteForceReverseSolver {

    public record Result(BoatInput input, boolean exact, double error, double ex, double ey, double ez, Map<BoatInput, D1> errorsByInput) {
        public static Result exactMatch(BoatInput input, Map<BoatInput, D1> errorsByInput) {
            return new Result(input, true, 0.0, 0.0, 0.0, 0.0, errorsByInput);
        }

        public static Result bestCandidate(BoatInput input, double error, double ex, double ey, double ez, Map<BoatInput, D1> errorsByInput) {
            return new Result(input, false, error, ex, ey, ez, errorsByInput);
        }
    }

    public static Result findInput(BoatModel n1, D0 n0) {
        BoatInput best = null;
        double bestError = Double.POSITIVE_INFINITY;
        double ex = 0, ey = 0, ez = 0;
        Map<BoatInput, D1> errors = new EnumMap<>(BoatInput.class);

        for (BoatInput value : BoatInput.values()) {
            BoatModel after = n1.copy();
            D2 accel = D2.fromInput(value);

            after.applyLocal(accel);

            D1 error = after.d0sub(n0);
            errors.put(value, error);

            if (after.d0xzIsAlmost(n0)) {
                boolean isGravity = error.dy() == 0.03999999910593033;

                if (error.dy() == 0 || isGravity) {
                    errors.put(value, D1.ZERO);
                    return Result.exactMatch(value, errors);
                }
            }

            if (error.d1err() < bestError) {
                bestError = error.d1err();
                ex = error.dx();
                ey = error.dy();
                ez = error.dz();
                best = value;
            }
        }

        return Result.bestCandidate(best, bestError, ex, ey, ez, errors);
    }
}