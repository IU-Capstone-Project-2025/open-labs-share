// API configuration and endpoints for the feedback service
// This will connect to the API Gateway which will proxy requests to the feedback service

export const API_CONFIG = {
  BASE_URL: '/api/v1/feedback', // This should point to the API Gateway
  ENDPOINTS: {
    // Comment endpoints
    COMMENTS: '/comments',
    LAB_COMMENTS: (labId) => `/comments/lab/${labId}`,
    ARTICLE_COMMENTS: (articleId) => `/comments/article/${articleId}`,
    COMMENT_REPLIES: (commentId) => `/comments/${commentId}/replies`,
    
    // Feedback endpoints (for future use)
    FEEDBACK: '/feedback',
    USER_FEEDBACK: (userId) => `/feedback/user/${userId}`,
    STUDENT_FEEDBACK: (studentId) => `/feedback/student/${studentId}`,
  }
};

// Mock data for testing
let mockComments = [
  {
    id: 1,
    content: "This is a great lab! Really helped me understand the concepts.",
    user_id: 2,
    lab_id: 1,
    article_id: null,
    parent_id: null,
    created_at: "2024-01-15T10:30:00Z",
    replies: []
  },
  {
    id: 2,
    content: "I found the exercise challenging but rewarding.",
    user_id: 3,
    lab_id: 1,
    article_id: null,
    parent_id: null,
    created_at: "2024-01-15T14:20:00Z",
    replies: []
  }
];

let nextCommentId = mockComments.length + 1;

