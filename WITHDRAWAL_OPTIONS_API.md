# Withdrawal Options API

## Overview

The Withdrawal Options API provides users with available withdrawal amounts, corresponding coin requirements, eligibility status, and default processing time. This API helps users understand their withdrawal options before making a withdrawal request.

## API Endpoint

### GET /api/withdrawals/options

**Description**: Get available withdrawal options for the authenticated user

**Authentication**: Required

**Response**: List of withdrawal options with amounts, coins, eligibility, and processing time

## Implementation Details

### WithdrawalOptionResponse DTO

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalOptionResponse {
    private BigDecimal amount;           // Withdrawal amount in rupees
    private Integer coins;               // Required coins for this amount
    private boolean isEligible;         // Whether user is eligible for withdrawal
}
```

### WithdrawalOptionsResponse DTO

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalOptionsResponse {
    private List<WithdrawalOptionResponse> options;  // List of withdrawal options
    private String defaultProcessingTime;             // Common processing time for all options
    private boolean pauseWithdrawals;                 // Whether withdrawals are paused
    private String pauseWithdrawalsReason;            // Reason for pausing withdrawals
    private String withdrawalConditions;              // Conditions users must meet for withdrawal
}
```

### Hardcoded Amounts

The API provides four fixed withdrawal amounts:
- **₹30** (30 rupees)
- **₹90** (90 rupees)
- **₹190** (190 rupees)
- **₹500** (500 rupees)

### Coin Calculation

Coins are calculated using the conversion rate from the default configuration:

```java
// Get conversion rate from config
String rateString = defaultConfigService.getByKey("1_rupee_equals_in_coins").getValue();
BigDecimal coinToRupeeRate = new BigDecimal(rateString);

// Calculate coins for each amount
Integer coins = amount.multiply(coinToRupeeRate).intValue();
```

### Eligibility Logic

A user is eligible for a specific withdrawal amount if they **don't have an existing withdrawal for that exact amount**:

```java
// Format amount to match the stored format (2 decimal places)
BigDecimal formattedAmount = amount.setScale(2, RoundingMode.HALF_UP);

// Check if user has already withdrawn this specific amount
boolean hasExistingWithdrawalForAmount = withdrawalRepository.existsByUserIdAndMoneyInRs(userId, formattedAmount);

// User is eligible if they don't have an existing withdrawal for this specific amount
boolean isEligible = !hasExistingWithdrawalForAmount;
```

### Processing Time

The default processing time is fetched from the default configuration:

```java
String defaultProcessingTime = defaultConfigService.getByKey("default_payment_processing_time").getValue();
```

**Fallback**: "3-5 business days" if configuration is not found

### Pause Configuration

The withdrawal pause status and reason are fetched from the default configuration:

```java
boolean pauseWithdrawals = Boolean.parseBoolean(defaultConfigService.getByKey("pause_withdrawls").getValue());
String pauseWithdrawalsReason = defaultConfigService.getByKey("pause_withdrawls_reason").getValue();
```

**Fallback**: `false` for pause status and "Withdrawals are temporarily paused" for reason if configuration is not found

### Withdrawal Conditions

The withdrawal conditions are fetched from the default configuration:

```java
String withdrawalConditions = defaultConfigService.getByKey("withdrawalConditions").getValue();
```

**Fallback**: "No specific conditions for withdrawal." if configuration is not found

## Response Examples

### Successful Response

```json
{
  "success": true,
  "data": {
    "options": [
      {
        "amount": 30.00,
        "coins": 60,
        "isEligible": true
      },
      {
        "amount": 90.00,
        "coins": 180,
        "isEligible": true
      },
      {
        "amount": 190.00,
        "coins": 380,
        "isEligible": true
      },
      {
        "amount": 500.00,
        "coins": 1000,
        "isEligible": true
      }
    ],
    "defaultProcessingTime": "24 hours",
    "pauseWithdrawals": false,
    "pauseWithdrawalsReason": "Withdrawals are temporarily paused",
    "withdrawalConditions": "You must upload at least one active story before you can withdraw coins. Only users who have contributed content to the platform are eligible for withdrawals."
  }
}
```

### User with Existing Withdrawal for ₹30 (Only ₹30 Not Eligible)

```json
{
  "success": true,
  "data": {
    "options": [
      {
        "amount": 30.00,
        "coins": 60,
        "isEligible": false
      },
      {
        "amount": 90.00,
        "coins": 180,
        "isEligible": true
      },
      {
        "amount": 190.00,
        "coins": 380,
        "isEligible": true
      },
      {
        "amount": 500.00,
        "coins": 1000,
        "isEligible": true
      }
    ],
    "defaultProcessingTime": "24 hours",
    "pauseWithdrawals": false,
    "pauseWithdrawalsReason": "Withdrawals are temporarily paused",
    "withdrawalConditions": "You must upload at least one active story before you can withdraw coins. Only users who have contributed content to the platform are eligible for withdrawals."
  }
}
```

### Error Response

```json
{
  "success": false,
  "error": "User not found",
  "message": "Failed to retrieve withdrawal options"
}
```

## Configuration Requirements

### Default Configuration Keys

The API requires the following configuration keys in the `default_config` collection:

1. **`1_rupee_equals_in_coins`**: Conversion rate for rupees to coins
   - Example: "2" (1 rupee = 2 coins)
   - Fallback: 2 coins per rupee

