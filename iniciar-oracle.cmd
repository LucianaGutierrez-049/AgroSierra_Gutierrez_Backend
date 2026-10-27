@echo off
setlocal
cd /d "%~dp0"
set "PATH=%SystemRoot%\System32\WindowsPowerShell\v1.0;%SystemRoot%\System32;%PATH%"
docker compose up -d --wait oracle
if errorlevel 1 exit /b 1
call mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=oracle"
