# RosterEntry

대기자(WAITING). 신청 ID 오름차순이며 waitingNumber는 1부터

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**enrollmentId** | **number** | 신청 ID | [optional] [default to undefined]
**employeeId** | **number** | 임직원 ID | [optional] [default to undefined]
**employeeNo** | **string** | 사번. 임직원 정보가 없으면 null | [optional] [default to undefined]
**name** | **string** | 이름. 임직원 정보가 없으면 null | [optional] [default to undefined]
**status** | **string** | 신청 상태 (CONFIRMED, COMPLETED, WAITING) | [optional] [default to undefined]
**enrolledAt** | **string** | 신청 시각 | [optional] [default to undefined]
**waitingNumber** | **number** | 대기 순번(1부터, 신청 ID 오름차순). 대기자만 값이 있고 나머지는 null | [optional] [default to undefined]

## Example

```typescript
import { RosterEntry } from './api';

const instance: RosterEntry = {
    enrollmentId,
    employeeId,
    employeeNo,
    name,
    status,
    enrolledAt,
    waitingNumber,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
