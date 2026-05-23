package com.github.jackstar.coronalis.commands;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.jackstar.coronalis.Coronalis;
import com.github.jackstar.coronalis.implementation.data.CoronalisNetwork;
import com.github.jackstar.coronalis.implementation.data.ObservationProgram;
import com.github.jackstar.coronalis.implementation.data.TelescopeState;
import com.github.jackstar.coronalis.implementation.items.ControlConsole;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Terminal textual de Coronalis.
 *
 * <p>Incluye guías de uso y un smoke test ejecutable desde consola para validar
 * registros básicos sin montar una instalación completa en el mundo.</p>
 */
public class CoronalisCommand implements CommandExecutor, TabCompleter {

    private static final String[] CORONALIS_ITEMS = {
        "CORONALIS_ANTENNA_DISH",
        "CORONALIS_RECEIVER",
        "CORONALIS_PID_CONTROLLER",
        "CORONALIS_DATA_CELL",
        "CORONALIS_COAXIAL_CABLE",
        "CORONALIS_SIGNAL_CORE",
        "CORONALIS_SIGNAL_AMPLIFIER",
        "CORONALIS_DATA_BANK",
        "CORONALIS_AUTO_CALIBRATOR",
        "CORONALIS_RADIO_TELESCOPE",
        "CORONALIS_CONTROL_CONSOLE",
        "CORONALIS_RECORD_DISCOVERED"
    };

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command,
                             @Nonnull String label, @Nonnull String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help", "ayuda" -> showHelp(sender);
            case "guide", "guia" -> showGuide(sender, args.length >= 2 ? args[1] : "inicio");
            case "items" -> showItems(sender);
            case "smoke", "selftest", "test" -> runSmoke(sender);
            case "compare" -> showSimulatorCompare(sender);
            case "move" -> handleMove(sender, args);
            case "reset" -> Coronalis.instance().getObservatoryOperations().reset(sender);
            case "fault" -> handleFault(sender, args);
            case "tune", "pid" -> handleTune(sender, args);
            case "step" -> handleStep(sender, args);
            case "scan" -> handleScan(sender, args);
            case "dashboard", "dash" -> Coronalis.instance().getObservatoryOperations().dashboard(sender);
            case "maintenance", "maint", "ai" -> handleMaintenance(sender, args);
            case "export" -> handleExport(sender, args);
            case "programs" -> Coronalis.instance().getObservationProgramManager().list(sender);
            case "program" -> handleProgram(sender, args);
            case "auth" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("[Coronalis] /coronalis auth requiere jugador.");
                    return true;
                }
                Coronalis.instance().getAccessManager().promptLastDeniedPassword(player);
            }
            case "status" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("[Coronalis] /coronalis status requiere jugador para localizar una consola cercana.");
                    return true;
                }
                showNearestStatus(player);
            }
            case "telemetry", "telemetria" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("[Coronalis] /coronalis telemetry requiere jugador para localizar una consola cercana.");
                    return true;
                }
                showNearestTelemetry(player);
            }
            default -> showHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command,
                                      @Nonnull String alias, @Nonnull String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of(
                "help", "guide", "items", "smoke", "status", "telemetry", "compare",
                "move", "reset", "fault", "tune", "step", "scan", "dashboard", "maintenance", "export",
                "programs", "program", "auth"
            ));
        }
        if (args.length == 2 && ("guide".equalsIgnoreCase(args[0]) || "guia".equalsIgnoreCase(args[0]))) {
            return filter(args[1], List.of("inicio", "energia", "cableado", "calibracion", "automatizacion", "programas", "acceso", "fallos", "ciencia", "operaciones"));
        }
        if (args.length == 2 && ("tune".equalsIgnoreCase(args[0]) || "pid".equalsIgnoreCase(args[0]))) {
            return filter(args[1], List.of("default", "aggressive", "damped", "sluggish", "precision", "storm_safe", "custom"));
        }
        if (args.length == 2 && "scan".equalsIgnoreCase(args[0])) {
            return filter(args[1], List.of("spiral", "wave", "raster", "stop", "status"));
        }
        if (args.length == 2 && "export".equalsIgnoreCase(args[0])) {
            return filter(args[1], List.of("json", "csv"));
        }
        if (args.length == 2 && "program".equalsIgnoreCase(args[0])) {
            List<String> options = new ArrayList<>(List.of("status", "clear", "complete"));
            for (ObservationProgram program : ObservationProgram.values()) {
                options.add(program.getId());
            }
            return filter(args[1], options);
        }
        if (args.length == 2 && ("maintenance".equalsIgnoreCase(args[0]) || "maint".equalsIgnoreCase(args[0]) || "ai".equalsIgnoreCase(args[0]))) {
            return filter(args[1], List.of("report", "repair"));
        }
        return List.of();
    }

    private static void showHelp(@Nonnull CommandSender sender) {
        sender.sendMessage("§5§l[Coronalis] §dTerminal de ayuda");
        sender.sendMessage("§d/coronalis help §8- §7muestra este resumen.");
        sender.sendMessage("§d/coronalis guide inicio §8- §7pasos para construir el primer array.");
        sender.sendMessage("§d/coronalis guide energia §8- §7cómo alimentar SU/Joules.");
        sender.sendMessage("§d/coronalis guide cableado §8- §7qué se conecta por cable coaxial.");
        sender.sendMessage("§d/coronalis guide calibracion §8- §7cómo calibrar y desbloquear correlación.");
        sender.sendMessage("§d/coronalis guide automatizacion §8- §7cargo, energía, auto-calibrador y modo auto.");
        sender.sendMessage("§d/coronalis guide acceso §8- §7owner, whitelist y contraseña.");
        sender.sendMessage("§d/coronalis guide fallos §8- §7qué hacer si algo no funciona.");
        sender.sendMessage("§d/coronalis items §8- §7lista de bloques/ítems del addon.");
        sender.sendMessage("§d/coronalis smoke §8- §7self-test de registros/config desde consola o juego.");
        sender.sendMessage("§d/coronalis status §8- §7diagnóstico de la consola cercana.");
        sender.sendMessage("§d/coronalis telemetry §8- §7telemetría AstroControlSim de la consola cercana.");
        sender.sendMessage("§d/coronalis compare §8- §7qué partes del simulador ya están portadas/faltan.");
        sender.sendMessage("§d/coronalis move <az> <el> §8- §7manda objetivo manual al array.");
        sender.sendMessage("§d/coronalis tune <preset|custom> [args] §8- §7ajusta PID global o por antena.");
        sender.sendMessage("§d/coronalis scan <spiral|wave|raster|stop|status> §8- §7patrones de búsqueda.");
        sender.sendMessage("§d/coronalis dashboard §8- §7panel textual tipo SciOps.");
        sender.sendMessage("§d/coronalis maintenance [repair] §8- §7AI maintenance y auto-repair.");
        sender.sendMessage("§d/coronalis export <json|csv> §8- §7exporta telemetría al data folder.");
        sender.sendMessage("§d/coronalis programs §8- §7lista misiones científicas.");
        sender.sendMessage("§d/coronalis program <id|status|complete|clear> §8- §7gestiona programa activo.");
        sender.sendMessage("§d/coronalis reset §8- §7limpia fallos runtime del array cercano.");
        sender.sendMessage("§d/coronalis auth §8- §7introducir contraseña de una consola protegida.");
    }

    private static void showGuide(@Nonnull CommandSender sender, @Nonnull String topicRaw) {
        String topic = topicRaw.toLowerCase(Locale.ROOT);
        sender.sendMessage("§5§l[Coronalis] §dGuía: §e" + topic);
        switch (topic) {
            case "inicio" -> {
                sender.sendMessage("§71. Craftea una §6Consola de Control§7 y colócala como centro del observatorio.");
                sender.sendMessage("§72. Coloca §6Radiotelescopios§7 alrededor. Máximo operativo: §e50§7 por consola.");
                sender.sendMessage("§73. Une consola, telescopios y módulos con §8Cable Coaxial de Fase§7.");
                sender.sendMessage("§74. Alimenta la red con §bNúcleos SU§7 o energía eléctrica Slimefun.");
                sender.sendMessage("§75. Calibra, elige objetivo, alinea PID y correlaciona con §bCeldas de Datos§7.");
            }
            case "energia" -> {
                sender.sendMessage("§7La consola acepta dos fuentes: §bNúcleos de Energía SU§7 por cable y §eJoules§7 de EnergyNet Slimefun.");
                sender.sendMessage("§7Conversión interna: §e4 J = 1 SU§7. El buffer eléctrico de consola es de §e8192 J§7.");
                sender.sendMessage("§7Si falta energía: conecta más núcleos, bancos de datos o una red eléctrica externa.");
            }
            case "cableado" -> {
                sender.sendMessage("§7El §8Cable Coaxial de Fase§7 es un bloque físico. La red se valida por continuidad.");
                sender.sendMessage("§7Conecta: consola, radiotelescopios, núcleos SU, amplificadores, bancos y calibradores.");
                sender.sendMessage("§7Si un telescopio no cuenta, revisa que no haya cortes y usa §d/coronalis status§7.");
            }
            case "calibracion" -> {
                sender.sendMessage("§7Cada radiotelescopio tiene Azimut, Elevación, Frecuencia, Fase y Ganancia.");
                sender.sendMessage("§7La correlación requiere todos los telescopios conectados calibrados.");
                sender.sendMessage("§7Puedes calibrar manualmente desde GUI o usar §aCalibrador Automático VLBI§7.");
            }
            case "automatizacion" -> {
                sender.sendMessage("§7La consola expone slot de entrada solo para §bCelda de Datos Celestes§7 y salida para §eEco de Fase§7.");
                sender.sendMessage("§7Compatible con cargo, storage/import-export y autocrafters que respeten InventoryBlock.");
                sender.sendMessage("§7Activa §aModo Automático§7 en la GUI para alinear y correlacionar con celdas insertadas.");
                sender.sendMessage("§7Usa amplificadores para bajar coste SU y bancos para ampliar buffer.");
            }
            case "programas", "programs", "misiones" -> {
                sender.sendMessage("§7Los programas científicos son misiones persistidas en la consola cercana.");
                sender.sendMessage("§7Piden objetivo, telescopios, baselines, calibración, señal y a veces módulos.");
                sender.sendMessage("§7Usa §d/coronalis programs §7para listar y §d/coronalis program <id> §7para activar.");
                sender.sendMessage("§7Cuando todo esté listo: §d/coronalis program complete§7.");
            }
            case "acceso" -> {
                sender.sendMessage("§7La primera persona que abre una consola la reclama como owner.");
                sender.sendMessage("§7El owner puede invitar operadores o configurar contraseña desde la GUI.");
                sender.sendMessage("§7Los invitados usan §d/coronalis auth§7 tras intentar abrir una consola protegida.");
                sender.sendMessage("§7Solo un operador usa la terminal a la vez para evitar bugs de concurrencia.");
            }
            case "fallos" -> {
                sender.sendMessage("§cSin telescopios: §7revisa cableado y que los bloques sean Slimefun correctos.");
                sender.sendMessage("§cSin energía: §7conecta núcleos SU o EnergyNet; mira el buffer en la GUI/status.");
                sender.sendMessage("§cNo correlaciona: §7calibra todos, alinea PID, mete Celda de Datos y revisa evento solar.");
                sender.sendMessage("§cInterferencia: §7otra red cercana apunta al mismo objetivo; cambia objetivo o separa arrays.");
            }
            case "ciencia", "science", "telemetria" -> {
                sender.sendMessage("§7Coronalis simula parte de AstroControlSim dentro de Minecraft:");
                sender.sendMessage("§8- §7Fase geométrica por baseline y dirección Az/El.");
                sender.sendMessage("§8- §7Amplitud de señal según calibración y error de apuntado.");
                sender.sendMessage("§8- §7Temperatura/corriente de motores por carga y viento sintético.");
                sender.sendMessage("§8- §7Baselines UV: §en*(n-1)/2§7 hasta 1225 con 50 antenas.");
                sender.sendMessage("§7Usa §d/coronalis telemetry§7 junto a una consola activa.");
            }
            case "operaciones", "ops", "comandos" -> {
                sender.sendMessage("§7Comandos tipo AstroControlSim:");
                sender.sendMessage("§8- §d/coronalis move <az> <el> §7equivale a CMD_MOVE broadcast.");
                sender.sendMessage("§8- §d/coronalis reset §7equivale a CMD_RESET global.");
                sender.sendMessage("§8- §d/coronalis tune <preset> [ant] §7equivale a CMD_TUNE_PID.");
                sender.sendMessage("§8- §d/coronalis step [ant] [dAz] [dEl] §7prueba de respuesta PID.");
                sender.sendMessage("§8- §d/coronalis scan spiral §7ejecuta patrón de búsqueda 10Hz.");
                sender.sendMessage("§8- §d/coronalis maintenance repair §7AI z-score + reset de fallos.");
            }
            default -> {
                sender.sendMessage("§7Tema no reconocido. Usa: §einicio, energia, cableado, calibracion, automatizacion, programas, acceso, fallos, ciencia, operaciones§7.");
            }
        }
    }

    private static void handleMove(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§5[Coronalis] §cUso: /coronalis move <az> <el>");
            return;
        }
        Double az = parseDouble(args[1]);
        Double el = parseDouble(args[2]);
        if (az == null || el == null) {
            sender.sendMessage("§5[Coronalis] §cAz/El deben ser números.");
            return;
        }
        Coronalis.instance().getObservatoryOperations().move(sender, az, el);
    }

    private static void handleFault(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5[Coronalis] §cUso: /coronalis fault <antena> [motivo]");
            return;
        }
        Integer antenna = parseInt(args[1]);
        if (antenna == null || antenna <= 0) {
            sender.sendMessage("§5[Coronalis] §cAntena debe ser un índice positivo.");
            return;
        }
        String reason = args.length >= 3 ? args[2] : "manual_injection";
        Coronalis.instance().getObservatoryOperations().injectFault(sender, antenna, reason);
    }

    private static void handleTune(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5[Coronalis] §cUso:");
            sender.sendMessage("§d/coronalis tune <default|aggressive|damped|sluggish|precision|storm_safe> [antena]");
            sender.sendMessage("§d/coronalis tune custom <kp> <ki> <kd> [antena]");
            return;
        }

        if ("custom".equalsIgnoreCase(args[1])) {
            if (args.length < 5) {
                sender.sendMessage("§5[Coronalis] §cUso: /coronalis tune custom <kp> <ki> <kd> [antena]");
                return;
            }
            Double kp = parseDouble(args[2]);
            Double ki = parseDouble(args[3]);
            Double kd = parseDouble(args[4]);
            Integer antenna = args.length >= 6 ? parseInt(args[5]) : 0;
            if (kp == null || ki == null || kd == null || antenna == null) {
                sender.sendMessage("§5[Coronalis] §cParámetros PID inválidos.");
                return;
            }
            Coronalis.instance().getObservatoryOperations().tune(sender, kp, ki, kd, antenna, "custom");
            return;
        }

        Integer antenna = args.length >= 3 ? parseInt(args[2]) : 0;
        if (antenna == null) {
            sender.sendMessage("§5[Coronalis] §cAntena inválida.");
            return;
        }
        Coronalis.instance().getObservatoryOperations().tunePreset(sender, args[1], antenna);
    }

    private static void handleStep(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Integer antenna = args.length >= 2 ? parseInt(args[1]) : 0;
        Double dAz = args.length >= 3 ? parseDouble(args[2]) : 5.0;
        Double dEl = args.length >= 4 ? parseDouble(args[3]) : 5.0;
        if (antenna == null || dAz == null || dEl == null) {
            sender.sendMessage("§5[Coronalis] §cUso: /coronalis step [antena|0] [deltaAz] [deltaEl]");
            return;
        }
        Coronalis.instance().getObservatoryOperations().stepTest(sender, antenna, dAz, dEl);
    }

    private static void handleScan(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5[Coronalis] §cUso: /coronalis scan <spiral|wave|raster|stop|status>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "stop" -> Coronalis.instance().getObservatoryOperations().stopScan(sender);
            case "status" -> Coronalis.instance().getObservatoryOperations().scanStatus(sender);
            default -> Coronalis.instance().getObservatoryOperations().startScan(sender, action);
        }
    }

    private static void handleMaintenance(@Nonnull CommandSender sender, @Nonnull String[] args) {
        boolean repair = args.length >= 2 && "repair".equalsIgnoreCase(args[1]);
        Coronalis.instance().getObservatoryOperations().maintenance(sender, repair);
    }

    private static void handleExport(@Nonnull CommandSender sender, @Nonnull String[] args) {
        String format = args.length >= 2 ? args[1] : "json";
        Coronalis.instance().getObservatoryOperations().exportTelemetry(sender, format);
    }

    private static void handleProgram(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5[Coronalis] §cUso: /coronalis program <id|status|complete|clear>");
            sender.sendMessage("§7Lista: §d/coronalis programs");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "status" -> Coronalis.instance().getObservationProgramManager().status(sender);
            case "complete" -> Coronalis.instance().getObservationProgramManager().complete(sender);
            case "clear" -> Coronalis.instance().getObservationProgramManager().clear(sender);
            default -> {
                ObservationProgram program = ObservationProgram.byId(action);
                if (program == null) {
                    sender.sendMessage("§5[Coronalis] §cPrograma desconocido. Usa §d/coronalis programs§c.");
                    return;
                }
                Coronalis.instance().getObservationProgramManager().select(sender, program);
            }
        }
    }

    private static void showItems(@Nonnull CommandSender sender) {
        sender.sendMessage("§5§l[Coronalis] §dÍtems y bloques registrados");
        for (String id : CORONALIS_ITEMS) {
            SlimefunItem item = SlimefunItem.getById(id);
            String state = item == null ? "§cNO REGISTRADO" : "§aOK";
            sender.sendMessage("§8- §e" + id + " §8-> " + state);
        }
    }

    private static void showSimulatorCompare(@Nonnull CommandSender sender) {
        sender.sendMessage("§5§l[Coronalis] §dComparación con AstroControlSim");
        sender.sendMessage("§aPortado: §750 antenas máximas, estados IDLE/SLEWING/TRACKING/FAULT, PID básico.");
        sender.sendMessage("§aPortado: §7telemetría de error Az/El, fase, amplitud, temperatura y corriente.");
        sender.sendMessage("§aPortado: §7baselines UV, energía, cableado, módulos, calibración y auto-repair manual.");
        sender.sendMessage("§aPortado: §7patrones de scan spiral/wave/raster, tuning PID, step-test y comandos reset/tune/scan.");
        sender.sendMessage("§eParcial: §7dashboard web queda representado por GUI/comandos, no por web socket externo.");
        sender.sendMessage("§cFalta: §7stream binario TCP real, FITS/imagenes astronómicas, WebRelay y AI z-score completo.");
        sender.sendMessage("§cFalta: §7visualización de espectro/FFT real y almacenamiento histórico de series temporales.");
        sender.sendMessage("§aNuevo: §7programas científicos con requisitos de señal, baselines y módulos.");
    }

    private static void runSmoke(@Nonnull CommandSender sender) {
        boolean admin = !(sender instanceof Player player) || player.hasPermission("coronalis.admin");
        if (!admin) {
            sender.sendMessage("§5[Coronalis] §cNecesitas coronalis.admin para ejecutar smoke.");
            return;
        }

        List<String> failures = new ArrayList<>();
        require(Coronalis.instance().getDiscoveryService() != null, "DiscoveryService activo", failures);
        require(Coronalis.instance().getCosmicEventManager() != null, "CosmicEventManager activo", failures);
        require(Coronalis.instance().getNetworkRegistry() != null, "NetworkRegistry activo", failures);
        require(Coronalis.instance().getAccessManager() != null, "AccessManager activo", failures);
        require(Coronalis.instance().getSoundManager() != null, "SoundManager activo", failures);
        require(CoronalisNetwork.MAX_TELESCOPES == 50, "Límite de 50 telescopios", failures);
        require(Coronalis.instance().getCommand("coronalis") != null, "Comando /coronalis registrado", failures);
        require(Coronalis.instance().getConfig().contains("discovery-xp.first_full_calibration"), "Config XP calibración", failures);

        for (String id : CORONALIS_ITEMS) {
            require(SlimefunItem.getById(id) != null, "Item registrado: " + id, failures);
        }

        if (failures.isEmpty()) {
            sender.sendMessage("§5§l[Coronalis] §aSmoke interno OK. Registros base y config coherentes.");
        } else {
            sender.sendMessage("§5§l[Coronalis] §cSmoke interno falló:");
            failures.forEach(f -> sender.sendMessage("§8- §c" + f));
        }
    }

    private static void require(boolean condition, @Nonnull String label, @Nonnull List<String> failures) {
        if (!condition) {
            failures.add(label);
        }
    }

    private static void showNearestStatus(@Nonnull Player player) {
        Location consoleLoc = findNearestConsole(player.getLocation(), 80.0);
        if (consoleLoc == null) {
            player.sendMessage("§5[Coronalis] §cNo hay consolas activas a 80 bloques.");
            player.sendMessage("§7Tip: coloca una consola y ábrela una vez para activarla en memoria.");
            return;
        }
        CoronalisNetwork network = Coronalis.instance().getNetworkRegistry().getOrCreate(consoleLoc);
        player.sendMessage("");
        player.sendMessage("§5§l[Coronalis] §dDiagnóstico de Array");
        player.sendMessage("§7Consola: §e" + fmt(consoleLoc));
        player.sendMessage("§7Telescopios: §a" + network.getTelescopeCount() + "§7/§e" + CoronalisNetwork.MAX_TELESCOPES);
        player.sendMessage("§7Calibrados: §b" + network.getCalibratedCount()
            + " §8(" + Math.round(network.getAverageCalibrationFactor() * 100.0) + "% global)");
        player.sendMessage("§7SU: §b" + network.getSignalUnits() + "§7/§b" + network.getMaxSignalUnits());
        player.sendMessage("§7Costes Slew/Correlación: §e" + network.getEffectiveSlewCost()
            + "§7/§e" + network.getEffectiveCorrelateCost() + " SU");
        player.sendMessage("§7Núcleos/Amplificadores/Bancos/Calibradores: §a"
            + network.getEnergyNodeCount() + "§7/§d" + network.getSignalAmplifierCount()
            + "§7/§b" + network.getDataBankCount() + "§7/§2" + network.getAutoCalibratorCount());
        int rivals = Coronalis.instance().getNetworkRegistry().findRivalNetworks(network).size();
        player.sendMessage("§7Redes cercanas detectadas: §d" + rivals);
        var program = Coronalis.instance().getObservationProgramManager().getActiveProgram(consoleLoc);
        player.sendMessage("§7Programa activo: " + (program == null ? "§8Ninguno" : "§a" + program.getDisplayName()));
        player.sendMessage("§7Baselines UV: §e" + network.getBaselineCount()
            + " §8| Flujo: §b" + round1(network.getTotalSignalAmplitude()) + " Jy");
        player.sendMessage("§7Temp/Corriente media: §c" + round1(network.getAverageMotorTemp())
            + " °C §8/ §e" + round1(network.getAverageMotorCurrent()) + " A");
        player.sendMessage("§8Usa §d/coronalis guide fallos §8si algo no aparece como esperabas.");
        player.sendMessage("");
    }

    private static void showNearestTelemetry(@Nonnull Player player) {
        Location consoleLoc = findNearestConsole(player.getLocation(), 80.0);
        if (consoleLoc == null) {
            player.sendMessage("§5[Coronalis] §cNo hay consolas activas a 80 bloques.");
            return;
        }
        CoronalisNetwork network = Coronalis.instance().getNetworkRegistry().getOrCreate(consoleLoc);
        player.sendMessage("§5§l[Coronalis] §dTelemetría AstroControlSim");
        player.sendMessage("§7Baselines UV: §e" + network.getBaselineCount()
            + " §7| Flujo total: §b" + round1(network.getTotalSignalAmplitude()) + " Jy");
        player.sendMessage("§7Temp media: §c" + round1(network.getAverageMotorTemp())
            + " °C §7| Corriente media: §e" + round1(network.getAverageMotorCurrent()) + " A");
        int shown = 0;
        for (TelescopeState state : network.getTelescopes().values()) {
            if (shown++ >= 8) break;
            player.sendMessage("§8ANT " + shown
                + " §7state=§f" + state.getRuntimeState()
                + " §7err=§c" + round1(Math.sqrt(Math.pow(state.getAzError(), 2) + Math.pow(state.getElError(), 2))) + "°"
                + " §7amp=§b" + round1(state.getSignalAmplitude())
                + " §7phase=§d" + round1(state.getSignalPhase())
                + " §7temp=§c" + round1(state.getMotorTemp()) + "C"
                + " §7curr=§e" + round1(state.getMotorCurrent()) + "A");
        }
        if (network.getTelescopeCount() > 8) {
            player.sendMessage("§8Mostrando 8/" + network.getTelescopeCount() + " antenas para no llenar el chat.");
        }
    }

    @Nullable
    private static Location findNearestConsole(@Nonnull Location origin, double maxDistance) {
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

    @Nonnull
    private static String fmt(@Nonnull Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    @Nullable
    private static Double parseDouble(@Nonnull String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer parseInt(@Nonnull String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nonnull
    private static List<String> filter(@Nonnull String prefix, @Nonnull List<String> options) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