// Helper function to get authorization headers
export const getAuthHeaders = () => {
  const token = localStorage.getItem('authToken');
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

// Check if we're in development mode and backend is not available
const isDevelopmentMode = () => {
  return process.env.NODE_ENV === 'development' || window.location.hostname === 'localhost';
};

// Mock API implementations for development
const mockAPI = {
  getLabComments: async (labId, page = 1, limit = 20) => {
    await new Promise(resolve => setTimeout(resolve, 300)); // Simulate network delay
    const labComments = mockComments.filter(c => c.lab_id == labId && !c.parent_id);
    return {
      comments: labComments,
      total_count: labComments.length,
      page,
      limit
    };
  },

  getArticleComments: async (articleId, page = 1, limit = 20) => {
    await new Promise(resolve => setTimeout(resolve, 300)); // Simulate network delay
    const articleComments = mockComments.filter(c => c.article_id == articleId && !c.parent_id);
    return {
      comments: articleComments,
      total_count: articleComments.length,
      page,
      limit
    };
  },

  getCommentReplies: async (commentId, page = 1, limit = 50) => {
    await new Promise(resolve => setTimeout(resolve, 200)); // Simulate network delay
    const replies = mockComments.filter(c => c.parent_id == commentId);
    return {
      comments: replies,
      total_count: replies.length,
      page,
      limit
    };
  },

  createComment: async (commentData) => {
    await new Promise(resolve => setTimeout(resolve, 500)); // Simulate network delay
    
    const newComment = {
      id: nextCommentId++,
      content: commentData.content,
      user_id: commentData.user_id,
      lab_id: commentData.lab_id || null,
      article_id: commentData.article_id || null,
      parent_id: commentData.parent_id || null,
      created_at: new Date().toISOString(),
      replies: []
    };

    mockComments.push(newComment);
    return newComment;
  },

  updateComment: async (commentId, content) => {
    await new Promise(resolve => setTimeout(resolve, 300));
    const comment = mockComments.find(c => c.id == commentId);
    if (comment) {
      comment.content = content;
      return comment;
    }
    throw new Error('Comment not found');
  },

  deleteComment: async (commentId) => {
    await new Promise(resolve => setTimeout(resolve, 300));
    const index = mockComments.findIndex(c => c.id == commentId);
    if (index !== -1) {
      mockComments.splice(index, 1);
      return { success: true };
    }
    throw new Error('Comment not found');
  }
};

// API call wrapper with error handling and fallback to mock
export const apiCall = async (endpoint, options = {}) => {
  try {
    const response = await fetch(endpoint, {
      headers: getAuthHeaders(),
      ...options,
    });

    if (!response.ok) {
      throw new Error(`API call failed: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('API call error:', error);
    
    // In development mode, if the API call fails, we'll fall back to mock data
    if (isDevelopmentMode()) {
      console.warn('Falling back to mock API for development');
      throw new Error('FALLBACK_TO_MOCK');
    }
    
    throw error;
  }
};

// Comment API functions with mock fallback
export const commentsAPI = {
  // Get comments for a lab
  getLabComments: async (labId, page = 1, limit = 20) => {
    try {
      const url = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.LAB_COMMENTS(labId)}?page=${page}&limit=${limit}`;
      return await apiCall(url);
    } catch (error) {
      if (isDevelopmentMode() && (error.message === 'FALLBACK_TO_MOCK' || error.message.includes('fetch'))) {
        console.log('Using mock API for lab comments');
        return await mockAPI.getLabComments(labId, page, limit);
      }
      throw error;
    }
  },

  // Get comments for an article
  getArticleComments: async (articleId, page = 1, limit = 20) => {
    try {
      const url = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.ARTICLE_COMMENTS(articleId)}?page=${page}&limit=${limit}`;
      return await apiCall(url);
    } catch (error) {
      if (isDevelopmentMode() && (error.message === 'FALLBACK_TO_MOCK' || error.message.includes('fetch'))) {
        console.log('Using mock API for article comments');
        return await mockAPI.getArticleComments(articleId, page, limit);
      }
      throw error;
    }
  },

  // Get comments for content (generic - can be lab or article)
  getContentComments: (contentType, contentId, page = 1, limit = 20) => {
    if (contentType === 'lab') {
      return commentsAPI.getLabComments(contentId, page, limit);
    } else if (contentType === 'article') {
      return commentsAPI.getArticleComments(contentId, page, limit);
    } else {
      throw new Error('Invalid content type. Must be "lab" or "article"');
    }
  },

  // Get replies for a comment
  getCommentReplies: async (commentId, page = 1, limit = 50) => {
    try {
      const url = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.COMMENT_REPLIES(commentId)}?page=${page}&limit=${limit}`;
      return await apiCall(url);
    } catch (error) {
      if (isDevelopmentMode() && (error.message === 'FALLBACK_TO_MOCK' || error.message.includes('fetch'))) {
        console.log('Using mock API for comment replies');
        return await mockAPI.getCommentReplies(commentId, page, limit);
      }
      throw error;
    }
  },

  // Create a new comment (works for both labs and articles)
  createComment: async (commentData) => {
    try {
      return await apiCall(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.COMMENTS}`, {
        method: 'POST',
        body: JSON.stringify(commentData),
      });
    } catch (error) {
      if (isDevelopmentMode() && (error.message === 'FALLBACK_TO_MOCK' || error.message.includes('fetch'))) {
        console.log('Using mock API for creating comment');
        return await mockAPI.createComment(commentData);
      }
      throw error;
    }
  },

  // Update a comment
  updateComment: async (commentId, content) => {
    try {
      return await apiCall(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.COMMENTS}/${commentId}`, {
        method: 'PUT',
        body: JSON.stringify({ content }),
      });
    } catch (error) {
      if (isDevelopmentMode() && (error.message === 'FALLBACK_TO_MOCK' || error.message.includes('fetch'))) {
        console.log('Using mock API for updating comment');
        return await mockAPI.updateComment(commentId, content);
      }
      throw error;
    }
  },

  // Delete a comment
  deleteComment: async (commentId) => {
    try {
      return await apiCall(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.COMMENTS}/${commentId}`, {
        method: 'DELETE',
      });
    } catch (error) {
      if (isDevelopmentMode() && (error.message === 'FALLBACK_TO_MOCK' || error.message.includes('fetch'))) {
        console.log('Using mock API for deleting comment');
        return await mockAPI.deleteComment(commentId);
      }
      throw error;
    }
  },
}; 