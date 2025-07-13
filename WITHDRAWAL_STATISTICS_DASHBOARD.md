# Withdrawal Statistics in Dashboard Stats API

## Overview

The withdrawal statistics have been integrated into the admin dashboard stats API (`/api/admin/dashboard/stats`) to provide comprehensive insights into withdrawal activities and financial metrics.

## Feature Details

### API Endpoint
- **URL**: `GET /api/admin/dashboard/stats`
- **Authentication**: Admin role required
- **Parameters**: 
  - `fromDate` (optional): Start date for statistics
  - `toDate` (optional): End date for statistics

### Enhanced Withdrawal Statistics Included

The dashboard stats API now includes a comprehensive `withdrawalStats` section with detailed metrics:

#### Status-Based Counts with Amounts
- `processedWithdrawalsCount`: Number of processed withdrawals in date range
- `processedAmount`: Total amount processed in rupees
- `processedCoins`: Total coins processed
- `pendingWithdrawalsCount`: Number of pending withdrawals in date range
- `pendingAmount`: Total pending amount in rupees
- `pendingCoins`: Total pending coins
- `rejectedWithdrawalsCount`: Number of rejected withdrawals in date range
- `rejectedAmount`: Total rejected amount in rupees
- `rejectedCoins`: Total rejected coins
- `processingWithdrawalsCount`: Number of processing withdrawals in date range
- `processingAmount`: Total processing amount in rupees
- `processingCoins`: Total processing coins

#### Overall Statistics
- `totalWithdrawalsInRange`: Total withdrawals in the specified date range
- `totalWithdrawalsOverall`: Total withdrawals across all time
- `totalAmountInRange`: Total withdrawal amount in rupees for the date range
- `totalCoinsInRange`: Total coins withdrawn in the date range

#### Overall Status Counts
- `totalPendingWithdrawals`: Total pending withdrawals across all time
- `totalProcessingWithdrawals`: Total processing withdrawals across all time
- `totalProcessedWithdrawals`: Total processed withdrawals across all time
- `totalRejectedWithdrawals`: Total rejected withdrawals across all time

#### Success and Performance Rates
- `successRate`: Percentage of successful withdrawals (processed vs total)
- `rejectionRate`: Percentage of rejected withdrawals
- `processingRate`: Percentage of processing withdrawals
- `pendingRate`: Percentage of pending withdrawals

#### Average Metrics
- `avgWithdrawalAmount`: Average withdrawal amount in rupees for the date range
- `avgProcessedAmount`: Average processed withdrawal amount
- `avgPendingAmount`: Average pending withdrawal amount
- `avgDailyWithdrawals`: Average daily withdrawal count
- `avgDailyProcessed`: Average daily processed withdrawals

#### Growth and Trend Metrics
- `growthRate`: Growth rate compared to previous period
- `amountGrowthRate`: Growth rate of withdrawal amounts compared to previous period

#### Advanced Analytics
- `topWithdrawals`: Top 5 highest withdrawal amounts with details
- `statusDistribution`: Comprehensive status breakdown with counts, amounts, and percentages

## Usage Examples

### Get Dashboard Stats (Last 30 Days)
```bash
curl -X GET "http://localhost:8080/api/admin/dashboard/stats" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### Get Dashboard Stats with Custom Date Range
```bash
curl -X GET "http://localhost:8080/api/admin/dashboard/stats?fromDate=2024-01-01T00:00:00&toDate=2024-01-31T23:59:59" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

## Sample Response

