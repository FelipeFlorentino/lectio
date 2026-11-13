# Publica uma versão nova do app no GitHub. Os celulares veem o aviso "Nova versão" ao abrir o app.
#
# Uso:  .\publicar.ps1 -Versao 1.2 -Notas "O que mudou nesta versão"
#
# O que faz: sobe o versionCode, troca o versionName, compila o APK assinado,
# grava o código no GitHub (commit + push) e cria a release com o APK anexado.

param(
    [Parameter(Mandatory = $true)][string]$Versao,
    [Parameter(Mandatory = $true)][string]$Notas,
    # Linha extra opcional no fim do commit (ex.: "Co-Authored-By: ...").
    [string]$Rodape = ''
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$gh = Join-Path $env:LOCALAPPDATA 'Programs\gh\bin\gh.exe'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot' }

if (-not (Test-Path 'assinatura\assinatura.properties')) {
    throw 'Chave de assinatura não encontrada em assinatura\. Sem ela o celular recusa a atualização.'
}

# 1. Versão
$gradle = 'app\build.gradle.kts'
$texto = Get-Content $gradle -Raw -Encoding UTF8
$codigo = [int]([regex]::Match($texto, 'versionCode = (\d+)').Groups[1].Value) + 1
$texto = $texto -replace 'versionCode = \d+', "versionCode = $codigo"
$texto = $texto -replace 'versionName = "[^"]*"', "versionName = `"$Versao`""
[IO.File]::WriteAllText((Resolve-Path $gradle), $texto, (New-Object Text.UTF8Encoding $false))
Write-Host "Versão $Versao (código $codigo)"

# 2. Compilação
& .\gradlew.bat assembleRelease --console=plain -q
if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou.' }
New-Item -ItemType Directory -Force dist | Out-Null
$apk = "dist\Lectio-$codigo.apk"
Copy-Item 'app\build\outputs\apk\release\app-release.apk' $apk -Force
Copy-Item $apk 'Lectio.apk' -Force

# 3. Código no GitHub
git add -A
$mensagem = @('-m', "Versão $Versao", '-m', $Notas)
if ($Rodape) { $mensagem += @('-m', $Rodape) }
git commit -q @mensagem
git push -q
if ($LASTEXITCODE -ne 0) { throw 'O push para o GitHub falhou.' }

# 4. Release com o APK (o nome do anexo traz o versionCode, que o app compara com o instalado)
& $gh release create "v$Versao" $apk --title $Versao --notes $Notas
if ($LASTEXITCODE -ne 0) { throw 'A criação da release falhou.' }
Write-Host "Publicado. Os celulares vão mostrar 'Nova versão $Versao disponível' ao abrir o app."
