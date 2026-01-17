# Tribal Trouble - Build Container
# This container is optimized for compiling the project and running server components.
# For full development with game client support, use Dockerfile.dev instead.

FROM eclipse-temurin:21-jdk-jammy

LABEL maintainer="Tribal Trouble Contributors"
LABEL description="Build environment for Tribal Trouble game"

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    apache-ant \
    git \
    curl \
    unzip \
    # Required for headless AWT operations during build
    libfontconfig1 \
    libfreetype6 \
    # Clean up apt cache
    && rm -rf /var/lib/apt/lists/*

# Set up Ant environment
ENV ANT_HOME=/usr/share/ant
ENV PATH="${ANT_HOME}/bin:${PATH}"

# Create a non-root user for development
ARG USERNAME=developer
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME

# Set working directory
WORKDIR /workspace

# Copy the project files
COPY --chown=$USERNAME:$USERNAME . .

# Switch to non-root user
USER $USERNAME

# Default command: show available build targets
CMD ["ant", "-projecthelp"]
