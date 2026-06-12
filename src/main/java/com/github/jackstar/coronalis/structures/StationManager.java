package com.github.jackstar.coronalis.structures;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.persistence.CoronalisDatabase;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida torres construidas libremente a partir de anclajes Slimefun, protege
 * sus componentes y calcula enlaces de radio sin cargar chunks.
 */
public final class StationManager implements Listener {

    public static final String FOUNDATION = "CORONALIS_FOUNDATION_ANCHOR";
    public static final String SEGMENT = "CORONALIS_TOWER_SEGMENT";
    public static final String PLATFORM = "CORONALIS_WATCH_PLATFORM";
    public static final String MAST = "CORONALIS_RADIO_MAST";
    public static final String DISH = "CORONALIS_PARABOLIC_DISH";
    public static final String REPEATER = "CORONALIS_SIGNAL_REPEATER";

    private static final Set<String> COMPONENT_IDS = Set.of(
        FOUNDATION, SEGMENT, PLATFORM, MAST, DISH, REPEATER
    );

    private final Coronalis plugin;
    private final CoronalisDatabase database;
    private final Map<Location, Component> components = new ConcurrentHashMap<>();
    private volatile List<Station> stations = List.of();
    private volatile List<CoronalisDatabase.LinkRow> links = List.of();
    private boolean rebuildQueued;

