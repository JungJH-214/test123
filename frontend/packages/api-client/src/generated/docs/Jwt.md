# Jwt


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**tokenValue** | **string** |  | [optional] [default to undefined]
**issuedAt** | **string** |  | [optional] [default to undefined]
**expiresAt** | **string** |  | [optional] [default to undefined]
**headers** | **{ [key: string]: object; }** |  | [optional] [default to undefined]
**claims** | **{ [key: string]: object; }** |  | [optional] [default to undefined]
**id** | **string** |  | [optional] [default to undefined]
**subject** | **string** |  | [optional] [default to undefined]
**issuer** | **string** |  | [optional] [default to undefined]
**notBefore** | **string** |  | [optional] [default to undefined]
**audience** | **Array&lt;string&gt;** |  | [optional] [default to undefined]

## Example

```typescript
import { Jwt } from './api';

const instance: Jwt = {
    tokenValue,
    issuedAt,
    expiresAt,
    headers,
    claims,
    id,
    subject,
    issuer,
    notBefore,
    audience,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
