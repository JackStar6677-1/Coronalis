package com.github.jackstar.coronalis.implementation.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Presets de PID inspirados en el panel web de AstroControlSim.
 */
public enum PidProfile {
    DEFAULT("default", 1.20, 0.05, 0.30, "Equilibrado para operación normal."),
    AGGRESSIVE("aggressive", 2.00, 0.10, 0.50, "Respuesta rápida, mayor riesgo de overshoot."),
    DAMPED("damped", 0.80, 0.02, 0.20, "Movimiento suave para arrays grandes o viento alto."),
    SLUGGISH("sluggish", 0.50, 0.01, 0.10, "Seguro y lento, útil tras fallos o baja energía."),
    PRECISION("precision", 1.05, 0.03, 0.42, "Bloqueo fino para correlación de alta fase."),
    STORM_SAFE("storm_safe", 0.65, 0.01, 0.35, "Reduce corriente durante tormentas magnéticas.");

    private final String id;
    private final double kp;
    private final double ki;
    private final double kd;
    private final String description;

    PidProfile(@Nonnull String id, double kp, double ki, double kd, @Nonnull String description) {
        this.id = id;
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.description = description;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public double getKp() {
        return kp;
    }

    public double getKi() {
        return ki;
    }

    public double getKd() {
        return kd;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    @Nullable
    public static PidProfile byId(@Nonnull String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        for (PidProfile profile : values()) {
            if (profile.id.equals(normalized)) {
                return profile;
            }
        }
        return null;
    }
}
