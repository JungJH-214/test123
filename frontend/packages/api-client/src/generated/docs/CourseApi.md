# CourseApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getCourse**](#getcourse) | **GET** /api/v1/courses/{id} | 과정 상세 조회 (회차 목록, 남은 자리 포함)|
|[**getCourses**](#getcourses) | **GET** /api/v1/courses | 과정 목록 조회 (제목 검색, 분류 필터, 페이지는 0부터)|

# **getCourse**
> CourseDetailResponse getCourse()


### Example

```typescript
import {
    CourseApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getCourse(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


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
|**404** | 과정이 없음 |  -  |
|**200** | 조회 성공 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getCourses**
> PageResponseCourseSummaryResponse getCourses()


### Example

```typescript
import {
    CourseApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new CourseApi(configuration);

let keyword: string; // (optional) (default to undefined)
let category: string; // (optional) (default to undefined)
let page: number; //Zero-based page index (0..N) (optional) (default to 0)
let size: number; //The size of the page to be returned (optional) (default to 20)
let sort: Array<string>; //Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. (optional) (default to undefined)

const { status, data } = await apiInstance.getCourses(
    keyword,
    category,
    page,
    size,
    sort
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **keyword** | [**string**] |  | (optional) defaults to undefined|
| **category** | [**string**] |  | (optional) defaults to undefined|
| **page** | [**number**] | Zero-based page index (0..N) | (optional) defaults to 0|
| **size** | [**number**] | The size of the page to be returned | (optional) defaults to 20|
| **sort** | **Array&lt;string&gt;** | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | (optional) defaults to undefined|


### Return type

**PageResponseCourseSummaryResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**404** | Not Found |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

