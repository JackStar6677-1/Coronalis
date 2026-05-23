package com.github.jackstar.coronalis.managers;

import com.github.drakescraft_labs.slimefun4.legacy.api.BlockStorage;
import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.implementation.data.CoronalisNetwork;
import com.github.jackstar.coronalis.implementation.data.ObservationProgram;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona programas científicos persistidos en la consola.
 */
public class ObservationProgramManager {

    private static final String STORAGE_KEY = "active_program";

    private final Coronalis plugin;

    public ObservationProgramManager(@Nonnull Coronalis plugin) {
        this.plugin = plugin;
    }

    public void list(@Nonnull CommandSender sender) {
        sender.sendMessage("§5§l[Coronalis] §dProgramas científicos");
        for (ObservationProgram program : ObservationProgram.values()) {
            sender.sendMessage("§8- §e" + program.getId()
                + " §7(" + program.getMinTelescopes() + " ant, "
                + program.getMinBaselines() + " baselines, "
                + program.getMinCalibrationPercent() + "% calib) §8- §f"
                + program.getDisplayName());
        }
        sender.sendMessage("§7Usa §d/coronalis program <id> §7junto a una consola.");
    }

    public void select(@Nonnull CommandSender sender, @Nonnull ObservationProgram program) {
        CoronalisNetwork network = plugin.getObservatoryOperations().requireNearestNetwork(sender);
        if (network == null) return;

        Location loc = network.getConsoleLoc();
        BlockStorage.addBlockInfo(loc, STORAGE_KEY, program.getId());
        BlockStorage.addBlockInfo(loc, "target_az", String.valueOf(program.getTarget().az));
        BlockStorage.addBlockInfo(loc, "target_el", String.valueOf(program.getTarget().el));
        BlockStorage.addBlockInfo(loc, "selected_target", program.getTarget().displayName);
        BlockStorage.addBlockInfo(loc, "correlation_progress", "0");

        sender.sendMessage("§5[Coronalis] §aPrograma activo: §e" + program.getDisplayName());
        sender.sendMessage("§7Objetivo fijado: §d" + program.getTarget().displayName
            + " §8Az/El " + program.getTarget().az + "/" + program.getTarget().el);
        sender.sendMessage("§7Usa §d/coronalis program status §7para revisar requisitos.");
        Coronalis.log("[Coronalis/Program] " + network.getId() + " selected " + program.getId());
    }

    public void clear(@Nonnull CommandSender sender) {
        CoronalisNetwork network = plugin.getObservatoryOperations().requireNearestNetwork(sender);
        if (network == null) return;
        BlockStorage.addBlockInfo(network.getConsoleLoc(), STORAGE_KEY, "");
        sender.sendMessage("§5[Coronalis] §7Programa científico limpiado.");
    }

    public void status(@Nonnull CommandSender sender) {
        CoronalisNetwork network = plugin.getObservatoryOperations().requireNearestNetwork(sender);
        if (network == null) return;
        ObservationProgram program = getActiveProgram(network.getConsoleLoc());
        if (program == null) {
            sender.sendMessage("§5[Coronalis] §cNo hay programa activo. Usa §d/coronalis programs§c.");
            return;
        }
        ProgramReport report = evaluate(network, program);
        sender.sendMessage("§5§l[Coronalis] §dPrograma: §e" + program.getDisplayName());
        sender.sendMessage("§7Objetivo: §d" + program.getTarget().displayName + " §8| §7XP: §a" + program.getXpReward());
        sender.sendMessage("§7" + program.getDescription());
        report.lines().forEach(sender::sendMessage);
        sender.sendMessage(report.ready()
            ? "§aListo para completar: §d/coronalis program complete"
            : "§cAún faltan requisitos para completar.");
    }

    public void complete(@Nonnull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[Coronalis] Completar programas requiere jugador para entregar XP.");
            return;
        }
        CoronalisNetwork network = plugin.getObservatoryOperations().requireNearestNetwork(sender);
        if (network == null) return;
        ObservationProgram program = getActiveProgram(network.getConsoleLoc());
        if (program == null) {
            player.sendMessage("§5[Coronalis] §cNo hay programa activo.");
            return;
        }

        ProgramReport report = evaluate(network, program);
        if (!report.ready()) {
            player.sendMessage("§5[Coronalis] §cPrograma incompleto:");
            report.lines().forEach(player::sendMessage);
            return;
        }

        String discoveryId = "program_" + program.getId();
        plugin.getDiscoveryService().tryDiscoverWithMultiplier(
            player,
            discoveryId,
            "Programa: " + program.getDisplayName(),
            "discovery-xp." + discoveryId,
            1.0
        );
        BlockStorage.addBlockInfo(network.getConsoleLoc(), STORAGE_KEY, "");
        player.sendMessage("§5§l[Coronalis] §aPrograma científico completado: §e" + program.getDisplayName());
        player.sendMessage("§7Recompensa configurada: §a+" + program.getXpReward() + " XP base");
        Coronalis.log("[Coronalis/Program] " + player.getName() + " completed " + program.getId()
            + " on " + network.getId());
    }

    @Nullable
    public ObservationProgram getActiveProgram(@Nonnull Location consoleLoc) {
        String raw = BlockStorage.getLocationInfo(consoleLoc, STORAGE_KEY);
        return raw == null || raw.isBlank() ? null : ObservationProgram.byId(raw);
    }

    @Nonnull
    private ProgramReport evaluate(@Nonnull CoronalisNetwork network, @Nonnull ObservationProgram program) {
        List<String> lines = new ArrayList<>();
        boolean ready = true;

        ready &= check(lines, "Telescopios", network.getTelescopeCount(), program.getMinTelescopes());
        ready &= check(lines, "Baselines UV", network.getBaselineCount(), program.getMinBaselines());
        ready &= check(lines, "Calibración global",
            (int) Math.round(network.getAverageCalibrationFactor() * 100.0), program.getMinCalibrationPercent());
        ready &= checkDouble(lines, "Flujo simulado", network.getTotalSignalAmplitude(), program.getMinSignal());

        String selected = BlockStorage.getLocationInfo(network.getConsoleLoc(), "selected_target");
        boolean targetOk = program.getTarget().displayName.equals(selected);
        lines.add((targetOk ? "§a✔ " : "§c✘ ") + "Objetivo: §e"
            + (selected == null || selected.isBlank() ? "Ninguno" : selected)
            + "§7 requerido: §d" + program.getTarget().displayName);
        ready &= targetOk;

        String module = program.getRequiredModule();
        if (module != null) {
            int count = network.getModuleCount(module);
            boolean moduleOk = count > 0;
            lines.add((moduleOk ? "§a✔ " : "§c✘ ") + "Módulo requerido: §e" + module + " §7x" + count);
            ready &= moduleOk;
        }

        return new ProgramReport(ready, lines);
    }

    private static boolean check(@Nonnull List<String> lines, @Nonnull String label, int actual, int required) {
        boolean ok = actual >= required;
        lines.add((ok ? "§a✔ " : "§c✘ ") + label + ": §e" + actual + "§7/§e" + required);
        return ok;
    }

    private static boolean checkDouble(@Nonnull List<String> lines, @Nonnull String label, double actual, double required) {
        boolean ok = actual >= required;
        lines.add((ok ? "§a✔ " : "§c✘ ") + label + ": §e" + round1(actual) + "§7/§e" + round1(required));
        return ok;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record ProgramReport(boolean ready, List<String> lines) {}
}
