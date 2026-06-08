@echo off
set APP_BASE_URL=http://localhost:8080
set CORS_ORIGINS=http://localhost:5173,http://localhost:4173,http://localhost:5174
java -jar backend\target\shortener-1.0.0.jar
