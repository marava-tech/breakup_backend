package com.breakupstories.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NamesListDeserializer extends JsonDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        List<String> result = new ArrayList<>();
        
        if (node.isArray()) {
            // Handle array case: ["name1", "name2"]
            for (JsonNode element : node) {
                result.add(element.asText());
            }
        } else if (node.isObject()) {
            // Handle object case: {} - return empty list
            // This handles cases where API returns {} instead of []
            return result;
        } else if (node.isNull()) {
            // Handle null case
            return result;
        }
        
        return result;
    }
} 