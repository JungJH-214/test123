# MyEnrollmentResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **number** | 신청 ID | [optional] [default to undefined]
**sessionId** | **number** | 회차 ID | [optional] [default to undefined]
**courseTitle** | **string** | 과정 제목 | [optional] [default to undefined]
**startDate** | **string** | 회차 시작일 | [optional] [default to undefined]
**place** | **string** | 회차 장소 | [optional] [default to undefined]
**status** | **string** | 신청 상태 | [optional] [default to undefined]
**waitingNumber** | **number** | 대기 순번 (WAITING일 때만, 1부터) | [optional] [default to undefined]
**createdAt** | **string** | 신청 일시 | [optional] [default to undefined]

## Example

```typescript
import { MyEnrollmentResponse } from './api';

const instance: MyEnrollmentResponse = {
    id,
    sessionId,
    courseTitle,
    startDate,
    place,
    status,
    waitingNumber,
    createdAt,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
