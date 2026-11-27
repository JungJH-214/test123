# CompletionResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**completed** | **Array&lt;number&gt;** | 수료 처리된 임직원 ID. 요청 순서 | [optional] [default to undefined]
**failed** | [**Array&lt;CompletionFailure&gt;**](CompletionFailure.md) | 수료 처리하지 못한 임직원과 사유. 요청 순서 | [optional] [default to undefined]

## Example

```typescript
import { CompletionResponse } from './api';

const instance: CompletionResponse = {
    completed,
    failed,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
