package com.github.jackstar.coronalis.implementation.data;

import org.bukkit.Location;

import javax.annotation.Nonnull;

/**
 * Estado de calibración individual de un Radiotelescopio en la red Coronalis.
 *
 * <p>Un telescopio completamente calibrado tiene los 5 parámetros al 100 %,
 * lo que maximiza su contribución a la eficiencia del array.</p>
 *
 * <h3>Parámetros de calibración:</h3>
 * <ol>
 *   <li>Azimut (AZIMUTH) — alineación horizontal</li>
 *   <li>Elevación (ELEVATION) — alineación vertical</li>
 *   <li>Frecuencia (FREQUENCY) — sintonización de la banda de radio</li>
 *   <li>Fase (PHASE) — coherencia de fase con la red</li>
 *   <li>Ganancia (GAIN) — amplificación de la señal recibida</li>
 * </ol>
 *
 * <p>Cada parámetro es un valor de 0 (sin calibrar) a 100 (calibración perfecta).</p>
 */
public final class TelescopeState {

    /** Ubicación del bloque de telescopio. */
    private final Location loc;

    // ── Parámetros de calibración (0–100) ────────────────────────────────────

    private int azimuthCalib    = 0;
    private int elevationCalib  = 0;
    private int frequencyCalib  = 0;
    private int phaseCalib      = 0;
    private int gainCalib       = 0;

    /** Si true, el telescopio sufrió BEARING_FAILURE y necesita recalibración total. */
    private boolean needsFullRecal = false;

    /** Número de calibraciones totales realizadas (estadística). */
    private int calibCount = 0;

    // ── Telemetría científica estilo AstroControlSim ────────────────────────
    private double azError = 0.0;
    private double elError = 0.0;
    private double signalAmplitude = 0.0;
    private double signalPhase = 0.0;
    private double motorTemp = 25.0;
    private double motorCurrent = 0.0;
    private String runtimeState = "IDLE";
    private double pidKp = PidProfile.DEFAULT.getKp();
    private double pidKi = PidProfile.DEFAULT.getKi();
    private double pidKd = PidProfile.DEFAULT.getKd();
    private int resetCount = 0;
    private int faultCount = 0;

    public TelescopeState(@Nonnull Location loc) {
        this.loc = loc.clone();
    }

    // ── Parámetros ───────────────────────────────────────────────────────────

    public enum CalibParam {
        AZIMUTH    ("§dAzimut",     "§7Alineación horizontal del plato parabólico."),
        ELEVATION  ("§bElevación",  "§7Alineación vertical de la antena."),
        FREQUENCY  ("§eFrequencia", "§7Sintonización de banda de radio (1mm–21cm)."),
        PHASE      ("§5Fase",       "§7Coherencia de fase con la red interferométrica."),
        GAIN       ("§aGanancia",   "§7Factor de amplificación de la señal recibida.");

        public final String label;
        public final String description;

        CalibParam(@Nonnull String label, @Nonnull String description) {
            this.label       = label;
            this.description = description;
        }
    }

    // ── Getters y Setters ────────────────────────────────────────────────────

    public int get(CalibParam param) {
        return switch (param) {
            case AZIMUTH   -> azimuthCalib;
            case ELEVATION -> elevationCalib;
            case FREQUENCY -> frequencyCalib;
            case PHASE     -> phaseCalib;
            case GAIN      -> gainCalib;
        };
    }

    /**
     * Aumenta la calibración de un parámetro en {@code amount} puntos.
     * El valor se limita a [0, 100].
     */
    public void advance(CalibParam param, int amount) {
        switch (param) {
            case AZIMUTH   -> azimuthCalib    = clamp(azimuthCalib    + amount);
            case ELEVATION -> elevationCalib  = clamp(elevationCalib  + amount);
            case FREQUENCY -> frequencyCalib  = clamp(frequencyCalib  + amount);
            case PHASE     -> phaseCalib      = clamp(phaseCalib      + amount);
            case GAIN      -> gainCalib       = clamp(gainCalib       + amount);
        }
    }

    /** Reinicia la calibración de todos los parámetros a 0. */
    public void resetAll() {
        azimuthCalib = elevationCalib = frequencyCalib = phaseCalib = gainCalib = 0;
        needsFullRecal = false;
    }

    /** Marca el telescopio como "dañado" — necesita recalibración total. */
    public void markBearingFailure() {
        // Degradar a la mitad de la calibración anterior
        azimuthCalib   = azimuthCalib   / 2;
        elevationCalib = elevationCalib / 2;
        frequencyCalib = frequencyCalib / 2;
        phaseCalib     = phaseCalib     / 2;
        gainCalib      = gainCalib      / 2;
        needsFullRecal = true;
    }

    // ── Estado global ────────────────────────────────────────────────────────

    /**
     * @return true si todos los parámetros están al 100 %.
     */
    public boolean isFullyCalibrated() {
        return azimuthCalib == 100 && elevationCalib == 100
            && frequencyCalib == 100 && phaseCalib == 100
            && gainCalib == 100;
    }

