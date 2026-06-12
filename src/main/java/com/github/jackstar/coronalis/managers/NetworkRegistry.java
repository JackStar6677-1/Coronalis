package com.github.jackstar.coronalis.managers;

import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.implementation.data.CoronalisNetwork;
import com.github.jackstar.coronalis.implementation.data.TelescopeState;
import com.github.jackstar.coronalis.implementation.items.ControlConsole;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Registro global de todas las redes Coronalis activas en el servidor.
 *
 * <p>Una "red" es un grafo de bloques: una Consola de Control conectada mediante
 * cables coaxiales ({@code CORONALIS_COAXIAL_CABLE}) a uno o más Radiotelescopios
 * ({@code CORONALIS_RADIO_TELESCOPE}).</p>
 *
 * <h3>Validación de conexión por BFS:</h3>
 * Un telescopio se considera conectado a una consola si existe al menos un camino
 * continuo de cables coaxiales desde el telescopio hasta la consola.
 * El BFS explora los 6 vecinos cardinales de cada bloque cable.
 *
 * <h3>Detección de redes rivales:</h3>
 * Dos redes son "rivales" si sus consolas están dentro de 200 bloques (mismo mundo)
 * y apuntan al mismo objetivo celeste simultáneamente.
 */
public class NetworkRegistry {

    /** Redes indexadas por ID de red (= ubicación de la consola). */
    private final Map<String, CoronalisNetwork> networks = new ConcurrentHashMap<>();

    private double globalScienceTime = 0.0;

    /** Límite BFS de bloques a explorar por validación de cable. */
    private static final int BFS_LIMIT = 8192;

    /** Radio de detección de redes rivales (bloques). */
    public static final int RIVAL_DETECT_RADIUS = 200;

    private static final Set<String> MODULE_IDS = Set.of(
        "CORONALIS_SIGNAL_AMPLIFIER",
        "CORONALIS_DATA_BANK",
        "CORONALIS_AUTO_CALIBRATOR"
    );

    private static final Set<String> ENDPOINT_IDS = Set.of(
        "CORONALIS_RADIO_TELESCOPE",
        "CORONALIS_SIGNAL_CORE",
        "CORONALIS_SIGNAL_AMPLIFIER",
        "CORONALIS_DATA_BANK",
        "CORONALIS_AUTO_CALIBRATOR"
    );

    // ── Gestión de redes ─────────────────────────────────────────────────────

    /**
     * Crea o recupera la red asociada a una consola.
     */
    @Nonnull
    public CoronalisNetwork getOrCreate(@Nonnull Location consoleLoc) {
        String id = networkId(consoleLoc);
        return networks.computeIfAbsent(id, k -> {
            CoronalisNetwork net = new CoronalisNetwork(consoleLoc);
            Coronalis.log("[NetworkRegistry] Nueva red creada: " + id);
            return net;
        });
    }

    /**
     * Elimina la red de una consola (consola destruida).
     */
    public void remove(@Nonnull Location consoleLoc) {
        CoronalisNetwork removed = networks.remove(networkId(consoleLoc));
        if (removed != null) {
            Coronalis.log("[NetworkRegistry] Red eliminada: " + removed.getId());
        }
    }

    /**
     * @return Red de la consola, o null si no existe.
     */
    @Nullable
    public CoronalisNetwork get(@Nonnull Location consoleLoc) {
        return networks.get(networkId(consoleLoc));
    }

    @Nonnull
    public Collection<CoronalisNetwork> getAllNetworks() {
        return Collections.unmodifiableCollection(networks.values());
    }

    public int getNetworkCount() { return networks.size(); }

    // ── Validación de cable por BFS ──────────────────────────────────────────

