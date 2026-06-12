package com.github.jackstar.coronalis.implementation;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BookMeta;

import javax.annotation.Nonnull;

public final class Items {

    private Items() {}

    /* Componentes de Crafteo */
    public static final SlimefunItemStack CORONALIS_ANTENNA_DISH = new SlimefunItemStack(
            "CORONALIS_ANTENNA_DISH",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTI2ZDhjMDJmZjM2Y2QyM2EyNzBhYjNjMzk5YTQxYzE1MzhhMTcyNzM0MzZiMzg4ZjhiMThlYmZlZDRjNzU1In19fQ==",
            "&dPlato de Antena Parabólica",
            "&7Componente de ingeniería de radio",
            "&7utilizado en la construcción de radiotelescopios.",
            "",
            "&5&oCoronalis Array Labs"
    );

    public static final SlimefunItemStack CORONALIS_RECEIVER = new SlimefunItemStack(
            "CORONALIS_RECEIVER",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJkMjU2ODBlY2EyMzk3Zjg4M2FlNjFmOTVkMzcyY2FiZjFmMTBhZDY1YjUyYjNjYTQzODdhMDdiOTlhOCJ9fX0=",
            "&dReceptor de Radiofrecuencia (1mm)",
            "&7Receptor criogénico de alta fidelidad",
            "&7sintonizado para ondas milimétricas de 1mm.",
            "&7Captura variaciones de fase y amplitud.",
            "",
            "&5&oCoronalis Array Labs"
    );

    public static final SlimefunItemStack CORONALIS_PID_CONTROLLER = new SlimefunItemStack(
            "CORONALIS_PID_CONTROLLER",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzhkMWEyODFhNDVkOWZlOTk3MTVkNzhkYTg5MDJhMmRkMGUyYmE1YzVkMDUwNzAxNjc5MDdjZTcxMWRjMmRlIn19fQ==",
            "&dControlador PID del Vacío",
            "&7Controlador Proporcional-Integral-Derivativo.",
            "&7Ajusta automáticamente los servomotores",
            "&7para contrarrestar disturbios de viento en las antenas.",
            "",
            "&5&oCoronalis Array Labs"
    );

    public static final SlimefunItemStack CORONALIS_DATA_CELL = new SlimefunItemStack(
            "CORONALIS_DATA_CELL",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjU2Y2M2Yjg4MzMwZjU2M2RhMjJkOGU0YWM4ZjEwMWVhMzhiNjg5YTc3NTc2NTFkNjVhMWIyOTgxMTU0OWUifX19",
            "&bCelda de Datos Celestes",
            "&7Celda de almacenamiento de telemetría y datos.",
            "&7Insértala en la consola de control para",
            "&7registrar nuevos descubrimientos espaciales.",
            "&7Automatizable: entra por cargo/importadores",
            "&7al slot de entrada de la consola.",
            "",
            "&eGuía: &f/coronalis guide automatizacion",
            "",
            "&9&oCoronalis Array Command"
    );

    public static final SlimefunItemStack CORONALIS_COAXIAL_CABLE = new SlimefunItemStack(
            "CORONALIS_COAXIAL_CABLE",
            Material.CHAIN,
            "&8Cable Coaxial de Fase",
            "&7Bloque conductor para enlazar consolas,",
            "&7núcleos SU y radiotelescopios Coronalis.",
            "&7La red valida continuidad bloque a bloque.",
            "&7Conecta también amplificadores, bancos",
            "&7y calibradores automáticos.",
            "",
            "&eGuía: &f/coronalis guide cableado",
            "",
            "&5&oCoronalis Array Infrastructure"
    );

    public static final SlimefunItemStack CORONALIS_SIGNAL_CORE = new SlimefunItemStack(
            "CORONALIS_SIGNAL_CORE",
            Material.LODESTONE,
            "&bNúcleo de Energía SU",
            "&7Genera Signal Units para alimentar",
            "&7movimiento PID, calibración y correlación.",
            "&7Debe conectarse a la consola con cable coaxial.",
            "&7También puedes alimentar la consola con",
            "&7red eléctrica Slimefun compatible.",
            "",
            "&eGuía: &f/coronalis guide energia",
            "",
            "&9&oCoronalis Array Power"
    );

    public static final SlimefunItemStack CORONALIS_SIGNAL_AMPLIFIER = new SlimefunItemStack(
            "CORONALIS_SIGNAL_AMPLIFIER",
            Material.SCULK_SENSOR,
            "&dAmplificador Criogénico",
            "&7Módulo cableado que aumenta la sensibilidad",
            "&7del array y reduce el coste de correlación.",
            "&7Útil para redes grandes y automatizadas.",
            "&7Conexión: cable coaxial hacia la consola.",
            "",
            "&eGuía: &f/coronalis guide automatizacion",
            "",
            "&5&oCoronalis Array Infrastructure"
    );

