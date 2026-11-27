# RosterResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**sessionId** | **number** | 회차 ID | [optional] [default to undefined]
**courseTitle** | **string** | 과정 제목 | [optional] [default to undefined]
**startDate** | **string** | 회차 시작일 | [optional] [default to undefined]
**capacity** | **number** | 정원 | [optional] [default to undefined]
**confirmed** | [**Array&lt;RosterEntry&gt;**](RosterEntry.md) | 확정자(CONFIRMED, COMPLETED). 신청 ID 오름차순 | [optional] [default to undefined]
**waiting** | [**Array&lt;RosterEntry&gt;**](RosterEntry.md) | 대기자(WAITING). 신청 ID 오름차순이며 waitingNumber는 1부터 | [optional] [default to undefined]

## Example

```typescript
import { RosterResponse } from './api';

const instance: RosterResponse = {
    sessionId,
    courseTitle,
    startDate,
    capacity,
    confirmed,
    waiting,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
