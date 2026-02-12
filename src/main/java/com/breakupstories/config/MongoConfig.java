package com.breakupstories.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.breakupstories.repository")
public class MongoConfig {

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Value("${spring.data.mongodb.database:breakup_stories}")
    private String database;
    
    private String getDatabase() {
        // Ensure database name is never empty - use default if empty or null
        return (database != null && !database.trim().isEmpty()) ? database.trim() : "breakup_stories";
    }

    @Value("${spring.data.mongodb.username:}")
    private String username;

    @Value("${spring.data.mongodb.password:}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database:admin}")
    private String authenticationDatabase;

    @Value("${spring.data.mongodb.read-preference:primary}")
    private String readPreference;

    @Bean
    @Primary
    public MongoClient mongoClient() {
        // Build connection string with proper URL encoding for special characters
        StringBuilder connectionString = new StringBuilder("mongodb://");
        
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            // URL encode username and password to handle special characters like @, :, etc.
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            connectionString.append(encodedUsername).append(":").append(encodedPassword).append("@");
        }
        
        String dbName = getDatabase();
        connectionString.append(host).append(":").append(port);
        connectionString.append("/").append(dbName);
        connectionString.append("?authSource=").append(authenticationDatabase);
        connectionString.append("&retryWrites=true&retryReads=true");
        connectionString.append("&serverSelectionTimeoutMS=30000");
        connectionString.append("&connectTimeoutMS=30000");
        connectionString.append("&socketTimeoutMS=30000");
        
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString.toString()));
        if ("secondaryPreferred".equalsIgnoreCase(readPreference)) {
            builder.readPreference(ReadPreference.secondaryPreferred());
        }
        MongoClientSettings settings = builder.build();
        
        return MongoClients.create(settings);
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabase());
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoDatabaseFactory());
    }
} 