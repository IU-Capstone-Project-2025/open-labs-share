// In production, all API calls are sent to the same origin, and Nginx proxies them.
// In development, we explicitly target the API gateway's exposed port.
const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_ENDPOINT || (import.meta.env.PROD ? '' : 'http://localhost/api/v1');

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
  getAllUsers: (page = 1, limit = 20) => apiCall(`/users?page=${page}&limit=${limit}`),
  deleteUser: (userId) => apiCall(`/users/${userId}`, { method: 'DELETE' }),
  getUserLabs: (userId, page = 1, limit = 20) => apiCall(`/users/${userId}/labs?page=${page}&limit=${limit}`),
  getUserArticles: (userId, page = 1, limit = 20) => apiCall(`/users/${userId}/articles?page=${page}&limit=${limit}`),
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
  // ML service is also not on the gateway, it's called directly.
  getChatHistory: (uuid, assignment_id) => apiCall(`/ml/get_chat_history?uuid=${uuid}&assignment_id=${assignment_id}`),
  askAgent: (uuid, assignment_id, content) => apiCall('/ml/ask', {
    method: 'POST',
    body: JSON.stringify({ uuid, assignment_id, content }),
  }),
};

// --- Feedback API ---
export const feedbackAPI = {
  createFeedback: (formData) => apiCall('/feedback', {
    method: 'POST',
    body: formData, // multipart/form-data
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

}

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