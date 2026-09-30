FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B verify
FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends libgtk-3-0 libasound2t64 libgl1 libxtst6 fonts-dejavu-core xvfb x11vnc novnc websockify openbox curl tini && rm -rf /var/lib/apt/lists/* && useradd --create-home --uid 10001 editor && mkdir /workspace && chown editor:editor /workspace
COPY --from=build /build/target/uni-editor-2.0.0.jar /app/editor.jar
COPY --from=build /build/target/lib /app/lib
COPY docker/start.sh /app/start.sh
RUN chmod +x /app/start.sh
USER editor
WORKDIR /workspace
ENV DISPLAY=:99
EXPOSE 6080
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s CMD curl -fsS http://127.0.0.1:6080/vnc.html >/dev/null || exit 1
ENTRYPOINT ["/usr/bin/tini", "--", "/app/start.sh"]
