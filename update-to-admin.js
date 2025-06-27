// MongoDB script to update a user's role to ADMIN
// Usage: mongo breakup_stories update-to-admin.js

// Replace 'your-email@example.com' with your actual email
const userEmail = 'your-email@example.com';

// Update the user's role to ADMIN
db.users.updateOne(
    { email: userEmail },
    { $set: { role: 'ADMIN' } }
);

// Verify the update
const updatedUser = db.users.findOne({ email: userEmail });
if (updatedUser) {
    print('User updated successfully:');
    print('Email: ' + updatedUser.email);
    print('Name: ' + updatedUser.name);
    print('Role: ' + updatedUser.role);
} else {
    print('User not found with email: ' + userEmail);
}

// Alternative: Update by user ID if you know the ID
// const userId = 'your-user-id-here';
// db.users.updateOne(
//     { _id: ObjectId(userId) },
//     { $set: { role: 'ADMIN' } }
// ); 