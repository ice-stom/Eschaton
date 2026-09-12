package io.gitlab.icestom.eschaton.kinematics;

public interface WorldLike {
    Float getBlockSlipperiness(int x, int y, int z);
    boolean isWater(int x, int y, int z);
    boolean isSlime(int x, int y, int z);
}
