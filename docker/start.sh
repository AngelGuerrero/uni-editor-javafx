#!/bin/bash
set -euo pipefail
pids=()
cleanup() { kill "${pids[@]}" 2>/dev/null || true; wait || true; }
trap cleanup EXIT
trap 'exit 0' TERM INT
Xvfb :99 -screen 0 1440x900x24 -nolisten tcp &
pids+=("$!")
for attempt in {1..50}; do
    [ -S /tmp/.X11-unix/X99 ] && break
    sleep 0.1
done
openbox &
pids+=("$!")
x11vnc -display :99 -rfbport 5900 -localhost -forever -shared -nopw &
pids+=("$!")
websockify --web=/usr/share/novnc 6080 localhost:5900 &
pids+=("$!")
java --enable-native-access=javafx.graphics --module-path /app/lib --add-modules javafx.controls -Dprism.order=sw -cp /app/editor.jar io.github.unieditor.Launcher &
pids+=("$!")
wait -n "${pids[@]}"
