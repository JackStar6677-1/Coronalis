package com.github.jackstar.coronalis.managers;

import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.implementation.data.CoronalisNetwork;
import com.github.jackstar.coronalis.implementation.data.PidProfile;
import com.github.jackstar.coronalis.implementation.data.TelescopeState;
import com.github.jackstar.coronalis.implementation.items.ControlConsole;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Capa de operaciones tipo "Central Control" inspirada en AstroControlSim.
 *
 * <p>Esta clase no toca el simulador C++ original. Reproduce sus conceptos en
 * gameplay Slimefun: comandos de movimiento, tuning PID, escaneos, reset,
 * mantenimiento predictivo y export de telemetría.</p>
 */
public class ObservatoryOperations {

    private static final double DEFAULT_SEARCH_RADIUS = 80.0;
    private static final DateTimeFormatter EXPORT_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Coronalis plugin;
    private final Map<String, ScanSession> scanSessions = new HashMap<>();
    private final Map<String, HealthHistory> healthHistories = new HashMap<>();

    public ObservatoryOperations(@Nonnull Coronalis plugin) {
        this.plugin = plugin;
    }

    public void stopAll() {
        for (ScanSession session : scanSessions.values()) {
            session.cancel();
        }
        scanSessions.clear();
        healthHistories.clear();
    }

    @Nullable
    public Location findNearestConsole(@Nonnull Location origin, double maxDistance) {
        Location best = null;
        double bestSq = maxDistance * maxDistance;
        for (Location loc : ControlConsole.ACTIVE_CONSOLES) {
            if (loc.getWorld() == null || origin.getWorld() == null || !loc.getWorld().equals(origin.getWorld())) continue;
            double distSq = loc.distanceSquared(origin);
            if (distSq <= bestSq) {
                best = loc;
                bestSq = distSq;
            }
        }
        return best;
    }

