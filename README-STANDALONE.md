# Breakup Stories API - Standalone Deployment

This is a standalone docker-compose file that you can use to run the Breakup Stories API on any machine with Docker installed.

## Quick Start

### 1. Prerequisites
- **Docker** (version 20.10 or higher)
- **Docker Compose** (version 2.0 or higher)

### 2. Download the docker-compose file
Copy the `docker-compose-standalone.yml` file to your machine.

### 3. Run the application
```bash
# Start all services
docker-compose -f docker-compose-standalone.yml up -d

# Check status
docker-compose -f docker-compose-standalone.yml ps

# View logs
docker-compose -f docker-compose-standalone.yml logs -f
```

### 4. Access the application
- **API Base URL**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **MongoDB Express**: http://localhost:8081 (admin/admin123)

## Services Included

### 1. MongoDB Database
- **Image**: mongo:7.0
- **Port**: 27017
- **No Authentication**: Configured without username/password
- **Database**: breakup_stories

### 2. Breakup Stories API
- **Image**: madhukinnera/breakup-stories-api:latest
- **Port**: 8080
- **Features**: 
  - Story management
  - User authentication
  - File uploads
  - Audio streaming
  - Comments and likes
  - Notifications

### 3. MongoDB Express (Optional)
- **Image**: mongo-express:1.0.0
- **Port**: 8081
- **Credentials**: admin/admin123
- **Purpose**: Web-based MongoDB administration

## Useful Commands

```bash
# Start services
docker-compose -f docker-compose-standalone.yml up -d

# Stop services
docker-compose -f docker-compose-standalone.yml down

# Restart services
docker-compose -f docker-compose-standalone.yml restart

# View logs
docker-compose -f docker-compose-standalone.yml logs -f

# Check status
docker-compose -f docker-compose-standalone.yml ps

# Remove everything (including data)
docker-compose -f docker-compose-standalone.yml down -v

# Update to latest image
docker-compose -f docker-compose-standalone.yml pull
docker-compose -f docker-compose-standalone.yml up -d
```

## Configuration

### Environment Variables
The docker-compose file includes all necessary environment variables:

- **MongoDB**: No authentication required
- **JWT**: Default secret (change for production)
- **Email**: Gmail configuration included
- **Upload Service**: Configured for local development

### Customization
To customize the configuration, edit the environment variables in the docker-compose file:

```yaml
environment:
  # Change JWT secret
  JWT_SECRET: your-custom-secret
  
  # Change email settings
  SPRING_MAIL_USERNAME: your-email@gmail.com
  SPRING_MAIL_PASSWORD: your-app-password
  
  # Change server port
  SERVER_PORT: 9090
```

## Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using the port
lsof -i :8080  # Linux/macOS
netstat -ano | findstr :8080  # Windows

# Change port in docker-compose file
ports:
  - "9090:8080"  # Use port 9090 instead
```

#### 2. MongoDB Connection Issues
```bash
# Check MongoDB logs
docker-compose -f docker-compose-standalone.yml logs mongodb

# Restart MongoDB
docker-compose -f docker-compose-standalone.yml restart mongodb
```

#### 3. API Not Starting
```bash
# Check API logs
docker-compose -f docker-compose-standalone.yml logs breakup-stories-api

# Check health
curl http://localhost:8080/actuator/health
```

### Health Checks

```bash
# Check API health
curl http://localhost:8080/actuator/health

# Check MongoDB
docker exec breakup-stories-mongodb mongosh --eval "db.adminCommand('ping')"

# Check all services
docker-compose -f docker-compose-standalone.yml ps
```

## Data Persistence

### MongoDB Data
- MongoDB data is persisted in a Docker volume named `mongodb_data`
- Data survives container restarts and updates
- To completely remove data: `docker-compose -f docker-compose-standalone.yml down -v`

### Backup Database
```bash
# Create backup
docker exec breakup-stories-mongodb mongodump --out /backup

# Copy backup to host
docker cp breakup-stories-mongodb:/backup ./backup
```

## Security Notes

⚠️ **Important**: This configuration is for development/local use only.

For production deployment:
1. Change the JWT secret
2. Use proper email credentials
3. Enable MongoDB authentication
4. Use HTTPS
5. Configure proper firewall rules
6. Set up regular backups

## Support

If you encounter any issues:
1. Check the logs: `docker-compose -f docker-compose-standalone.yml logs -f`
2. Verify Docker is running: `docker info`
3. Check service status: `docker-compose -f docker-compose-standalone.yml ps`
4. Test health endpoint: `curl http://localhost:8080/actuator/health` 