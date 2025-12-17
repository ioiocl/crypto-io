@echo off
setlocal

if "%1"=="" (
    echo Usage: docker-build-push.bat ^<registry-url^> [tag]
    echo Example: docker-build-push.bat registry.us-west-1.aliyuncs.com/finbot latest
    exit /b 1
)

set REGISTRY=%1
set TAG=%2
if "%TAG%"=="" set TAG=latest

echo ==========================================
echo Building and Pushing Docker Images
echo Registry: %REGISTRY%
echo Tag: %TAG%
echo ==========================================

echo Building ingestion-service...
docker build -t %REGISTRY%/ingestion-service:%TAG% -f ingestion-service/Dockerfile .
if %errorlevel% neq 0 exit /b %errorlevel%

echo Pushing ingestion-service...
docker push %REGISTRY%/ingestion-service:%TAG%
if %errorlevel% neq 0 exit /b %errorlevel%

echo Building analytics-service...
docker build -t %REGISTRY%/analytics-service:%TAG% -f analytics-service/Dockerfile .
if %errorlevel% neq 0 exit /b %errorlevel%

echo Pushing analytics-service...
docker push %REGISTRY%/analytics-service:%TAG%
if %errorlevel% neq 0 exit /b %errorlevel%

echo Building websocket-api...
docker build -t %REGISTRY%/websocket-api:%TAG% -f websocket-api/Dockerfile .
if %errorlevel% neq 0 exit /b %errorlevel%

echo Pushing websocket-api...
docker push %REGISTRY%/websocket-api:%TAG%
if %errorlevel% neq 0 exit /b %errorlevel%

echo Building dashboard...
docker build -t %REGISTRY%/dashboard:%TAG% -f dashboard/Dockerfile ./dashboard
if %errorlevel% neq 0 exit /b %errorlevel%

echo Pushing dashboard...
docker push %REGISTRY%/dashboard:%TAG%
if %errorlevel% neq 0 exit /b %errorlevel%

echo ==========================================
echo All images built and pushed successfully!
echo ==========================================
