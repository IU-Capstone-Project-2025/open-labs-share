// API configuration and endpoints for Open Labs Share
// This connects to the API Gateway which proxies requests to various microservices

export const API_CONFIG = {
  API_GATEWAY_URL: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
  API_GATEWAY_ENDPOINT: `${import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080'}/api/v1`,
  ENDPOINTS: {
    // User endpoints (through API Gateway to Users Service)
    USERS: '/users',
    USER_BY_ID: (userId) => `/users/${userId}`,
    
    // Lab endpoints (through API Gateway to Labs Service)
    LABS: '/labs',
    LAB_BY_ID: (labId) => `/labs/${labId}`,
    LAB_ASSETS: (labId) => `/labs/${labId}/assets`,
    LAB_ASSET_DOWNLOAD: (labId, assetId) => `/labs/${labId}/assets/${assetId}/download`,
    LAB_ASSET_UPLOAD: (labId) => `/labs/${labId}/assets/upload`,
    
    // Submission endpoints (through API Gateway to Labs Service)
    SUBMISSIONS: '/submissions',
    LAB_SUBMISSIONS: (labId) => `/labs/${labId}/submissions`,
    SUBMISSION_BY_ID: (submissionId) => `/submissions/${submissionId}`,
    SUBMISSION_ASSETS: (submissionId) => `/submissions/${submissionId}/assets`,
    SUBMISSION_ASSET_DOWNLOAD: (submissionId, assetId) => `/submissions/${submissionId}/assets/${assetId}/download`,
    SUBMISSION_ASSET_UPLOAD: (submissionId) => `/submissions/${submissionId}/assets/upload`,
    
    // Article endpoints (currently not connected as per requirements)
    // ARTICLES: '/articles',
    // ARTICLE_BY_ID: (articleId) => `/articles/${articleId}`,
    
    // Comments/Feedback endpoints (when feedback controller is implemented)
    // COMMENTS: '/feedback/comments',
    // LAB_COMMENTS: (labId) => `/feedback/comments/lab/${labId}`,
    // ARTICLE_COMMENTS: (articleId) => `/feedback/comments/article/${articleId}`,
  }
};

