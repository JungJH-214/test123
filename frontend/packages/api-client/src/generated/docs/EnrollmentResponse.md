# EnrollmentResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **number** |  | [optional] [default to undefined]
**sessionId** | **number** |  | [optional] [default to undefined]
**employeeId** | **number** |  | [optional] [default to undefined]
**status** | **string** |  | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]
**waitingNumber** | **number** | 대기 순번. WAITING일 때만 값이 있고(1부터), 신청 시점 기준이다 | [optional] [default to undefined]

## Example

```typescript
import { EnrollmentResponse } from './api';

const instance: EnrollmentResponse = {
    id,
    sessionId,
    employeeId,
    status,
    createdAt,
    waitingNumber,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
