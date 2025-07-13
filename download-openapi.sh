#!/bin/bash

# Download OpenAPI specification in different formats
echo "Downloading OpenAPI specification..."

# Download as JSON (for Postman import)
curl -s http://localhost:8080/api-docs > openapi-spec.json
echo "✅ Downloaded openapi-spec.json"

# Download as YAML (alternative format)
curl -s http://localhost:8080/api-docs > openapi-spec.yaml
echo "✅ Downloaded openapi-spec.yaml"

# Create a Postman collection URL
echo ""
echo "🌐 OpenAPI Documentation URLs:"
echo "   Swagger UI: http://localhost:8080/swagger-ui.html"
echo "   JSON Spec: http://localhost:8080/api-docs"
echo ""
echo "📥 Postman Import Instructions:"
echo "   1. Open Postman"
echo "   2. Click 'Import' button"
echo "   3. Choose 'Link' tab"
echo "   4. Enter: http://localhost:8080/api-docs"
echo "   5. Click 'Continue' and 'Import'"
echo ""
echo "📋 Or import the downloaded file:"
echo "   - Use openapi-spec.json for Postman import"
echo ""
echo "🔑 Authentication Headers for Testing:"
echo "   JWT Token: Authorization: Bearer <your-jwt-token>"
echo "   Test Bypass: X-BS-Authorization: true, X-BS-UserId: <user-id>"
echo "   Admin Access: X-BS-Admin: true" 