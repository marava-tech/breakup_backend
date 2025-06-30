# Breakup Stories API - Deployment Guide

## Overview

This guide explains how to deploy and run the Breakup Stories API on any machine (Linux, macOS, Windows).

## Prerequisites

### Required Software
- **Docker** (version 20.10 or higher)
- **Docker Compose** (version 2.0 or higher)
- **Git** (for cloning the repository)

### System Requirements
- **RAM**: Minimum 4GB, Recommended 8GB+
- **Storage**: At least 10GB free space
- **CPU**: 2 cores minimum, 4+ cores recommended

## Quick Start (Recommended)

### 1. Clone the Repository
```bash
git clone <repository-url>
cd breakup_be
```

### 2. Run with Docker Compose (Easiest Method)

#### For Regular Deployment (Port 8080):
```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f breakup-stories-api
```

#### For Linux Deployment (Port 9100):
```bash
# Start all services
docker-compose -f docker-compose.linux.yml up -d

# Check status
docker-compose -f docker-compose.linux.yml ps

# View logs
docker-compose -f docker-compose.linux.yml logs -f breakup-stories-api
```

### 3. Access the Application
- **API Base URL**: http://localhost:8080 (regular) or http://localhost:9100 (Linux)
- **Health Check**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **MongoDB Express**: http://localhost:8081 (admin/admin)

## Alternative Deployment Methods

### Method 1: Using Pre-built Docker Images

#### Pull and Run the Latest Image
```bash
# Pull the latest image
docker pull madhukinnera/breakup-stories-api:latest

# Run with MongoDB
docker run -d \
  --name breakup-stories-api \
  -p 8080:8080 \
  -e SPRING_DATA_MONGODB_HOST=your-mongodb-host \
  -e SPRING_DATA_MONGODB_PORT=27017 \
  -e SPRING_DATA_MONGODB_DATABASE=breakup_stories \
  madhukinnera/breakup-stories-api:latest
```

#### Run with External MongoDB
```bash
# Start MongoDB (if not already running)
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=password123 \
  mongo:7.0

# Start the API
docker run -d \
  --name breakup-stories-api \
  -p 8080:8080 \
  --link mongodb:mongodb \
  -e SPRING_DATA_MONGODB_HOST=mongodb \
  -e SPRING_DATA_MONGODB_PORT=27017 \
  -e SPRING_DATA_MONGODB_DATABASE=breakup_stories \
  -e SPRING_DATA_MONGODB_USERNAME=admin \
  -e SPRING_DATA_MONGODB_PASSWORD=password123 \
  madhukinnera/breakup-stories-api:latest
```

### Method 2: Build from Source

#### Prerequisites for Building
- **Java 17** (OpenJDK or Oracle JDK)
- **Maven 3.6+**
- **Docker**

#### Build and Run
```bash
# Clone repository
git clone <repository-url>
cd breakup_be

# Build the JAR
mvn clean package -DskipTests

# Build Docker image
docker build -t breakup-stories-api:local .

# Run with Docker Compose
docker-compose up -d
```

### Method 3: Manual Setup (Without Docker)

#### Prerequisites
- **Java 17**
- **MongoDB 7.0+**
- **Maven 3.6+**

#### Setup Steps
```bash
# 1. Install MongoDB
# Ubuntu/Debian:
sudo apt update
sudo apt install mongodb

# macOS:
brew install mongodb/brew/mongodb-community

# 2. Start MongoDB
sudo systemctl start mongodb  # Linux
brew services start mongodb/brew/mongodb-community  # macOS

# 3. Clone and build
git clone <repository-url>
cd breakup_be
mvn clean package -DskipTests

# 4. Run the application
java -jar target/breakup-be-1.0.0.jar
```

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# MongoDB Configuration
SPRING_DATA_MONGODB_HOST=mongodb
SPRING_DATA_MONGODB_PORT=27017
SPRING_DATA_MONGODB_DATABASE=breakup_stories
SPRING_DATA_MONGODB_USERNAME=admin
SPRING_DATA_MONGODB_PASSWORD=password123

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production
JWT_EXPIRATION=2592000000

# Email Configuration (Gmail)
GMAIL_USERNAME=your-email@gmail.com
GMAIL_APP_PASSWORD=your-app-password

