@rem Roda o Gradle com o cache do projeto fora do OneDrive (evita "Acesso negado").
@call "%~dp0gradlew.bat" --project-cache-dir "%LOCALAPPDATA%\Lectio-build\.gradle" %*