    public static final SlimefunItemStack CORONALIS_DATA_BANK = new SlimefunItemStack(
            "CORONALIS_DATA_BANK",
            Material.CHISELED_BOOKSHELF,
            "&bBanco de Datos Astronómicos",
            "&7Módulo cableado que amplía el buffer",
            "&7de SU y estabiliza operaciones automáticas.",
            "&7Conexión: cable coaxial hacia la consola.",
            "",
            "&eGuía: &f/coronalis guide energia",
            "",
            "&9&oCoronalis Array Storage"
    );

    public static final SlimefunItemStack CORONALIS_AUTO_CALIBRATOR = new SlimefunItemStack(
            "CORONALIS_AUTO_CALIBRATOR",
            Material.CALIBRATED_SCULK_SENSOR,
            "&aCalibrador Automático VLBI",
            "&7Módulo cableado que calibra lentamente",
            "&7los radiotelescopios usando SU del array.",
            "&7Conexión: cable coaxial hacia la consola.",
            "&7Consume SU por cada paso automático.",
            "",
            "&eGuía: &f/coronalis guide calibracion",
            "",
            "&2&oCoronalis Array Automation"
    );

    /* Bloques Funcionales */
    public static final SlimefunItemStack CORONALIS_RADIO_TELESCOPE = new SlimefunItemStack(
            "CORONALIS_RADIO_TELESCOPE",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWM5MzhjYTFkYjBiYzhhMTc3M2RlOTRkNzNjYzFmODU5OTM0M2M1YjI3NDQ3NDliY2VkM2QxMWNjMTlhIn19fQ==",
            "&6Escucha del Vacío — Antena",
            "&7Unidad de antena parabólica del observatorio.",
            "&7Debe conectarse por cable coaxial",
            "&7a una consola de control.",
            "&7Máximo operativo: 50 por red.",
            "&7Debe calibrarse antes de correlacionar.",
            "",
            "&eGuía: &f/coronalis guide inicio",
            "",
            "&5&oCoronalis Array Labs"
    );

    public static final SlimefunItemStack CORONALIS_CONTROL_CONSOLE = new SlimefunItemStack(
            "CORONALIS_CONTROL_CONSOLE",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTE0N2M3OWU3MTRlYmNmYTliZjVlOTFkMzA1Y2VjYmI3ODhlNGM0MDIyM2ZkM2E0Y2MzNWM4YWQyMTZhMDFlIn19fQ==",
            "&6Consola de Control del Observatorio",
            "&7Permite seleccionar objetivos cósmicos,",
            "&7alinear las antenas con el bucle PID",
            "&7y correlacionar señales electromagnéticas.",
            "&7Acepta energía Slimefun y celdas por cargo.",
            "&7Usa /coronalis status para diagnosticarla.",
            "",
            "&eGuía: &f/coronalis help",
            "",
            "&9&oCoronalis Array Command"
    );

    /* Infraestructura de estaciones */
    public static final SlimefunItemStack CORONALIS_FOUNDATION_ANCHOR = new SlimefunItemStack(
            "CORONALIS_FOUNDATION_ANCHOR",
            Material.LODESTONE,
            "&6Anclaje de Cimentación Coronalis",
            "&7Marca la base y el propietario de una estación.",
            "&7Colócalo en suelo firme o sobre una montaña.",
            "",
            "&eGuía: &f/coronalis guide torres"
    );

    public static final SlimefunItemStack CORONALIS_TOWER_SEGMENT = new SlimefunItemStack(
            "CORONALIS_TOWER_SEGMENT",
            Material.CUT_COPPER,
            "&6Segmento Estructural de Torre",
            "&7Anclaje intermedio para una torre libre.",
            "&7Debe haber uno al menos cada 16 bloques",
            "&7entre la cimentación y la plataforma.",
            "",
            "&eGuía: &f/coronalis guide torres"
    );

    public static final SlimefunItemStack CORONALIS_WATCH_PLATFORM = new SlimefunItemStack(
            "CORONALIS_WATCH_PLATFORM",
            Material.SMITHING_TABLE,
            "&ePlataforma de Vigilancia Firewatch",
            "&7Corona habitable de la torre.",
            "&7Debe quedar al menos 12 bloques sobre la base.",
            "&7Puede alcanzarse con elevadores Slimefun.",
            "",
            "&eGuía: &f/coronalis guide torres"
    );

