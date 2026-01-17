# Docker Development Environment

This guide explains how to use Docker for Tribal Trouble development.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) (20.10+)
- [Docker Compose](https://docs.docker.com/compose/install/) (v2.0+)

## Quick Start

### Option 1: Build Only (Recommended for most development)

Compile the entire project:

```bash
docker compose run --rm build ant compile
```

### Option 2: Full Development Environment

Start an interactive shell with all tools:

```bash
# Start MySQL first (required for server development)
docker compose up -d mysql

# Enter development container
docker compose run --rm dev
```

Inside the container:

```bash
# Compile everything
ant compile

# Run the game client (requires X11 - see below)
cd tt && ant run

# Run the matchmaker server
cd server && ant run-matchmaker
```

## Available Services

| Service | Description | Command |
|---------|-------------|---------|
| `build` | Lightweight build container | `docker compose run --rm build ant compile` |
| `dev` | Full dev environment with X11/OpenGL | `docker compose run --rm dev` |
| `mysql` | MySQL 8.0 database | `docker compose up -d mysql` |
| `matchmaker` | Game matchmaking server | `docker compose up matchmaker` |
| `router` | Game routing server | `docker compose up router` |

## Common Tasks

### Compile the Project

```bash
docker compose run --rm build ant compile
```

### Run Specific Build Targets

```bash
# Check available targets
docker compose run --rm build ant -projecthelp

# Format code
docker compose run --rm build ant format

# Clean build artifacts
docker compose run --rm build ant clean
```

### Server Development

Start the MySQL database and matchmaker:

```bash
docker compose up mysql matchmaker
```

Or run them separately:

```bash
# Start MySQL in background
docker compose up -d mysql

# Wait for MySQL to be ready, then start matchmaker
docker compose up matchmaker
```

### Running the Game Client (Linux with X11)

To run the game client from Docker, you need X11 forwarding:

```bash
# Allow Docker to access your display
xhost +local:docker

# Start the dev container
docker compose run --rm dev

# Inside the container
cd tt && ant run
```

**Note:** Running the game client in Docker requires:
- Linux host with X11
- Working OpenGL drivers
- Audio configured (PulseAudio)

For macOS or Windows, native development is recommended for client testing.

## Environment Variables

Create a `.env` file to customize settings:

```bash
# .env
TT_SERVER_PASSWORD=your_secure_password
MYSQL_ROOT_PASSWORD=your_root_password
UID=1000
GID=1000
```

| Variable | Default | Description |
|----------|---------|-------------|
| `TT_SERVER_PASSWORD` | `tribaltrouble` | MySQL matchmaker user password |
| `MYSQL_ROOT_PASSWORD` | `rootpassword` | MySQL root password |
| `UID` | `1000` | User ID for file permissions |
| `GID` | `1000` | Group ID for file permissions |
| `DISPLAY` | `:0` | X11 display for game client |

## Database Access

Connect to MySQL from your host:

```bash
mysql -h 127.0.0.1 -u matchmaker -p oddlabs
# Password: tribaltrouble (or your TT_SERVER_PASSWORD)
```

Or from within the dev container:

```bash
mysql -h mysql -u matchmaker -p oddlabs
```

## Troubleshooting

### Permission Issues

If you get permission errors, ensure UID/GID match your host user:

```bash
# Find your UID and GID
id -u  # UID
id -g  # GID

# Set them in .env or run with:
UID=$(id -u) GID=$(id -g) docker compose run --rm dev
```

### X11 Display Errors

If you see "cannot open display" errors:

```bash
# Allow local Docker connections
xhost +local:docker

# Or for more security, allow only root
xhost +local:root
```

### OpenGL/Graphics Issues

For GPU-accelerated graphics, you may need nvidia-docker:

```bash
# Install nvidia-container-toolkit
# Then use:
docker compose run --rm --gpus all dev
```

### MySQL Connection Refused

Ensure MySQL is fully started before connecting:

```bash
# Check MySQL health
docker compose ps mysql

# View MySQL logs
docker compose logs mysql

# Wait for healthy status
docker compose up -d mysql
docker compose exec mysql mysqladmin ping -h localhost -u root -prootpassword
```

### Build Failures

If Ivy dependency resolution fails:

```bash
# Clear Ivy cache and retry
docker compose run --rm build rm -rf /home/developer/.ivy2/cache
docker compose run --rm build ant compile
```

## Building Platform Distributions

**Note:** Cross-platform builds (Windows .exe, macOS .dmg) require native tools that aren't available in Linux containers. The Docker environment can build:

- Linux AppImage: `cd tt && ant build-linux`

For Windows and macOS builds, use a native development environment or CI/CD.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose                            │
├─────────────────┬─────────────────┬─────────────────────────┤
│   build         │      dev        │        mysql            │
│   (JDK + Ant)   │ (JDK + Ant +    │    (MySQL 8.0)          │
│                 │  X11 + OpenGL)  │                         │
│   Compiles      │   Compiles +    │   Stores game data      │
│   code only     │   runs game     │   for server            │
└─────────────────┴─────────────────┴─────────────────────────┘
         │                 │                    │
         └─────────────────┴────────────────────┘
                           │
                    ┌──────┴──────┐
                    │  /workspace │  (mounted source code)
                    └─────────────┘
```

## Tips for Efficient Development

1. **Use the build container for quick compiles** - It's lighter and faster
2. **Keep MySQL running** - `docker compose up -d mysql` keeps it ready
3. **Mount your source** - Changes are reflected immediately (no rebuild needed)
4. **Ivy cache persists** - Dependencies are cached in a Docker volume

## Cleaning Up

```bash
# Stop all containers
docker compose down

# Remove containers and volumes (including database data)
docker compose down -v

# Remove built images
docker compose down --rmi all
```
