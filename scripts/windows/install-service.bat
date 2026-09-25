@echo off
title ZenTrack - Instalador de Servicio de Windows (Pantalla de Bloqueo)
echo =================================================================
echo   ⚡ ZenTrack - Instalando Servicio del Sistema Windows
echo =================================================================
echo.
:: Requiere privilegios de Administrador
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Este script requiere permisos de Administrador para registrar servicios.
    echo Por favor haz clic derecho en el archivo y selecciona "Ejecutar como administrador".
    echo.
    pause
    exit /b 1
)

set "BIN_PATH=%LOCALAPPDATA%\Programs\ZenTrack\ZenTrack.exe"
if not exist "%BIN_PATH%" (
    set "BIN_PATH=%~dp0..\..\dist\windows\ZenTrack-Portable\ZenTrack.exe"
)
if not exist "%BIN_PATH%" (
    set "BIN_PATH=%~dp0ZenTrack.exe"
)

if not exist "%BIN_PATH%" (
    echo [ERROR] No se encontro el ejecutable ZenTrack.exe en las rutas estandar.
    pause
    exit /b 1
)

echo Registrando ZenTrackService con binPath: "%BIN_PATH%" --headless
sc stop ZenTrackService >nul 2>&1
sc delete ZenTrackService >nul 2>&1

sc create ZenTrackService binPath= "\"%BIN_PATH%\" --headless" start= auto DisplayName= "ZenTrack Ultra-Low Latency System Service"
sc description ZenTrackService "Servidor nativo de 500Hz para control de raton y teclado tactil desde el movil (soporte pantalla de bloqueo de Windows)."
sc start ZenTrackService

echo.
echo =================================================================
echo   ¡Listo! ZenTrackService instalado e iniciado con exito.
echo   Ahora puedes desbloquear Windows y controlar la pantalla
echo   de inicio de sesion (PIN/Contrasena) desde tu movil.
echo =================================================================
echo.
pause