    public static final SlimefunItemStack CORONALIS_RADIO_MAST = new SlimefunItemStack(
            "CORONALIS_RADIO_MAST",
            Material.LIGHTNING_ROD,
            "&dMástil de Radio Coronalis",
            "&7Elemento obligatorio de toda estación operativa.",
            "&7Instálalo sobre o junto a la plataforma.",
            "",
            "&eGuía: &f/coronalis guide radio"
    );

    public static final SlimefunItemStack CORONALIS_PARABOLIC_DISH = new SlimefunItemStack(
            "CORONALIS_PARABOLIC_DISH",
            Material.DAYLIGHT_DETECTOR,
            "&bPlato Parabólico de Largo Alcance",
            "&7Amplía el enlace directo de 192 a 512 bloques.",
            "&7Requiere línea de visión entre estaciones.",
            "",
            "&eGuía: &f/coronalis guide radio"
    );

    public static final SlimefunItemStack CORONALIS_SIGNAL_REPEATER = new SlimefunItemStack(
            "CORONALIS_SIGNAL_REPEATER",
            Material.SCULK_SENSOR,
            "&5Repetidor de Señal Coronalis",
            "&7Aumenta el alcance de estaciones cercanas.",
            "&7No carga chunks ni transmite a mundos distintos.",
            "",
            "&eGuía: &f/coronalis guide radio"
    );

    public static final SlimefunItemStack CORONALIS_FIELD_GUIDE = new SlimefunItemStack(
            "CORONALIS_FIELD_GUIDE",
            Material.WRITTEN_BOOK,
            "&dManual de Campo Coronalis",
            "&7Construcción, radio, seguridad y operación.",
            "&7Clic derecho para abrir."
    );

    /* Ítems Especiales */
    public static final SlimefunItemStack CORONALIS_RECORD_DISCOVERED = new SlimefunItemStack(
            "CORONALIS_RECORD_DISCOVERED",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjVkMWMyMjk1OGE1Yzg3M2JiYWFlZTQ0Nzg5ODcxOTM1M2NmYmQzMDkyODk5In19fQ==",
            "&e&lEco de Fase Coronalis",
            "&7Contiene datos de telemetría y firmas de fase",
            "&7de un cuerpo celeste recién descubierto.",
            "",
            "&eClic derecho para analizar y recibir XP."
    );

    static {
        // Darle un brillo encantador al registro y celdas
        addGlow(CORONALIS_RECORD_DISCOVERED);
        addGlow(CORONALIS_DATA_CELL);
        configureGuide();
    }

    private static void addGlow(@Nonnull SlimefunItemStack item) {
        item.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private static void configureGuide() {
        ItemMeta rawMeta = CORONALIS_FIELD_GUIDE.getItemMeta();
        if (!(rawMeta instanceof BookMeta meta)) {
            return;
        }
        meta.setTitle("Manual Coronalis");
        meta.setAuthor("DrakesCraft Array Labs");
        meta.addPage(
            "§5§lCORONALIS\n\n§0Manual de estaciones de escucha, radioastronomía y enlaces.\n\n" +
            "§8Comando rápido:\n§5/coronalis station"
        );
        meta.addPage(
            "§6§l1. CIMENTACIÓN\n\n§0Coloca el Anclaje. Construye libremente hacia arriba con bloques seguros. " +
            "Añade un Segmento Coronalis cada 16 bloques como máximo."
        );
        meta.addPage(
            "§6§l2. PLATAFORMA\n\n§0La Plataforma Firewatch debe quedar 12 bloques o más sobre la base. " +
            "Puedes usar escaleras, agua o elevadores Slimefun."
        );
        meta.addPage(
            "§5§l3. RADIO\n\n§0Un Mástil junto a la plataforma vuelve operativa la estación. " +
            "El Plato Parabólico sube el alcance de 192 a 512 bloques."
        );
        meta.addPage(
            "§5§l4. ENLACES\n\n§0Dos estaciones necesitan línea de visión. Un Repetidor cercano añade alcance. " +
            "Coronalis nunca fuerza la carga de chunks."
        );
        meta.addPage(
            "§4§l5. SEGURIDAD\n\n§0Cada componente queda ligado a quien lo coloca. " +
            "Sólo el dueño o un administrador puede retirarlo. Todos los cambios se auditan."
        );
        meta.addPage(
            "§1§l6. APAGADO\n\n§0SQLite guarda estaciones y enlaces. Slimefun guarda los inventarios de sus máquinas. " +
            "No se generan drops adicionales al restaurar."
        );
        meta.addPage(
            "§2§l7. DIAGNÓSTICO\n\n§0Usa:\n§5/coronalis station\n§5/coronalis status\n§5/coronalis smoke\n\n" +
            "Consulta /coronalis guide torres o radio."
        );
        CORONALIS_FIELD_GUIDE.setItemMeta(meta);
    }
}
