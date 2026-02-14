# ============================================================
# Tribal Trouble Server - Multi-stage Docker Build
# ============================================================
# Stage 1 (builder): Compiles the full project using JDK + Ant
# Stage 2 (server):  Lean runtime image for matchmaker/router
#
# Usage:
#   docker build -t tribaltrouble-server .
#   docker run -e SERVER_ROLE=matchmaker tribaltrouble-server
#   docker run -e SERVER_ROLE=router tribaltrouble-server
#
# Or use docker-compose.yml for the full local dev stack.
# ============================================================

# --------------- Stage 1: Builder ---------------
FROM eclipse-temurin:24-jdk AS builder

# Install Apache Ant
ENV ANT_VERSION=1.10.15
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    curl -fsSL "https://dlcdn.apache.org/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz" \
      | tar -xz -C /opt && \
    ln -s "/opt/apache-ant-${ANT_VERSION}" /opt/ant && \
    apt-get purge -y curl && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*
ENV ANT_HOME=/opt/ant
ENV PATH="${ANT_HOME}/bin:${PATH}"

WORKDIR /build

# Copy the full source tree
COPY . .

# Resolve Ivy dependencies and compile server + common
RUN cd server && ant compile

# --------------- Stage 2: Server Runtime ---------------
FROM eclipse-temurin:24-jre AS server

WORKDIR /app

# Copy compiled classes
COPY --from=builder /build/server/build/classes server/build/classes
COPY --from=builder /build/common/build/classes common/build/classes

# Copy static resources (registration keys, fonts, etc.)
COPY --from=builder /build/common/static common/static

# Copy required library jars
COPY --from=builder /build/common/lib/java/commons-pool-1.2.jar common/lib/java/
COPY --from=builder /build/common/lib/java/commons-dbcp-1.2.1.jar common/lib/java/
COPY --from=builder /build/common/lib/java/commons-collections-3.1.jar common/lib/java/
COPY --from=builder /build/common/lib/java/mysql-connector-j-9.3.0.jar common/lib/java/

# Copy Ivy-resolved dependencies (discord4j and transitive deps)
COPY --from=builder /build/ivy_lib ivy_lib

# Create logs directory
RUN mkdir -p server/logs

# Copy entrypoint
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Matchmaker port: 33214, Router port: 11221
EXPOSE 33214 11221

ENTRYPOINT ["/app/entrypoint.sh"]
