#!/bin/bash

# Build and Push Script for Breakup Stories API (Linux)
# Usage: ./build-and-push-linux.sh [version]

set -e  # Exit on any error

# Configuration
DOCKER_USERNAME="madhukinnera"
IMAGE_NAME="breakup-stories-api"
DEFAULT_VERSION="linux-latest"

# Get version from command line or use default
VERSION=${1:-$DEFAULT_VERSION}

echo "🐳 Building and pushing Linux Docker image..."
echo "📦 Image: $DOCKER_USERNAME/$IMAGE_NAME:$VERSION"

# Detect platform
if [[ "$(uname -m)" == "arm64" ]]; then
    PLATFORM="linux/arm64"
    echo "🖥️  Platform: $PLATFORM (Apple Silicon)"
else
    PLATFORM="linux/amd64"
    echo "🖥️  Platform: $PLATFORM (x86_64)"
fi

# Build the Docker image for Linux
echo "🔨 Building Linux Docker image..."
docker build --platform $PLATFORM -f Dockerfile.linux -t $DOCKER_USERNAME/$IMAGE_NAME:$VERSION .

# Tag as linux-latest if not already
if [ "$VERSION" != "linux-latest" ]; then
    echo "🏷️  Tagging as linux-latest..."
    docker tag $DOCKER_USERNAME/$IMAGE_NAME:$VERSION $DOCKER_USERNAME/$IMAGE_NAME:linux-latest
fi

# Tag as latest for cross-platform compatibility
echo "🏷️  Tagging as latest..."
docker tag $DOCKER_USERNAME/$IMAGE_NAME:$VERSION $DOCKER_USERNAME/$IMAGE_NAME:latest

# Push to Docker Hub
echo "📤 Pushing to Docker Hub..."
docker push $DOCKER_USERNAME/$IMAGE_NAME:$VERSION

if [ "$VERSION" != "linux-latest" ]; then
    echo "📤 Pushing linux-latest tag..."
    docker push $DOCKER_USERNAME/$IMAGE_NAME:linux-latest
fi

echo "📤 Pushing latest tag..."
docker push $DOCKER_USERNAME/$IMAGE_NAME:latest

echo "✅ Successfully built and pushed $DOCKER_USERNAME/$IMAGE_NAME:$VERSION"
echo "🎉 Linux image is now available on Docker Hub!"

# Optional: Show image info
echo ""
echo "📋 Image Information:"
docker images $DOCKER_USERNAME/$IMAGE_NAME:$VERSION

echo ""
echo "🚀 To run the Linux application:"
echo "   docker run -p 9100:8080 $DOCKER_USERNAME/$IMAGE_NAME:$VERSION"
echo ""
echo "📖 Or use docker-compose (Linux):"
echo "   docker-compose -f docker-compose.linux.yml up -d"
echo ""
echo "🌐 Access the API at: http://localhost:9100"
echo "🔍 Health check: http://localhost:9100/actuator/health"
echo "📝 Note: Application runs internally on port 8080, mapped to external port 9100" 