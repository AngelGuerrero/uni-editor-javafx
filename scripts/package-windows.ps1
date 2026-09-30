param([string]$Destination = "dist")
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
foreach ($tool in @('java', 'mvn', 'jpackage')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "Falta $tool. Instala JDK 25 y Maven 3.9 o usa GitHub Actions." }
}
$output = [IO.Path]::GetFullPath((Join-Path (Get-Location) $Destination))
if (Test-Path (Join-Path $output 'UniEditor')) { throw "Ya existe $output\UniEditor. Elige otro destino." }
& mvn -B clean verify
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }
$inputDir = Join-Path (Get-Location) 'target/package-input'
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
Copy-Item target/uni-editor-2.0.0.jar $inputDir
Copy-Item target/lib/*.jar $inputDir
& jpackage --type app-image --name UniEditor --app-version 2.0.0 --vendor 'Uni Editor' --input $inputDir --main-jar uni-editor-2.0.0.jar --main-class io.github.unieditor.Launcher --dest $output --add-modules java.desktop,java.logging,java.xml,jdk.unsupported --java-options '--enable-native-access=ALL-UNNAMED'
if ($LASTEXITCODE -ne 0) { throw 'Falló jpackage.' }
Compress-Archive -Path (Join-Path $output 'UniEditor') -DestinationPath (Join-Path $output 'UniEditor-windows-x64.zip') -Force
Write-Host "Portable generado: $output\UniEditor-windows-x64.zip"