    /**
     * Realiza un BFS desde {@code start} buscando la {@code targetConsole}.
     * Solo atraviesa bloques de tipo {@code CORONALIS_COAXIAL_CABLE}.
     *
     * @param start          Ubicación del telescopio.
     * @param targetConsole  Ubicación de la consola que se quiere alcanzar.
     * @return true si hay un camino de cable continuo entre los dos bloques.
     */
    public boolean isConnectedByCable(@Nonnull Location start, @Nonnull Location targetConsole) {
        if (start.getWorld() == null || !start.getWorld().equals(targetConsole.getWorld())) {
            return false;
        }
        return scanConnectedNetwork(targetConsole).contains(start.toBlockLocation());
    }

    /**
     * Dado un conjunto de ubicaciones de telescopios candidatos,
     * devuelve solo los que están conectados a la consola por cable.
     *
     * @param consoleLoc     Consola central de la red.
     * @param candidates     Telescopios candidatos (radio de búsqueda).
     * @return Lista de telescopios con conexión válida.
     */
    @Nonnull
    public List<Location> findConnectedTelescopes(@Nonnull Location consoleLoc,
                                                   @Nonnull List<Location> candidates) {
        List<Location> connected = new ArrayList<>();
        for (Location candidate : candidates) {
            if (isConnectedByCable(candidate, consoleLoc)) {
                connected.add(candidate);
            }
        }
        return connected;
    }

    @Nonnull
    public List<Location> findConnectedEnergyNodes(@Nonnull Location consoleLoc) {
        return scanConnectedNetwork(consoleLoc).get("CORONALIS_SIGNAL_CORE");
    }

    /**
     * Reconstruye completamente la lista de telescopios conectados a una red.
     * Llamado cuando se coloca/rompe un cable o telescopio.
     */
    public void rebuildNetwork(@Nonnull Location consoleLoc) {
        try {
            CoronalisNetwork net = getOrCreate(consoleLoc);

            // Limpiar listas dinámicas antes de recalcular enlaces.
            net.clearTelescopes();
            net.clearEnergyNodes();
            net.clearModules();

            NetworkScan scan = scanConnectedNetwork(consoleLoc);
            List<Location> connected = scan.get("CORONALIS_RADIO_TELESCOPE");
            List<Location> energyNodes = scan.get("CORONALIS_SIGNAL_CORE");

            int added = 0;
            for (Location loc : connected) {
                if (net.addTelescope(loc)) {
                    added++;
                } else {
                    Coronalis.log("[NetworkRegistry] Límite de " + CoronalisNetwork.MAX_TELESCOPES
                        + " telescopios alcanzado en " + net.getId());
                    break;
                }
            }
            for (Location loc : energyNodes) {
                net.addEnergyNode(loc);
            }
            for (String moduleId : MODULE_IDS) {
                for (Location loc : scan.get(moduleId)) {
                    net.addModule(moduleId, loc);
                }
            }
            net.setMaxSignalUnits(1000 + (net.getEnergyNodeCount() * 500) + (net.getDataBankCount() * 750));

            Coronalis.log("[NetworkRegistry] Red " + net.getId()
                + " reconstruida: " + added + " telescopio(s), "
                + net.getEnergyNodeCount() + " núcleo(s) SU, "
                + net.getSignalAmplifierCount() + " amplificador(es), "
                + net.getDataBankCount() + " banco(s), "
                + net.getAutoCalibratorCount() + " calibrador(es).");

        } catch (Exception e) {
            Coronalis.instance().getLogger().log(Level.WARNING,
                "[NetworkRegistry] Error al reconstruir red en " + consoleLoc, e);
        }
    }

    public void rebuildNetworksNear(@Nonnull Location loc, int radius) {
        if (loc.getWorld() == null) return;
        double radiusSquared = (double) radius * radius;
        for (Location consoleLoc : new ArrayList<>(ControlConsole.ACTIVE_CONSOLES)) {
            if (consoleLoc.getWorld() != null
                && consoleLoc.getWorld().equals(loc.getWorld())
                && consoleLoc.distanceSquared(loc) <= radiusSquared) {
                rebuildNetwork(consoleLoc);
            }
        }
    }