    @Nullable
    public CoronalisNetwork requireNearestNetwork(@Nonnull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[Coronalis] Este comando requiere jugador para ubicar una consola cercana.");
            return null;
        }
        Location consoleLoc = findNearestConsole(player.getLocation(), DEFAULT_SEARCH_RADIUS);
        if (consoleLoc == null) {
            player.sendMessage("§5[Coronalis] §cNo hay consolas activas a " + (int) DEFAULT_SEARCH_RADIUS + " bloques.");
            player.sendMessage("§7Abre una consola una vez para registrarla en memoria y vuelve a intentar.");
            return null;
        }
        plugin.getNetworkRegistry().rebuildNetwork(consoleLoc);
        return plugin.getNetworkRegistry().getOrCreate(consoleLoc);
    }

    public void move(@Nonnull CommandSender sender, double azimuth, double elevation) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        Location loc = network.getConsoleLoc();
        BlockStorage.addBlockInfo(loc, "target_az", String.valueOf(clampAz(azimuth)));
        BlockStorage.addBlockInfo(loc, "target_el", String.valueOf(clampEl(elevation)));
        BlockStorage.addBlockInfo(loc, "selected_target", "Comando manual");
        BlockStorage.addBlockInfo(loc, "correlation_progress", "0");
        plugin.getSoundManager().playAt(loc, SoundManager.CoronalisSound.SLEW_MOTOR);

        sender.sendMessage("§5[Coronalis] §aMOVE enviado al array cercano.");
        sender.sendMessage("§7Target Az/El: §e" + round2(clampAz(azimuth)) + "° §7/ §e" + round2(clampEl(elevation)) + "°");
        Coronalis.log("[Coronalis/Ops] MOVE " + network.getId() + " az=" + azimuth + " el=" + elevation);
    }

    public void reset(@Nonnull CommandSender sender) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        Location loc = network.getConsoleLoc();
        BlockStorage.addBlockInfo(loc, "fault_state", "NORMAL");
        BlockStorage.addBlockInfo(loc, "correlation_progress", "0");
        for (TelescopeState state : network.getTelescopes().values()) {
            state.resetRuntime();
        }
        plugin.getSoundManager().playAt(loc, SoundManager.CoronalisSound.CALIBRATE);
        sender.sendMessage("§5[Coronalis] §aRESET global enviado. Fallos runtime limpiados.");
        Coronalis.log("[Coronalis/Ops] RESET " + network.getId());
    }

    public void injectFault(@Nonnull CommandSender sender, int antennaIndex, @Nonnull String reason) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        TelescopeState state = getByHumanIndex(network, antennaIndex);
        if (state == null) {
            sender.sendMessage("§5[Coronalis] §cNo existe ANT-" + antennaIndex + " en esta red.");
            return;
        }
        state.injectFault(reason);
        BlockStorage.addBlockInfo(network.getConsoleLoc(), "fault_state", "INJECTED_FAULT");
        plugin.getSoundManager().playAt(network.getConsoleLoc(), SoundManager.CoronalisSound.FAULT);
        sender.sendMessage("§5[Coronalis] §cFAULT inyectado en ANT-" + antennaIndex + ": §e" + reason);
        Coronalis.log("[Coronalis/Ops] FAULT " + network.getId() + " ant=" + antennaIndex + " reason=" + reason);
    }

    public void tunePreset(@Nonnull CommandSender sender, @Nonnull String presetId, int antennaIndex) {
        PidProfile profile = PidProfile.byId(presetId);
        if (profile == null) {
            sender.sendMessage("§5[Coronalis] §cPreset desconocido. Usa: default, aggressive, damped, sluggish, precision, storm_safe.");
            return;
        }
        tune(sender, profile.getKp(), profile.getKi(), profile.getKd(), antennaIndex, profile.getId());
    }

    public void tune(@Nonnull CommandSender sender, double kp, double ki, double kd, int antennaIndex, @Nonnull String label) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        int tuned = 0;
        if (antennaIndex <= 0) {
            for (TelescopeState state : network.getTelescopes().values()) {
                state.tunePid(kp, ki, kd);
                tuned++;
            }
        } else {
            TelescopeState state = getByHumanIndex(network, antennaIndex);
            if (state == null) {
                sender.sendMessage("§5[Coronalis] §cNo existe ANT-" + antennaIndex + " en esta red.");
                return;
            }
            state.tunePid(kp, ki, kd);
            tuned = 1;
        }
        sender.sendMessage("§5[Coronalis] §aPID aplicado (" + label + ") a §e" + tuned + "§a antena(s).");
        sender.sendMessage("§7Kp/Ki/Kd: §e" + round2(kp) + " §7/ §e" + round2(ki) + " §7/ §e" + round2(kd));
        Coronalis.log("[Coronalis/Ops] PID " + network.getId() + " label=" + label
            + " kp=" + kp + " ki=" + ki + " kd=" + kd + " ant=" + antennaIndex);
    }

    public void stepTest(@Nonnull CommandSender sender, int antennaIndex, double deltaAz, double deltaEl) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        double currentAz = getDouble(network.getConsoleLoc(), "current_az");
        double currentEl = getDouble(network.getConsoleLoc(), "current_el");
        move(sender, currentAz + deltaAz, currentEl + deltaEl);
        sender.sendMessage("§5[Coronalis] §dStep-test iniciado"
            + (antennaIndex <= 0 ? " global" : " ANT-" + antennaIndex)
            + ". Observa /coronalis telemetry para respuesta.");
    }

    public void startScan(@Nonnull CommandSender sender, @Nonnull String patternRaw) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        ScanPattern pattern = ScanPattern.byId(patternRaw);
        if (pattern == null) {
            sender.sendMessage("§5[Coronalis] §cPatrón desconocido. Usa: spiral, wave, raster, stop, status.");
            return;
        }

        String id = network.getId();
        stopScan(id);
        ScanSession session = new ScanSession(network, pattern);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, session::tick, 0L, pattern.periodTicks);
        session.task = task;
        scanSessions.put(id, session);
        sender.sendMessage("§5[Coronalis] §aScan iniciado: §e" + pattern.id + "§7 (" + pattern.description + ")");
        Coronalis.log("[Coronalis/Ops] SCAN_START " + network.getId() + " pattern=" + pattern.id);
    }

    public void stopScan(@Nonnull CommandSender sender) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;
        boolean stopped = stopScan(network.getId());
        sender.sendMessage(stopped
            ? "§5[Coronalis] §aScan detenido."
            : "§5[Coronalis] §7No había scan activo en esta red.");
    }

    public void scanStatus(@Nonnull CommandSender sender) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;
        ScanSession session = scanSessions.get(network.getId());
        if (session == null) {
            sender.sendMessage("§5[Coronalis] §7Scan: §cINACTIVO");
            return;
        }
        sender.sendMessage("§5[Coronalis] §7Scan: §a" + session.pattern.id
            + " §7ticks=" + session.ticks + " target="
            + round2(getDouble(network.getConsoleLoc(), "target_az")) + "/"
            + round2(getDouble(network.getConsoleLoc(), "target_el")));
    }

    public boolean stopScan(@Nonnull String networkId) {
        ScanSession old = scanSessions.remove(networkId);
        if (old == null) return false;
        old.cancel();
        Coronalis.log("[Coronalis/Ops] SCAN_STOP " + networkId);
        return true;
    }

    public void dashboard(@Nonnull CommandSender sender) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        sender.sendMessage("§5§l[Coronalis] §dSciOps Dashboard");
        sender.sendMessage("§7Array: §e" + network.getId());
        sender.sendMessage("§7Antenas activas: §a" + (network.getTelescopeCount() - countFaults(network))
            + "§7/§e" + network.getTelescopeCount() + " §8(max " + CoronalisNetwork.MAX_TELESCOPES + ")");
        sender.sendMessage("§7Baselines UV: §b" + network.getBaselineCount()
            + " §7| Flujo total: §b" + round2(network.getTotalSignalAmplitude()) + " Jy");
        sender.sendMessage("§7Error medio: §c" + round2(avgPointingError(network)) + "°"
            + " §7| Temp/Corriente: §c" + round2(network.getAverageMotorTemp()) + "C"
            + "§7/§e" + round2(network.getAverageMotorCurrent()) + "A");
        sender.sendMessage("§7SU: §b" + network.getSignalUnits() + "§7/§b" + network.getMaxSignalUnits()
            + " §7| PID medio: §e" + round2(avgPidKp(network)) + "/"
            + round2(avgPidKi(network)) + "/" + round2(avgPidKd(network)));
        ScanSession scan = scanSessions.get(network.getId());
        sender.sendMessage("§7Scan: " + (scan == null ? "§cINACTIVO" : "§a" + scan.pattern.id + " §8t=" + scan.ticks));
        sender.sendMessage("§8Usa /coronalis telemetry para detalle por antena.");
    }

    public void maintenance(@Nonnull CommandSender sender, boolean repair) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        List<String> advisories = new ArrayList<>();
        int repaired = 0;
        for (Map.Entry<Location, TelescopeState> entry : network.getTelescopes().entrySet()) {
            TelescopeState state = entry.getValue();
            HealthHistory history = healthHistories.computeIfAbsent(locationKey(entry.getKey()), key -> new HealthHistory());
            history.add(state.getMotorTemp(), state.getMotorCurrent());

            double tempZ = history.zTemp(state.getMotorTemp());
            double currentZ = history.zCurrent(state.getMotorCurrent());
            boolean hot = state.getMotorTemp() >= 70.0 || tempZ >= 3.0;
            boolean overloaded = state.getMotorCurrent() >= 8.0 || currentZ >= 3.0;
            boolean fault = "FAULT".equals(state.getRuntimeState()) || state.needsFullRecal();

            if (hot || overloaded || fault) {
                String advisory = "ANT " + humanIndex(network, entry.getKey())
                    + " state=" + state.getRuntimeState()
                    + " temp=" + round2(state.getMotorTemp()) + "C zT=" + round2(tempZ)
                    + " curr=" + round2(state.getMotorCurrent()) + "A zI=" + round2(currentZ);
                advisories.add(advisory);
                if (repair && fault) {
                    state.resetRuntime();
                    repaired++;
                }
            }
        }

        sender.sendMessage("§5§l[Coronalis] §dAI Maintenance");
        if (advisories.isEmpty()) {
            sender.sendMessage("§aSin anomalías detectadas. Array dentro de parámetros nominales.");
        } else {
            sender.sendMessage("§eAdvisories: §6" + advisories.size());
            advisories.stream().limit(8).forEach(msg -> sender.sendMessage("§8- §e" + msg));
            if (advisories.size() > 8) {
                sender.sendMessage("§8Mostrando 8/" + advisories.size() + " avisos.");
            }
        }
        if (repair) {
            sender.sendMessage("§aAuto-repair aplicado a §e" + repaired + "§a antena(s) en FAULT.");
        }
    }

    public void exportTelemetry(@Nonnull CommandSender sender, @Nonnull String formatRaw) {
        CoronalisNetwork network = requireNearestNetwork(sender);
        if (network == null) return;

        String format = formatRaw.toLowerCase(Locale.ROOT);
        if (!format.equals("json") && !format.equals("csv")) {
            sender.sendMessage("§5[Coronalis] §cFormato inválido. Usa json o csv.");
            return;
        }

        Path exportDir = plugin.getDataFolder().toPath().resolve("exports");
        try {
            Files.createDirectories(exportDir);
            String name = "telemetry-" + EXPORT_TS.format(LocalDateTime.now()) + "." + format;
            Path file = exportDir.resolve(name);
            String body = format.equals("json") ? telemetryJson(network) : telemetryCsv(network);
            Files.writeString(file, body, StandardCharsets.UTF_8);
            sender.sendMessage("§5[Coronalis] §aTelemetría exportada: §e" + file);
        } catch (IOException e) {
            sender.sendMessage("§5[Coronalis] §cNo se pudo exportar telemetría: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "[Coronalis/Ops] Error exportando telemetría", e);
        }
    }

    @Nullable
    private static TelescopeState getByHumanIndex(@Nonnull CoronalisNetwork network, int index) {
        if (index <= 0) return null;
        List<Location> locs = sortedLocations(network);
        if (index > locs.size()) return null;
        return network.getTelescopeState(locs.get(index - 1));
    }

    private static int humanIndex(@Nonnull CoronalisNetwork network, @Nonnull Location loc) {
        List<Location> locs = sortedLocations(network);
        for (int i = 0; i < locs.size(); i++) {
            if (locs.get(i).equals(loc)) return i + 1;
        }
        return -1;
    }

    @Nonnull
    private static List<Location> sortedLocations(@Nonnull CoronalisNetwork network) {
        List<Location> locs = new ArrayList<>(network.getTelescopes().keySet());
        locs.sort(Comparator
            .comparingInt(Location::getBlockX)
            .thenComparingInt(Location::getBlockY)
            .thenComparingInt(Location::getBlockZ));
        return locs;
    }

    private static int countFaults(@Nonnull CoronalisNetwork network) {
        int faults = 0;
        for (TelescopeState state : network.getTelescopes().values()) {
            if ("FAULT".equals(state.getRuntimeState()) || state.needsFullRecal()) {
                faults++;
            }
        }
        return faults;
    }

    private static double avgPointingError(@Nonnull CoronalisNetwork network) {
        if (network.getTelescopeCount() == 0) return 0.0;
        double total = 0.0;
        for (TelescopeState state : network.getTelescopes().values()) {
            total += Math.sqrt(Math.pow(state.getAzError(), 2) + Math.pow(state.getElError(), 2));
        }
        return total / network.getTelescopeCount();
    }

    private static double avgPidKp(@Nonnull CoronalisNetwork network) {
        if (network.getTelescopeCount() == 0) return 0.0;
        double total = 0.0;
        for (TelescopeState state : network.getTelescopes().values()) total += state.getPidKp();
        return total / network.getTelescopeCount();
    }

    private static double avgPidKi(@Nonnull CoronalisNetwork network) {
        if (network.getTelescopeCount() == 0) return 0.0;
        double total = 0.0;
        for (TelescopeState state : network.getTelescopes().values()) total += state.getPidKi();
        return total / network.getTelescopeCount();
    }

    private static double avgPidKd(@Nonnull CoronalisNetwork network) {
        if (network.getTelescopeCount() == 0) return 0.0;
        double total = 0.0;
        for (TelescopeState state : network.getTelescopes().values()) total += state.getPidKd();
        return total / network.getTelescopeCount();
    }

    @Nonnull
    private static String telemetryJson(@Nonnull CoronalisNetwork network) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"network\": \"").append(escape(network.getId())).append("\",\n");
        json.append("  \"telescopes\": ").append(network.getTelescopeCount()).append(",\n");
        json.append("  \"baselines\": ").append(network.getBaselineCount()).append(",\n");
        json.append("  \"signalUnits\": ").append(network.getSignalUnits()).append(",\n");
        json.append("  \"records\": [\n");
        List<Location> locs = sortedLocations(network);
        for (int i = 0; i < locs.size(); i++) {
            TelescopeState state = network.getTelescopeState(locs.get(i));
            if (state == null) continue;
            json.append("    {")
                .append("\"id\": ").append(i + 1)
                .append(", \"state\": \"").append(escape(state.getRuntimeState())).append("\"")
                .append(", \"azError\": ").append(round2(state.getAzError()))
                .append(", \"elError\": ").append(round2(state.getElError()))
                .append(", \"amp\": ").append(round2(state.getSignalAmplitude()))
                .append(", \"phase\": ").append(round2(state.getSignalPhase()))
                .append(", \"temp\": ").append(round2(state.getMotorTemp()))
                .append(", \"current\": ").append(round2(state.getMotorCurrent()))
                .append(", \"kp\": ").append(round2(state.getPidKp()))
                .append(", \"ki\": ").append(round2(state.getPidKi()))
                .append(", \"kd\": ").append(round2(state.getPidKd()))
                .append("}");
            if (i < locs.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    @Nonnull
    private static String telemetryCsv(@Nonnull CoronalisNetwork network) {
        StringBuilder csv = new StringBuilder("id,state,az_error,el_error,amp,phase,temp,current,kp,ki,kd\n");
        List<Location> locs = sortedLocations(network);
        for (int i = 0; i < locs.size(); i++) {
            TelescopeState state = network.getTelescopeState(locs.get(i));
            if (state == null) continue;
            csv.append(i + 1).append(',')
                .append(state.getRuntimeState()).append(',')
                .append(round2(state.getAzError())).append(',')
                .append(round2(state.getElError())).append(',')
                .append(round2(state.getSignalAmplitude())).append(',')
                .append(round2(state.getSignalPhase())).append(',')
                .append(round2(state.getMotorTemp())).append(',')
                .append(round2(state.getMotorCurrent())).append(',')
                .append(round2(state.getPidKp())).append(',')
                .append(round2(state.getPidKi())).append(',')
                .append(round2(state.getPidKd())).append('\n');
        }
        return csv.toString();
    }

    @Nonnull
    private static String escape(@Nonnull String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static double getDouble(@Nonnull Location loc, @Nonnull String key) {
        String raw = BlockStorage.getLocationInfo(loc, key);
        if (raw == null) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static double clampAz(double value) {
        double result = value % 360.0;
        return result < 0 ? result + 360.0 : result;
    }

    private static double clampEl(double value) {
        return Math.max(0.0, Math.min(90.0, value));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Nonnull
    private static String locationKey(@Nonnull Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private enum ScanPattern {
        SPIRAL("spiral", "búsqueda arquimediana 10Hz", 2L),
        WAVE("wave", "onda de calibración tipo controller.py", 10L),
        RASTER("raster", "barrido por franjas Az/El", 8L);

        private final String id;
        private final String description;
        private final long periodTicks;

        ScanPattern(@Nonnull String id, @Nonnull String description, long periodTicks) {
            this.id = id;
            this.description = description;
            this.periodTicks = periodTicks;
        }

        @Nullable
        private static ScanPattern byId(@Nonnull String raw) {
            String normalized = raw.toLowerCase(Locale.ROOT);
            for (ScanPattern pattern : values()) {
                if (pattern.id.equals(normalized)) {
                    return pattern;
                }
            }
            return null;
        }
    }

    private final class ScanSession {
        private final CoronalisNetwork network;
        private final ScanPattern pattern;
        private BukkitTask task;
        private int ticks = 0;

        private ScanSession(@Nonnull CoronalisNetwork network, @Nonnull ScanPattern pattern) {
            this.network = network;
            this.pattern = pattern;
        }

        private void tick() {
            ticks++;
            Location loc = network.getConsoleLoc();
            double t = ticks / 10.0;
            double az;
            double el;

            switch (pattern) {
                case SPIRAL -> {
                    double theta = t * 0.5;
                    double radius = 5.0 + ((2.0 * theta) % 80.0);
                    az = (theta * 50.0) % 360.0;
                    el = Math.min(90.0, radius);
                }
                case WAVE -> {
                    az = (t * 10.0) % 360.0;
                    el = 45.0 + (30.0 * Math.sin(t * 0.5));
                }
                case RASTER -> {
                    int row = (ticks / 30) % 9;
                    boolean reverse = row % 2 == 1;
                    double progress = (ticks % 30) / 29.0;
                    az = reverse ? 360.0 - (progress * 360.0) : progress * 360.0;
                    el = 10.0 + (row * 8.0);
                }
                default -> {
                    az = 0.0;
                    el = 45.0;
                }
            }

            BlockStorage.addBlockInfo(loc, "target_az", String.valueOf(clampAz(az)));
            BlockStorage.addBlockInfo(loc, "target_el", String.valueOf(clampEl(el)));
            BlockStorage.addBlockInfo(loc, "selected_target", "Scan " + pattern.id);
            BlockStorage.addBlockInfo(loc, "correlation_progress", "0");

            if (ticks % 20 == 0) {
                Coronalis.log("[Coronalis/Scan] " + network.getId() + " pattern=" + pattern.id
                    + " az=" + round2(az) + " el=" + round2(el));
            }
        }

        private void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }

    private static final class HealthHistory {
        private static final int MAX_SAMPLES = 50;
        private final Deque<Double> temps = new ArrayDeque<>();
        private final Deque<Double> currents = new ArrayDeque<>();

        private void add(double temp, double current) {
            temps.addLast(temp);
            currents.addLast(current);
            while (temps.size() > MAX_SAMPLES) temps.removeFirst();
            while (currents.size() > MAX_SAMPLES) currents.removeFirst();
        }

        private double zTemp(double value) {
            return zScore(temps, value);
        }

        private double zCurrent(double value) {
            return zScore(currents, value);
        }

        private static double zScore(@Nonnull Deque<Double> samples, double value) {
            if (samples.size() < 6) return 0.0;
            double mean = samples.stream().mapToDouble(Double::doubleValue).average().orElse(value);
            double variance = 0.0;
            for (double sample : samples) {
                variance += Math.pow(sample - mean, 2);
            }
            double std = Math.sqrt(variance / samples.size());
            return std <= 0.001 ? 0.0 : Math.abs(value - mean) / std;
        }
    }
}
