
// In production, all API calls are sent to the same origin, and Nginx proxies them.
// In development, we explicitly target the API gateway's exposed port.
export const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_URL ? 
                     `${import.meta.env.VITE_API_GATEWAY_URL}/api/v1` :
                     'http://localhost:8080/api/v1';
                     
const ML_BASE_URL = import.meta.env.VITE_ML_ENDPOINT || 'http://localhost:8081';


/**
 * A unified function for making API calls to the backend gateway.
 * It automatically handles authentication headers, content types, and error formatting.
 * @param {string} path - The API endpoint path, e.g., '/users/1'.
 * @param {object} options - Configuration for the fetch call (method, body, etc.).
 * @returns {Promise<any>} - The JSON response from the API.
 */
const apiCall = async (path, options = {}) => {
  const url = `${API_BASE_URL}${path}`;
  const token = localStorage.getItem('authToken');

  const headers = {
    ...options.headers,
  };

  // Add auth token if it exists
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  const isFormData = options.body instanceof FormData;

  // Don't set Content-Type for FormData, the browser does it best.
  // For other requests, default to application/json.
  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  try {
    const response = await fetch(url, { ...options, headers });

    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch (e) {
        const textResponse = await response.text();
        // Give up on providing a detailed error message if the response is too long
        const shortText = textResponse.length > 500 ? textResponse.substring(0, 500) + '...' : textResponse;
        errorData = { 
          message: `API call failed with status ${response.status}. Server response: ${shortText}` 
        };
      }
      // Throw the full error object, not just a string
      const error = new Error(errorData.message || `API error: ${response.statusText}`);
      error.data = errorData;
      error.status = response.status;
      throw error;
    }

    // Handle responses that might not have a JSON body
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }
    // For file downloads or other non-json responses
    return response;
  } catch (error) {
    console.error(`API call to "${url}" failed:`, error);
    throw error;
  }
};

/**
 * A dedicated function for making API calls directly to the Auth service.
 * This is necessary because the Auth service is not exposed via the API Gateway.
 */
const authApiCall = async (path, options = {}) => {
    const url = `${API_BASE_URL}/auth${path}`;
    const token = localStorage.getItem('authToken');

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(url, { ...options, headers });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({
                message: `Auth API call failed with status ${response.status}`,
            }));
            throw new Error(errorData.message || `Auth API error: ${response.statusText}`);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return response.json();
        }
        return response;
    } catch (error) {
        console.error(`Auth API call to "${url}" failed:`, error);
        throw error;
    }
};

