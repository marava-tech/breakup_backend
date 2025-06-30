#!/bin/bash

# Quick Setup Script for Breakup Stories API
# This script helps you quickly deploy the application on any machine

set -e

echo "🚀 Breakup Stories API - Quick Setup"
echo "====================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        echo "Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    print_status "Docker is installed"
}

# Check if Docker Compose is installed
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        echo "Visit: https://docs.docker.com/compose/install/"
        exit 1
    fi
    print_status "Docker Compose is installed"
}

# Check if Docker daemon is running
check_docker_daemon() {
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running. Please start Docker."
        exit 1
    fi
    print_status "Docker daemon is running"
}

# Pull latest images
pull_images() {
    print_info "Pulling latest Docker images..."
    docker pull madhukinnera/breakup-stories-api:latest
    docker pull mongo:7.0
    docker pull mongo-express:1.0.0
    print_status "Images pulled successfully"
}

# Create .env file if it doesn't exist
create_env_file() {
    if [ ! -f .env ]; then
        print_info "Creating .env file with default configuration..."
        cat > .env << EOF
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
GMAIL_USERNAME=abhedyam.team@gmail.com
GMAIL_APP_PASSWORD=aualeeatizzmmjyl

# Upload Service Configuration
UPLOAD_SERVICE_URL=http://localhost:9090
UPLOAD_SERVICE_ENDPOINT=/api/v1/upload

# Server Configuration
SERVER_PORT=8080
EOF
        print_status ".env file created"
    else
        print_info ".env file already exists"
    fi
}

# Start services
start_services() {
    print_info "Starting services..."
    
    # Check if services are already running
    if docker-compose ps | grep -q "Up"; then
        print_warning "Services are already running. Stopping them first..."
        docker-compose down
    fi
    
    # Start services
    docker-compose up -d
    
    print_status "Services started successfully"
}

# Wait for services to be ready
wait_for_services() {
    print_info "Waiting for services to be ready..."
    
    # Wait for MongoDB
    echo "Waiting for MongoDB..."
    while ! docker exec mongodb mongosh --eval "db.adminCommand('ping')" &> /dev/null; do
        sleep 2
    done
    print_status "MongoDB is ready"
    
    # Wait for API
    echo "Waiting for API..."
    while ! curl -s http://localhost:8080/actuator/health &> /dev/null; do
        sleep 5
    done
    print_status "API is ready"
}

# Show status
show_status() {
    echo ""
    echo "🎉 Setup Complete!"
    echo "=================="
    echo ""
    echo "📋 Service Status:"
    docker-compose ps
    echo ""
    echo "🌐 Access Points:"
    echo "   API Base URL: http://localhost:8080"
    echo "   Health Check: http://localhost:8080/actuator/health"
    echo "   Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "   MongoDB Express: http://localhost:8081 (admin/admin)"
    echo ""
    echo "📝 Useful Commands:"
    echo "   View logs: docker-compose logs -f"
    echo "   Stop services: docker-compose down"
    echo "   Restart services: docker-compose restart"
    echo "   Check health: curl http://localhost:8080/actuator/health"
    echo ""
}

# Main execution
main() {
    echo "Checking prerequisites..."
    check_docker
    check_docker_compose
    check_docker_daemon
    
    echo ""
    echo "Setting up environment..."
    create_env_file
    pull_images
    
    echo ""
    echo "Starting services..."
    start_services
    
    echo ""
    echo "Waiting for services to be ready..."
    wait_for_services
    
    show_status
}

# Handle script arguments
case "${1:-}" in
    "stop")
        print_info "Stopping services..."
        docker-compose down
        print_status "Services stopped"
        ;;
    "restart")
        print_info "Restarting services..."
        docker-compose restart
        print_status "Services restarted"
        ;;
    "logs")
        print_info "Showing logs..."
        docker-compose logs -f
        ;;
    "status")
        print_info "Service status:"
        docker-compose ps
        ;;
    "health")
        print_info "Health check:"
        curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
        ;;
    "clean")
        print_warning "This will remove all containers, volumes, and images. Are you sure? (y/N)"
        read -r response
        if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
            print_info "Cleaning up..."
            docker-compose down -v
            docker rmi madhukinnera/breakup-stories-api:latest 2>/dev/null || true
            docker system prune -f
            print_status "Cleanup completed"
        else
            print_info "Cleanup cancelled"
        fi
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  (no args)  - Setup and start services"
        echo "  stop       - Stop services"
        echo "  restart    - Restart services"
        echo "  logs       - Show logs"
        echo "  status     - Show service status"
        echo "  health     - Check API health"
        echo "  clean      - Remove all containers and images"
        echo "  help       - Show this help"
        ;;
    *)
        main
        ;;
esac 