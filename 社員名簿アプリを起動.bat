@echo off
title 社員名簿アプリ
cd /d "C:\Users\PC_User\Desktop\dejiina_agent\crowdsourcing\03_java-web-app\employee-app"

echo.
echo  ============================================
echo    社員名簿アプリを起動します
echo  ============================================
echo.
echo  起動に 30秒ほどかかります。
echo  準備ができたら、ブラウザが自動で開きます。
echo.
echo  ログインID   admin
echo  パスワード   meibo-admin-2026!
echo.
echo  終わるときは、この黒い画面を閉じてください。
echo  （閉じるとアプリも止まります）
echo.
echo  ============================================
echo.

start "" /min powershell -NoProfile -ExecutionPolicy Bypass -Command "$n=0; while($n -lt 120){ try{ $r=Invoke-WebRequest -Uri 'http://localhost:8080/login' -UseBasicParsing -TimeoutSec 2; if($r.StatusCode -eq 200){ break } }catch{}; Start-Sleep -Seconds 1; $n++ }; if(Test-Path 'C:\Program Files\Google\Chrome\Application\chrome.exe'){ Start-Process 'C:\Program Files\Google\Chrome\Application\chrome.exe' 'http://localhost:8080' } else { Start-Process 'http://localhost:8080' }"

call mvnw.cmd spring-boot:run

echo.
echo  アプリが止まりました。この画面を閉じてください。
pause