// --- Auth API ---
export const authAPI = {
  login: (credentials) => authApiCall('/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  }),
  register: (userData) => {
    // Only send the fields that the backend SignUpRequest DTO expects
    const { firstName, lastName, username, email, password } = userData;
    return authApiCall('/register', {
      method: 'POST',
      body: JSON.stringify({
        firstName,
        lastName,
        username,
        email,
        password
      }),
    });
  },
  logout: () => authApiCall('/logout', { method: 'POST' }),
  refreshToken: (tokenData) => authApiCall('/refresh', {
    method: 'POST',
    body: JSON.stringify(tokenData),
  }),
  getProfile: () => authApiCall('/profile'),
  updateProfile: (profileData) => authApiCall('/profile', {
      method: 'PUT',
      body: JSON.stringify(profileData),
  }),
};

// --- Users API ---
export const usersAPI = {
  getUserById: (userId) => apiCall(`/users/${userId}`),
  getUserProfile: (userId) => apiCall(`/users/profile/${userId}`),

};

// --- Labs API ---
export const labsAPI = {
  getLabs: (page = 1, limit = 20, search = "", tags = "") => apiCall(`/labs?page=${page}&limit=${limit}&text=${encodeURIComponent(search)}&tags=${tags}`),
  getMyLabs: (page = 1, limit = 20) => apiCall(`/labs/my?page=${page}&limit=${limit}`),
  getLabById: (labId) => apiCall(`/labs/${labId}`),
  createLab: (formData) => apiCall('/labs', {
    method: 'POST',
    body: formData, // FormData for multipart/form-data uploads
  }),
  updateLab: (labId, labData) => apiCall(`/labs/${labId}`, {
    method: 'PUT',
    body: JSON.stringify(labData),
  }),
  deleteLab: (labId) => apiCall(`/labs/${labId}`, { method: 'DELETE' }),
  getLabAssets: (labId) => apiCall(`/labs/${labId}/assets`),
  uploadLabAsset: (labId, formData) => apiCall(`/labs/${labId}/assets/upload`, {
    method: 'POST',
    body: formData,
  }),
  downloadLabAsset: (labId, assetId) => apiCall(`/labs/${labId}/assets/${assetId}/download`),
  searchLabs: (query, page = 1, limit = 20, tags = '') => 
    apiCall(`/labs?text=${encodeURIComponent(query)}&page=${page}&limit=${limit}&tags=${tags}`),
};

// --- Articles API ---
export const articlesAPI = {
  getArticles: (page = 1, limit = 20, search = "", tags = "") => apiCall(`/articles?page=${page}&limit=${limit}&text=${encodeURIComponent(search)}&tags=${tags}`),
  getMyArticles: (page = 1, limit = 20) => apiCall(`/articles/my?page=${page}&limit=${limit}`),
  getArticleById: (articleId) => apiCall(`/articles/${articleId}`),
  createArticle: (formData) => apiCall('/articles', {
    method: 'POST',
    body: formData,
  }),
  deleteArticle: (articleId) => apiCall(`/articles/${articleId}`, { method: 'DELETE' }),
  searchArticles: (query, page = 1, limit = 20, tags = '') => 
    apiCall(`/articles?text=${encodeURIComponent(query)}&page=${page}&limit=${limit}&tags=${tags}`),
};

// --- Submissions API ---
export const submissionsAPI = {
  getSubmissionsForReview: (page = 1, limit = 20) => apiCall(`/submissions/review?page=${page}&limit=${limit}`),
  getLabSubmissions: (labId, page = 1, limit = 20) => apiCall(`/submissions/lab/${labId}?page=${page}&limit=${limit}`),
  getSubmissionById: (submissionId) => apiCall(`/submissions/${submissionId}`),
  getMySubmissions: (page = 1, limit = 20) => apiCall(`/submissions/my?page=${page}&limit=${limit}`),
  submitLabSolution: async (labId, solutionText, files) => {
    const formData = new FormData();
    formData.append('labId', labId);
    formData.append('textComment', solutionText);

    if (files && files.length > 0) {
      files.forEach(file => {
        formData.append('files', file);
      });
    }

    return apiCall('/submissions', {
      method: 'POST',
      body: formData,
    });
  },
  updateSubmission: (submissionId, submissionData) => apiCall(`/submissions/${submissionId}`, {
    method: 'PUT',
    body: JSON.stringify(submissionData),
  }),
  deleteSubmission: (submissionId) => apiCall(`/submissions/${submissionId}`, { method: 'DELETE' }),
};

// --- ML API ---
export const mlAPI = {
  getChatHistory: async (uuid, assignment_id) => {
    const resp = await fetch(`${ML_BASE_URL}/get_chat_history?uuid=${uuid}&assignment_id=${assignment_id}`, {
      headers: { 'Content-Type': 'application/json' }
    });
    if (!resp.ok) {
      let errorText = await resp.text();
      throw new Error(errorText);
    }
    return resp.json();
  },
  askAgent: async (uuid, assignment_id, content) => {
    const resp = await fetch(`${ML_BASE_URL}/ask`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uuid, assignment_id, content }),
    });
    if (!resp.ok) {
      let errorText = await resp.text();
      throw new Error(errorText);
    }
    return resp.json();
  },
  startAutoGrading: async ({ uuid, assignment_id, submission_id }) => await fetch(`${ML_BASE_URL}/auto_grade_submission`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ uuid, assignment_id, submission_id}),
  }),
  getGradingResult: async ({ uuid, assignment_id, submission_id }) => {
    const params = new URLSearchParams({
      uuid: String(uuid),
      assignment_id: String(assignment_id),
      submission_id: String(submission_id),
    });
    const resp = await fetch(`${ML_BASE_URL}/get_auto_grade_result?${params.toString()}`, {
      headers: { 'Content-Type': 'application/json' }
    });
    if (!resp.ok) {
      let errorText = await resp.text();
      throw new Error(errorText);
    }
    return resp.json();
  },
  getGradingStatus: async ({ uuid, assignment_id, submission_id, webhook_url }) => {
    const params = new URLSearchParams({
      uuid: String(uuid),
      assignment_id: String(assignment_id),
      submission_id: String(submission_id),
      webhook_url: String(webhook_url)
    });
    const resp = await fetch(`${ML_BASE_URL}/get_auto_grade_status?${params.toString()}`, {
      headers: { 'Content-Type': 'application/json' }
    });
    if (!resp.ok) {
      let errorText = await resp.text();
      throw new Error(errorText);
    }
    return resp.json();
  },
  indexAssignment: async (assignment_id) => {
    const formData = new FormData();
    console.log("ID", assignment_id)
    formData.append("assignment_id", String(assignment_id));


    const resp = await fetch(`${ML_BASE_URL}/index_assignment`, {
      method: 'POST',
      body: formData
    });
    if (!resp.ok) {
      let errorText = await resp.text();
      throw new Error(errorText);
    }
    return;
  },
};

