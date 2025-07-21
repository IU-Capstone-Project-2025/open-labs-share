// Authentication utility for Open Labs Share
// This connects to the real auth service API

import { API_BASE_URL } from './api';

let userData = null;
const userDataUpdateCallbacks = [];

// Use API Gateway for all authentication requests in production
const AUTH_API_ENDPOINT = `${import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080'}/api/v1/auth`;

// Helper function to make API calls
const makeAuthRequest = async (endpoint, options = {}) => {
  const url = `${API_BASE_URL}/auth${endpoint}`;
  
  const defaultOptions = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const finalOptions = { ...defaultOptions, ...options };
  
  try {
    const response = await fetch(url, finalOptions);
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Auth API request failed:', error);
    throw error;
  }
};

// Helper to process and store successful auth responses
const handleAuthSuccess = (response) => {
  const { accessToken, refreshToken, userInfo } = response;
  
  const userData = {
    id: userInfo.userId,
    firstName: userInfo.firstName,
    lastName: userInfo.lastName,
    username: userInfo.username,
    email: userInfo.email,
    role: userInfo.role,
  };

  localStorage.setItem('authToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  localStorage.setItem('user', JSON.stringify(userData));

  return { user: userData, token: accessToken };
};

// Get current user from localStorage or return null
export const getCurrentUser = () => {
  try {
    const userJson = localStorage.getItem('user');
    const authToken = localStorage.getItem('authToken');
    
    if (userJson && authToken) {
      return JSON.parse(userJson);
    }
    
    return null;
  } catch (error) {
    console.error('Error getting current user:', error);
    return null;
  }
};

// Check if user is authenticated
export const isAuthenticated = () => {
  const authToken = localStorage.getItem('authToken');
  const user = localStorage.getItem('user');
  return !!(authToken && user);
};

// Sign in with email/username and password
export const signIn = async (emailOrUsername, password) => {
  try {
    const response = await makeAuthRequest('/login', {
      method: 'POST',
      body: JSON.stringify({
        usernameOrEmail: emailOrUsername,
        password: password
      })
    });
    
    // Store user data and token from auth service response
    const { accessToken, refreshToken, userInfo } = response;
    
    const userData = {
      id: userInfo.userId,
      firstName: userInfo.firstName,
      lastName: userInfo.lastName,
      username: userInfo.username,
      email: userInfo.email,
      role: userInfo.role,
      balance: userInfo.balance,
      labsSolved: userInfo.labsSolved,
      labsReviewed: userInfo.labsReviewed,
    };
    
    localStorage.setItem('authToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(userData));
    
    // Immediately notify all components about the user data update
    notifyUserDataUpdate();
    
    return { user: userData, token: accessToken };
  } catch (error) {
    console.error('Sign in error:', error);
    throw error;
  }
};

// Sign up with user data
export const signUp = async (userData) => {
  try {
    const { firstName, lastName, username, email, password } = userData;
    
    // Validate required fields
    if (!firstName || !lastName || !username || !email || !password) {
      throw new Error('All fields are required');
    }
    
    const response = await makeAuthRequest('/register', {
      method: 'POST',
      body: JSON.stringify({
        firstName,
        lastName,
        username,
        email,
        password
      })
    });
    
    // Store user data and token from auth service response
    const { accessToken, refreshToken, userInfo } = response;
    
    const userDataToStore = {
      id: userInfo.userId,
      firstName: userInfo.firstName,
      lastName: userInfo.lastName,
      username: userInfo.username,
      email: userInfo.email,
      role: userInfo.role,
      balance: userInfo.balance,
      labsSolved: userInfo.labsSolved,
      labsReviewed: userInfo.labsReviewed,
    };
    
    localStorage.setItem('authToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(userDataToStore));
    
    // Immediately notify all components about the user data update
    notifyUserDataUpdate();
    
    return { user: userDataToStore, token: accessToken };
  } catch (error) {
    console.error('Sign up error:', error);
    throw error;
  }
};

// Sign out
export const signOut = async () => {
  try {
    const authToken = localStorage.getItem('authToken');
    
    if (authToken) {
      // Call logout endpoint to invalidate token on server
      await makeAuthRequest('/logout', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        }
      });
    }
  } catch (error) {
    console.error('Logout API call failed, proceeding with local cleanup:', error);
    // Continue with local logout even if API call fails
  } finally {
    // Stop token refresh
    stopTokenRefresh();
    
    // Clear local storage
    localStorage.removeItem('authToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    
    // Immediately notify all components about the user data update
    notifyUserDataUpdate();
  }
};

// ---
// Event-based mechanism for components to listen for user data changes
// ---

// Rate limiting for user data updates
let lastUpdateTime = 0;
const UPDATE_THROTTLE_MS = 200; // Minimum 200ms between updates

// Function to dispatch a custom event when user data is updated
export const notifyUserDataUpdate = () => {
  const now = Date.now();
  if (now - lastUpdateTime < UPDATE_THROTTLE_MS) {
    // Skip this update if it's too soon after the last one
    console.log('User data update throttled - too soon after last update');
    return;
  }
  
  lastUpdateTime = now;
  console.log('Dispatching userDataUpdated event at:', new Date().toISOString());
  const event = new Event('userDataUpdated');
  window.dispatchEvent(event);
};

// Helper for components to subscribe to user data updates
export const setupAuthEventListeners = (callback) => {
  const handleUpdate = () => {
    const user = getCurrentUser();
    callback(user);
  };

  window.addEventListener('userDataUpdated', handleUpdate);

  // Return a cleanup function
  return () => {
    window.removeEventListener('userDataUpdated', handleUpdate);
  };
};

// Refresh token
export const refreshToken = async () => {
  try {
    const refreshTokenValue = localStorage.getItem('refreshToken');
    
    if (!refreshTokenValue) {
      throw new Error('No refresh token available');
    }
    
    const response = await makeAuthRequest('/refresh', {
      method: 'POST',
      body: JSON.stringify({
        refreshToken: refreshTokenValue
      })
    });
    
    const { accessToken, refreshToken: newRefreshToken, userInfo } = response;
    
    // Update stored tokens
    localStorage.setItem('authToken', accessToken);
    if (newRefreshToken) {
      localStorage.setItem('refreshToken', newRefreshToken);
    }
    
    // Update user info if provided
    if (userInfo) {
      const userData = {
        id: userInfo.userId,
        firstName: userInfo.firstName,
        lastName: userInfo.lastName,
        username: userInfo.username,
        email: userInfo.email,
        role: userInfo.role,
        balance: userInfo.balance,
        labsSolved: userInfo.labsSolved,
        labsReviewed: userInfo.labsReviewed,
      };
      
      // Check if user data actually changed before notifying
      const currentUser = getCurrentUser();
      const hasChanged = !currentUser || 
        currentUser.balance !== userData.balance ||
        currentUser.labsSolved !== userData.labsSolved ||
        currentUser.labsReviewed !== userData.labsReviewed;
      
      localStorage.setItem('user', JSON.stringify(userData));
      
      // Only notify if user data actually changed
      if (hasChanged) {
        notifyUserDataUpdate();
      }
    }
    
    return accessToken;
  } catch (error) {
    console.error('Token refresh failed:', error);
    // Clear invalid tokens if refresh fails
    signOut();
    throw error;
  }
};

// Update user profile
export const updateProfile = async (updatedData) => {
  try {
    const response = await authAPI.updateProfile(updatedData);
    
    // Assuming the API returns the full updated user object
    const { userInfo } = response;
    
    if (userInfo) {
      const updatedUser = {
        id: userInfo.userId,
        firstName: userInfo.firstName,
        lastName: userInfo.lastName,
        username: userInfo.username,
        email: userInfo.email,
        role: userInfo.role,
      };
      
      localStorage.setItem('user', JSON.stringify(updatedUser));
      notifyUserDataUpdate(); // Notify components of the change
    }
    
    return response;
  } catch (error) {
    console.error('Update profile error:', error);
    throw error;
  }
};

// Change user password
export const changePassword = async (currentPassword, newPassword) => {
  try {
    const response = await authAPI.changePassword({ currentPassword, newPassword });
    return response;
  } catch (error) {
    console.error('Change password error:', error);
    throw error;
  }
};

// Fetch user profile from the server and update local storage
export const getUserProfile = async () => {
  try {
    // Note: /api/v1/auth/profile endpoint doesn't exist in current API
    // For now, return cached user data from localStorage
    const cachedUser = getCurrentUser();
    if (cachedUser) {
      return cachedUser;
    }
    
    // If no cached data, try the API call (will likely fail until backend implements this)
    const response = await authAPI.getProfile();
    const { userInfo } = response;

    if (userInfo) {
      const userData = {
        id: userInfo.userId,
        firstName: userInfo.firstName,
        lastName: userInfo.lastName,
        username: userInfo.username,
        email: userInfo.email,
        role: userInfo.role,
        balance: userInfo.balance,
        labsSolved: userInfo.labsSolved,
        labsReviewed: userInfo.labsReviewed,
      };

      // Update local storage
      localStorage.setItem('user', JSON.stringify(userData));
      
      return userData;
    }
    return null;
  } catch (error) {
    console.warn('Get user profile API not available, using cached data:', error);
    // Return cached user data instead of failing
    const cachedUser = getCurrentUser();
    return cachedUser;
  }
};

// Validate sign up data
export const validateSignUpData = (userData) => {
  const { firstName, lastName, username, email, password, confirmPassword } = userData;
  const errors = {};

  if (!firstName || firstName.trim().length < 2) {
    errors.firstName = 'First name must be at least 2 characters long';
  }

  if (!lastName || lastName.trim().length < 2) {
    errors.lastName = 'Last name must be at least 2 characters long';
  }

  if (!username || username.trim().length < 3) {
    errors.username = 'Username must be at least 3 characters long';
  }

  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = 'Please enter a valid email address';
  }

  if (!password || password.length < 6) {
    errors.password = 'Password must be at least 6 characters long';
  }

  if (password !== confirmPassword) {
    errors.confirmPassword = 'Passwords do not match';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors: errors
  };
};

// Auto-refresh token before expiration
let refreshInterval = null;

export const startTokenRefresh = () => {
  stopTokenRefresh(); // Clear any existing interval
  
  // Refresh token every 20 minutes (tokens expire in 24 hours)
  refreshInterval = setInterval(async () => {
    if (isAuthenticated()) {
      try {
        await refreshToken();
        console.log('Token refreshed automatically');
      } catch (error) {
        console.error('Auto token refresh failed:', error);
        // If refresh fails, user will be logged out
      }
    }
  }, 20 * 60 * 1000); // 20 minutes
};

export const stopTokenRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
}; 

// Helper function to handle user data updates across tabs
function handleCrossTabUpdate(event) {
  if (event.key === 'user' || event.key === 'isAuthenticated') {
    window.dispatchEvent(new CustomEvent('userDataUpdated'));
  }
}

// Listen for storage changes in other tabs
window.addEventListener('storage', handleCrossTabUpdate);
