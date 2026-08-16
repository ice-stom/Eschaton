package io.gitlab.icestom.eschaton.kinematics;

@FunctionalInterface
public interface WorldLike {
    Float getBlockSlipperiness(int x, int y, int z);
}
