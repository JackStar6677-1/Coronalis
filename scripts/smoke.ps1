param(
    [switch] $SkipBuild
)

$ErrorActionPreference = "Stop"

function Pass($message) {
    Write-Host "[PASS] $message" -ForegroundColor Green
}

function Fail($message) {
    Write-Host "[FAIL] $message" -ForegroundColor Red
    $script:Failed = $true
}

function Assert-File($path, $message) {
    if (Test-Path $path) { Pass $message } else { Fail $message }
}

function Assert-Text($path, $pattern, $message) {
    if (Select-String -Path $path -Pattern $pattern -Quiet) { Pass $message } else { Fail $message }
}

$script:Failed = $false
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $SkipBuild) {
    Write-Host "[INFO] Compilando Coronalis..." -ForegroundColor Cyan
    mvn -q clean package -DskipTests
}

Assert-File "target/Coronalis.jar" "Jar generado"
Assert-Text "target/classes/plugin.yml" "main: com.github.jackstar.coronalis.Coronalis" "plugin.yml empaquetado"
Assert-Text "target/classes/plugin.yml" "coronalis:" "Comando /coronalis declarado"
Assert-Text "target/classes/plugin.yml" "programs\|program" "Comandos de programas publicados"
Assert-Text "target/classes/plugin.yml" "coronalis.admin" "Permiso coronalis.admin declarado"
Assert-Text "target/classes/config.yml" "first_full_calibration" "Config XP de calibracion presente"
Assert-Text "target/classes/config.yml" "program_full_array_first_light" "Config XP de programas cientificos presente"

$classes = @(
    "target/classes/com/github/jackstar/coronalis/Coronalis.class",
    "target/classes/com/github/jackstar/coronalis/commands/CoronalisCommand.class",
    "target/classes/com/github/jackstar/coronalis/implementation/items/ControlConsole.class",
    "target/classes/com/github/jackstar/coronalis/implementation/items/ArrayNetworkBlock.class",
    "target/classes/com/github/jackstar/coronalis/managers/NetworkRegistry.class",
    "target/classes/com/github/jackstar/coronalis/managers/AccessManager.class",
    "target/classes/com/github/jackstar/coronalis/managers/ObservatoryOperations.class",
    "target/classes/com/github/jackstar/coronalis/managers/ObservationProgramManager.class",
    "target/classes/com/github/jackstar/coronalis/implementation/data/PidProfile.class",
    "target/classes/com/github/jackstar/coronalis/implementation/data/ObservationProgram.class"
)

foreach ($class in $classes) {
    Assert-File $class "Clase empaquetada: $class"
}

Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/items/ControlConsole.java" "implements InventoryBlock, EnergyNetComponent" "Consola compatible con cargo y EnergyNet"
Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/items/ControlConsole.java" "EnergyNetComponentType.CONSUMER" "Consola declarada como consumidor energetico"
Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/items/ControlConsole.java" "CORONALIS_DATA_CELL" "Input de automatizacion acepta Celda de Datos"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "showGuide" "Comando de guia presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "runSmoke" "Smoke interno presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "showSimulatorCompare" "Comando compare presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "showNearestTelemetry" "Comando telemetry presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "handleMove" "Comando move presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "handleTune" "Comando tune presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "handleScan" "Comando scan presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "handleProgram" "Comando program presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/commands/CoronalisCommand.java" "onTabComplete" "Tab-complete presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/data/TelescopeState.java" "updateTelemetry" "Telemetria cientifica por antena presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/data/TelescopeState.java" "tunePid" "Tuning PID por antena presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/data/CoronalisNetwork.java" "getBaselineCount" "Conteo de baselines UV presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/managers/ObservatoryOperations.java" "exportTelemetry" "Export de telemetria presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/managers/ObservatoryOperations.java" "maintenance" "AI maintenance presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/managers/ObservatoryOperations.java" "ScanPattern" "Patrones de scan presentes"
Assert-Text "src/main/java/com/github/jackstar/coronalis/managers/ObservationProgramManager.java" "complete" "Programas cientificos completables presentes"
Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/data/ObservationProgram.java" "FULL_ARRAY_FIRST_LIGHT" "Programa de 50 telescopios presente"
Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/setup/ItemSetup.java" "SlimefunItems\." "Recetas usan componentes Slimefun"

$ids = @(
    "CORONALIS_COAXIAL_CABLE",
    "CORONALIS_SIGNAL_CORE",
    "CORONALIS_SIGNAL_AMPLIFIER",
    "CORONALIS_DATA_BANK",
    "CORONALIS_AUTO_CALIBRATOR",
    "CORONALIS_RADIO_TELESCOPE",
    "CORONALIS_CONTROL_CONSOLE"
)

foreach ($id in $ids) {
    Assert-Text "src/main/java/com/github/jackstar/coronalis/implementation/setup/ItemSetup.java" $id "Registro/receta presente: $id"
}

if ($script:Failed) {
    Write-Host "[RESULT] Smoke falló." -ForegroundColor Red
    exit 1
}

Write-Host "[RESULT] Smoke OK. Coronalis esta coherente a nivel de build, recursos y registros estaticos." -ForegroundColor Green
