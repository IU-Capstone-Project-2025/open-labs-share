// Authentication utility for Open Labs Share
// This connects to the real auth service API

import { authAPI } from './api';

// Helper function to make API calls
const makeAuthRequest = async (endpoint, options = {}) => {
  const url = `${AUTH_API_ENDPOINT}${endpoint}`;
  
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
    const response = await authAPI.login({
        usernameOrEmail: emailOrUsername,
      password: password,
    });
    return handleAuthSuccess(response);
  } catch (error) {
    console.error('Sign in error:', error);
    throw error;
  }
};

// Sign up with user data
export const signUp = async (userData) => {
  try {
    const { firstName, lastName, username, email, password } = userData;
    const response = await authAPI.register({
        firstName,
        lastName,
        username,
        email,
        password,
      role: 'ROLE_USER', // Default role
    });
    return handleAuthSuccess(response);
  } catch (error) {
    console.error('Sign up error:', error);
    throw error;
  }
};

// Sign out
export const signOut = async () => {
  try {
      // Call logout endpoint to invalidate token on server
    await authAPI.logout();
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
  }
};

// Refresh token
export const refreshToken = async () => {
  try {
    const refreshTokenValue = localStorage.getItem('refreshToken');
    if (!refreshTokenValue) throw new Error('No refresh token available');
    
    const response = await authAPI.refreshToken({ refreshToken: refreshTokenValue });
    
    // After a successful refresh, the API returns new tokens and user info
    return handleAuthSuccess(response);

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

    // After a successful update, the API might return new tokens or user info
    if (response.accessToken) {
        return handleAuthSuccess(response);
    } 
    
    // If no new tokens, just update local user data from response
    const currentUser = getCurrentUser();
    const updatedUser = { ...currentUser, ...response.userInfo };
    localStorage.setItem('user', JSON.stringify(updatedUser));
    
    return { user: updatedUser };
  } catch (error) {
    console.error('Update profile error:', error);
    throw error;
  }
};

// Change password
export const changePassword = async (currentPassword, newPassword) => {
  try {
    // This function needs a corresponding endpoint in the auth service.
    // Assuming the endpoint is /auth/change-password
    await authAPI.changePassword({ currentPassword, newPassword });
  } catch (error) {
    console.error('Change password error:', error);
    throw error;
  }
};

// Get user profile from auth service
export const getUserProfile = async () => {
  try {
    const response = await authAPI.getProfile();
    return response;
  } catch (error) {
    console.error('Get user profile error:', error);
    throw error;
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