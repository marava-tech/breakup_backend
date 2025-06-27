# Languages API Implementation

## Overview
Added a new API endpoint to retrieve available languages from the default configuration. The API fetches the "languages" configuration key, splits the comma-separated value, and returns a list of language strings.

## Changes Made

### 1. DefaultConfigService Updates
Added a new method `getLanguages()` that:
- Fetches the configuration with key "languages"
- Splits the value by comma
- Trims whitespace from each language
- Filters out empty strings
- Returns a list of language strings
- Handles errors gracefully by returning an empty list

```java
public List<String> getLanguages() {
    try {
        DefaultConfig languagesConfig = defaultConfigRepository.findByKey("languages")
                .orElseThrow(() -> new RuntimeException("Languages configuration not found"));
        
        if (languagesConfig.getValue() == null || languagesConfig.getValue().trim().isEmpty()) {
            return List.of();
        }
        
        // Split the value by comma and trim whitespace
        return List.of(languagesConfig.getValue().split(","))
                .stream()
                .map(String::trim)
                .filter(lang -> !lang.isEmpty())
                .collect(Collectors.toList());
    } catch (Exception e) {
        // Return empty list if there's any error
        return List.of();
    }
}
```

### 2. DefaultConfigController Updates
Added a new GET endpoint `/api/configs/languages` that:
- Is accessible to all users (no admin restriction)
- Returns a list of language strings
- Has proper Swagger documentation

```java
@GetMapping("/languages")
@Operation(summary = "Get list of available languages")
public ResponseEntity<List<String>> getLanguages() {
    return ResponseEntity.ok(defaultConfigService.getLanguages());
}
```

## API Details

### Endpoint
- **URL**: `GET /api/configs/languages`
- **Access**: Public (no authentication required)
- **Response**: `List<String>` - Array of language strings

### Example Usage

#### Request
```http
GET /api/configs/languages
```

#### Response
```json
[
    "English",
    "Hindi",
    "Spanish",
    "French",
    "German"
]
```

### Configuration Setup
The API expects a default configuration entry with:
- **Key**: `languages`
- **Value**: Comma-separated list of languages (e.g., "English, Hindi, Spanish, French, German")

## Features

### 1. **Robust Error Handling**
- Returns empty list if configuration not found
- Handles null or empty values gracefully
- Catches exceptions and returns empty list

### 2. **Data Cleaning**
- Trims whitespace from each language
- Filters out empty strings
- Removes leading/trailing spaces

### 3. **Public Access**
- No authentication required
- Accessible to all users
- Useful for frontend language selection

### 4. **Swagger Documentation**
- Proper API documentation
- Clear summary and description

## Use Cases

1. **Frontend Language Selection**: Populate language dropdowns
2. **User Preferences**: Show available languages for user selection
3. **Content Filtering**: Filter stories by available languages
4. **Localization**: Determine supported languages for the application

## Database Configuration Example

To set up the languages configuration, create a default config entry:

```json
{
    "key": "languages",
    "value": "English, Hindi, Spanish, French, German, Chinese, Japanese, Korean",
    "description": "Available languages for the application",
    "active": true
}
```

## Testing

### Test Cases
1. **Valid Configuration**: Should return list of languages
2. **Empty Value**: Should return empty list
3. **Null Value**: Should return empty list
4. **Missing Configuration**: Should return empty list
5. **Extra Whitespace**: Should trim and return clean list
6. **Empty Strings**: Should filter out empty entries

### Example Test Data
- Input: `"English, Hindi , Spanish, , French"`
- Output: `["English", "Hindi", "Spanish", "French"]` 