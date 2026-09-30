# GHN (Giao Hàng Nhanh) Integration

## Overview

This project integrates with GHN (Giao Hàng Nhanh) API to provide:
- Address management with 2-level (NEW) and 3-level (OLD) support
- Shipping fee calculation
- Location data (provinces, districts, wards)

## Database Changes

### 1. Addresses Table
```sql
-- New GHN fields added to existing Addresses table
ALTER TABLE Addresses
  ADD COLUMN province_id    INT,
  ADD COLUMN district_id    INT,
  ADD COLUMN ward_code      VARCHAR(20),
  ADD COLUMN address_detail VARCHAR(255),
  ADD COLUMN province_name  VARCHAR(100),
  ADD COLUMN district_name  VARCHAR(100),
  ADD COLUMN ward_name      VARCHAR(100),
  ADD COLUMN address_mode   VARCHAR(8) DEFAULT 'OLD' CHECK (address_mode IN ('OLD','NEW'));
```

### 2. Products Table
```sql
-- Shipping dimensions for fee calculation
ALTER TABLE Products
  ADD COLUMN weight_g  INT DEFAULT 500 CHECK (weight_g > 0),
  ADD COLUMN length_cm INT DEFAULT 20  CHECK (length_cm > 0),
  ADD COLUMN width_cm  INT DEFAULT 20  CHECK (width_cm  > 0),
  ADD COLUMN height_cm INT DEFAULT 30  CHECK (height_cm > 0);
```

### 3. Orders Table
```sql
-- Shipping fee tracking
ALTER TABLE Orders
  ADD COLUMN shipping_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (shipping_fee >= 0);
```

## Address Modes

### OLD Mode (3-level)
- **Required**: `province_id`, `district_id`, `ward_code`, `address_detail`
- **Use case**: Full administrative hierarchy
- **Validation**: All fields must be provided

### NEW Mode (2-level) 
- **Required**: `province_id`, `ward_code`, `address_detail`
- **Optional**: `district_id`
- **Use case**: Simplified address after administrative mergers

## API Endpoints

### Location APIs
```
GET /api/locations/provinces
GET /api/locations/districts?province_id={id}
GET /api/locations/wards?district_id={id}
GET /api/locations/status
```

### Address APIs
```
POST /api/addresses                    # Create/update address
GET /api/addresses                     # Get user addresses
GET /api/addresses/default             # Get default address
GET /api/addresses/ghn-compatible      # Get GHN-compatible addresses
DELETE /api/addresses/{id}             # Delete address
```

### Shipping APIs
```
POST /api/shipping/fee                 # Calculate shipping fee
GET /api/shipping/status               # Check service availability
GET /api/shipping/service-types/default # Get default service type
```

## Configuration

### Environment Variables
```bash
# Required
GHN_TOKEN=your_ghn_token_here
GHN_SHOP_ID=your_shop_id_here

# Optional (for shipping fee calculation)
GHN_FROM_PROVINCE_ID=province_id_of_your_shop
GHN_FROM_DISTRICT_ID=district_id_of_your_shop  
GHN_FROM_WARD_CODE=ward_code_of_your_shop
```

### Application Configuration
```yaml
ghn:
  base-url: https://online-gateway.ghn.vn/shiip/public-api
  token: ${GHN_TOKEN:cd8acb1f-9e9c-11f0-b0e8-f2157cd9069b}
  shop-id: ${GHN_SHOP_ID:4983244}
  service-type-id-default: 2
  from:
    province-id: ${GHN_FROM_PROVINCE_ID:}
    district-id: ${GHN_FROM_DISTRICT_ID:}
    ward-code: ${GHN_FROM_WARD_CODE:}
```

## Usage Examples

### 1. Create Address (OLD Mode)
```json
POST /api/addresses
{
  "addressMode": "OLD",
  "provinceId": 202,
  "districtId": 1442,
  "wardCode": "21211",
  "addressDetail": "123 Nguyen Van Linh",
  "provinceName": "TP. Hồ Chí Minh",
  "districtName": "Quận 7", 
  "wardName": "Phường Tân Phú",
  "isDefault": true
}
```

### 2. Create Address (NEW Mode)
```json
POST /api/addresses
{
  "addressMode": "NEW",
  "provinceId": 202,
  "wardCode": "21211",
  "addressDetail": "123 Nguyen Van Linh",
  "provinceName": "TP. Hồ Chí Minh",
  "wardName": "Phường Tân Phú",
  "isDefault": false
}
```

### 3. Calculate Shipping Fee
```json
POST /api/shipping/fee
{
  "addressId": 1,
  "serviceTypeId": 2
}
```

Response:
```json
{
  "data": {
    "shippingFee": 25000,
    "serviceTypeIdUsed": 2,
    "message": "Tính phí thành công",
    "success": true
  },
  "error": null
}
```

## Integration Points

### 1. Checkout Flow
- Validate user has GHN-compatible address
- Calculate shipping fee before order creation
- Include shipping fee in total amount

### 2. Order Creation
- Use `calculateTotalAmountWithShippingFee()` method
- Store shipping fee separately in `shipping_fee` field
- Update total amount to include shipping

### 3. Address Validation
- OLD mode: requires all administrative levels
- NEW mode: allows missing district for merged areas
- Automatic validation in `AddressService`

## Error Handling

### Common Errors
- **Invalid GHN configuration**: Check token and shop ID
- **Address not GHN compatible**: Missing required fields
- **FROM location not configured**: Shipping fee calculation may fail
- **API rate limits**: GHN may throttle requests

### Fallback Behavior
- If GHN API fails, return empty lists for locations
- If shipping calculation fails, return error message
- Cache location data to reduce API calls

## Security Considerations

1. **API Keys**: Store GHN token in environment variables
2. **Rate Limiting**: Implement caching for location data
3. **Validation**: Always validate address data on backend
4. **Authorization**: Ensure users can only access their own addresses

## Testing

### Test Data
Use the provided test credentials:
- Token: `cd8acb1f-9e9c-11f0-b0e8-f2157cd9069b`
- Shop ID: `4983244`

### Test Cases
1. Create addresses in both OLD and NEW modes
2. Calculate shipping fees for different locations
3. Test with missing FROM location configuration
4. Verify address validation rules

## Troubleshooting

### GHN API Issues
1. Check token validity in GHN dashboard
2. Verify shop ID is correct
3. Ensure API endpoints are accessible
4. Check request/response format

### Address Validation
1. Verify required fields for each mode
2. Check province/district/ward code validity
3. Ensure address detail is provided

### Shipping Calculation
1. Verify FROM location is configured
2. Check product dimensions are set
3. Ensure destination address is GHN-compatible

## Future Enhancements

1. **Order Tracking**: Integrate GHN order tracking APIs
2. **Delivery Options**: Support multiple service types
3. **Pickup Points**: Add pickup location support
4. **Bulk Operations**: Batch address validation
5. **Analytics**: Track shipping costs and delivery performance
