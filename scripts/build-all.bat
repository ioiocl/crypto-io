@echo off
echo ==========================================
echo Building Finbot - All Services
echo ==========================================

echo Building shared domain...
call mvn -f shared-domain/pom.xml clean install -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%

echo Building ingestion service...
call mvn -f ingestion-service/pom.xml clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%

echo Building analytics service...
call mvn -f analytics-service/pom.xml clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%

echo Building websocket API...
call mvn -f websocket-api/pom.xml clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%

echo Building dashboard...
cd dashboard
call npm install
if %errorlevel% neq 0 exit /b %errorlevel%
call npm run build
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo ==========================================
echo Build completed successfully!
echo ==========================================