    public void tickAll() {
        removeMissingLoadedConsoles();
        for (CoronalisNetwork network : networks.values()) {
            try {
                network.generateSU();
                network.drainStandbySU();
                tickAutoCalibrators(network);
                updateScienceTelemetry(network);
            } catch (Exception e) {
                Coronalis.instance().getLogger().log(Level.WARNING,
                    "[NetworkRegistry] Error al alimentar red " + network.getId(), e);
            }
        }
    }

    private void updateScienceTelemetry(@Nonnull CoronalisNetwork network) {
        globalScienceTime += 0.1;
        Location consoleLoc = network.getConsoleLoc();
        double currentAz = getConsoleDouble(consoleLoc, "current_az");
        double currentEl = getConsoleDouble(consoleLoc, "current_el");
        double targetAz = getConsoleDouble(consoleLoc, "target_az");
        double targetEl = getConsoleDouble(consoleLoc, "target_el");
        for (TelescopeState state : network.getTelescopes().values()) {
            state.updateTelemetry(consoleLoc, currentAz, currentEl, targetAz, targetEl, globalScienceTime);
        }
    }

    private void tickAutoCalibrators(@Nonnull CoronalisNetwork network) {
        int calibrators = network.getAutoCalibratorCount();
        if (calibrators <= 0 || network.getTelescopeCount() == 0) return;

        int steps = 0;
        for (int i = 0; i < calibrators; i++) {
            if (!network.drainSU(CoronalisNetwork.SU_COST_AUTO_CALIBRATE)) break;
            if (advanceOneCalibration(network)) {
                steps++;
            } else {
                break;
            }
        }
        if (steps > 0) {
            Location loc = network.getConsoleLoc();
            Coronalis.log("[Coronalis/AutoCalib] Red " + network.getId()
                + " calibró " + steps + " parámetro(s). SU="
                + network.getSignalUnits() + "/" + network.getMaxSignalUnits());
            Coronalis.instance().getSoundManager().playAt(loc, SoundManager.CoronalisSound.CALIBRATE);
        }
    }

    private boolean advanceOneCalibration(@Nonnull CoronalisNetwork network) {
        for (TelescopeState state : network.getTelescopes().values()) {
            for (TelescopeState.CalibParam param : TelescopeState.CalibParam.values()) {
                if (state.get(param) < 100) {
                    state.advance(param, 10);
                    state.incrementCalibCount();
                    return true;
                }
            }
        }
        return false;
    }

    // ── Detección de redes rivales ───────────────────────────────────────────

    /**
     * Busca redes rivales dentro del radio de detección de {@code network}.
     * Una red rival es cualquier otra red activa en el mismo mundo
     * dentro de {@value #RIVAL_DETECT_RADIUS} bloques de la consola.
     *
     * @return Lista de redes rivales detectadas.
     */
    @Nonnull
    public List<CoronalisNetwork> findRivalNetworks(@Nonnull CoronalisNetwork network) {
        List<CoronalisNetwork> rivals = new ArrayList<>();
        Location myLoc = network.getConsoleLoc();
        if (myLoc.getWorld() == null) return rivals;

        for (CoronalisNetwork other : networks.values()) {
            if (other.getId().equals(network.getId())) continue;
            Location otherLoc = other.getConsoleLoc();
            if (otherLoc.getWorld() == null) continue;
            if (!otherLoc.getWorld().equals(myLoc.getWorld())) continue;
            if (myLoc.distance(otherLoc) <= RIVAL_DETECT_RADIUS) {
                rivals.add(other);
            }
        }
        return rivals;
    }