    public StationManager(
        @Nonnull Coronalis plugin,
        @Nonnull CoronalisDatabase database
    ) {
        this.plugin = plugin;
        this.database = database;
        for (CoronalisDatabase.ComponentRow row : database.loadComponents()) {
            components.put(
                row.location().toBlockLocation(),
                new Component(row.type(), row.owner())
            );
        }
        rebuildNow();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void enforceComponentLimit(@Nonnull BlockPlaceEvent event) {
        SlimefunItem item = SlimefunItem.getByItem(event.getItemInHand());
        if (item == null || !COMPONENT_IDS.contains(item.getId())) {
            return;
        }
        if (components.size() >= plugin.getConfig().getInt("structures.max-components", 5000)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                "§5[Coronalis] §cLímite seguro de componentes alcanzado; avisa a administración."
            );
            database.audit(
                event.getPlayer().getUniqueId(),
                "PLACE_DENIED_LIMIT",
                event.getBlockPlaced().getLocation(),
                item.getId()
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectComponent(@Nonnull BlockBreakEvent event) {
        Component component = components.get(event.getBlock().getLocation().toBlockLocation());
        if (component == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!component.owner().equals(player.getUniqueId())
            && !player.hasPermission("coronalis.admin")) {
            event.setCancelled(true);
            player.sendMessage("§5[Coronalis] §cEste componente pertenece a otra estación.");
            database.audit(
                player.getUniqueId(),
                "BREAK_DENIED",
                event.getBlock().getLocation(),
                component.type()
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void registerComponent(@Nonnull BlockPlaceEvent event) {
        SlimefunItem item = SlimefunItem.getByItem(event.getItemInHand());
        if (item == null || !COMPONENT_IDS.contains(item.getId())) {
            return;
        }
        Location location = event.getBlock().getLocation().toBlockLocation();
        UUID owner = event.getPlayer().getUniqueId();
        if (database.upsertComponent(location, item.getId(), owner)) {
            components.put(location, new Component(item.getId(), owner));
            database.audit(owner, "PLACE_COMPONENT", location, item.getId());
            queueRebuild();
        } else {
            event.getPlayer().sendMessage(
                "§5[Coronalis] §cEl componente quedó colocado, pero no pudo auditarse. No lo muevas y avisa a un admin."
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void unregisterComponent(@Nonnull BlockBreakEvent event) {
        Location location = event.getBlock().getLocation().toBlockLocation();
        Component removed = components.remove(location);
        if (removed == null) {
            return;
        }
        if (!database.deleteComponent(location)) {
            components.put(location, removed);
            plugin.getLogger().severe("Se restauró el componente en memoria por fallo al borrar DB: " + location);
            return;
        }
        database.audit(
            event.getPlayer().getUniqueId(),
            "BREAK_COMPONENT",
            location,
            removed.type()
        );
        queueRebuild();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(@Nonnull BlockExplodeEvent event) {
        boolean changed = false;
        for (org.bukkit.block.Block block : event.blockList()) {
            Location location = block.getLocation().toBlockLocation();
            Component removed = components.remove(location);
            if (removed != null) {
                if (!database.deleteComponent(location)) {
                    components.put(location, removed);
                    plugin.getLogger().severe("Se restauró el componente en memoria por fallo al borrar DB (Explode): " + location);
                } else {
                    database.audit(null, "EXPLODE_COMPONENT", location, removed.type());
                    changed = true;
                }
            }
        }
        if (changed) {
            queueRebuild();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(@Nonnull EntityExplodeEvent event) {
        boolean changed = false;
        for (org.bukkit.block.Block block : event.blockList()) {
            Location location = block.getLocation().toBlockLocation();
            Component removed = components.remove(location);
            if (removed != null) {
                if (!database.deleteComponent(location)) {
                    components.put(location, removed);
                    plugin.getLogger().severe("Se restauró el componente en memoria por fallo al borrar DB (EntityExplode): " + location);
                } else {
                    database.audit(null, "EXPLODE_COMPONENT", location, removed.type());
                    changed = true;
                }
            }
        }
        if (changed) {
            queueRebuild();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void inspectComponent(@Nonnull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || !event.getAction().isRightClick()) {
            return;
        }
        Location location = event.getClickedBlock().getLocation().toBlockLocation();
        Component component = components.get(location);
        if (component == null) {
            return;
        }
        Station station = nearestStation(location, 48.0);
        event.getPlayer().sendMessage("§5[Coronalis] §7Componente: §d" + readable(component.type()));
        if (station != null) {
            sendStationStatus(event.getPlayer(), station);
        } else {
            event.getPlayer().sendMessage(
                "§7Aún no forma una estación. Usa §d/coronalis guide torres§7."
            );
        }
    }

    @EventHandler
    public void onChunkLoad(@Nonnull ChunkLoadEvent event) {
        if (hasComponentsInChunk(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ())) {
            queueRebuild();
        }
    }

    @EventHandler
    public void onChunkUnload(@Nonnull ChunkUnloadEvent event) {
        if (hasComponentsInChunk(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ())) {
            queueRebuild();
        }
    }

    public void sendNearestStatus(@Nonnull Player player) {
        Station station = nearestStation(player.getLocation(), 128.0);
        if (station == null) {
            player.sendMessage("§5[Coronalis] §cNo hay estaciones registradas a 128 bloques.");
            return;
        }
        sendStationStatus(player, station);
    }

    public int getStationCount() {
        return stations.size();
    }

    public int getValidStationCount() {
        return (int) stations.stream().filter(Station::valid).count();
    }

    public int getLinkCount() {
        return links.size();
    }

    public void stop() {
        rebuildNow();
        database.checkpoint();
    }

    private void queueRebuild() {
        if (rebuildQueued) {
            return;
        }
        rebuildQueued = true;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            rebuildQueued = false;
            rebuildNow();
        }, 2L);
    }

    private void rebuildNow() {
        List<Station> rebuilt = buildStations();
        List<CoronalisDatabase.LinkRow> rebuiltLinks = buildLinks(rebuilt);
        stations = List.copyOf(rebuilt);
        links = List.copyOf(rebuiltLinks);

        List<CoronalisDatabase.StationRow> rows = rebuilt.stream()
            .map(station -> new CoronalisDatabase.StationRow(
                station.id(),
                station.foundation(),
                station.owner(),
                station.tier().name(),
                station.valid(),
                station.detail()
            ))
            .toList();
        database.replaceStations(rows, rebuiltLinks);
    }

    @Nonnull
    private List<Station> buildStations() {
        List<Station> output = new ArrayList<>();
        for (Map.Entry<Location, Component> entry : components.entrySet()) {
            if (!FOUNDATION.equals(entry.getValue().type()) || !isLoaded(entry.getKey())) {
                continue;
            }
            output.add(validateFoundation(entry.getKey(), entry.getValue().owner()));
        }
        return output;
    }

    @Nonnull
    private Station validateFoundation(@Nonnull Location foundation, @Nonnull UUID owner) {
        List<Location> owned = components.entrySet().stream()
            .filter(entry -> entry.getValue().owner().equals(owner))
            .filter(entry -> sameWorld(entry.getKey(), foundation))
            .filter(entry -> isLoaded(entry.getKey()))
            .map(Map.Entry::getKey)
            .toList();

        Location platform = owned.stream()
            .filter(location -> PLATFORM.equals(typeAt(location)))
            .filter(location -> horizontalDistanceSquared(location, foundation) <= 9)
            .filter(location -> location.getBlockY() > foundation.getBlockY())
            .max(Comparator.comparingInt(Location::getBlockY))
            .orElse(null);

        if (platform == null) {
            return station(foundation, owner, null, Tier.OUTPOST, false, false, false,
                "Falta plataforma sobre el anclaje.");
        }

        int rise = platform.getBlockY() - foundation.getBlockY();
        int minimumRise = plugin.getConfig().getInt("structures.minimum-rise", 12);
        int maxAnchorGap = plugin.getConfig().getInt("structures.max-segment-gap", 16);
        List<Integer> anchors = new ArrayList<>();
        anchors.add(foundation.getBlockY());
        anchors.add(platform.getBlockY());
        owned.stream()
            .filter(location -> SEGMENT.equals(typeAt(location)))
            .filter(location -> horizontalDistanceSquared(location, foundation) <= 4)
            .filter(location -> location.getBlockY() > foundation.getBlockY())
            .filter(location -> location.getBlockY() < platform.getBlockY())
            .map(Location::getBlockY)
            .forEach(anchors::add);
        anchors.sort(Integer::compareTo);

        boolean covered = true;
        for (int index = 1; index < anchors.size(); index++) {
            if (anchors.get(index) - anchors.get(index - 1) > maxAnchorGap) {
                covered = false;
                break;
            }
        }

        boolean mast = hasNear(owned, MAST, platform, 4, 0, 16);
        boolean dish = hasNear(owned, DISH, platform, 5, -2, 18);
        boolean valid = rise >= minimumRise && covered && mast;
        Tier tier = classify(foundation, platform, rise);
        String detail;
        if (rise < minimumRise) {
            detail = "Altura insuficiente: " + rise + "/" + minimumRise + " bloques.";
        } else if (!covered) {
            detail = "Faltan segmentos de anclaje; separación máxima " + maxAnchorGap + ".";
        } else if (!mast) {
            detail = "Falta un mástil de radio junto a la plataforma.";
        } else {
            detail = dish
                ? "Estación operativa con plato parabólico."
                : "Estación operativa; plato parabólico recomendado para largo alcance.";
        }
        return station(foundation, owner, platform, tier, valid, mast, dish, detail);
    }

    @Nonnull
    private List<CoronalisDatabase.LinkRow> buildLinks(@Nonnull List<Station> rebuilt) {
        List<CoronalisDatabase.LinkRow> output = new ArrayList<>();
        for (int first = 0; first < rebuilt.size(); first++) {
            Station a = rebuilt.get(first);
            if (!a.valid() || a.platform() == null) {
                continue;
            }
            for (int second = first + 1; second < rebuilt.size(); second++) {
                Station b = rebuilt.get(second);
                if (!b.valid() || b.platform() == null || !sameWorld(a.platform(), b.platform())) {
                    continue;
                }
                double distance = a.platform().distance(b.platform());
                if (distance > Math.min(rangeOf(a), rangeOf(b)) || !hasLineOfSight(a, b, distance)) {
                    continue;
                }
                String left = a.id().compareTo(b.id()) <= 0 ? a.id() : b.id();
                String right = left.equals(a.id()) ? b.id() : a.id();
                output.add(new CoronalisDatabase.LinkRow(left, right, distance));
            }
        }
        return output;
    }

    private double rangeOf(@Nonnull Station station) {
        double range = plugin.getConfig().getDouble("radio.base-range", 192.0);
        if (station.dish()) {
            range = plugin.getConfig().getDouble("radio.dish-range", 512.0);
        }
        double repeaterRadius = plugin.getConfig().getDouble("radio.repeater-radius", 64.0);
        boolean repeater = components.entrySet().stream()
            .anyMatch(entry -> REPEATER.equals(entry.getValue().type())
                && isLoaded(entry.getKey())
                && sameWorld(entry.getKey(), station.foundation())
                && entry.getKey().distanceSquared(station.foundation()) <= repeaterRadius * repeaterRadius);
        if (repeater) {
            range += plugin.getConfig().getDouble("radio.repeater-bonus", 128.0);
        }
        return range;
    }

    private static boolean hasLineOfSight(
        @Nonnull Station a,
        @Nonnull Station b,
        double distance
    ) {
        Location start = a.platform().clone().add(0.5, 2.5, 0.5);
        Location end = b.platform().clone().add(0.5, 2.5, 0.5);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        World world = start.getWorld();
        if (world == null || distance <= 3.0) {
            return true;
        }
        RayTraceResult hit = world.rayTraceBlocks(
            start,
            direction,
            Math.max(0.0, distance - 2.0),
            FluidCollisionMode.NEVER,
            true
        );
        return hit == null;
    }

    private void sendStationStatus(@Nonnull Player player, @Nonnull Station station) {
        long linksForStation = links.stream()
            .filter(link -> link.stationA().equals(station.id()) || link.stationB().equals(station.id()))
            .count();
        int rise = station.platform() == null
            ? 0
            : station.platform().getBlockY() - station.foundation().getBlockY();
        player.sendMessage("§5§l[Coronalis] §dEstación " + station.tier().displayName);
        player.sendMessage("§7Base: §e" + format(station.foundation()) + " §8| §7Altura: §e" + rise);
        player.sendMessage("§7Estado: " + (station.valid() ? "§aOPERATIVA" : "§cINCOMPLETA"));
        player.sendMessage("§7Mástil/Plato: " + (station.mast() ? "§aSí" : "§cNo")
            + "§7/" + (station.dish() ? "§aSí" : "§eNo"));
        player.sendMessage("§7Enlaces directos: §b" + linksForStation);
        player.sendMessage("§8" + station.detail());
    }

    @Nullable
    private Station nearestStation(@Nonnull Location origin, double maximumDistance) {
        Station nearest = null;
        double best = maximumDistance * maximumDistance;
        for (Station station : stations) {
            if (!sameWorld(origin, station.foundation())) {
                continue;
            }
            double distance = origin.distanceSquared(station.foundation());
            if (distance <= best) {
                nearest = station;
                best = distance;
            }
        }
        return nearest;
    }

    private Tier classify(@Nonnull Location foundation, @Nonnull Location platform, int rise) {
        World world = foundation.getWorld();
        if (world == null) {
            return Tier.OUTPOST;
        }
        int stratosphericThreshold = world.getMaxHeight()
            - plugin.getConfig().getInt("structures.stratospheric-margin", 50);
        if (platform.getBlockY() >= stratosphericThreshold) {
            return Tier.STRATOSPHERIC;
        }
        if (foundation.getBlockY() >= world.getSeaLevel() + 32) {
            return Tier.MOUNTAIN;
        }
        return rise >= plugin.getConfig().getInt("structures.minimum-rise", 12)
            ? Tier.FIREWATCH
            : Tier.OUTPOST;
    }

    private static boolean hasNear(
        @Nonnull List<Location> locations,
        @Nonnull String type,
        @Nonnull Location center,
        int horizontalRadius,
        int minimumYOffset,
        int maximumYOffset
    ) {
        int radiusSquared = horizontalRadius * horizontalRadius;
        return locations.stream()
            .filter(location -> type.equals(typeAt(location)))
            .filter(location -> horizontalDistanceSquared(location, center) <= radiusSquared)
            .anyMatch(location -> {
                int offset = location.getBlockY() - center.getBlockY();
                return offset >= minimumYOffset && offset <= maximumYOffset;
            });
    }

    @Nullable
    private static String typeAt(@Nonnull Location location) {
        if (!isLoaded(location)) {
            return null;
        }
        SlimefunItem item = BlockStorage.check(location.getBlock());
        return item == null ? null : item.getId();
    }

    private boolean hasComponentsInChunk(@Nonnull UUID worldId, int chunkX, int chunkZ) {
        return components.keySet().stream().anyMatch(location ->
            location.getWorld() != null
                && location.getWorld().getUID().equals(worldId)
                && (location.getBlockX() >> 4) == chunkX
                && (location.getBlockZ() >> 4) == chunkZ
        );
    }

    private static boolean isLoaded(@Nonnull Location location) {
        return location.getWorld() != null
            && location.getWorld().isChunkLoaded(
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
            );
    }

    private static Station station(
        @Nonnull Location foundation,
        @Nonnull UUID owner,
        @Nullable Location platform,
        @Nonnull Tier tier,
        boolean valid,
        boolean mast,
        boolean dish,
        @Nonnull String detail
    ) {
        return new Station(
            format(foundation),
            foundation,
            platform,
            owner,
            tier,
            valid,
            mast,
            dish,
            detail
        );
    }

    private static int horizontalDistanceSquared(@Nonnull Location a, @Nonnull Location b) {
        int dx = a.getBlockX() - b.getBlockX();
        int dz = a.getBlockZ() - b.getBlockZ();
        return dx * dx + dz * dz;
    }

    private static boolean sameWorld(@Nonnull Location a, @Nonnull Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }

    @Nonnull
    private static String format(@Nonnull Location location) {
        return location.getWorld().getName() + ":"
            + location.getBlockX() + ":"
            + location.getBlockY() + ":"
            + location.getBlockZ();
    }

    @Nonnull
    private static String readable(@Nonnull String type) {
        return switch (type) {
            case FOUNDATION -> "Anclaje de cimentación";
            case SEGMENT -> "Segmento estructural";
            case PLATFORM -> "Plataforma Firewatch";
            case MAST -> "Mástil de radio";
            case DISH -> "Plato parabólico";
            case REPEATER -> "Repetidor de señal";
            default -> type;
        };
    }

    private record Component(String type, UUID owner) {}

    private record Station(
        String id,
        Location foundation,
        Location platform,
        UUID owner,
        Tier tier,
        boolean valid,
        boolean mast,
        boolean dish,
        String detail
    ) {}

    private enum Tier {
        OUTPOST("Puesto"),
        FIREWATCH("Firewatch"),
        MOUNTAIN("Montaña"),
        STRATOSPHERIC("Estratosférica");

        private final String displayName;

        Tier(String displayName) {
            this.displayName = displayName;
        }
    }
}
