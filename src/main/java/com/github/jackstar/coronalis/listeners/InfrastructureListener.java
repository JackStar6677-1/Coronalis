package com.github.jackstar.coronalis.listeners;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.implementation.items.ControlConsole;
import com.github.jackstar.coronalis.managers.SoundManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Centraliza el ciclo de vida de la infraestructura para que cada evento se
 * procese una sola vez aunque existan varios tipos de bloques de red.
 */
public final class InfrastructureListener implements Listener {

    private static final String CONTROL_CONSOLE_ID = "CORONALIS_CONTROL_CONSOLE";
    private static final Set<String> NETWORK_BLOCK_IDS = Set.of(
        "CORONALIS_COAXIAL_CABLE",
        "CORONALIS_SIGNAL_CORE",
        "CORONALIS_SIGNAL_AMPLIFIER",
        "CORONALIS_DATA_BANK",
        "CORONALIS_AUTO_CALIBRATOR"
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(@Nonnull BlockPlaceEvent event) {
        SlimefunItem item = SlimefunItem.getByItem(event.getItemInHand());
        if (item == null || !NETWORK_BLOCK_IDS.contains(item.getId())) {
            return;
        }

        Block block = event.getBlockPlaced();
        block.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            block.getLocation().add(0.5, 0.6, 0.5),
            10,
            0.25,
            0.25,
            0.25,
            0.03
        );
        Coronalis.instance().getSoundManager().playAt(
            block.getLocation(),
            SoundManager.CoronalisSound.CABLE_CONNECT
        );
        event.getPlayer().sendMessage(
            "§5[Coronalis] §7Infraestructura de red instalada: §d" + item.getItemName()
        );
        refreshNearby(block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(@Nonnull BlockBreakEvent event) {
        SlimefunItem item = BlockStorage.check(event.getBlock());
        if (item == null) {
            return;
        }

        Location location = event.getBlock().getLocation().toBlockLocation();
        if (CONTROL_CONSOLE_ID.equals(item.getId())) {
            event.getPlayer().sendMessage("§5[Coronalis] §7Consola retirada. Cerrando la red local...");
            Coronalis.instance().getServer().getScheduler().runTaskLater(
                Coronalis.instance(),
                () -> {
                    ControlConsole.ACTIVE_CONSOLES.remove(location);
                    Coronalis.instance().getNetworkRegistry().remove(location);
                },
                1L
            );
            return;
        }

        if (NETWORK_BLOCK_IDS.contains(item.getId())) {
            event.getPlayer().sendMessage(
                "§5[Coronalis] §7Red Coronalis alterada. Recalculando enlaces..."
            );
            refreshNearby(location);
        }
    }

    private static void refreshNearby(@Nonnull Location location) {
        Coronalis.instance().getServer().getScheduler().runTaskLater(
            Coronalis.instance(),
            () -> Coronalis.instance().getNetworkRegistry().rebuildNetworksNear(location, 64),
            2L
        );
    }
}
