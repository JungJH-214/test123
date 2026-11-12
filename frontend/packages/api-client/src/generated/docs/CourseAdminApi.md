# CourseAdminApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**closeCourse**](#closecourse) | **POST** /api/v1/admin/courses/{courseId}/close | 과정 종료 (회차도 모두 종료, 이미 종료돼 있으면 그대로 반환)|
|[**closeSession**](#closesession) | **POST** /api/v1/admin/courses/{courseId}/sessions/{sessionId}/close | 회차 종료 (이미 종료돼 있으면 그대로 반환)|
|[**createCourse**](#createcourse) | **POST** /api/v1/admin/courses | 과정 등록|
|[**createSession**](#createsession) | **POST** /api/v1/admin/courses/{courseId}/sessions | 회차 등록 (종료된 과정은 409)|
|[**updateCourse**](#updatecourse) | **PUT** /api/v1/admin/courses/{courseId} | 과정 수정 (종료된 과정은 409)|
|[**updateSession**](#updatesession) | **PUT** /api/v1/admin/courses/{courseId}/sessions/{sessionId} | 회차 수정 (종료된 회차는 409)|

# **closeCourse**
> CourseDetailResponse closeCourse()


### Example

```typescript
import {
    CourseAdminApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseAdminApi(configuration);

let courseId: number; // (default to undefined)

const { status, data } = await apiInstance.closeCourse(
    courseId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **courseId** | [**number**] |  | defaults to undefined|


### Return type

**CourseDetailResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**409** | Conflict |  -  |
|**404** | 과정이 없음 (COURSE_NOT_FOUND) |  -  |
|**200** | 종료 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **closeSession**
> SessionResponse closeSession()


### Example

```typescript
import {
    CourseAdminApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseAdminApi(configuration);

let courseId: number; // (default to undefined)
let sessionId: number; // (default to undefined)

const { status, data } = await apiInstance.closeSession(
    courseId,
    sessionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **courseId** | [**number**] |  | defaults to undefined|
| **sessionId** | [**number**] |  | defaults to undefined|


### Return type

**SessionResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**409** | Conflict |  -  |
|**404** | 과정 또는 회차가 없음 (COURSE_NOT_FOUND, SESSION_NOT_FOUND) |  -  |
|**200** | 종료 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **createCourse**
> CourseDetailResponse createCourse(courseRequest)


### Example

```typescript
import {
    CourseAdminApi,
    Configuration,
    CourseRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseAdminApi(configuration);

let courseRequest: CourseRequest; //

const { status, data } = await apiInstance.createCourse(
    courseRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **courseRequest** | **CourseRequest**|  | |


### Return type

**CourseDetailResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | 입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST) |  -  |
|**409** | Conflict |  -  |
|**404** | Not Found |  -  |
|**201** | 등록 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **createSession**
> SessionResponse createSession(sessionRequest)


### Example

```typescript
import {
    CourseAdminApi,
    Configuration,
    SessionRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseAdminApi(configuration);

let courseId: number; // (default to undefined)
let sessionRequest: SessionRequest; //

const { status, data } = await apiInstance.createSession(
    courseId,
    sessionRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionRequest** | **SessionRequest**|  | |
| **courseId** | [**number**] |  | defaults to undefined|


### Return type

**SessionResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | 입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST) |  -  |
|**409** | 종료된 과정 (COURSE_CLOSED) |  -  |
|**404** | 과정이 없음 (COURSE_NOT_FOUND) |  -  |
|**201** | 등록 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateCourse**
> CourseDetailResponse updateCourse(courseRequest)


### Example

```typescript
import {
    CourseAdminApi,
    Configuration,
    CourseRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseAdminApi(configuration);

let courseId: number; // (default to undefined)
let courseRequest: CourseRequest; //

const { status, data } = await apiInstance.updateCourse(
    courseId,
    courseRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **courseRequest** | **CourseRequest**|  | |
| **courseId** | [**number**] |  | defaults to undefined|


### Return type

**CourseDetailResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | 입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST) |  -  |
|**409** | 종료된 과정 (COURSE_CLOSED) |  -  |
|**404** | 과정이 없음 (COURSE_NOT_FOUND) |  -  |
|**200** | 수정 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateSession**
> SessionResponse updateSession(sessionRequest)


### Example

```typescript
import {
    CourseAdminApi,
    Configuration,
    SessionRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseAdminApi(configuration);

let courseId: number; // (default to undefined)
let sessionId: number; // (default to undefined)
let sessionRequest: SessionRequest; //

const { status, data } = await apiInstance.updateSession(
    courseId,
    sessionId,
    sessionRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionRequest** | **SessionRequest**|  | |
| **courseId** | [**number**] |  | defaults to undefined|
| **sessionId** | [**number**] |  | defaults to undefined|


### Return type

**SessionResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | 입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST) |  -  |
|**409** | 종료된 회차 (SESSION_CLOSED) |  -  |
|**404** | 과정 또는 회차가 없음 (COURSE_NOT_FOUND, SESSION_NOT_FOUND) |  -  |
|**200** | 수정 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

