@echo off
REM Run from cmd after: docker compose up --build -d
setlocal
cd /d "%~dp0\.."
set FAILED=0

echo Checking container status...
docker compose ps
if errorlevel 1 set FAILED=1

echo.
echo Checking dashboard health...
curl --fail --silent http://localhost:8080/actuator/health
if errorlevel 1 set FAILED=1

echo.
echo Checking ingestion seed/live status...
curl --fail --silent http://localhost:8081/api/ingest/status
if errorlevel 1 set FAILED=1

echo.
echo Checking product API...
curl --fail --silent http://localhost:8081/api/products
if errorlevel 1 set FAILED=1

echo.
echo Checking sentiment API...
curl --fail --silent http://localhost:8082/api/sentiment/by-flavor
if errorlevel 1 set FAILED=1

echo.
echo Checking simulated reconciliation API...
curl --fail --silent http://localhost:8083/api/reconciliation/alerts
if errorlevel 1 set FAILED=1

echo.
if "%FAILED%"=="0" (
    echo Docker smoke test PASSED. Open http://localhost:8080
    exit /b 0
)
echo Docker smoke test FAILED. Run "docker compose logs --tail=150" for details.
exit /b 1