# Upload Service Configuration
UPLOAD_SERVICE_URL=http://localhost:9090
UPLOAD_SERVICE_ENDPOINT=/api/v1/upload

# Server Configuration
SERVER_PORT=8080
```

### Production Configuration

For production deployment, update the following:

1. **Change default passwords**
2. **Use strong JWT secret**
3. **Configure proper email settings**
4. **Set up SSL/TLS certificates**
5. **Configure proper logging**

## Platform-Specific Instructions

### Linux (Ubuntu/Debian)

```bash
# Install Docker
sudo apt update
sudo apt install docker.io docker-compose

# Add user to docker group
sudo usermod -aG docker $USER

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Clone and run
git clone <repository-url>
cd breakup_be
docker-compose up -d
```

### macOS

```bash
# Install Docker Desktop
# Download from: https://www.docker.com/products/docker-desktop

# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Clone and run
git clone <repository-url>
cd breakup_be
docker-compose up -d
```

### Windows

```bash
# Install Docker Desktop
# Download from: https://www.docker.com/products/docker-desktop

# Using PowerShell or Command Prompt
git clone <repository-url>
cd breakup_be
docker-compose up -d
```

## Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using the port
lsof -i :8080  # Linux/macOS
netstat -ano | findstr :8080  # Windows

# Kill the process or change the port
```

#### 2. MongoDB Connection Issues
```bash
# Check MongoDB status
docker-compose logs mongodb

# Restart MongoDB
docker-compose restart mongodb
```

#### 3. Memory Issues
```bash
# Increase Docker memory limit
# Docker Desktop -> Settings -> Resources -> Memory

# Or run with memory limits
docker run -m 2g -p 8080:8080 madhukinnera/breakup-stories-api:latest
```

#### 4. Permission Issues (Linux)
```bash
# Fix Docker permissions
sudo chmod 666 /var/run/docker.sock

# Or add user to docker group
sudo usermod -aG docker $USER
```

### Health Checks

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check MongoDB
docker exec -it mongodb mongosh --eval "db.adminCommand('ping')"

# Check all services
docker-compose ps
```

### Logs and Debugging

```bash
# View application logs
docker-compose logs -f breakup-stories-api

# View MongoDB logs
docker-compose logs -f mongodb

# View all logs
docker-compose logs -f

# Access application shell
docker exec -it breakup-stories-api sh
```

## Monitoring and Maintenance

### Backup Database
```bash
# Create backup
docker exec mongodb mongodump --out /backup

# Copy backup to host
docker cp mongodb:/backup ./backup
```

### Update Application
```bash
# Pull latest image
docker pull madhukinnera/breakup-stories-api:latest

# Restart services
docker-compose down
docker-compose up -d
```

### Clean Up
```bash
# Remove containers and volumes
docker-compose down -v

# Remove images
docker rmi madhukinnera/breakup-stories-api:latest

# Clean up unused resources
docker system prune -a
```

## Security Considerations

### Production Checklist
- [ ] Change default passwords
- [ ] Use strong JWT secret
- [ ] Enable HTTPS/SSL
- [ ] Configure firewall rules
- [ ] Set up proper logging
- [ ] Regular security updates
- [ ] Database backup strategy
- [ ] Monitor resource usage

### Network Security
```bash
# Restrict MongoDB access
# Only allow connections from application container
# Use internal Docker networks

# Configure firewall (Linux)
sudo ufw allow 8080/tcp
sudo ufw deny 27017/tcp  # MongoDB should not be exposed
```

## Performance Optimization

### Docker Configuration
```yaml
# docker-compose.yml
services:
  breakup-stories-api:
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'
```

### JVM Tuning
```bash
# Optimize JVM for production
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseContainerSupport"
```

## Support and Resources

### Useful Commands
```bash
# Quick status check
docker-compose ps && curl -s http://localhost:8080/actuator/health

# Restart specific service
docker-compose restart breakup-stories-api

# Scale services
docker-compose up -d --scale breakup-stories-api=2
```

### Documentation Links
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)
- [MongoDB Documentation](https://docs.mongodb.com/)

### Getting Help
- Check logs: `docker-compose logs -f`
- Health check: `curl http://localhost:8080/actuator/health`
- Database status: `docker exec mongodb mongosh --eval "db.stats()"` 