```json
{
  "userStats": { ... },
  "storyStats": { ... },
  "commentStats": { ... },
  "likeStats": { ... },
  "viewStats": { ... },
  "engagementStats": { ... },
  "platformHealth": { ... },
  "withdrawalStats": {
    "totalWithdrawalsInRange": 150,
    "totalWithdrawalsOverall": 1250,
    "totalAmountInRange": 45000.00,
    "totalCoinsInRange": 135000,
    
    "processedWithdrawalsCount": 100,
    "processedAmount": 30000.00,
    "processedCoins": 90000,
    "pendingWithdrawalsCount": 25,
    "pendingAmount": 7500.00,
    "pendingCoins": 22500,
    "rejectedWithdrawalsCount": 10,
    "rejectedAmount": 3000.00,
    "rejectedCoins": 9000,
    "processingWithdrawalsCount": 15,
    "processingAmount": 4500.00,
    "processingCoins": 13500,
    
    "totalPendingWithdrawals": 45,
    "totalProcessingWithdrawals": 30,
    "totalProcessedWithdrawals": 1100,
    "totalRejectedWithdrawals": 75,
    
    "successRate": 66.67,
    "rejectionRate": 6.67,
    "processingRate": 10.0,
    "pendingRate": 16.67,
    
    "avgWithdrawalAmount": 300.00,
    "avgProcessedAmount": 300.00,
    "avgPendingAmount": 300.00,
    "avgDailyWithdrawals": 5.0,
    "avgDailyProcessed": 3.33,
    
    "growthRate": 15.5,
    "amountGrowthRate": 12.3,
    
    "topWithdrawals": [
      {
        "withdrawalId": "w123",
        "userId": "user456",
        "amount": 500.00,
        "coins": 1500,
        "status": "PROCESSED",
        "createdAt": "2024-01-15T10:30:00"
      },
      {
        "withdrawalId": "w124",
        "userId": "user789",
        "amount": 400.00,
        "coins": 1200,
        "status": "PENDING",
        "createdAt": "2024-01-16T14:20:00"
      }
    ],
    
    "statusDistribution": {
      "processed": {
        "count": 100,
        "amount": 30000.00,
        "percentage": 66.67
      },
      "pending": {
        "count": 25,
        "amount": 7500.00,
        "percentage": 16.67
      },
      "processing": {
        "count": 15,
        "amount": 4500.00,
        "percentage": 10.0
      },
      "rejected": {
        "count": 10,
        "amount": 3000.00,
        "percentage": 6.67
      }
    }
  },
  "dateRange": {
    "fromDate": "2024-01-01T00:00:00",
    "toDate": "2024-01-31T23:59:59",
    "durationDays": 30
  }
}
```

## Key Metrics Explained

### Success Rate
- **Calculation**: (Processed Withdrawals / Total Withdrawals) × 100
- **Purpose**: Measures the efficiency of withdrawal processing
- **Target**: Higher percentage indicates better processing efficiency

### Rejection Rate
- **Calculation**: (Rejected Withdrawals / Total Withdrawals) × 100
- **Purpose**: Monitors withdrawal rejection patterns
- **Action**: High rates may indicate policy issues or fraud attempts

### Processing Rate
- **Calculation**: (Processing Withdrawals / Total Withdrawals) × 100
- **Purpose**: Shows current processing workload
- **Insight**: Helps understand processing queue status

### Pending Rate
- **Calculation**: (Pending Withdrawals / Total Withdrawals) × 100
- **Purpose**: Indicates new withdrawal requests
- **Use Case**: Helps plan processing capacity

### Growth Rate
- **Calculation**: ((Current Period - Previous Period) / Previous Period) × 100
- **Purpose**: Shows withdrawal activity trends
- **Interpretation**: Positive values indicate increasing withdrawal activity

### Amount Growth Rate
- **Calculation**: ((Current Amount - Previous Amount) / Previous Amount) × 100
- **Purpose**: Tracks financial growth in withdrawal amounts
- **Insight**: Shows if users are withdrawing larger amounts

### Average Withdrawal Amount
- **Calculation**: Total Amount / Number of Withdrawals
- **Purpose**: Helps understand user withdrawal patterns
- **Use Case**: Can help optimize withdrawal options and thresholds

### Status Distribution
- **Purpose**: Comprehensive breakdown of withdrawal statuses
- **Includes**: Count, amount, and percentage for each status
- **Benefit**: Complete overview of withdrawal pipeline health

## Repository Methods Added

The following methods were added to `WithdrawalRepository`:

```java
// Count withdrawals in date range
long countByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);

// Count withdrawals by status in date range
long countByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus status, LocalDateTime fromDate, LocalDateTime toDate);

// Find withdrawals in date range
List<Withdrawal> findByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);

// Find withdrawals by status in date range
List<Withdrawal> findByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus status, LocalDateTime fromDate, LocalDateTime toDate);
```

## Benefits

1. **Comprehensive Financial Overview**: Provides complete withdrawal activity insights
2. **Performance Monitoring**: Tracks processing efficiency and success rates
3. **Trend Analysis**: Enables analysis of withdrawal patterns over time
4. **Decision Support**: Helps in optimizing withdrawal processes and policies
5. **Real-time Metrics**: Integrates with existing dashboard for unified admin view

## Security Considerations

- **Access Control**: Only admin users can access withdrawal statistics
- **Data Privacy**: Financial data is protected through role-based access
- **Audit Trail**: All withdrawal activities are logged for compliance

## Error Handling

The withdrawal statistics include comprehensive error handling:
- Graceful degradation if calculation errors occur
- Detailed logging for troubleshooting
- Fallback values when data is unavailable

## Future Enhancements

Potential improvements for withdrawal statistics:
1. **Currency Conversion**: Support for multiple currencies
2. **Advanced Analytics**: Machine learning-based fraud detection
3. **Real-time Notifications**: Alerts for unusual withdrawal patterns
4. **Export Functionality**: CSV/PDF export of withdrawal reports
5. **Custom Date Ranges**: More flexible date range selection options 