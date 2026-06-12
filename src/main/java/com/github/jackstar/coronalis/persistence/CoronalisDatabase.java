package com.github.jackstar.coronalis.persistence;

import com.github.jackstar.coronalis.Coronalis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persistencia transaccional de estaciones. Los inventarios permanecen bajo
 * BlockStorage; esta base sólo conserva metadatos y auditoría.
 */
public final class CoronalisDatabase implements AutoCloseable {

    private final Coronalis plugin;
    private Connection connection;

    public CoronalisDatabase(@Nonnull Coronalis plugin) {
        this.plugin = plugin;
    }

    public synchronized void open() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new SQLException("No se pudo crear " + plugin.getDataFolder());
        }
        File databaseFile = new File(plugin.getDataFolder(), "coronalis.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = FULL");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS components (
                    world_uuid TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    component_type TEXT NOT NULL,
                    owner_uuid TEXT NOT NULL,
                    placed_at INTEGER NOT NULL,
                    PRIMARY KEY (world_uuid, x, y, z)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS stations (
                    station_id TEXT PRIMARY KEY,
                    world_uuid TEXT NOT NULL,
                    foundation_x INTEGER NOT NULL,
                    foundation_y INTEGER NOT NULL,
                    foundation_z INTEGER NOT NULL,
                    owner_uuid TEXT NOT NULL,
                    tier TEXT NOT NULL,
                    valid INTEGER NOT NULL,
                    detail TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS links (
                    station_a TEXT NOT NULL,
                    station_b TEXT NOT NULL,
                    distance REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    PRIMARY KEY (station_a, station_b)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    created_at INTEGER NOT NULL,
                    actor_uuid TEXT,
                    action TEXT NOT NULL,
                    world_uuid TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    detail TEXT NOT NULL
                )
                """);
        }
    }

    @Nonnull
    public synchronized List<ComponentRow> loadComponents() {
        List<ComponentRow> rows = new ArrayList<>();
        String sql = "SELECT world_uuid,x,y,z,component_type,owner_uuid FROM components";
        try (Statement statement = requireConnection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                World world = Bukkit.getWorld(UUID.fromString(result.getString("world_uuid")));
                if (world == null) {
                    continue;
                }
                rows.add(new ComponentRow(
                    new Location(world, result.getInt("x"), result.getInt("y"), result.getInt("z")),
                    result.getString("component_type"),
                    UUID.fromString(result.getString("owner_uuid"))
                ));
            }
        } catch (SQLException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.SEVERE, "No se pudieron cargar componentes Coronalis", exception);
        }
        return rows;
    }

    public synchronized boolean upsertComponent(
        @Nonnull Location location,
        @Nonnull String type,
        @Nonnull UUID owner
    ) {
        String sql = """
            INSERT INTO components(world_uuid,x,y,z,component_type,owner_uuid,placed_at)
            VALUES(?,?,?,?,?,?,?)
            ON CONFLICT(world_uuid,x,y,z) DO UPDATE SET
                component_type=excluded.component_type,
                owner_uuid=excluded.owner_uuid,
                placed_at=excluded.placed_at
            """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindLocation(statement, location, 1);
            statement.setString(5, type);
            statement.setString(6, owner.toString());
            statement.setLong(7, Instant.now().toEpochMilli());
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar componente " + location, exception);
            return false;
        }
    }

    public synchronized boolean deleteComponent(@Nonnull Location location) {
        String sql = "DELETE FROM components WHERE world_uuid=? AND x=? AND y=? AND z=?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindLocation(statement, location, 1);
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo retirar componente " + location, exception);
            return false;
        }
    }

    public synchronized void replaceStations(
        @Nonnull List<StationRow> stations,
        @Nonnull List<LinkRow> links
    ) {
        Connection db = requireConnection();
        try {
            db.setAutoCommit(false);
            try (Statement statement = db.createStatement()) {
                statement.executeUpdate("DELETE FROM links");
                statement.executeUpdate("DELETE FROM stations");
            }
            String stationSql = """
                INSERT INTO stations(station_id,world_uuid,foundation_x,foundation_y,foundation_z,
                    owner_uuid,tier,valid,detail,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)
                """;
            try (PreparedStatement statement = db.prepareStatement(stationSql)) {
                for (StationRow station : stations) {
                    statement.setString(1, station.id());
                    bindLocation(statement, station.foundation(), 2);
                    statement.setString(6, station.owner().toString());
                    statement.setString(7, station.tier());
                    statement.setInt(8, station.valid() ? 1 : 0);
                    statement.setString(9, station.detail());
                    statement.setLong(10, Instant.now().toEpochMilli());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            String linkSql = "INSERT INTO links(station_a,station_b,distance,created_at) VALUES(?,?,?,?)";
            try (PreparedStatement statement = db.prepareStatement(linkSql)) {
                for (LinkRow link : links) {
                    statement.setString(1, link.stationA());
                    statement.setString(2, link.stationB());
                    statement.setDouble(3, link.distance());
                    statement.setLong(4, Instant.now().toEpochMilli());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            db.commit();
        } catch (SQLException exception) {
            try {
                db.rollback();
            } catch (SQLException rollbackError) {
                exception.addSuppressed(rollbackError);
            }
            plugin.getLogger().log(Level.SEVERE, "No se pudo persistir el mapa de estaciones", exception);
        } finally {
            try {
                db.setAutoCommit(true);
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.WARNING, "No se pudo restaurar auto-commit", exception);
            }
        }
    }

    public synchronized void audit(
        @Nullable UUID actor,
        @Nonnull String action,
        @Nullable Location location,
        @Nonnull String detail
    ) {
        String sql = """
            INSERT INTO audit_log(created_at,actor_uuid,action,world_uuid,x,y,z,detail)
            VALUES(?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setLong(1, Instant.now().toEpochMilli());
            statement.setString(2, actor == null ? null : actor.toString());
            statement.setString(3, action);
            if (location == null || location.getWorld() == null) {
                statement.setString(4, null);
                statement.setObject(5, null);
                statement.setObject(6, null);
                statement.setObject(7, null);
            } else {
                statement.setString(4, location.getWorld().getUID().toString());
                statement.setInt(5, location.getBlockX());
                statement.setInt(6, location.getBlockY());
                statement.setInt(7, location.getBlockZ());
            }
            statement.setString(8, detail.length() > 500 ? detail.substring(0, 500) : detail);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "No se pudo escribir auditoría " + action, exception);
        }
    }

    public synchronized void checkpoint() {
        try (Statement statement = requireConnection().createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "No se pudo cerrar el WAL de Coronalis", exception);
        }
    }

    @Override
    public synchronized void close() {
        if (connection == null) {
            return;
        }
        checkpoint();
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "No se pudo cerrar coronalis.db", exception);
        } finally {
            connection = null;
        }
    }

    @Nonnull
    private Connection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("CoronalisDatabase aún no está abierta");
        }
        return connection;
    }

    private static void bindLocation(
        @Nonnull PreparedStatement statement,
        @Nonnull Location location,
        int firstIndex
    ) throws SQLException {
        if (location.getWorld() == null) {
            throw new SQLException("Ubicación sin mundo");
        }
        statement.setString(firstIndex, location.getWorld().getUID().toString());
        statement.setInt(firstIndex + 1, location.getBlockX());
        statement.setInt(firstIndex + 2, location.getBlockY());
        statement.setInt(firstIndex + 3, location.getBlockZ());
    }

    public record ComponentRow(Location location, String type, UUID owner) {}

    public record StationRow(
        String id,
        Location foundation,
        UUID owner,
        String tier,
        boolean valid,
        String detail
    ) {}

    public record LinkRow(String stationA, String stationB, double distance) {}
}
