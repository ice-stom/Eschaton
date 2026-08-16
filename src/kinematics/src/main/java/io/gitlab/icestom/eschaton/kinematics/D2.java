package io.gitlab.icestom.eschaton.kinematics;

public interface D2 {
    double ax();
    double ay();
    double az();
    float aYaw();

    D2 ZERO = new D2Record(0, 0, 0, 0);

    static D2 fromInput(BoatInput input) {

        KeyInput keys = input.getKeys();

        float fw = 0;
        float rt = 0;

        if (keys.w()) fw += 0.04f;
        if (keys.s()) fw -= 0.005f;
        if (keys.a()) rt -= 1f;
        if (keys.d()) rt += 1f;

        if (input == BoatInput.A || input == BoatInput.D) {
            fw += 0.005f;
        }

        return new D2Record(0, 0, fw, rt);
    }

    record D2Record(
            double ax,
            double ay,
            double az,
            float aYaw
    ) implements D2 {}
}
