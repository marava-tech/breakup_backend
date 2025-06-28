# User Profile API

This document describes the new user profile functionality in the Breakup Stories application.

## Overview

The profile API provides comprehensive user information including personal details and engagement statistics from their stories.

## API Endpoint

### GET /api/users/profile

Retrieve the authenticated user's profile with statistics.

**Authentication:** Required (JWT Token) - Any authenticated user can access their own profile

**Security Note:** This endpoint is accessible to all authenticated users (ROLE_USER and ROLE_ADMIN), not just administrators.

**Response Format:**
```json
{
  "id": "user_id",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "profileImageUrl": "https://example.com/profile.jpg",
  "gender": "MALE",
  "age": 25,
  "preferredStoryLanguage": "en",
  "role": "USER",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "totalStories": 15,
  "totalLikes": 250,
  "totalViews": 1500,
  "totalComments": 45
}
```

## Response Fields

### Personal Information
- `id`: Unique user identifier
- `name`: User's display name
- `email`: User's email address
- `profileImageUrl`: URL to user's profile image
- `gender`: User's gender (MALE, FEMALE, OTHER)
- `age`: User's age
- `preferredStoryLanguage`: User's preferred story language
- `role`: User's role (USER, ADMIN)
- `createdAt`: Account creation timestamp
- `updatedAt`: Last profile update timestamp

### Statistics
- `totalStories`: Number of active stories uploaded by the user
- `totalLikes`: Total likes received across all user's stories
- `totalViews`: Total views across all user's stories
- `totalComments`: Total comments received across all user's stories

## Usage Examples

### cURL Example
```bash
curl -X GET "http://localhost:8080/api/users/profile" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### JavaScript Example
```javascript
const getUserProfile = async () => {
  const response = await fetch('/api/users/profile', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    const profile = await response.json();
    console.log('User Profile:', profile);
    return profile;
  } else {
    throw new Error('Failed to fetch user profile');
  }
};
```

## Error Responses

### 401 Unauthorized
```json
{
  "error": "User not authenticated",
  "timestamp": "2024-01-15T10:30:00",
  "status": 401
}
```

### 404 Not Found
```json
{
  "error": "User not found with email: user@example.com",
  "timestamp": "2024-01-15T10:30:00",
  "status": 404
}
```

## Flutter Frontend Implementation Suggestions

### Profile Page Features

1. **Profile Header Section**
   - Large profile image with edit functionality
   - User name and email
   - Edit profile button
   - Settings/gear icon

2. **Statistics Cards**
   - Stories count with story icon
   - Total likes with heart icon
   - Total views with eye icon
   - Total comments with comment icon
   - Use attractive cards with gradients or shadows

3. **User Information Section**
   - Age and gender display
   - Preferred language setting
   - Account creation date
   - Member since badge

4. **Action Buttons**
   - Edit Profile
   - Change Profile Picture
   - Update Preferred Language
   - Privacy Settings
   - Logout

5. **Story Management**
   - "My Stories" section with preview
   - Draft stories (if applicable)
   - Story analytics/insights
   - Story performance metrics

6. **Achievements/Badges**
   - Story count milestones
   - Like count achievements
   - View count badges
   - Comment engagement rewards

7. **Social Features**
   - Followers/Following count (if implemented)
   - User's favorite stories
   - Recently viewed stories
   - Story recommendations

### UI/UX Recommendations

1. **Visual Design**
   - Clean, modern interface
   - Consistent color scheme with app theme
   - Proper spacing and typography
   - Responsive design for different screen sizes

2. **Interactive Elements**
   - Smooth animations for statistics
   - Pull-to-refresh functionality
   - Loading states and skeleton screens
   - Error handling with retry options

3. **Navigation**
   - Easy access from bottom navigation
   - Back button functionality
   - Deep linking support

4. **Performance**
   - Cache profile data
   - Optimize image loading
   - Lazy loading for story lists
   - Efficient API calls

### Sample Flutter Widget Structure

```dart
class ProfilePage extends StatefulWidget {
  @override
  _ProfilePageState createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  UserProfileResponse? profile;
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    try {
      final response = await ApiService.getUserProfile();
      setState(() {
        profile = response;
        isLoading = false;
      });
    } catch (e) {
      // Handle error
    }
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return ProfileSkeleton();
    }

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 200,
            flexibleSpace: FlexibleSpaceBar(
              background: ProfileHeader(profile: profile!),
            ),
          ),
          SliverToBoxAdapter(
            child: Column(
              children: [
                StatisticsCards(profile: profile!),
                UserInfoSection(profile: profile!),
                ActionButtons(),
                MyStoriesSection(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
```

### Statistics Cards Widget

```dart
class StatisticsCards extends StatelessWidget {
  final UserProfileResponse profile;

  const StatisticsCards({Key? key, required this.profile}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.all(16),
      child: Row(
        children: [
          Expanded(
            child: StatCard(
              icon: Icons.story,
              value: profile.totalStories.toString(),
              label: 'Stories',
            ),
          ),
          Expanded(
            child: StatCard(
              icon: Icons.favorite,
              value: profile.totalLikes.toString(),
              label: 'Likes',
            ),
          ),
          Expanded(
            child: StatCard(
              icon: Icons.visibility,
              value: profile.totalViews.toString(),
              label: 'Views',
            ),
          ),
          Expanded(
            child: StatCard(
              icon: Icons.comment,
              value: profile.totalComments.toString(),
              label: 'Comments',
            ),
          ),
        ],
      ),
    );
  }
}
```

## Future Enhancements

1. **Advanced Analytics**
   - Story performance over time
   - Engagement rate calculations
   - Audience demographics
   - Story completion rates

2. **Social Features**
   - User followers/following
   - Story sharing statistics
   - Community engagement metrics

3. **Achievement System**
   - Badges for milestones
   - Level progression
   - Rewards and incentives

4. **Privacy Controls**
   - Profile visibility settings
   - Statistics privacy options
   - Data export functionality

5. **Integration Features**
   - Social media sharing
   - Profile QR codes
   - Deep linking to specific stories 