// --- Feedback API ---
export const feedbackAPI = {
  createFeedback: (formData) => apiCall('/feedback', {
    method: 'POST',
    body: formData, 
  }),
  deleteFeedback: (feedbackId) => apiCall(`/feedback/${feedbackId}`, {
    method: 'DELETE',
  }),
  getMyFeedbackForSubmission: (submissionId) => apiCall(`/feedback/my/${submissionId}`),
  //Получение фидбеков для студента (которые он получил)
  listMyFeedbacks: (page = 1, limit = 20) => apiCall(`/feedback/my?page=${page}&limit=${limit}`),
  listMyCreatedFeedbacks: (reviewerId, page = 1, limit = 20) => apiCall(`/feedback/reviewer/${reviewerId}?page=${page}&limit=${limit}`),
  getFeedbackById: (feedbackId) => apiCall(`/feedback/${feedbackId}`),
  listStudentFeedbacks: (studentId, page = 1, limit = 20) => apiCall(`/feedback/student/${studentId}?page=${page}&limit=${limit}`),
  listReviewerFeedbacks: (reviewerId, submissionId = null, page = 1, limit = 20) => {
    let url = `/feedback/reviewer/${reviewerId}?page=${page}&limit=${limit}`;
    if (submissionId) {
      url += `&submissionId=${submissionId}`;
    }
    return apiCall(url);
  },
};

const MARIMO_API_BASE_URL = 'http://localhost:8084/api/v1';

/**
 * A dedicated function for making API calls directly to the Marimo Manager service.
 * This is necessary because the Marimo service is not exposed via the API Gateway.
 * @param {string} path - The API endpoint path, e.g., '/components'.
 * @param {object} options - Configuration for the fetch call (method, body, etc.).
 * @returns {Promise<any>} - The JSON response from the API.
 */
const marimoApiCall = async (path, options = {}) => {
  const url = `${MARIMO_API_BASE_URL}${path}`;
  const token = localStorage.getItem('authToken');

  const headers = {
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  const isFormData = options.body instanceof FormData;

  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  try {
    const response = await fetch(url, { ...options, headers });

    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch (e) {
        errorData = { message: `Marimo API call failed with status ${response.status}.` };
      }
      throw new Error(errorData.message || `API error: ${response.statusText}`);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }
    return response;
  } catch (error) {
    console.error(`Marimo API call to "${url}" failed:`, error);
    throw error;
  }
};

