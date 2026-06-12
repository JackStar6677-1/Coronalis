package com.github.jackstar.coronalis;

import java.io.File;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.plugin.java.JavaPlugin;

import com.github.drakescraft_labs.labupdate.DrakesLabsReleaseUpdate;
import com.github.drakescraft_labs.slimefun4.api.SlimefunAddon;
import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.jackstar.coronalis.discovery.DiscoveryListener;
import com.github.jackstar.coronalis.discovery.DiscoveryService;
import com.github.jackstar.coronalis.commands.CoronalisCommand;
import com.github.jackstar.coronalis.implementation.items.ControlConsole;
import com.github.jackstar.coronalis.implementation.setup.ItemSetup;
import com.github.jackstar.coronalis.listeners.InfrastructureListener;
import com.github.jackstar.coronalis.managers.AccessManager;
import com.github.jackstar.coronalis.managers.CosmicEventManager;
import com.github.jackstar.coronalis.managers.NetworkRegistry;
import com.github.jackstar.coronalis.managers.ObservationProgramManager;
import com.github.jackstar.coronalis.managers.ObservatoryOperations;
import com.github.jackstar.coronalis.managers.SoundManager;
import com.github.jackstar.coronalis.persistence.CoronalisDatabase;
import com.github.jackstar.coronalis.structures.StationManager;

import java.sql.SQLException;

/**
 * Addon Slimefun Coronalis — radiotelescopios, correlación de fase y catálogo XP.
 */
public class Coronalis extends JavaPlugin implements SlimefunAddon {

    private static Coronalis instance;
    private DiscoveryService discoveryService;
    private CosmicEventManager cosmicEventManager;
    private AccessManager accessManager;
    private NetworkRegistry networkRegistry;
    private SoundManager soundManager;
    private ObservatoryOperations observatoryOperations;
    private ObservationProgramManager observationProgramManager;
    private CoronalisDatabase database;
    private StationManager stationManager;

    @Override
    public void onEnable() {
        DrakesLabsReleaseUpdate.schedule(this, "Coronalis-drake");
        instance = this;

        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        reloadConfig();
        discoveryService = new DiscoveryService(this);
        cosmicEventManager = new CosmicEventManager();
        accessManager = new AccessManager();
        networkRegistry = new NetworkRegistry();
        soundManager = new SoundManager();
        observatoryOperations = new ObservatoryOperations(this);
        observationProgramManager = new ObservationProgramManager(this);

        log("§d==================================================");
        log("§5[Coronalis] §dSintonizando el array de fase...");

        getServer().getPluginManager().registerEvents(new DiscoveryListener(discoveryService), this);
        getServer().getPluginManager().registerEvents(new InfrastructureListener(), this);
        accessManager.register();
        if (getCommand("coronalis") != null) {
            CoronalisCommand command = new CoronalisCommand();
            getCommand("coronalis").setExecutor(command);
            getCommand("coronalis").setTabCompleter(command);
        }
        loadItems();
        database = new CoronalisDatabase(this);
        try {
            database.open();
        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, "No se pudo abrir coronalis.db; se deshabilita para evitar datos inseguros.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        stationManager = new StationManager(this, database);
        getServer().getPluginManager().registerEvents(stationManager, this);
        cosmicEventManager.start();
        getServer().getScheduler().runTaskTimer(this, ControlConsole::tickAll, 40L, 40L);
        getServer().getScheduler().runTaskTimer(this, () -> networkRegistry.tickAll(), 80L, 80L);

        startSignalSequence();
        log("§5[Coronalis] §aAddon cargado.");
        log("§d==================================================");
    }

    @Override
    public void onDisable() {
        if (instance != null) {
            log("§5[Coronalis/DEEP-LINK] §7Archivando matriz de señales y telemetría...");
        }
        if (cosmicEventManager != null) {
            cosmicEventManager.stop();
        }
        if (soundManager != null) {
            soundManager.stopAll();
        }
        if (observatoryOperations != null) {
            observatoryOperations.stopAll();
        }
        if (stationManager != null) {
            stationManager.stop();
        }
        saveSlimefunInventories();
        if (database != null) {
            database.close();
        }
        ControlConsole.ACTIVE_CONSOLES.clear();
        log("§5[Coronalis] §cDeshabilitado.");
        instance = null;
        discoveryService = null;
        cosmicEventManager = null;
        accessManager = null;
        networkRegistry = null;
        soundManager = null;
        observatoryOperations = null;
        observationProgramManager = null;
        stationManager = null;
        database = null;
    }

    @Nonnull
    public DiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    @Nonnull
    public CosmicEventManager getCosmicEventManager() {
        return cosmicEventManager;
    }

    @Nonnull
    public AccessManager getAccessManager() {
        return accessManager;
    }

    @Nonnull
    public NetworkRegistry getNetworkRegistry() {
        return networkRegistry;
    }

    @Nonnull
    public SoundManager getSoundManager() {
        return soundManager;
    }

    @Nonnull
    public ObservatoryOperations getObservatoryOperations() {
        return observatoryOperations;
    }

    @Nonnull
    public ObservationProgramManager getObservationProgramManager() {
        return observationProgramManager;
    }

    @Nonnull
    public StationManager getStationManager() {
        return stationManager;
    }

    @Nonnull
    public CoronalisDatabase getDatabase() {
        return database;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/JackStar6677-1/Coronalis/issues";
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nonnull
    public static Coronalis instance() {
        return instance;
    }

    public static void log(@Nonnull String message) {
        instance().getLogger().info(message);
    }

    private void loadItems() {
        try {
            ItemSetup.setup(this);
        } catch (Exception | LinkageError x) {
            instance().getLogger().log(Level.SEVERE, x, () -> "Error loading Coronalis items");
        }
    }

    private void startSignalSequence() {
        log("§8[Coronalis/DEEP-LINK] §7Escuchando portadora 1420.405 MHz...");
        getServer().getScheduler().runTaskLater(this, () ->
            log("§5[Coronalis/DEEP-LINK] §dSeñal no catalogada detectada. Origen: UNKNOWN_ADDON"), 15L);
        getServer().getScheduler().runTaskLater(this, () ->
            log("§5[Coronalis/DEEP-LINK] §dHandshake 43:4F:52:4F:4E:41:4C:49:53 aceptado."), 30L);
        getServer().getScheduler().runTaskLater(this, () ->
            log("§8[Coronalis/DEEP-LINK] §aEnlace estabilizado; estaciones en escucha."), 45L);
    }

    private void saveSlimefunInventories() {
        try {
            for (var world : getServer().getWorlds()) {
                BlockStorage storage = BlockStorage.getStorage(world);
                if (storage != null) {
                    storage.save();
                }
            }
            BlockStorage.saveChunks();
            log("§5[Coronalis/Persistencia] §aInventarios Slimefun sincronizados.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Falló el guardado explícito de inventarios Slimefun", exception);
        }
    }
}
