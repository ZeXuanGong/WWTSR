# Builds the release: a folder with WWTSR.exe and its own Java runtime, and a zip of it.
# Nothing needs to be installed on the computer that runs the result.
#
# usage:  powershell -ExecutionPolicy Bypass -File package.ps1 [-Maven <mvn.cmd>]
# needs:  JAVA_HOME pointing at JDK 25 or newer (jlink and jpackage come with it)
# result: target\dist\WWTSR\  and  target\dist\WWTSR-<version>-windows-x64.zip
param([string]$Maven = 'mvn')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Run([string]$what, [scriptblock]$command) {
    Write-Host "== $what"
    & $command
    if ($LASTEXITCODE -ne 0) { throw "$what failed (exit $LASTEXITCODE)" }
}

if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK (25 or newer).' }
$jlink = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
[xml]$pom = Get-Content pom.xml
$version = $pom.project.version
$jar = "target\wwtsr-$version.jar"

Run 'tests and jar' { & $Maven -B -q clean package }
Run 'dependencies' { & $Maven -B -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/mods }
# JavaFX's jars without a platform in their name are empty placeholders.
Get-ChildItem target\mods -Filter 'javafx-*.jar' | Where-Object { $_.Name -notmatch '-win\.jar$' } | Remove-Item

Run 'icon' { & $Maven -B -q javafx:run '-Dmain.class=com.wwtsr/com.wwtsr.app.IconFile' '-Djavafx.args=target/wwtsr.ico' }
if (-not (Test-Path target\wwtsr.ico)) { throw 'The icon was not written.' }

Run 'runtime' {
    & $jlink --module-path "target\mods;$jar" --add-modules com.wwtsr `
        --strip-debug --no-header-files --no-man-pages --compress zip-6 --output target\runtime
}
# No -Dwwtsr.debug here: the release keeps no watchdog log (it still restarts dropped input hooks).
Run 'app' {
    & $jpackage --type app-image --name WWTSR --app-version $version --vendor WWTSR `
        --description 'WWTSR desktop pet' --icon target\wwtsr.ico `
        --runtime-image target\runtime --module com.wwtsr/com.wwtsr.app.WwtsrApp `
        --java-options '--enable-native-access=javafx.graphics,com.sun.jna' `
        --dest target\dist
}

$zip = "target\dist\WWTSR-$version-windows-x64.zip"
Compress-Archive -Path target\dist\WWTSR -DestinationPath $zip -Force
$size = '{0:N1}' -f ((Get-Item $zip).Length / 1MB)
Write-Host "Done: $zip ($size MB)"
