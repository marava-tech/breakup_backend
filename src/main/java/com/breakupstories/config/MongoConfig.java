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

    @Value("${spring.data.mongodb.uri:}")
    private String uri;

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Value("${spring.data.mongodb.database:breakup_stories}")
    private String database;

    @Value("${spring.data.mongodb.username:}")
    private String username;

    @Value("${spring.data.mongodb.password:}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database:admin}")
    private String authenticationDatabase;

    @Value("${spring.data.mongodb.read-preference:primary}")
    private String readPreference;

    private String getDatabase() {
        return (database != null && !database.trim().isEmpty()) ? database.trim() : "breakup_stories";
    }

    @Bean
    @Primary
    public MongoClient mongoClient() {
        String connectionString;
        if (uri != null && !uri.trim().isEmpty()) {
            connectionString = uri.trim();
        } else {
            boolean hasAuth = username != null && !username.isEmpty() && password != null && !password.isEmpty();
            StringBuilder sb = new StringBuilder("mongodb://");
            if (hasAuth) {
                sb.append(URLEncoder.encode(username, StandardCharsets.UTF_8))
                  .append(":").append(URLEncoder.encode(password, StandardCharsets.UTF_8))
                  .append("@");
            }
            sb.append(host).append(":").append(port).append("/").append(getDatabase());
            sb.append("?");
            if (hasAuth) {
                sb.append("authSource=").append(authenticationDatabase).append("&");
            }
            sb.append("retryWrites=true&retryReads=true");
            sb.append("&serverSelectionTimeoutMS=30000");
            sb.append("&connectTimeoutMS=30000");
            sb.append("&socketTimeoutMS=30000");
            connectionString = sb.toString();
        }

        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString));
        if ("secondaryPreferred".equalsIgnoreCase(readPreference)) {
            builder.readPreference(ReadPreference.secondaryPreferred());
        }
        return MongoClients.create(builder.build());
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