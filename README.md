# Uni Editor

Editor de texto JavaFX con Java 25 y Maven. Incluye pestañas, apertura múltiple, guardar como, renombrado, eliminación con confirmación, ajuste de líneas, zoom y protección de cambios sin guardar.

## Docker

Con Docker Desktop y contenedores Linux:

```powershell
docker compose up --build -d
```

Abre http://localhost:6080/vnc.html?autoconnect=true&resize=remote . El editor JavaFX se ejecuta en un escritorio virtual con noVNC. El selector muestra archivos del contenedor. El puerto solamente se publica en localhost y no tiene autenticación; para acceso remoto utiliza un túnel o proxy autenticado.

Los documentos en `/workspace` persisten en un volumen. Para importar y recuperar archivos:

```powershell
docker compose cp ./mi-archivo.txt editor:/workspace/mi-archivo.txt
docker compose cp editor:/workspace/mi-archivo.txt ./mi-archivo-editado.txt
docker compose down
```

`down` conserva los documentos; `down -v` elimina el volumen. Cerrar la aplicación detiene el contenedor; vuelve a iniciarlo con `docker compose up -d`.

## Windows portable

GitHub Actions genera `UniEditor-windows-x64.zip` en cada push o ejecución manual. Descarga el artefacto, extrae todo el ZIP y ejecuta `UniEditor/UniEditor.exe`. Incluye Java: no requiere instalación. Conserva las carpetas `app` y `runtime` junto al ejecutable.

Para compilar localmente necesitas Windows x64, JDK 25 y Maven 3.9 en PATH:

```powershell
./scripts/package-windows.ps1
```

El ZIP queda en `dist/`. Si ya existe una compilación, usa `-Destination dist-otra`. El empaquetado usa `jpackage --type app-image`, sin instalador ni WiX. El ejecutable no está firmado.

## Desarrollo

```powershell
mvn clean verify
mvn javafx:run
```

Código en `src/main/java`, estilos en `src/main/resources`, pruebas en `src/test/java`. Docker ejecuta las pruebas al construir. Se retiraron los binarios antiguos, Java Web Start, configuración particular de IntelliJ y recursos de la interfaz reemplazada.

## Archivos y límites

Trabaja con UTF-8 y conserva BOM, saltos LF/CRLF/CR y la ausencia de salto final. Archivos que mezclan tipos de salto se normalizan (prioridad CRLF, CR, LF). Convierte otras codificaciones a UTF-8 antes de abrirlas. Guardado mediante reemplazo atómico cuando el sistema lo permite; puede modificar permisos personalizados. No incluye resaltado de sintaxis. La lectura y escritura son síncronas: pensado para documentos de texto pequeños o medianos.

Referencias de empaquetado: [JavaFX 25](https://openjfx.io/highlights/25/) y [jpackage app-image](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-overview.html).
