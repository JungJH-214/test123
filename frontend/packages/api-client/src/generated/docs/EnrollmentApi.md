# EnrollmentApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**enroll**](#enroll) | **POST** /api/v1/sessions/{sessionId}/enrollments | 수강 신청 (정원 안이면 확정, 정원이 차면 대기)|

# **enroll**
> EnrollmentResponse enroll()


### Example

```typescript
import {
    EnrollmentApi,
    Configuration,
    Jwt
} from './api';

const configuration = new Configuration();
const apiInstance = new EnrollmentApi(configuration);

let sessionId: number; // (default to undefined)
let jwt: Jwt; // (default to undefined)

const { status, data } = await apiInstance.enroll(
    sessionId,
    jwt
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionId** | [**number**] |  | defaults to undefined|
| **jwt** | **Jwt** |  | defaults to undefined|


### Return type

**EnrollmentResponse**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**401** | 토큰이 없거나 올바르지 않음 (UNAUTHORIZED) |  -  |
|**400** | 경로 값 형식 오류 (INVALID_REQUEST) |  -  |
|**409** | 종료된 회차, 중복 신청, 정원·대기 마감 (SESSION_CLOSED, ENROLLMENT_DUPLICATE, SESSION_FULL) |  -  |
|**404** | 회차가 없음 (SESSION_NOT_FOUND) |  -  |
|**201** | 신청 성공 (status: CONFIRMED 또는 WAITING) |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

