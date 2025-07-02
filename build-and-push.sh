#!/bin/bash

# Build and Push Script for Breakup Stories API
# Creates separate tags for Mac (latest) and Linux (linux-amd64)
# Usage: ./build-and-push.sh [version]

set -e  # Exit on any error

# Configuration
DOCKER_USERNAME="madhukinnera"
IMAGE_NAME="breakup-stories-api"
DEFAULT_VERSION="latest"

# Get version from command line or use default
VERSION=${1:-$DEFAULT_VERSION}

echo "🐳 Building and pushing Docker images for different platforms..."
echo "📦 Image: $DOCKER_USERNAME/$IMAGE_NAME"
echo "🏷️  Tags: latest (Mac/ARM64), linux-amd64 (Linux/AMD64)"

# Check Docker Hub authentication
echo "🔐 Checking Docker Hub authentication..."

# Try multiple methods to check authentication
AUTH_CHECKED=false

# Method 1: Check docker info
if docker info 2>/dev/null | grep -q "Username"; then
    CURRENT_USER=$(docker info 2>/dev/null | grep "Username" | awk '{print $2}')
    echo "✅ Logged in as: $CURRENT_USER"
    AUTH_CHECKED=true
fi

# Method 2: Try to access Docker Hub directly
if [ "$AUTH_CHECKED" = false ]; then
    if docker search hello-world >/dev/null 2>&1; then
        echo "✅ Docker Hub access confirmed"
        AUTH_CHECKED=true
    fi
fi

# Method 3: Check if we can pull a public image
if [ "$AUTH_CHECKED" = false ]; then
    if docker pull hello-world:latest >/dev/null 2>&1; then
        echo "✅ Docker Hub authentication confirmed"
        AUTH_CHECKED=true
    fi
fi

if [ "$AUTH_CHECKED" = false ]; then
    echo "❌ Not logged in to Docker Hub. Please login first:"
    echo "   docker login"
    echo ""
    echo "💡 If you don't have a Docker Hub account, create one at:"
    echo "   https://hub.docker.com/signup"
    exit 1
fi

# Check if Docker Buildx is available
if ! docker buildx version > /dev/null 2>&1; then
    echo "❌ Docker Buildx is not available. Please install Docker Buildx."
    exit 1
fi

# Create and use a new builder instance if it doesn't exist
BUILDER_NAME="multi-platform-builder"
if ! docker buildx inspect $BUILDER_NAME > /dev/null 2>&1; then
    echo "🔧 Creating new builder instance: $BUILDER_NAME"
    docker buildx create --name $BUILDER_NAME --use
else
    echo "🔧 Using existing builder instance: $BUILDER_NAME"
    docker buildx use $BUILDER_NAME
fi

# Function to check if repository exists
check_repository() {
    local repo="$1"
    echo "🔍 Checking if repository $repo exists..."
    
    # Try to pull a non-existent tag to check repository access
    if docker pull "$repo:__check_repo__" 2>&1 | grep -q "repository does not exist"; then
        echo "❌ Repository $repo does not exist on Docker Hub."
        echo ""
        echo "📝 To create the repository:"
        echo "   1. Go to https://hub.docker.com/repositories"
        echo "   2. Click 'Create Repository'"
        echo "   3. Set Repository Name: $IMAGE_NAME"
        echo "   4. Set Visibility: Public or Private"
        echo "   5. Click 'Create'"
        echo ""
        echo "🔐 Make sure you're logged in with the correct account:"
        echo "   docker login"
        echo ""
        echo "💡 Or update the DOCKER_USERNAME in this script to match your Docker Hub username."
        return 1
    elif docker pull "$repo:__check_repo__" 2>&1 | grep -q "unauthorized"; then
        echo "❌ Unauthorized access to repository $repo."
        echo "🔐 Please check your Docker Hub credentials:"
        echo "   docker logout"
        echo "   docker login"
        return 1
    fi
    return 0
}

# Check repository before building
if ! check_repository "$DOCKER_USERNAME/$IMAGE_NAME"; then
    exit 1
fi

echo "✅ Repository check passed. Proceeding with build..."

# Build and push Mac version (ARM64) as latest
echo "🍎 Building Mac version (ARM64) as 'latest'..."
if docker buildx build \
    --platform linux/arm64 \
    --tag $DOCKER_USERNAME/$IMAGE_NAME:latest \
    --push \
    .; then
    echo "✅ Successfully built and pushed Mac version (latest)"
else
    echo "❌ Failed to build and push Mac version"
    exit 1
fi

# Build and push Linux version (AMD64) as linux-amd64
echo "🐧 Building Linux version (AMD64) as 'linux-amd64'..."
if docker buildx build \
    --platform linux/amd64 \
    --tag $DOCKER_USERNAME/$IMAGE_NAME:linux-amd64 \
    --push \
    .; then
    echo "✅ Successfully built and pushed Linux version (linux-amd64)"
else
    echo "❌ Failed to build and push Linux version"
    exit 1
fi

echo "✅ Successfully built and pushed both platform versions!"
echo "🎉 Images are now available on Docker Hub!"

# Show image info for both tags
echo ""
echo "📋 Image Information:"
echo "🍎 Mac version (latest):"
docker buildx imagetools inspect $DOCKER_USERNAME/$IMAGE_NAME:latest 2>/dev/null || echo "   Image info not available yet"
echo ""
echo "🐧 Linux version (linux-amd64):"
docker buildx imagetools inspect $DOCKER_USERNAME/$IMAGE_NAME:linux-amd64 2>/dev/null || echo "   Image info not available yet"

echo ""
echo "🚀 To run the application:"
echo "   Mac:     docker run -p 8080:8080 $DOCKER_USERNAME/$IMAGE_NAME:latest"
echo "   Linux:   docker run -p 8080:8080 $DOCKER_USERNAME/$IMAGE_NAME:linux-amd64"
echo ""
echo "📖 Or use docker-compose:"
echo "   docker-compose up -d"
echo ""
echo "🌐 Platform-specific tags:"
echo "   - latest:     Mac (Apple Silicon/ARM64)"
echo "   - linux-amd64: Linux (Intel/AMD 64-bit)" 