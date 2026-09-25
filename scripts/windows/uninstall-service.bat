@echo off
title ZenTrack - Desinstalador de Servicio de Windows
echo =================================================================
echo   ⚡ ZenTrack - Eliminando Servicio del Sistema Windows
echo =================================================================
echo.
:: Requiere privilegios de Administrador
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Este script requiere permisos de Administrador para eliminar servicios.
    echo Por favor haz clic derecho en el archivo y selecciona "Ejecutar como administrador".
    echo.
    pause
    exit /b 1
)

echo Deteniendo y eliminando ZenTrackService...
sc stop ZenTrackService
sc delete ZenTrackService

echo.
echo =================================================================
echo   ZenTrackService ha sido desinstalado con exito.
echo =================================================================
echo.
pause
