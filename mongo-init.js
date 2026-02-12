// MongoDB initialization script for Breakup Stories application
// This script runs when the MongoDB container starts for the first time

// Switch to the breakup_stories database
db = db.getSiblingDB('breakup_stories');

// Create a user for the application with read/write permissions
db.createUser({
  user: 'breakup_user',
  pwd: 'breakup_password',
  roles: [
    {
      role: 'readWrite',
      db: 'breakup_stories'
    }
  ]
});

// Create collections with proper indexes
db.createCollection('users');
db.createCollection('stories');
db.createCollection('likes');
db.createCollection('comments');
db.createCollection('bookmarks');
db.createCollection('emotions');
db.createCollection('keywords');
db.createCollection('feedbacks');
db.createCollection('audits');
db.createCollection('default_configs');

// Create indexes for better performance
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "createdAt": -1 });
db.users.createIndex({ "preferredStoryLanguage": 1 });

db.stories.createIndex({ "userId": 1 });
db.stories.createIndex({ "status": 1 });
db.stories.createIndex({ "metadata.language": 1 });
db.stories.createIndex({ "viewCount": -1 });
db.stories.createIndex({ "createdAt": -1 });

// Compound indexes for common query patterns
db.stories.createIndex(
  { "language": 1, "status": 1, "createdAt": -1 },
  { name: "idx_stories_lang_status_date" }
);
db.stories.createIndex(
  { "status": 1, "viewCount": -1 },
  { name: "idx_stories_status_views" }
);
db.stories.createIndex(
  { "status": 1, "createdAt": -1 },
  { name: "idx_stories_status_date" }
);
db.stories.createIndex(
  { "userId": 1, "status": 1, "createdAt": -1 },
  { name: "idx_stories_user_status_date" }
);

db.likes.createIndex({ "userId": 1 });
db.likes.createIndex({ "storyId": 1 });
db.likes.createIndex({ "userId": 1, "storyId": 1 }, { unique: true });

db.comments.createIndex({ "storyId": 1 });
db.comments.createIndex({ "userId": 1 });
db.comments.createIndex({ "parentId": 1 });
db.comments.createIndex({ "createdAt": -1 });
db.comments.createIndex(
  { "storyId": 1, "parentId": 1, "active": 1, "createdAt": -1 },
  { name: "idx_comments_story_parent_active_date" }
);

db.bookmarks.createIndex({ "userId": 1 });
db.bookmarks.createIndex({ "storyId": 1 });
db.bookmarks.createIndex({ "userId": 1, "storyId": 1 }, { unique: true });

db.emotions.createIndex({ "storyId": 1 });
db.keywords.createIndex({ "storyId": 1 });

db.feedbacks.createIndex({ "userId": 1 });
db.feedbacks.createIndex({ "createdAt": -1 });

db.audits.createIndex({ "userId": 1 });
db.audits.createIndex({ "entityType": 1 });
db.audits.createIndex({ "entityId": 1 });
db.audits.createIndex({ "createdAt": -1 });
db.audits.createIndex({ "userId": 1, "entityType": 1, "actionType": 1 });
db.audits.createIndex({ "userId": 1, "entityType": 1, "actionType": 1, "createdAt": -1 });
db.audits.createIndex({ "entityId": 1, "entityType": 1, "actionType": 1 });
db.audits.createIndex({ "entityId": 1, "entityType": 1, "actionType": 1, "createdAt": -1 });
db.audits.createIndex({ "entityId": 1, "createdAt": -1 });

db.default_configs.createIndex({ "key": 1 }, { unique: true });

// coin_history - for valid balance calculation
db.coin_history.createIndex(
  { "userId": 1, "invalidate": 1 },
  { name: "idx_coinhistory_user_valid" }
);

// withdrawals - for user and status queries
db.withdrawals.createIndex(
  { "userId": 1, "createdAt": -1 },
  { name: "idx_withdrawals_user_date" }
);
db.withdrawals.createIndex(
  { "status": 1, "createdAt": -1 },
  { name: "idx_withdrawals_status_date" }
);

// users - for referral stats
db.users.createIndex(
  { "referredBy": 1 },
  { name: "idx_users_referredby" }
);

print('MongoDB initialization completed successfully!');
print('Database: breakup_stories');
print('User: breakup_user');
print('Collections and indexes created.');

print('Updating user role to ADMIN');
db.users.updateOne(
    { email: "kinneramadhu123@gmail.com" },
    { $set: { role: "ADMIN" } }
); 