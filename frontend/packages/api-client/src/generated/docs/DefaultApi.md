# DefaultApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**complete**](#complete) | **POST** /api/admin/sessions/{sessionId}/completions | 수료 처리|
|[**getRoster**](#getroster) | **GET** /api/admin/sessions/{sessionId}/roster | 회차 명단 조회 (확정자·대기자)|

# **complete**
> CompletionResponse complete(completionRequest)

확정자만 수료 처리한다. 일부가 실패해도 나머지는 처리하고 실패 사유를 돌려준다. 회차 시작일(KST) 전에는 할 수 없다.

### Example

```typescript
import {
    DefaultApi,
    Configuration,
    CompletionRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new DefaultApi(configuration);

let sessionId: number; // (default to undefined)
let completionRequest: CompletionRequest; //

const { status, data } = await apiInstance.complete(
    sessionId,
    completionRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **completionRequest** | **CompletionRequest**|  | |
| **sessionId** | [**number**] |  | defaults to undefined|


### Return type

**CompletionResponse**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**401** | 토큰이 없거나 올바르지 않음 (UNAUTHORIZED) |  -  |
|**400** | 임직원 ID 목록이 비었거나 형식이 올바르지 않음 (VALIDATION_FAILED, INVALID_REQUEST) |  -  |
|**409** | 회차가 아직 시작되지 않음 (SESSION_NOT_STARTED) |  -  |
|**404** | 회차가 없음 (SESSION_NOT_FOUND) |  -  |
|**200** | 처리 결과 (수료된 ID와 실패 사유) |  -  |
|**403** | 관리자가 아님 (FORBIDDEN) |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getRoster**
> RosterResponse getRoster()


### Example

```typescript
import {
    DefaultApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new DefaultApi(configuration);

let sessionId: number; // (default to undefined)

const { status, data } = await apiInstance.getRoster(
    sessionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionId** | [**number**] |  | defaults to undefined|


### Return type

**RosterResponse**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**401** | 토큰이 없거나 올바르지 않음 (UNAUTHORIZED) |  -  |
|**400** | Bad Request |  -  |
|**409** | Conflict |  -  |
|**404** | 회차가 없음 (SESSION_NOT_FOUND) |  -  |
|**403** | 관리자가 아님 (FORBIDDEN) |  -  |
|**200** | 명단 (확정자, 대기자와 대기 순번) |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

