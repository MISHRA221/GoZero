@echo off
REM Starts all 4 services locally with embedded H2 databases (no PostgreSQL / Docker needed).
REM Prerequisite: run "mvn clean install" once from the repository root.
setlocal
cd /d "%~dp0\.."

for %%S in (ingestion-service sentiment-service reconciliation-service dashboard-service) do (
    if not exist "%%S\target\%%S.jar" (
        echo [ERROR] %%S\target\%%S.jar not found. Run "mvn clean install" first.
        exit /b 1
    )
)

echo Starting ingestion-service on 8081 (initial scrape / seed load takes a few seconds)...
start "ingestion-service :8081" java -jar ingestion-service\target\ingestion-service.jar
timeout /t 25 /nobreak >nul

echo Starting sentiment-service on 8082...
start "sentiment-service :8082" java -jar sentiment-service\target\sentiment-service.jar

echo Starting reconciliation-service on 8083...
start "reconciliation-service :8083" java -jar reconciliation-service\target\reconciliation-service.jar

echo Starting dashboard-service on 8080...
start "dashboard-service :8080" java -jar dashboard-service\target\dashboard-service.jar

echo.
echo All services launching in separate windows. Give them ~30 seconds, then open:
echo   Dashboard : http://localhost:8080
echo   Swagger   : http://localhost:8081/swagger-ui.html  (and 8082, 8083, 8080)
echo Close the four service windows to stop everything.
endlocal
