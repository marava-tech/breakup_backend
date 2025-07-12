# Language Mapping Feature

## Overview

The language mapping feature enhances the `getUserLanguage` method in `StoryProcessingWorker` to return full language names instead of language codes. This provides better readability and consistency in language handling across the application.

## Implementation Details

### Language Code to Full Name Mapping

The feature uses a hardcoded `LANGUAGE_MAP` that maps 2-character language codes to their full language names:

```java
private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();
```

#### Supported Languages

**South Indian Languages:**
- `te` → `telugu`
- `ta` → `tamil`
- `ka` → `kannada`
- `ml` → `malayalam`

**Hindi and English:**
- `hi` → `hindi`
- `en` → `english`

**Additional Indian Languages:**
- `bn` → `bengali`
- `gu` → `gujarati`
- `pa` → `punjabi`
- `mr` → `marathi`
- `or` → `odia`
- `as` → `assamese`

### Method Logic

The `getUserLanguage` method now follows this logic:

1. **Extract user language preference** from `user.getPreferredStoryLanguage()`
2. **Check if it's a 2-character code** (e.g., "te", "hi", "en")
3. **Look up full name** in the `LANGUAGE_MAP`
4. **Return full name** if found, otherwise return the original code
5. **Fallback to "english"** if no language is specified

### Code Example

```java
private String getUserLanguage(User user) {
    try {
        String userLanguage = user.getPreferredStoryLanguage();
        if (userLanguage != null && !userLanguage.trim().isEmpty()) {
            String languageCode = userLanguage.toLowerCase().trim();
            
            // If it's a 2-character code, try to get the full name
            if (languageCode.length() == 2) {
                String fullLanguageName = LANGUAGE_MAP.get(languageCode);
                if (fullLanguageName != null) {
                    return fullLanguageName;
                }
            }
            
            // If it's already a full name or not found in map, return as is
            return languageCode;
        }
    } catch (Exception e) {
        log.warn("Error getting user language for userId: {}, using default", user.getId(), e);
    }
    
    // Default language
    return "english";
}
```

## Usage Examples

### Input → Output Examples

| Input | Output | Description |
|-------|--------|-------------|
| `"te"` | `"telugu"` | Telugu language code |
| `"hi"` | `"hindi"` | Hindi language code |
| `"en"` | `"english"` | English language code |
| `"ta"` | `"tamil"` | Tamil language code |
| `"ka"` | `"kannada"` | Kannada language code |
| `"ml"` | `"malayalam"` | Malayalam language code |
| `"telugu"` | `"telugu"` | Already full name |
| `"hindi"` | `"hindi"` | Already full name |
| `null` | `"english"` | Default fallback |
| `""` | `"english"` | Empty string fallback |

## Benefits

1. **Consistency**: All language references use full names
2. **Readability**: Full language names are more descriptive
3. **Extensibility**: Easy to add new language mappings
4. **Backward Compatibility**: Still works with existing full language names
5. **Error Handling**: Graceful fallback to default language

## Integration Points

This method is used in:

- **Story Processing Worker**: For AI service calls that require language context
- **Audio Generation**: When generating audio from text
- **Story Analysis**: When analyzing story content
- **Transcription Services**: When processing audio files

## Error Handling

- **Null/Empty Language**: Returns "english" as default
- **Invalid Language Code**: Returns the original code if not found in map
- **Exception Handling**: Logs warning and returns default language

## Future Enhancements

1. **Database Storage**: Move language mappings to database for easier management
2. **API Endpoint**: Add endpoint to retrieve available languages
3. **Dynamic Loading**: Load language mappings from configuration files
4. **Localization**: Support for language names in different locales

## Testing

The feature can be tested with various language codes:

```java
// Test cases
assertEquals("telugu", getUserLanguage(userWithLanguage("te")));
assertEquals("hindi", getUserLanguage(userWithLanguage("hi")));
assertEquals("english", getUserLanguage(userWithLanguage("en")));
assertEquals("english", getUserLanguage(userWithLanguage(null)));
assertEquals("telugu", getUserLanguage(userWithLanguage("telugu"))); // Already full name
```

## Dependencies

- **Java Collections**: Uses `HashMap` for efficient lookups
- **Logging**: Uses SLF4J for error logging
- **User Model**: Depends on `User.getPreferredStoryLanguage()` method

## Performance Considerations

- **Static Map**: Language mappings are loaded once at class initialization
- **O(1) Lookup**: HashMap provides constant-time lookups
- **Memory Efficient**: Small map size with minimal memory footprint
- **Thread Safe**: Read-only static map, no concurrent modification issues 