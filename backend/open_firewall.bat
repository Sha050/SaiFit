@echo off
echo Requesting administrator privileges to open port 8000 for the SaiFit backend...
powershell -Command "Start-Process powershell -ArgumentList '-Command \"New-NetFirewallRule -DisplayName ''SaiFit Backend'' -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow\"' -Verb RunAs"
echo Done! Please try using the app again.
pause