// --- Marimo API ---
export const marimoAPI = {
  // Component Management
  createComponent: (data) => marimoApiCall('/marimo/components', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  getComponent: (componentId) => marimoApiCall(`/marimo/components/${componentId}`),
  getComponentsByContent: (contentType, contentId) => marimoApiCall(`/marimo/components/${contentType}/${contentId}`),
  updateComponentCode: (componentId, code) => marimoApiCall(`/marimo/components/${componentId}/code`, {
    method: 'PUT',
    headers: { 'Content-Type': 'text/plain' },
    body: code,
  }),
  deleteComponent: (componentId) => marimoApiCall(`/marimo/components/${componentId}`, {
    method: 'DELETE'
  }),

  // Asset Management
  uploadAsset: (formData) => marimoApiCall('/marimo/assets/upload', {
    method: 'POST',
    body: formData,
  }),
  getAssets: (componentId) => marimoApiCall(`/marimo/assets/component/${componentId}`),
  downloadAsset: (assetId) => marimoApiCall(`/marimo/assets/${assetId}/download`),
  deleteAsset: (assetId) => marimoApiCall(`/marimo/assets/${assetId}`, {
    method: 'DELETE'
  }),

  // Session Management
  createSession: (componentId) => marimoApiCall('/marimo/sessions', {
    method: 'POST',
    body: JSON.stringify({ componentId }),
  }),
  closeSession: (sessionId) => marimoApiCall(`/marimo/sessions/${sessionId}/close`, {
    method: 'POST'
  }),

  // Execution
  runCode: (sessionId, code, cellId = '0') => marimoApiCall(`/marimo/sessions/${sessionId}/execute`, {
    method: 'POST',
    body: JSON.stringify({ code, cellId }),
  }),
  setUIElementValue: (sessionId, data) => marimoApiCall(`/marimo/sessions/${sessionId}/set-ui-element`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),

  // Widget Management
  updateWidgetValue: (sessionId, widgetId, value) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/value`, {
    method: 'PUT',
    body: JSON.stringify({ value }),
  }),

  // Batch widget updates for performance
  batchUpdateWidgets: (sessionId, updates) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/batch`, {
    method: 'PUT',
    body: JSON.stringify({ updates }),
  }),
};