    /**
     * Determina si hay interferencia entre dos redes:
     * ambas apuntan al mismo objetivo celeste y están a menos de 200 bloques.
     *
     * @param net1 Red propia.
     * @param net2 Red rival.
     * @return true si se produce interferencia.
     */
    public boolean isInterferingWith(@Nonnull CoronalisNetwork net1, @Nonnull CoronalisNetwork net2) {
        String target1 = getConsoleTarget(net1.getConsoleLoc());
        String target2 = getConsoleTarget(net2.getConsoleLoc());
        if (target1.isBlank() || target2.isBlank() || "Ninguno".equals(target1) || "Ninguno".equals(target2)) {
            return false;
        }
        return target1.equals(target2)
            && net1.getConsoleLoc().distance(net2.getConsoleLoc()) <= RIVAL_DETECT_RADIUS;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Nonnull
    private static String networkId(@Nonnull Location loc) {
        return loc.getWorld().getName()
            + ":" + loc.getBlockX()
            + ":" + loc.getBlockY()
            + ":" + loc.getBlockZ();
    }

    @Nullable
    private static String slimefunId(@Nonnull Block block) {
        var sf = BlockStorage.check(block);
        return sf != null ? sf.getId() : null;
    }

    private static void addNeighbors(@Nonnull Location loc,
                                     @Nonnull Queue<Location> queue,
                                     @Nonnull Set<Location> visited) {
        for (BlockFace face : new BlockFace[]{
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        }) {
            Location neighbor = loc.getBlock().getRelative(face).getLocation().toBlockLocation();
            if (neighbor.getWorld() != null
                && neighbor.getWorld().isChunkLoaded(neighbor.getBlockX() >> 4, neighbor.getBlockZ() >> 4)
                && visited.add(neighbor)) {
                queue.offer(neighbor);
            }
        }
    }

    @Nonnull
    private static NetworkScan scanConnectedNetwork(@Nonnull Location consoleLoc) {
        NetworkScan scan = new NetworkScan();
        if (consoleLoc.getWorld() == null) {
            return scan;
        }

        Location origin = consoleLoc.toBlockLocation();
        Set<Location> visited = new HashSet<>();
        Queue<Location> queue = new ArrayDeque<>();
        visited.add(origin);
        addNeighbors(origin, queue, visited);

        int steps = 0;
        while (!queue.isEmpty() && steps++ < BFS_LIMIT) {
            Location current = queue.poll();
            String id = slimefunId(current.getBlock());
            if ("CORONALIS_COAXIAL_CABLE".equals(id)) {
                addNeighbors(current, queue, visited);
            } else if (ENDPOINT_IDS.contains(id)) {
                scan.add(id, current);
            }
        }

        if (!queue.isEmpty()) {
            Coronalis.log("[NetworkRegistry] Recorrido limitado a " + BFS_LIMIT
                + " bloques cargados desde " + networkId(origin) + ".");
        }
        return scan;
    }

    private void removeMissingLoadedConsoles() {
        for (Location consoleLoc : new ArrayList<>(ControlConsole.ACTIVE_CONSOLES)) {
            if (consoleLoc.getWorld() == null
                || !consoleLoc.getWorld().isChunkLoaded(
                    consoleLoc.getBlockX() >> 4,
                    consoleLoc.getBlockZ() >> 4)) {
                continue;
            }
            if (!"CORONALIS_CONTROL_CONSOLE".equals(slimefunId(consoleLoc.getBlock()))) {
                ControlConsole.ACTIVE_CONSOLES.remove(consoleLoc);
                remove(consoleLoc);
            }
        }
    }

    private static final class NetworkScan {
        private final Map<String, List<Location>> endpoints = new HashMap<>();

        private void add(@Nonnull String id, @Nonnull Location location) {
            endpoints.computeIfAbsent(id, ignored -> new ArrayList<>()).add(location);
        }

        @Nonnull
        private List<Location> get(@Nonnull String id) {
            return endpoints.getOrDefault(id, Collections.emptyList());
        }

        private boolean contains(@Nonnull Location location) {
            return endpoints.values().stream().anyMatch(locations -> locations.contains(location));
        }
    }

    @Nonnull
    private static String getConsoleTarget(@Nonnull Location consoleLoc) {
        String t = BlockStorage.getLocationInfo(consoleLoc, "selected_target");
        return t != null ? t : "";
    }

    private static double getConsoleDouble(@Nonnull Location consoleLoc, @Nonnull String key) {
        String raw = BlockStorage.getLocationInfo(consoleLoc, key);
        if (raw == null) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }
}
