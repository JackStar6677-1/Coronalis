package com.github.jackstar.coronalis.implementation.setup;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.implementation.Items;
import com.github.jackstar.coronalis.implementation.items.ArrayNetworkBlock;
import com.github.jackstar.coronalis.implementation.items.ControlConsole;
import com.github.jackstar.coronalis.implementation.items.DataRecordItem;
import com.github.jackstar.coronalis.implementation.items.FieldGuideItem;
import com.github.jackstar.coronalis.implementation.items.RadioTelescope;
import com.github.drakescraft_labs.slimefun4.implementation.SlimefunItems;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.api.researches.Research;

public class ItemSetup {

    private static boolean registered = false;

    private ItemSetup() {}

    public static void setup(@Nonnull Coronalis plugin) {
        if (registered) {
            throw new UnsupportedOperationException("Coronalis items can only be registered once!");
        }
        registered = true;

        Research radioEngineering = new Research(new NamespacedKey(plugin, "radio_engineering"), 88620, "Ingeniería de Radio", 14);
        Research pidControl = new Research(new NamespacedKey(plugin, "pid_control"), 88621, "Control PID del Vacío", 24);
        Research interferometry = new Research(new NamespacedKey(plugin, "interferometry"), 88622, "Interferometría VLBI", 34);
        Research deepSkyOps = new Research(new NamespacedKey(plugin, "deep_sky_ops"), 88623, "Operaciones de Cielo Profundo", 44);
        Research arrayInfrastructure = new Research(new NamespacedKey(plugin, "array_infrastructure"), 88624, "Infraestructura de Array", 18);
        Research arrayAutomation = new Research(new NamespacedKey(plugin, "array_automation"), 88625, "Automatización de Observatorio", 36);
        Research stationEngineering = new Research(new NamespacedKey(plugin, "station_engineering"), 88626, "Estaciones de Escucha", 28);

        /* 1. Componentes Básicos */
        SlimefunItem antenna = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_ANTENNA_DISH, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.STEEL_PLATE, SlimefunItems.ALUMINUM_INGOT, SlimefunItems.STEEL_PLATE,
                SlimefunItems.ALUMINUM_INGOT, new ItemStack(Material.GLASS_PANE), SlimefunItems.ALUMINUM_INGOT,
                SlimefunItems.COPPER_WIRE, new ItemStack(Material.GLASS_PANE), SlimefunItems.COPPER_WIRE
        });
        antenna.setResearch(radioEngineering);
        antenna.register(plugin);

        SlimefunItem receiver = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_RECEIVER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.GOLD_24K, SlimefunItems.SYNTHETIC_SAPPHIRE, SlimefunItems.GOLD_24K,
                SlimefunItems.COPPER_WIRE, SlimefunItems.SILICON, SlimefunItems.COPPER_WIRE,
                SlimefunItems.COPPER_INGOT, SlimefunItems.BASIC_CIRCUIT_BOARD, SlimefunItems.COPPER_INGOT
        });
        receiver.setResearch(radioEngineering);
        receiver.register(plugin);

        SlimefunItem pid = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_PID_CONTROLLER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.REPEATER), SlimefunItems.BASIC_CIRCUIT_BOARD, new ItemStack(Material.COMPARATOR),
                SlimefunItems.COPPER_WIRE, SlimefunItems.ADVANCED_CIRCUIT_BOARD, SlimefunItems.COPPER_WIRE,
                SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.BATTERY, SlimefunItems.ELECTRIC_MOTOR
        });
        pid.setResearch(pidControl);
        pid.register(plugin);

        SlimefunItem dataCell = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_DATA_CELL, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.GLASS), SlimefunItems.SILICON, new ItemStack(Material.GLASS),
                SlimefunItems.COPPER_WIRE, SlimefunItems.SYNTHETIC_DIAMOND, SlimefunItems.COPPER_WIRE,
                new ItemStack(Material.GLASS), SlimefunItems.BASIC_CIRCUIT_BOARD, new ItemStack(Material.GLASS)
        });
        dataCell.setResearch(interferometry);
        dataCell.register(plugin);

        ArrayNetworkBlock cable = new ArrayNetworkBlock(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_COAXIAL_CABLE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.COPPER_WIRE, SlimefunItems.COPPER_INGOT, SlimefunItems.COPPER_WIRE,
                new ItemStack(Material.CHAIN), SlimefunItems.SILICON, new ItemStack(Material.CHAIN),
                SlimefunItems.COPPER_WIRE, SlimefunItems.COPPER_INGOT, SlimefunItems.COPPER_WIRE
        });
        cable.setResearch(arrayInfrastructure);
        cable.register(plugin);

        ArrayNetworkBlock signalCore = new ArrayNetworkBlock(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_SIGNAL_CORE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.BATTERY, Items.CORONALIS_RECEIVER, SlimefunItems.BATTERY,
                SlimefunItems.ENERGY_CONNECTOR, SlimefunItems.SOLAR_PANEL, SlimefunItems.ENERGY_CONNECTOR,
                SlimefunItems.REINFORCED_PLATE, Items.CORONALIS_PID_CONTROLLER, SlimefunItems.REINFORCED_PLATE
        });
        signalCore.setResearch(arrayInfrastructure);
        signalCore.register(plugin);

        ArrayNetworkBlock amplifier = new ArrayNetworkBlock(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_SIGNAL_AMPLIFIER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.SYNTHETIC_SAPPHIRE, Items.CORONALIS_RECEIVER, SlimefunItems.SYNTHETIC_SAPPHIRE,
                SlimefunItems.COPPER_WIRE, SlimefunItems.ADVANCED_CIRCUIT_BOARD, SlimefunItems.COPPER_WIRE,
                SlimefunItems.REINFORCED_PLATE, Items.CORONALIS_COAXIAL_CABLE, SlimefunItems.REINFORCED_PLATE
        });
        amplifier.setResearch(arrayAutomation);
        amplifier.register(plugin);

        ArrayNetworkBlock dataBank = new ArrayNetworkBlock(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_DATA_BANK, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.SYNTHETIC_DIAMOND, Items.CORONALIS_DATA_CELL, SlimefunItems.SYNTHETIC_DIAMOND,
                SlimefunItems.BASIC_CIRCUIT_BOARD, SlimefunItems.GPS_TRANSMITTER_2, SlimefunItems.BASIC_CIRCUIT_BOARD,
                SlimefunItems.REINFORCED_PLATE, Items.CORONALIS_COAXIAL_CABLE, SlimefunItems.REINFORCED_PLATE
        });
        dataBank.setResearch(arrayAutomation);
        dataBank.register(plugin);

        ArrayNetworkBlock autoCalibrator = new ArrayNetworkBlock(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_AUTO_CALIBRATOR, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.ELECTRIC_MOTOR, Items.CORONALIS_PID_CONTROLLER, SlimefunItems.ELECTRIC_MOTOR,
                SlimefunItems.ADVANCED_CIRCUIT_BOARD, SlimefunItems.GPS_TRANSMITTER_3, SlimefunItems.ADVANCED_CIRCUIT_BOARD,
                SlimefunItems.ENERGY_CONNECTOR, Items.CORONALIS_SIGNAL_CORE, SlimefunItems.ENERGY_CONNECTOR
        });
        autoCalibrator.setResearch(arrayAutomation);
        autoCalibrator.register(plugin);

        /* 2. Bloques Interactivos */
        RadioTelescope telescope = new RadioTelescope(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_RADIO_TELESCOPE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.CORONALIS_ANTENNA_DISH, Items.CORONALIS_RECEIVER, Items.CORONALIS_ANTENNA_DISH,
                SlimefunItems.REINFORCED_PLATE, Items.CORONALIS_PID_CONTROLLER, SlimefunItems.REINFORCED_PLATE,
                SlimefunItems.ELECTRIC_MOTOR, Items.CORONALIS_COAXIAL_CABLE, SlimefunItems.ELECTRIC_MOTOR
        });
        telescope.setResearch(pidControl);
        telescope.register(plugin);

        ControlConsole console = new ControlConsole(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_CONTROL_CONSOLE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.LAPIS_BLOCK), Items.CORONALIS_PID_CONTROLLER, new ItemStack(Material.LAPIS_BLOCK),
                SlimefunItems.GPS_CONTROL_PANEL, Items.CORONALIS_RECEIVER, SlimefunItems.ADVANCED_CIRCUIT_BOARD,
                Items.CORONALIS_SIGNAL_CORE, Items.CORONALIS_DATA_CELL, Items.CORONALIS_SIGNAL_CORE
        });
        console.setResearch(deepSkyOps);
        console.register(plugin);

        /* 3. Estaciones Firewatch y enlaces */
        SlimefunItem foundation = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_FOUNDATION_ANCHOR, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.REINFORCED_PLATE, SlimefunItems.STEEL_PLATE, SlimefunItems.REINFORCED_PLATE,
                SlimefunItems.LEAD_INGOT, new ItemStack(Material.LODESTONE), SlimefunItems.LEAD_INGOT,
                SlimefunItems.STEEL_PLATE, SlimefunItems.HARDENED_METAL_INGOT, SlimefunItems.STEEL_PLATE
        });
        foundation.setResearch(stationEngineering);
        foundation.register(plugin);

        SlimefunItem segment = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_TOWER_SEGMENT, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.STEEL_PLATE, SlimefunItems.COPPER_INGOT, SlimefunItems.STEEL_PLATE,
                SlimefunItems.COPPER_WIRE, new ItemStack(Material.SCAFFOLDING), SlimefunItems.COPPER_WIRE,
                SlimefunItems.STEEL_PLATE, SlimefunItems.COPPER_INGOT, SlimefunItems.STEEL_PLATE
        });
        segment.setResearch(stationEngineering);
        segment.register(plugin);

        SlimefunItem platform = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_WATCH_PLATFORM, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.REINFORCED_PLATE, SlimefunItems.STEEL_PLATE, SlimefunItems.REINFORCED_PLATE,
                Items.CORONALIS_TOWER_SEGMENT, new ItemStack(Material.SMITHING_TABLE), Items.CORONALIS_TOWER_SEGMENT,
                SlimefunItems.STEEL_PLATE, SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.STEEL_PLATE
        });
        platform.setResearch(stationEngineering);
        platform.register(plugin);

        SlimefunItem mast = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_RADIO_MAST, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.COPPER_WIRE, Items.CORONALIS_RECEIVER, SlimefunItems.COPPER_WIRE,
                SlimefunItems.STEEL_PLATE, new ItemStack(Material.LIGHTNING_ROD), SlimefunItems.STEEL_PLATE,
                Items.CORONALIS_COAXIAL_CABLE, SlimefunItems.ELECTRIC_MOTOR, Items.CORONALIS_COAXIAL_CABLE
        });
        mast.setResearch(stationEngineering);
        mast.register(plugin);

        SlimefunItem dish = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_PARABOLIC_DISH, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.ALUMINUM_INGOT, Items.CORONALIS_ANTENNA_DISH, SlimefunItems.ALUMINUM_INGOT,
                SlimefunItems.COPPER_WIRE, Items.CORONALIS_RECEIVER, SlimefunItems.COPPER_WIRE,
                SlimefunItems.STEEL_PLATE, Items.CORONALIS_RADIO_MAST, SlimefunItems.STEEL_PLATE
        });
        dish.setResearch(stationEngineering);
        dish.register(plugin);

        SlimefunItem repeater = new SlimefunItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_SIGNAL_REPEATER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.CORONALIS_RECEIVER, SlimefunItems.GPS_TRANSMITTER_2, Items.CORONALIS_RECEIVER,
                Items.CORONALIS_SIGNAL_AMPLIFIER, SlimefunItems.ADVANCED_CIRCUIT_BOARD, Items.CORONALIS_SIGNAL_AMPLIFIER,
                SlimefunItems.ENERGY_CONNECTOR, Items.CORONALIS_SIGNAL_CORE, SlimefunItems.ENERGY_CONNECTOR
        });
        repeater.setResearch(stationEngineering);
        repeater.register(plugin);

        FieldGuideItem guide = new FieldGuideItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_FIELD_GUIDE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.PAPER), new ItemStack(Material.COMPASS), new ItemStack(Material.PAPER),
                new ItemStack(Material.BOOK), SlimefunItems.GPS_TRANSMITTER, new ItemStack(Material.BOOK),
                new ItemStack(Material.PAPER), Items.CORONALIS_RECEIVER, new ItemStack(Material.PAPER)
        });
        guide.register(plugin);

        /* 4. Recompensas (Descubrimientos) */
        DataRecordItem record = new DataRecordItem(ItemGroups.CORONALIS_GROUP, Items.CORONALIS_RECORD_DISCOVERED, RecipeType.NULL, new ItemStack[9]);
        record.register(plugin);

        radioEngineering.addItems(antenna, receiver);
        radioEngineering.register();

        pidControl.addItems(pid, telescope);
        pidControl.register();

        interferometry.addItems(dataCell);
        interferometry.register();

        arrayInfrastructure.addItems(cable, signalCore);
        arrayInfrastructure.register();

        arrayAutomation.addItems(amplifier, dataBank, autoCalibrator);
        arrayAutomation.register();

        stationEngineering.addItems(foundation, segment, platform, mast, dish, repeater);
        stationEngineering.register();

        deepSkyOps.addItems(console, record);
        deepSkyOps.register();
    }
}