// --- Comments API ---
export const commentsAPI = {
  createComment: (labId, commentData) => apiCall(`/labs/${labId}/comments`, {
    method: 'POST',
    body: JSON.stringify(commentData),
  }),
  getLabComments: (labId, page = 1, limit = 20) => apiCall(`/labs/${labId}/comments?page=${page}&limit=${limit}`),
  getCommentById: (commentId) => apiCall(`/comments/${commentId}`),
  getCommentReplies: (commentId, page = 1, size = 20) => apiCall(`/comments/${commentId}/replies?page=${page}&size=${size}`),
  updateComment: (commentId, content) => {
    const payload = { content };
    return apiCall(`/comments/${commentId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },
  deleteComment: (commentId) => apiCall(`/comments/${commentId}`, {
    method: 'DELETE',
  }),
  // Article comments
  createArticleComment: (articleId, commentData) => apiCall(`/articles/${articleId}/comments`, {
    method: 'POST',
    body: JSON.stringify(commentData),
  }),
  getArticleComments: (articleId, page = 1, limit = 20) => apiCall(`/articles/${articleId}/comments?page=${page}&limit=${limit}`),
};

// --- Tags API ---
export const tagsAPI = {
  createTag: (tagData) => apiCall('/tags', {
    method: 'POST',
    body: JSON.stringify(tagData),
  }),
  getTagById: (tagId) => apiCall(`/tags/${tagId}`),
  getTagsByIds: (ids) => apiCall('/tags/by-ids', {
    method: 'POST',
    body: JSON.stringify({ ids }),
  }),
  getTags: (page = 0, limit = 50) => apiCall(`/tags?page=${page}&limit=${limit}`),
  updateTag: (tagData) => apiCall('/tags/update', {
    method: 'PUT',
    body: JSON.stringify(tagData),
  }),
  deleteTag: (tagId) => apiCall(`/tags/${tagId}`, { method: 'DELETE' }),

};

// --- Statistics API ---
export const statisticsAPI = {
  getStatistics: () => apiCall('/statistics'),
};

export const marimo = {
  // Component Management
  createComponent: (data) => apiCall('/marimo/components', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  getComponent: (id) => apiCall(`/marimo/components/${id}`),
  updateComponent: (id, data) => apiCall(`/marimo/components/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  deleteComponent: (id) => apiCall(`/marimo/components/${id}`, { method: 'DELETE' }),
  listComponents: (params) => apiCall('/marimo/components', { params }),
  searchComponents: (params) => apiCall('/marimo/components/search', { params }),

  // Session Management
  startSession: (data) => apiCall('/marimo/sessions', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  endSession: (id) => apiCall(`/marimo/sessions/${id}`, { method: 'DELETE' }),
  getSessionStatus: (id) => apiCall(`/marimo/sessions/${id}`),
  listUserSessions: (params) => apiCall('/marimo/sessions', { params }),
  getExecutionHistory: (sessionId, params) => apiCall(`/marimo/sessions/${sessionId}/history`, { params }),
  getSessionVariables: (sessionId) => apiCall(`/marimo/sessions/${sessionId}/variables`),

  // Code Execution
  executeCell: (sessionId, data) => apiCall(`/marimo/sessions/${sessionId}/execute`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),

  // Widget Management
  updateWidgetValue: (sessionId, widgetId, value) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/value`, {
    method: 'PUT',
    body: JSON.stringify({ value }),
  }),

  // Batch widget updates for performance
  batchUpdateWidgets: (sessionId, updates) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/batch`, {
    method: 'PUT',
    body: JSON.stringify({ updates }),
  }),

  // Widget analytics and performance
  getWidgetAnalytics: (sessionId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/analytics`),
  
  // Widget state management
  getWidgetState: (sessionId, widgetId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/state`),
  
  // Widget constraints and validation
  getWidgetConstraints: (sessionId, widgetId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/constraints`),

  // Phase 5: Widget persistence endpoints
  saveWidgetState: (sessionId, widgetId, state) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/state`, {
    method: 'POST',
    body: JSON.stringify({ state }),
  }),

  loadWidgetState: (sessionId, widgetId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/state`),

  deleteWidgetState: (sessionId, widgetId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/state`, {
    method: 'DELETE',
  }),

  batchSaveWidgetStates: (batchData) => marimoApiCall('/marimo/widgets/states/batch', {
    method: 'POST',
    body: JSON.stringify({ states: batchData }),
  }),

  batchLoadWidgetStates: (sessionId, widgetIds) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/states/batch`, {
    method: 'POST',
    body: JSON.stringify({ widgetIds }),
  }),

  clearSessionWidgetStates: (sessionId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/states`, {
    method: 'DELETE',
  }),

  // Phase 5: Widget versioning endpoints
  getWidgetVersions: (sessionId, widgetId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/versions`),

  createWidgetVersion: (sessionId, widgetId, version) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/versions`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  }),

  revertWidgetToVersion: (sessionId, widgetId, versionId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/versions/${versionId}/revert`, {
    method: 'POST',
  }),

  // Phase 5: Widget templates endpoints
  getWidgetTemplates: (category = null) => marimoApiCall(`/marimo/templates${category ? `?category=${category}` : ''}`),

  saveWidgetTemplate: (template) => marimoApiCall('/marimo/templates', {
    method: 'POST',
    body: JSON.stringify(template),
  }),

  deleteWidgetTemplate: (templateId) => marimoApiCall(`/marimo/templates/${templateId}`, {
    method: 'DELETE',
  }),

  createWidgetFromTemplate: (templateId, overrides = {}) => marimoApiCall(`/marimo/templates/${templateId}/create`, {
    method: 'POST',
    body: JSON.stringify({ overrides }),
  }),

  // Phase 5: Widget collaboration endpoints
  getSessionCollaborators: (sessionId) => marimoApiCall(`/marimo/sessions/${sessionId}/collaborators`),

  requestWidgetLock: (sessionId, widgetId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/lock`, {
    method: 'POST',
  }),

  releaseWidgetLock: (sessionId, widgetId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/${widgetId}/lock`, {
    method: 'DELETE',
  }),

  getWidgetLocks: (sessionId) => marimoApiCall(`/marimo/sessions/${sessionId}/widgets/locks`),

  // Asset Management
  uploadAsset: (formData) => apiCall('/marimo/assets/upload', {
    method: 'POST',
    body: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  }),
  getAssetInfo: (id) => apiCall(`/marimo/assets/${id}`),
  downloadAsset: (id) => apiCall(`/marimo/assets/${id}/download`, { responseType: 'blob' }),
  listAssets: (componentId) => apiCall(`/marimo/components/${componentId}/assets`),
  deleteAsset: (id) => apiCall(`/marimo/assets/${id}`, { method: 'DELETE' }),
};