    /**
     * Media aritmética de los 5 parámetros (0.0–1.0).
     * Usada para calcular la contribución del telescopio al array.
     */
    public double getCalibrationFactor() {
        return (azimuthCalib + elevationCalib + frequencyCalib + phaseCalib + gainCalib) / 500.0;
    }

    public boolean needsFullRecal() { return needsFullRecal; }
    public void clearRecalFlag()    { needsFullRecal = false; }

    public int getCalibCount()      { return calibCount; }
    public void incrementCalibCount() { calibCount++; }

    @Nonnull public Location getLoc() { return loc.clone(); }

    public double getAzError() { return azError; }
    public double getElError() { return elError; }
    public double getSignalAmplitude() { return signalAmplitude; }
    public double getSignalPhase() { return signalPhase; }
    public double getMotorTemp() { return motorTemp; }
    public double getMotorCurrent() { return motorCurrent; }
    @Nonnull public String getRuntimeState() { return runtimeState; }
    public double getPidKp() { return pidKp; }
    public double getPidKi() { return pidKi; }
    public double getPidKd() { return pidKd; }
    public int getResetCount() { return resetCount; }
    public int getFaultCount() { return faultCount; }

    public void tunePid(double kp, double ki, double kd) {
        pidKp = clampDouble(kp, 0.05, 5.0);
        pidKi = clampDouble(ki, 0.0, 1.0);
        pidKd = clampDouble(kd, 0.0, 2.0);
    }

    public void tunePid(@Nonnull PidProfile profile) {
        tunePid(profile.getKp(), profile.getKi(), profile.getKd());
    }

    public void resetRuntime() {
        needsFullRecal = false;
        runtimeState = "IDLE";
        motorCurrent = 0.5;
        motorTemp = Math.max(25.0, motorTemp - 8.0);
        resetCount++;
    }

    public void injectFault(@Nonnull String reason) {
        needsFullRecal = true;
        runtimeState = "FAULT";
        motorCurrent = 0.0;
        motorTemp = Math.min(95.0, motorTemp + 10.0);
        faultCount++;
    }

    /**
     * Actualiza una telemetría simplificada inspirada en AstroControlSim:
     * error de apuntado, señal compleja, fase geométrica y sensores de salud.
     */
    public void updateTelemetry(@Nonnull Location consoleLoc,
                                double currentAz, double currentEl,
                                double targetAz, double targetEl,
                                double windPhase) {
        azError = targetAz - currentAz;
        elError = targetEl - currentEl;
        double pointingError = Math.sqrt((azError * azError) + (elError * elError));
        double calibration = getCalibrationFactor();

        runtimeState = needsFullRecal ? "FAULT"
            : pointingError < 0.1 ? "TRACKING"
            : pointingError > 0.1 ? "SLEWING"
            : "IDLE";

        double dx = loc.getBlockX() - consoleLoc.getBlockX();
        double dz = loc.getBlockZ() - consoleLoc.getBlockZ();
        double azRad = Math.toRadians(currentAz);
        double elRad = Math.toRadians(currentEl);
        double sx = Math.cos(elRad) * Math.sin(azRad);
        double sy = Math.cos(elRad) * Math.cos(azRad);

        double lambda = 0.001;
        double waveK = (2.0 * Math.PI) / lambda;
        signalPhase = positiveModulo(waveK * ((dx * sx) + (dz * sy)), 2.0 * Math.PI);
        signalAmplitude = Math.max(0.0, calibration * (1.0 - Math.min(0.75, pointingError / 180.0)));

        double windLoad = Math.abs(Math.sin(windPhase + (dx * 0.13) + (dz * 0.07)));
        double pidAggression = Math.max(0.25, pidKp + pidKd - pidKi);
        motorCurrent = "SLEWING".equals(runtimeState)
            ? 2.0 + ((pointingError / 30.0) * pidAggression) + windLoad
            : 0.5 + (windLoad * 0.5);
        motorTemp = Math.max(20.0, Math.min(95.0,
            25.0 + (motorCurrent * 3.0) + ((1.0 - calibration) * 12.0)));
    }

    // ── Bar util ─────────────────────────────────────────────────────────────

    /**
     * Genera una barra de progreso de 10 chars para un valor 0–100.
     */
    @Nonnull
    public static String bar(int value) {
        int filled = value / 10;
        return (value == 100 ? "§a" : value >= 50 ? "§e" : "§c")
            + "█".repeat(filled) + "§8" + "█".repeat(10 - filled)
            + " §7(" + value + "%)";
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double positiveModulo(double value, double modulo) {
        double result = value % modulo;
        return result < 0 ? result + modulo : result;
    }

    @Override
    public String toString() {
        return "TelescopeState{az=" + azimuthCalib + ",el=" + elevationCalib
            + ",fr=" + frequencyCalib + ",ph=" + phaseCalib + ",ga=" + gainCalib + "}";
    }
}
