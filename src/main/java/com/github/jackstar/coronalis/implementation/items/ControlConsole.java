package com.github.jackstar.coronalis.implementation.items;

import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.discovery.DiscoveryService;
import com.github.jackstar.coronalis.implementation.Items;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.utils.ChestMenuUtils;
import com.github.drakescraft_labs.slimefun4.legacy.Objects.SlimefunItem.interfaces.InventoryBlock;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.BlockMenuPreset;
import com.github.drakescraft_labs.slimefun4.legacy.api.inventory.DirtyChestMenu;
import com.github.drakescraft_labs.slimefun4.legacy.api.item_transport.ItemTransportFlow;
import dev.drake.dough.items.CustomItemStack;
import dev.drake.dough.protection.Interaction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ControlConsole extends SlimefunItem implements InventoryBlock {

    private final Random random = new Random();

    // Slots de Background
    private static final int[] BORDER_SLOTS = {
            0, 1, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 32, 33, 35, 36, 37, 38, 39, 40, 41,
            42, 43, 44, 45, 46, 47, 49, 51, 52, 53
    };

    // Slots de Interacción
    private static final int INPUT_SLOT = 48; // Celda de datos
    private static final int OUTPUT_SLOT = 50; // Registro científico

    // Paneles de Estado
    private static final int PANEL_TELESCOPES = 2;
    private static final int PANEL_TARGET = 3;
    private static final int PANEL_TELEMETRY = 4;
    private static final int PANEL_PID = 5;
    private static final int PANEL_FAULT = 6;

    // Botones de Operación
    private static final int BUTTON_SLEW = 28;
    private static final int BUTTON_REPAIR = 30;
    private static final int BUTTON_CORRELATE = 34;

    // Estructura de un Objetivo Astronómico
    private static class AstroTarget {
        final String name;
        final double az;
        final double el;
        final String desc;
        final int slot;

        AstroTarget(String name, double az, double el, String desc, int slot) {
            this.name = name;
            this.az = az;
            this.el = el;
            this.desc = desc;
            this.slot = slot;
        }
    }

    private static final List<AstroTarget> TARGETS = new ArrayList<>();
    static {
        TARGETS.add(new AstroTarget("Nebulosa del Cangrejo", 83.6, 22.0, "Remanente de supernova brillante en la constelación de Tauro. Emite radiación de sincrotrón continua.", 10));
        TARGETS.add(new AstroTarget("Sagitario A*", 177.3, 29.1, "El agujero negro supermasivo en el dinámico centro de la Vía Láctea. Emite potentes ondas de radio.", 11));
        TARGETS.add(new AstroTarget("Púlsar PSR B1919+21", 295.4, 50.8, "Primera estrella de neutrones pulsante descubierta. Su señal tiene un período regular de 1.337s.", 12));
        TARGETS.add(new AstroTarget("Nebulosa Cabeza de Caballo", 95.1, 14.5, "Nube de gas interestelar densa y fría. Revelada mediante radioastronomía de moléculas orgánicas.", 13));
        TARGETS.add(new AstroTarget("Galaxia de Andrómeda", 310.2, 41.2, "Galaxia espiral vecina. Sus líneas de hidrógeno neutro de 21cm muestran un claro desplazamiento al azul.", 14));
        TARGETS.add(new AstroTarget("Exoplaneta Kepler-186f", 45.9, 68.7, "Planeta terrestre en zona habitable. Analizando firmas electromagnéticas térmicas y modulaciones.", 15));
        TARGETS.add(new AstroTarget("Agujero Negro M87*", 215.0, 37.8, "Agujero negro supermasivo gigante con un chorro relativista colosal. Imagen capturada por VLBI.", 16));
    }

    public ControlConsole(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setupMenu(this);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                initializeBlockStorage(b.getLocation());
                updateMenuVisuals(menu, b.getLocation());

                // Registrar manejadores de clics de los botones de objetivo
                for (AstroTarget target : TARGETS) {
                    menu.addMenuClickHandler(target.slot, (player, slot, item, action) -> {
                        selectTarget(menu, b.getLocation(), target, player);
                        return false;
                    });
                }

                // Botón Slew
                menu.addMenuClickHandler(BUTTON_SLEW, (player, slot, item, action) -> {
                    performSlewStep(menu, b.getLocation(), player);
                    return false;
                });

                // Botón Reparar
                menu.addMenuClickHandler(BUTTON_REPAIR, (player, slot, item, action) -> {
                    performRepair(menu, b.getLocation(), player);
                    return false;
                });

                // Botón Correlacionar
                menu.addMenuClickHandler(BUTTON_CORRELATE, (player, slot, item, action) -> {
                    performCorrelation(menu, b.getLocation(), player);
                    return false;
                });
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.INSERT) {
                    return new int[] { INPUT_SLOT };
                } else {
                    return new int[] { OUTPUT_SLOT };
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || (com.github.drakescraft_labs.slimefun4.implementation.Slimefun.getProtectionManager().hasPermission(p, b, Interaction.INTERACT_BLOCK));
            }
        };
    }

    private void setupMenu(BlockMenuPreset preset) {
        // Dibujar bordes con cristales grises
        preset.drawBackground(ChestMenuUtils.getBackground(), BORDER_SLOTS);
        
        // Dibujar texturas especiales para slots de entrada y salida
        preset.drawBackground(ChestMenuUtils.getInputSlotTexture(), new int[] { INPUT_SLOT - 1, INPUT_SLOT + 1 });
        preset.drawBackground(ChestMenuUtils.getOutputSlotTexture(), new int[] { OUTPUT_SLOT - 1, OUTPUT_SLOT + 1 });

        // Inicializar botones de objetivos fijos
        for (AstroTarget target : TARGETS) {
            preset.addItem(target.slot, new CustomItemStack(
                    Material.ORANGE_CONCRETE,
                    "&6&l" + target.name,
                    "&7Azimut: &e" + target.az + "°",
                    "&7Elevación: &e" + target.el + "°",
                    "",
                    "&dHaz clic para fijar como objetivo."
            ));
        }
    }

    private void initializeBlockStorage(Location loc) {
        if (BlockStorage.getLocationInfo(loc, "current_az") == null) {
            BlockStorage.addBlockInfo(loc, "current_az", "0.0");
            BlockStorage.addBlockInfo(loc, "current_el", "0.0");
            BlockStorage.addBlockInfo(loc, "target_az", "0.0");
            BlockStorage.addBlockInfo(loc, "target_el", "0.0");
            BlockStorage.addBlockInfo(loc, "selected_target", "Ninguno");
            BlockStorage.addBlockInfo(loc, "fault_state", "NORMAL");
            BlockStorage.addBlockInfo(loc, "correlation_progress", "0");
            BlockStorage.addBlockInfo(loc, "connected_telescopes", String.valueOf(countConnectedTelescopes(loc)));
        }
    }

    private int countConnectedTelescopes(Location loc) {
        int count = 0;
        int radius = 8; // Radio optimizado
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    SlimefunItem sfItem = BlockStorage.check(b);
                    if (sfItem != null && sfItem.getId().equals("CORONALIS_RADIO_TELESCOPE")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void selectTarget(BlockMenu menu, Location loc, AstroTarget target, Player player) {
        BlockStorage.addBlockInfo(loc, "target_az", String.valueOf(target.az));
        BlockStorage.addBlockInfo(loc, "target_el", String.valueOf(target.el));
        BlockStorage.addBlockInfo(loc, "selected_target", target.name);
        BlockStorage.addBlockInfo(loc, "correlation_progress", "0");

        player.playSound(loc, Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
        player.sendMessage("§5[Coronalis] §dFijando objetivo: §e" + target.name);

        Coronalis.instance().getDiscoveryService().tryDiscover(
            player,
            "first_target_lock",
            "Primer objetivo astronómico",
            "discovery-xp.first_target_lock"
        );

        updateMenuVisuals(menu, loc);
    }

    private void performSlewStep(BlockMenu menu, Location loc, Player player) {
        String fault = BlockStorage.getLocationInfo(loc, "fault_state");
        if (fault != null && !fault.equals("NORMAL")) {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§5[Coronalis] §c¡ERROR DE OPERACIÓN! Falla activa en el sistema de apuntado.");
            return;
        }

        double curAz = Double.parseDouble(BlockStorage.getLocationInfo(loc, "current_az"));
        double curEl = Double.parseDouble(BlockStorage.getLocationInfo(loc, "current_el"));
        double tarAz = Double.parseDouble(BlockStorage.getLocationInfo(loc, "target_az"));
        double tarEl = Double.parseDouble(BlockStorage.getLocationInfo(loc, "target_el"));

        double diffAz = tarAz - curAz;
        double diffEl = tarEl - curEl;

        double distance = Math.sqrt(diffAz * diffAz + diffEl * diffEl);
        if (distance < 0.1) {
            player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage("§5[Coronalis] §aTelescopios alineados con precisión en el objetivo.");
            Coronalis.instance().getDiscoveryService().tryDiscover(
                player,
                "tracking_lock",
                "Bloqueo de seguimiento (TRACKING)",
                "discovery-xp.tracking_lock"
            );
            updateMenuVisuals(menu, loc);
            return;
        }

        // Bucle PID simplificado (proporcional progresivo)
        double stepAz = diffAz * 0.45;
        double stepEl = diffEl * 0.45;

        // Limitar la velocidad máxima por paso
        if (Math.abs(stepAz) > 25.0) stepAz = Math.signum(stepAz) * 25.0;
        if (Math.abs(stepEl) > 20.0) stepEl = Math.signum(stepEl) * 20.0;

        curAz += stepAz;
        curEl += stepEl;

        // Redondear a un decimal
        curAz = Math.round(curAz * 10.0) / 10.0;
        curEl = Math.round(curEl * 10.0) / 10.0;

        BlockStorage.addBlockInfo(loc, "current_az", String.valueOf(curAz));
        BlockStorage.addBlockInfo(loc, "current_el", String.valueOf(curEl));

        // 10% de probabilidad de generar una falla aleatoria de hardware durante el slewing
        if (random.nextInt(10) == 0) {
            String newFault = random.nextBoolean() ? "MOTOR_STUCK" : "PID_OVERLOAD";
            BlockStorage.addBlockInfo(loc, "fault_state", newFault);
            player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            player.sendMessage("§5[Coronalis] §c🚨 ¡ADVERTENCIA! Se ha detectado una falla mecánica: " + newFault);
        } else {
            player.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.8f, 1.2f);
        }

        updateMenuVisuals(menu, loc);
    }

    private void performRepair(BlockMenu menu, Location loc, Player player) {
        String fault = BlockStorage.getLocationInfo(loc, "fault_state");
        if (fault == null || fault.equals("NORMAL")) {
            player.sendMessage("§5[Coronalis] §aTodos los sistemas operan dentro del rango normal.");
            return;
        }

        // Para reparar, necesitamos un Controlador PID en la mano o en el slot de entrada,
        // o simplemente podemos repararlo con un mini-juego / click consumiendo recursos.
        // Vamos a pedir que tenga un repuesto en el inventario o hacer una reparación directa con enfriamiento
        BlockStorage.addBlockInfo(loc, "fault_state", "NORMAL");
        player.playSound(loc, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        player.sendMessage("§5[Coronalis] §aSistemas reparados y calibrados con éxito. Estado: NORMAL.");

        updateMenuVisuals(menu, loc);
    }

    private void performCorrelation(BlockMenu menu, Location loc, Player player) {
        String fault = BlockStorage.getLocationInfo(loc, "fault_state");
        if (fault != null && !fault.equals("NORMAL")) {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§5[Coronalis] §cCorrelación bloqueada por falla activa.");
            return;
        }

        double curAz = Double.parseDouble(BlockStorage.getLocationInfo(loc, "current_az"));
        double curEl = Double.parseDouble(BlockStorage.getLocationInfo(loc, "current_el"));
        double tarAz = Double.parseDouble(BlockStorage.getLocationInfo(loc, "target_az"));
        double tarEl = Double.parseDouble(BlockStorage.getLocationInfo(loc, "target_el"));

        double diffAz = tarAz - curAz;
        double diffEl = tarEl - curEl;
        double distance = Math.sqrt(diffAz * diffAz + diffEl * diffEl);

        if (distance >= 0.2) {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§5[Coronalis] §cSeñal incoherente. El telescopio debe estar alineado (< 0.2° de error).");
            return;
        }

        // Verificar Celda de Datos en entrada
        ItemStack input = menu.getItemInSlot(INPUT_SLOT);
        SlimefunItem sfInput = SlimefunItem.getByItem(input);
        if (sfInput == null || !sfInput.getId().equals("CORONALIS_DATA_CELL")) {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§5[Coronalis] §cSe requiere una Celda de Datos Celestes en el slot de entrada.");
            return;
        }

        // Verificar espacio en salida
        ItemStack output = menu.getItemInSlot(OUTPUT_SLOT);
        if (output != null && output.getAmount() >= output.getMaxStackSize()) {
            player.sendMessage("§5[Coronalis] §cEl slot de salida está lleno.");
            return;
        }

        int progress = Integer.parseInt(BlockStorage.getLocationInfo(loc, "correlation_progress"));
        progress += 25; // 4 clics para correlación completa

        if (progress >= 100) {
            progress = 0;
            // Correlación completa: Generar el registro científico
            String targetName = BlockStorage.getLocationInfo(loc, "selected_target");
            AstroTarget selected = null;
            for (AstroTarget t : TARGETS) {
                if (t.name.equals(targetName)) {
                    selected = t;
                    break;
                }
            }

            if (selected == null) {
                selected = TARGETS.get(0);
            }

            // Consumir 1 celda de datos
            input.setAmount(input.getAmount() - 1);
            menu.replaceExistingItem(INPUT_SLOT, input);

            // Generar item
            ItemStack record = Items.CORONALIS_RECORD_DISCOVERED.clone();
            ItemMeta meta = record.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§8======================================");
                lore.add("§dObjeto: §e" + selected.name);
                lore.add("§dAz/El: §e" + selected.az + "° / " + selected.el + "°");
                lore.add("§dFase: §b" + (98.0 + random.nextDouble() * 2.0) + "%");
                lore.add("§dDescripción: §7" + selected.desc);
                lore.add("§8======================================");
                lore.add("§eClic derecho para analizar y recibir XP.");
                meta.setLore(lore);
                record.setItemMeta(meta);
            }

            if (output == null || output.getType() == Material.AIR) {
                menu.replaceExistingItem(OUTPUT_SLOT, record);
            } else {
                output.setAmount(output.getAmount() + 1);
                menu.replaceExistingItem(OUTPUT_SLOT, output);
            }

            player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.sendMessage("§5[Coronalis] §a⭐ ¡Señal correlacionada con éxito! Registro científico generado.");

            DiscoveryService discoveries = Coronalis.instance().getDiscoveryService();
            discoveries.tryDiscover(
                player,
                "first_correlation",
                "Primera correlación interferométrica",
                "discovery-xp.first_correlation"
            );
            discoveries.tryDiscover(
                player,
                DiscoveryService.targetDiscoveryId(selected.name),
                selected.name,
                DiscoveryService.targetConfigPath(selected.name)
            );
        } else {
            player.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.8f);
        }

        BlockStorage.addBlockInfo(loc, "correlation_progress", String.valueOf(progress));
        updateMenuVisuals(menu, loc);
    }

    private void updateMenuVisuals(BlockMenu menu, Location loc) {
        String targetName = BlockStorage.getLocationInfo(loc, "selected_target");
        String curAz = BlockStorage.getLocationInfo(loc, "current_az");
        String curEl = BlockStorage.getLocationInfo(loc, "current_el");
        String tarAz = BlockStorage.getLocationInfo(loc, "target_az");
        String tarEl = BlockStorage.getLocationInfo(loc, "target_el");
        String fault = BlockStorage.getLocationInfo(loc, "fault_state");
        String progress = BlockStorage.getLocationInfo(loc, "correlation_progress");
        int connected = countConnectedTelescopes(loc);

        double diffAz = Double.parseDouble(tarAz) - Double.parseDouble(curAz);
        double diffEl = Double.parseDouble(tarEl) - Double.parseDouble(curEl);
        double error = Math.round(Math.sqrt(diffAz * diffAz + diffEl * diffEl) * 10.0) / 10.0;

        String stateStr = "QUIETO (IDLE)";
        if (error >= 0.1 && fault.equals("NORMAL")) {
            stateStr = "MOVIÉNDOSE (SLEWING)";
        } else if (error < 0.1 && fault.equals("NORMAL") && !targetName.equals("Ninguno")) {
            stateStr = "SEGUIMIENTO (TRACKING)";
        } else if (!fault.equals("NORMAL")) {
            stateStr = "BLOQUEADO (FAULT)";
        }

        // Panel de Telescopios (Slot 2)
        menu.replaceExistingItem(PANEL_TELESCOPES, new CustomItemStack(
                Material.BEACON,
                "&d📡 Estado de la Red de Antenas",
                "&7Antenas detectadas: &a" + connected,
                "&7Alcance de baseline: &e" + (connected * 15) + " metros",
                "&7Sincronización: &bConstruyendo interferometría..."
        ));

        // Panel de Objetivo (Slot 3)
        menu.replaceExistingItem(PANEL_TARGET, new CustomItemStack(
                Material.COMPASS,
                "&d🎯 Objetivo Fijado",
                "&7Cuerpo Celeste: &6&l" + targetName,
                "&7Coordenadas Objetivo:",
                "  &7Azimut: &e" + tarAz + "°",
                "  &7Elevación: &e" + tarEl + "°"
        ));

        // Panel de Telemetría (Slot 4)
        menu.replaceExistingItem(PANEL_TELEMETRY, new CustomItemStack(
                Material.SPYGLASS,
                "&d📊 Telemetría en Vivo (10Hz)",
                "&7Ángulos Actuales:",
                "  &7Azimut: &e" + curAz + "°",
                "  &7Elevación: &e" + curEl + "°",
                "",
                "&7Error de apuntado: &c" + error + "°",
                "&7Estado actual: &b" + stateStr
        ));

        // Panel PID (Slot 5)
        menu.replaceExistingItem(PANEL_PID, new CustomItemStack(
                Material.REPEATER,
                "&d⚙️ Controlador PID de Apuntado",
                "&7Parámetros de Bucle:",
                "  &7Proporcional (Kp): &a0.45",
                "  &7Integral (Ki): &a0.02",
                "  &7Derivativo (Kd): &a0.15",
                "",
                "&7Salida del PID: &b" + (error * 0.45) + " Aceleración"
        ));

        // Panel de Fallas (Slot 6)
        Material faultMaterial = fault.equals("NORMAL") ? Material.LIME_DYE : Material.REDSTONE_BLOCK;
        String faultDisplay = fault.equals("NORMAL") ? "&aNORMAL (Sin Fallas)" : "&c🚨 ERROR: " + fault;
        menu.replaceExistingItem(PANEL_FAULT, new CustomItemStack(
                faultMaterial,
                "&d🛠️ Monitoreo de Diagnóstico",
                "&7Estado físico: " + faultDisplay,
                "&7Si hay una falla activa, debes usar el",
                "&7botón de calibración manual de abajo."
        ));

        // Botón Slew (Slot 28)
        menu.replaceExistingItem(BUTTON_SLEW, new CustomItemStack(
                Material.LEVER,
                "&aIniciar Alineación (PID)",
                "&7Ajusta dinámicamente los motores hacia",
                "&7las coordenadas del objetivo fijado.",
                "",
                "&dHaz clic para ejecutar un ciclo PID."
        ));

        // Botón Reparar (Slot 30)
        menu.replaceExistingItem(BUTTON_REPAIR, new CustomItemStack(
                Material.ANVIL,
                "&cCalibrar y Reparar Motores",
                "&7Resetea errores mecánicos y reajusta",
                "&7el controlador PID a los valores de fábrica.",
                "",
                "&eHaz clic para limpiar fallas activas."
        ));

        // Botón Correlacionar (Slot 34)
        menu.replaceExistingItem(BUTTON_CORRELATE, new CustomItemStack(
                Material.BREWING_STAND,
                "&dIniciar Correlación de Señal",
                "&7Combina fases e intensidades de radio",
                "&7para procesar un registro científico.",
                "",
                "&7Progreso actual: &b" + progress + "%",
                "&7Requiere: &eCelda de Datos Celeste en entrada.",
                "",
                "&dHaz clic para correlacionar la señal."
        ));
    }

    @Nonnull
    @Override
    public int[] getInputSlots() {
        return new int[] { INPUT_SLOT };
    }

    @Nonnull
    @Override
    public int[] getOutputSlots() {
        return new int[] { OUTPUT_SLOT };
    }
}
