// In production, all API calls are sent to the same origin, and Nginx proxies them.
// In development, we explicitly target the API gateway's exposed port.
const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_ENDPOINT || (import.meta.env.PROD ? '' : 'http://localhost:8080/api/v1');
const AUTH_SERVICE_BASE_URL = import.meta.env.VITE_AUTH_API_ENDPOINT || (import.meta.env.PROD ? '' : 'http://localhost:8081/api/v1/auth');

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
      throw new Error(errorData.message || `API error: ${response.statusText}`);
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
    const url = `${AUTH_SERVICE_BASE_URL}${path}`;
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
  getAllUsers: (page = 1, limit = 20) => apiCall(`/users?page=${page}&limit=${limit}`),
  deleteUser: (userId) => apiCall(`/users/${userId}`, { method: 'DELETE' }),
  getUserLabs: (userId, page = 1, limit = 20) => apiCall(`/users/${userId}/labs?page=${page}&limit=${limit}`),
  getUserArticles: (userId, page = 1, limit = 20) => apiCall(`/users/${userId}/articles?page=${page}&limit=${limit}`),
};

// --- Labs API ---
export const labsAPI = {
  getLabs: (page = 1, limit = 20) => apiCall(`/labs?page=${page}&limit=${limit}`),
  getMyLabs: (page = 1, limit = 20) => apiCall('/labs/my', {
    // Add a cache-busting parameter to ensure fresh data
    params: { _: new Date().getTime() },
  }),
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
};

// --- Articles API ---
export const articlesAPI = {
  getArticles: (page = 1, limit = 20) => apiCall(`/articles?page=${page}&limit=${limit}`),
  getMyArticles: (page = 1, limit = 20) => apiCall('/articles/my'),
  getArticleById: (articleId) => apiCall(`/articles/${articleId}`),
  createArticle: (formData) => apiCall('/articles', {
    method: 'POST',
    body: formData,
  }),
  deleteArticle: (articleId) => apiCall(`/articles/${articleId}`, { method: 'DELETE' }),
};

// --- Submissions API ---
export const submissionsAPI = {
  getAllSubmissions: (page = 1, limit = 100) => apiCall(`/submissions?page=${page}&limit=${limit}`),
  getLabSubmissions: (labId, page = 1, limit = 20) => apiCall(`/labs/${labId}/submissions?page=${page}&limit=${limit}`),
  getSubmissionById: (submissionId) => apiCall(`/submissions/${submissionId}`),
  createSubmission: (submissionData) => apiCall('/submissions', {
    method: 'POST',
    body: JSON.stringify(submissionData),
  }),
  updateSubmission: (submissionId, submissionData) => apiCall(`/submissions/${submissionId}`, {
    method: 'PUT',
    body: JSON.stringify(submissionData),
  }),
  deleteSubmission: (submissionId) => apiCall(`/submissions/${submissionId}`, { method: 'DELETE' }),
  uploadSubmissionAsset: (submissionId, formData) => apiCall(`/submissions/${submissionId}/assets/upload`, {
    method: 'POST',
    body: formData,
  }),
  submitLabSolution: async (labId, userId, solutionText, files) => {
    // 1. Create submission record
    const submissionData = {
      lab_id: parseInt(labId),
      owner_id: userId,
      status: 'submitted',
      text: solutionText, // Assuming the backend can handle a 'text' field for the solution comment
    };
    const submissionResponse = await submissionsAPI.createSubmission(submissionData);
    const submissionId = submissionResponse.id || submissionResponse.data?.id;
    if (!submissionId) throw new Error('Failed to create submission record.');

    // 2. Upload all files associated with the submission
    if (files && files.length > 0) {
      const uploadPromises = files.map(file => {
        const formData = new FormData();
        formData.append('file', file);
        return submissionsAPI.uploadSubmissionAsset(submissionId, formData);
      });
      const uploads = await Promise.all(uploadPromises);
      return { submission: submissionResponse, uploads };
    }

    return { submission: submissionResponse };
  },
};

// --- ML API ---
export const mlAPI = {
  // ML service is also not on the gateway, it's called directly.
  getChatHistory: (uuid, assignment_id) => apiCall(`/ml/get_chat_history?uuid=${uuid}&assignment_id=${assignment_id}`),
  askAgent: (uuid, assignment_id, content) => apiCall('/ml/ask', {
    method: 'POST',
    body: JSON.stringify({ uuid, assignment_id, content }),
  }),
};
