# EnrollmentApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**cancel**](#cancel) | **POST** /api/me/enrollments/{enrollmentId}/cancel | 내 신청 취소 (확정 취소 시 대기 1순위 자동 확정)|
|[**enroll**](#enroll) | **POST** /api/v1/sessions/{sessionId}/enrollments | 수강 신청 (정원 안이면 확정, 정원이 차면 대기)|
|[**myEnrollments**](#myenrollments) | **GET** /api/me/enrollments | 내 신청 내역 조회 (최근 신청순, 대기 순번 포함)|

# **cancel**
> EnrollmentResponse cancel()


### Example

```typescript
import {
    EnrollmentApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new EnrollmentApi(configuration);

let enrollmentId: number; // (default to undefined)

const { status, data } = await apiInstance.cancel(
    enrollmentId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **enrollmentId** | [**number**] |  | defaults to undefined|


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
|**409** | 이미 취소됨, 취소할 수 없는 상태, 취소 마감, 종료된 회차 (ENROLLMENT_ALREADY_CANCELLED, ENROLLMENT_NOT_CANCELLABLE, CANCEL_DEADLINE_PASSED, SESSION_CLOSED) |  -  |
|**404** | 내 신청이 없음 (ENROLLMENT_NOT_FOUND) |  -  |
|**200** | 취소 성공 (status: CANCELLED) |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **enroll**
> EnrollmentResponse enroll()


### Example

```typescript
import {
    EnrollmentApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new EnrollmentApi(configuration);

let sessionId: number; // (default to undefined)

const { status, data } = await apiInstance.enroll(
    sessionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionId** | [**number**] |  | defaults to undefined|


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
|**201** | 신청 성공 (status: CONFIRMED, 또는 WAITING과 waitingNumber) |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **myEnrollments**
> Array<MyEnrollmentResponse> myEnrollments()


### Example

```typescript
import {
    EnrollmentApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new EnrollmentApi(configuration);

let status: 'CONFIRMED' | 'WAITING' | 'COMPLETED' | 'CANCELLED'; //신청 상태. 생략하면 전체 (optional) (default to undefined)

const { status, data } = await apiInstance.myEnrollments(
    status
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **status** | [**&#39;CONFIRMED&#39; | &#39;WAITING&#39; | &#39;COMPLETED&#39; | &#39;CANCELLED&#39;**]**Array<&#39;CONFIRMED&#39; &#124; &#39;WAITING&#39; &#124; &#39;COMPLETED&#39; &#124; &#39;CANCELLED&#39;>** | 신청 상태. 생략하면 전체 | (optional) defaults to undefined|


### Return type

**Array<MyEnrollmentResponse>**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**401** | 토큰이 없거나 올바르지 않음 (UNAUTHORIZED) |  -  |
|**400** | 상태 값 형식 오류 (INVALID_REQUEST) |  -  |
|**409** | Conflict |  -  |
|**404** | Not Found |  -  |
|**200** | 조회 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