2. **`default_payment_processing_time`**: Default processing time message
   - Example: "24 hours"
   - Fallback: "3-5 business days"

3. **`pause_withdrawls`**: Whether withdrawals are paused
   - Example: "true" or "false"
   - Fallback: false

4. **`pause_withdrawls_reason`**: Reason for pausing withdrawals
   - Example: "due to technical issues we paused withdrawls"
   - Fallback: "Withdrawals are temporarily paused"

5. **`withdrawalConditions`**: Conditions users must meet for withdrawal
   - Example: "You must upload at least one active story before you can withdraw coins"
   - Fallback: "No specific conditions for withdrawal."

### Configuration Setup

```javascript
// Example MongoDB documents for default_config collection
{
  "key": "1_rupee_equals_in_coins",
  "value": "2",
  "description": "Number of coins equal to 1 rupee",
  "active": true
}

{
  "key": "default_payment_processing_time",
  "value": "24 hours",
  "description": "Default processing time for withdrawals",
  "active": true
}

{
  "key": "pause_withdrawls",
  "value": "true",
  "description": "Allow pause withdrawls",
  "active": true
}

{
  "key": "pause_withdrawls_reason",
  "value": "due to technical issues we paused withdrawls",
  "description": "Reason for pause withdrawls",
  "active": true
}

{
  "key": "withdrawalConditions",
  "value": "You must upload at least one active story before you can withdraw coins. Only users who have contributed content to the platform are eligible for withdrawals.",
  "description": "Conditions that users must meet to be eligible for withdrawal",
  "active": true
}
```

## Business Logic

### Eligibility Rules

1. **Amount-specific eligibility**: Each withdrawal amount is checked independently
2. **Users with existing withdrawal for specific amount**: Not eligible for that amount only
3. **Users without existing withdrawal for specific amount**: Eligible for that amount
4. **Decimal format consistency**: Amounts are stored and compared with 2 decimal places (e.g., "30.00")

### Coin Calculation

- **Formula**: `coins = amount × conversion_rate`
- **Example**: If 1 rupee = 2 coins, then ₹30 = 60 coins
- **Rounding**: Uses integer division (no decimal coins)

### Processing Time

- **Configurable**: Can be updated via default configuration
- **Consistent**: Same processing time for all withdrawal amounts
- **Fallback**: Default message if configuration is missing

## Integration Points

### Used By

- **Frontend Applications**: To display withdrawal options to users
- **Mobile Apps**: To show available withdrawal amounts and requirements
- **User Dashboard**: To inform users about withdrawal eligibility

### Dependencies

- **WithdrawalRepository**: To check existing withdrawals
- **DefaultConfigService**: To fetch conversion rate and processing time
- **UserService**: To get authenticated user information

## Error Handling

### Configuration Errors

- **Missing conversion rate**: Falls back to 2 coins per rupee
- **Missing processing time**: Falls back to "3-5 business days"
- **Invalid conversion rate**: Uses fallback value

### User Errors

- **User not found**: Returns error response
- **Database connection issues**: Returns error response

## Testing

### Unit Tests

```java
@Test
public void testGetWithdrawalOptionsForEligibleUser() {
    // Given: User with no existing withdrawals
    String userId = "user123";
    
    // When: Getting withdrawal options
    List<WithdrawalOptionResponse> options = withdrawalService.getWithdrawalOptions(userId);
    
    // Then: All options should be eligible
    assertThat(options).hasSize(4);
    options.forEach(option -> assertThat(option.isEligible()).isTrue());
}

@Test
public void testGetWithdrawalOptionsForIneligibleUser() {
    // Given: User with existing withdrawal
    String userId = "user456";
    // Mock existing withdrawal
    
    // When: Getting withdrawal options
    List<WithdrawalOptionResponse> options = withdrawalService.getWithdrawalOptions(userId);
    
    // Then: All options should be ineligible
    assertThat(options).hasSize(4);
    options.forEach(option -> assertThat(option.isEligible()).isFalse());
}
```

### Integration Tests

```java
@Test
public void testWithdrawalOptionsAPI() {
    // Given: Authenticated user
    String token = getAuthToken("user@example.com");
    
    // When: Calling withdrawal options API
    ResponseEntity<Map<String, Object>> response = 
        restTemplate.getForEntity("/api/withdrawals/options", Map.class);
    
    // Then: Should return successful response with options
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("success")).isEqualTo(true);
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> options = (List<Map<String, Object>>) response.getBody().get("data");
    assertThat(options).hasSize(4);
}
```

## Security Considerations

- **Authentication Required**: Only authenticated users can access the API
- **User Isolation**: Users can only see their own eligibility status
- **No Sensitive Data**: API doesn't expose user's coin balance or personal information

## Performance Considerations

- **Caching**: Consider caching configuration values for better performance
- **Database Queries**: Single query to check existing withdrawals
- **Memory Usage**: Minimal memory footprint for response generation

## Future Enhancements

1. **Dynamic Amounts**: Allow configuration of withdrawal amounts
2. **Tiered Eligibility**: Different eligibility rules for different amounts
3. **Processing Time by Amount**: Different processing times for different amounts
4. **Minimum Balance Check**: Check if user has enough coins for each amount
5. **Geographic Restrictions**: Different options based on user location 