// Helper function to get authorization headers
export const getAuthHeaders = () => {
  const token = localStorage.getItem('authToken');
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

// API call wrapper with error handling
export const apiCall = async (endpoint, options = {}) => {
  const url = `${API_CONFIG.API_GATEWAY_ENDPOINT}${endpoint}`;
  
  try {
    const response = await fetch(url, {
      headers: getAuthHeaders(),
      ...options,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `API call failed: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('API call error:', error);
    throw error;
  }
};

// User API functions
export const usersAPI = {
  // Get user by ID
  getUserById: async (userId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.USER_BY_ID(userId));
  },

  // Get all users with pagination
  getAllUsers: async (page = 1, limit = 20) => {
    return await apiCall(`${API_CONFIG.ENDPOINTS.USERS}?page=${page}&limit=${limit}`);
  },

  // Update user
  updateUser: async (userId, userData) => {
    return await apiCall(API_CONFIG.ENDPOINTS.USER_BY_ID(userId), {
      method: 'PUT',
      body: JSON.stringify(userData),
    });
  },

  // Delete user
  deleteUser: async (userId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.USER_BY_ID(userId), {
      method: 'DELETE',
    });
  },

  // Get current user's labs
  getUserLabs: async (userId, page = 1, limit = 20) => {
    return await apiCall(`${API_CONFIG.ENDPOINTS.USERS}/${userId}/labs?page=${page}&limit=${limit}`);
  },

  // Get current user's articles (when articles service is connected)
  getUserArticles: async (userId, page = 1, limit = 20) => {
    // TODO: Implement when articles service is connected
    throw new Error('Articles service not yet connected');
  },
};

// Labs API functions
export const labsAPI = {
  // Get all labs with pagination
  getLabs: async (page = 1, limit = 20) => {
    return await apiCall(`${API_CONFIG.ENDPOINTS.LABS}?page=${page}&limit=${limit}`);
  },

  // Get lab by ID
  getLabById: async (labId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.LAB_BY_ID(labId));
  },

  // Create a new lab with file upload
  createLab: async (labData) => {
    const formData = new FormData();
    formData.append('title', labData.title);
    formData.append('short_desc', labData.short_desc);
    formData.append('md_file', labData.md_file);
    
    // Add optional asset files
    if (labData.assets && labData.assets.length > 0) {
      for (const asset of labData.assets) {
        formData.append('assets', asset);
      }
    }
    
    const token = localStorage.getItem('authToken');
    const response = await fetch(`${API_CONFIG.API_GATEWAY_ENDPOINT}${API_CONFIG.ENDPOINTS.LABS}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Lab creation failed: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  },

  // Update lab
  updateLab: async (labId, labData) => {
    return await apiCall(API_CONFIG.ENDPOINTS.LAB_BY_ID(labId), {
      method: 'PUT',
      body: JSON.stringify(labData),
    });
  },

  // Delete lab
  deleteLab: async (labId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.LAB_BY_ID(labId), {
      method: 'DELETE',
    });
  },

  // Get lab assets
  getLabAssets: async (labId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.LAB_ASSETS(labId));
  },

  // Upload lab asset
  uploadLabAsset: async (labId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    
    const token = localStorage.getItem('authToken');
    const response = await fetch(`${API_CONFIG.API_GATEWAY_ENDPOINT}${API_CONFIG.ENDPOINTS.LAB_ASSET_UPLOAD(labId)}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Upload failed: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  },

  // Download lab asset
  downloadLabAsset: async (labId, assetId) => {
    const token = localStorage.getItem('authToken');
    const response = await fetch(`${API_CONFIG.API_GATEWAY_ENDPOINT}${API_CONFIG.ENDPOINTS.LAB_ASSET_DOWNLOAD(labId, assetId)}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error(`Download failed: ${response.status} ${response.statusText}`);
    }

    return response.blob();
  },
};

// Submissions API functions
export const submissionsAPI = {
  // Get all submissions with pagination
  getAllSubmissions: async (page = 1, limit = 100) => {
    return await apiCall(`${API_CONFIG.ENDPOINTS.SUBMISSIONS}?page=${page}&limit=${limit}`);
  },

  // Get submissions for a lab
  getLabSubmissions: async (labId, page = 1, limit = 20) => {
    return await apiCall(`${API_CONFIG.ENDPOINTS.LAB_SUBMISSIONS(labId)}?page=${page}&limit=${limit}`);
  },

  // Get submission by ID
  getSubmissionById: async (submissionId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.SUBMISSION_BY_ID(submissionId));
  },

  // Create a new submission
  createSubmission: async (submissionData) => {
    return await apiCall(API_CONFIG.ENDPOINTS.SUBMISSIONS, {
      method: 'POST',
      body: JSON.stringify(submissionData),
    });
  },

  // Update submission
  updateSubmission: async (submissionId, submissionData) => {
    return await apiCall(API_CONFIG.ENDPOINTS.SUBMISSION_BY_ID(submissionId), {
      method: 'PUT',
      body: JSON.stringify(submissionData),
    });
  },

  // Delete submission
  deleteSubmission: async (submissionId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.SUBMISSION_BY_ID(submissionId), {
      method: 'DELETE',
    });
  },

  // Get submission assets
  getSubmissionAssets: async (submissionId) => {
    return await apiCall(API_CONFIG.ENDPOINTS.SUBMISSION_ASSETS(submissionId));
  },

  // Upload submission asset
  uploadSubmissionAsset: async (submissionId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    
    const token = localStorage.getItem('authToken');
    const response = await fetch(`${API_CONFIG.API_GATEWAY_ENDPOINT}${API_CONFIG.ENDPOINTS.SUBMISSION_ASSET_UPLOAD(submissionId)}`, {
        method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Upload failed: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  },

  // Download submission asset
  downloadSubmissionAsset: async (submissionId, assetId) => {
    const token = localStorage.getItem('authToken');
    const response = await fetch(`${API_CONFIG.API_GATEWAY_ENDPOINT}${API_CONFIG.ENDPOINTS.SUBMISSION_ASSET_DOWNLOAD(submissionId, assetId)}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error(`Download failed: ${response.status} ${response.statusText}`);
    }

    return response.blob();
  },

  // Submit a file for a lab (creates submission and uploads file)
  submitLabFile: async (labId, userId, file) => {
    try {
      // First create a submission
      const submissionData = {
        lab_id: parseInt(labId),
        owner_id: userId,
        status: 'submitted'
      };
      
      const submissionResponse = await submissionsAPI.createSubmission(submissionData);
      const submissionId = submissionResponse.id || submissionResponse.data?.id;
      
      if (!submissionId) {
        throw new Error('Failed to create submission');
      }

      // Then upload the file as an asset to the submission
      const uploadResponse = await submissionsAPI.uploadSubmissionAsset(submissionId, file);
      
      return {
        submission: submissionResponse,
        upload: uploadResponse
      };
    } catch (error) {
      console.error('Error in submitLabFile:', error);
      throw error;
    }
  },
}; 

// Export the main APIs (clean backend-only APIs)
export { usersAPI as users, labsAPI as labs, submissionsAPI as submissions }; 