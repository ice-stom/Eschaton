package io.gitlab.icestom.eschaton.kinematics;

import java.util.HashMap;
import java.util.Map;

public enum BoatInput {
    NONE,
    W,
    WS,
    WA,
    WD,
    S,
    SA,
    SD,
    A,
    D,
    WSA,
    WSD;

    private static final Map<BoatInput, KeyInput> keys = new HashMap<>();

    static {
        for (BoatInput value : values()) {
            String name = value.toString();

            boolean w = name.contains("W");
            boolean s = name.contains("S");
            boolean a = name.contains("A");
            boolean d = name.contains("D");

            keys.put(value, new KeyInput(w, s, a, d));
        }
    }

    public KeyInput getKeys() {
        return keys.get(this);
    }
}