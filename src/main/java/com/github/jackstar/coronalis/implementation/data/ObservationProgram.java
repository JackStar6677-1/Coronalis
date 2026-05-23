package com.github.jackstar.coronalis.implementation.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Programas científicos de observatorio.
 *
 * <p>Funcionan como misiones: requieren objetivo, tamaño de array, baselines,
 * calibración, señal y a veces módulos específicos.</p>
 */
public enum ObservationProgram {
    ORION_MOLECULAR_MAP(
        "orion_molecular_map",
        "Mapa Molecular de Orión",
        CelestialTarget.HORSEHEAD_NEBULA,
        4, 6, 60, 1.2,
        null,
        140,
        "Cartografiar gas frío y moléculas orgánicas en la Nebulosa Cabeza de Caballo."
    ),
    CRAB_SYNCHROTRON_WATCH(
        "crab_synchrotron_watch",
        "Vigilancia Sincrotrón del Cangrejo",
        CelestialTarget.CRAB_NEBULA,
        6, 15, 65, 1.8,
        "CORONALIS_SIGNAL_AMPLIFIER",
        180,
        "Medir variaciones de flujo por emisión de sincrotrón del remanente de supernova."
    ),
    ANDROMEDA_HI_SURVEY(
        "andromeda_hi_survey",
        "Survey HI de Andrómeda",
        CelestialTarget.ANDROMEDA,
        8, 28, 70, 2.4,
        "CORONALIS_DATA_BANK",
        220,
        "Observar línea de hidrógeno neutro y extraer estructura de disco galáctico."
    ),
    PULSAR_TIMING_RUN(
        "pulsar_timing_run",
        "Timing de Púlsar B1919+21",
        CelestialTarget.PSR_B1919,
        12, 66, 80, 4.0,
        "CORONALIS_AUTO_CALIBRATOR",
        300,
        "Mantener fase estable para medir pulsos periódicos de estrella de neutrones."
    ),
    SAG_A_PHASE_LOCK(
        "sag_a_phase_lock",
        "Bloqueo de Fase Sagitario A*",
        CelestialTarget.SAGITTARIUS_A,
        16, 120, 85, 6.0,
        "CORONALIS_SIGNAL_AMPLIFIER",
        380,
        "Sincronizar un array grande contra el centro galáctico y sus variaciones rápidas."
    ),
    KEPLER_THERMAL_SIGNATURE(
        "kepler_thermal_signature",
        "Firma Térmica Kepler-186f",
        CelestialTarget.KEPLER_186F,
        24, 276, 90, 10.0,
        "CORONALIS_DATA_BANK",
        520,
        "Integrar señal débil de exoplaneta terrestre con alto buffer de datos."
    ),
    M87_EVENT_HORIZON_FRAME(
        "m87_event_horizon_frame",
        "Frame de Horizonte M87*",
        CelestialTarget.M87_BLACK_HOLE,
        32, 496, 95, 14.0,
        "CORONALIS_SIGNAL_AMPLIFIER",
        700,
        "Generar una visibilidad de larga base para emular observación tipo EHT."
    ),
    FULL_ARRAY_FIRST_LIGHT(
        "full_array_first_light",
        "Primera Luz del Array Completo",
        CelestialTarget.M87_BLACK_HOLE,
        50, 1225, 98, 22.0,
        "CORONALIS_AUTO_CALIBRATOR",
        1000,
        "Operar los 50 radiotelescopios con calibración casi perfecta y máxima ciencia."
    );

    private final String id;
    private final String displayName;
    private final CelestialTarget target;
    private final int minTelescopes;
    private final int minBaselines;
    private final int minCalibrationPercent;
    private final double minSignal;
    private final String requiredModule;
    private final int xpReward;
    private final String description;

    ObservationProgram(@Nonnull String id, @Nonnull String displayName,
                       @Nonnull CelestialTarget target, int minTelescopes,
                       int minBaselines, int minCalibrationPercent, double minSignal,
                       @Nullable String requiredModule, int xpReward,
                       @Nonnull String description) {
        this.id = id;
        this.displayName = displayName;
        this.target = target;
        this.minTelescopes = minTelescopes;
        this.minBaselines = minBaselines;
        this.minCalibrationPercent = minCalibrationPercent;
        this.minSignal = minSignal;
        this.requiredModule = requiredModule;
        this.xpReward = xpReward;
        this.description = description;
    }

    @Nonnull public String getId() { return id; }
    @Nonnull public String getDisplayName() { return displayName; }
    @Nonnull public CelestialTarget getTarget() { return target; }
    public int getMinTelescopes() { return minTelescopes; }
    public int getMinBaselines() { return minBaselines; }
    public int getMinCalibrationPercent() { return minCalibrationPercent; }
    public double getMinSignal() { return minSignal; }
    @Nullable public String getRequiredModule() { return requiredModule; }
    public int getXpReward() { return xpReward; }
    @Nonnull public String getDescription() { return description; }

    @Nullable
    public static ObservationProgram byId(@Nonnull String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        for (ObservationProgram program : values()) {
            if (program.id.equals(normalized)) {
                return program;
            }
        }
        return null;
    }
}
