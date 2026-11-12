import { Configuration, CourseApi } from '@eduhub/api-client';

// CourseApi 인스턴스는 여기서만 만든다. basePath ''는 같은 출처의 /api로 요청한다 (개발 중에는 Vite 프록시).
export const courseApi = new CourseApi(new Configuration({ basePath: '' }));
