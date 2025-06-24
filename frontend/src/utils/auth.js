// Authentication utility for Open Labs Share
// This provides mock authentication functionality that can be easily replaced with real API integration

// Mock user database (in a real app, this would be stored on the server)
const MOCK_USERS = [
  {
    id: 1,
    firstName: "Demo",
    lastName: "User",
    username: "demouser",
    email: "demo@example.com",
    password: "password123" // In real app, this would be hashed
  },
  {
    id: 2,
    firstName: "Ryan",
    lastName: "Gosling",
    username: "ryanGosling1980",
    email: "gosl1980@mail.com",
    password: "password123"
  },
  {
    id: 3,
    firstName: "Dr. Sarah",
    lastName: "Johnson",
    username: "sarahjohnson",
    email: "sarah.johnson@university.edu",
    password: "password123"
  }
];

// Get current user from localStorage or return null
export const getCurrentUser = () => {
  try {
    const userJson = localStorage.getItem('user');
    const authToken = localStorage.getItem('authToken');
    
    if (userJson && authToken) {
      return JSON.parse(userJson);
    }
    
    // For backwards compatibility, return mock user if token exists but no user data
    if (authToken) {
      const mockUser = {
        id: 1,
        firstName: "Demo",
        lastName: "User",
        username: "demouser",
        email: "demo@example.com"
      };
      localStorage.setItem('user', JSON.stringify(mockUser));
      return mockUser;
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
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // Find user by email or username
    const user = MOCK_USERS.find(u => 
      (u.email === emailOrUsername || u.username === emailOrUsername) && 
      u.password === password
    );
    
    if (!user) {
      throw new Error('Invalid email/username or password');
    }
    
    // Generate mock JWT token
    const authToken = `mock-jwt-${user.id}-${Date.now()}`;
    
    // Store user data (without password)
    const userData = {
      id: user.id,
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
      email: user.email
    };
    
    localStorage.setItem('authToken', authToken);
    localStorage.setItem('user', JSON.stringify(userData));
    
    return { user: userData, token: authToken };
  } catch (error) {
    console.error('Sign in error:', error);
    throw error;
  }
};

// Sign up with user data
export const signUp = async (userData) => {
  try {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    
    const { firstName, lastName, username, email, password } = userData;
    
    // Validate required fields
    if (!firstName || !lastName || !username || !email || !password) {
      throw new Error('All fields are required');
    }
    
    // Check if username or email already exists
    const existingUser = MOCK_USERS.find(u => 
      u.username === username || u.email === email
    );
    
    if (existingUser) {
      if (existingUser.username === username) {
        throw new Error('Username is already taken');
      }
      if (existingUser.email === email) {
        throw new Error('Email is already registered');
      }
    }
    
    // Create new user
    const newUser = {
      id: MOCK_USERS.length + 1,
      firstName,
      lastName,
      username,
      email,
      password // In real app, this would be hashed
    };
    
    // Add to mock database
    MOCK_USERS.push(newUser);
    
    // Generate mock JWT token
    const authToken = `mock-jwt-${newUser.id}-${Date.now()}`;
    
    // Store user data (without password)
    const userDataToStore = {
      id: newUser.id,
      firstName: newUser.firstName,
      lastName: newUser.lastName,
      username: newUser.username,
      email: newUser.email
    };
    
    localStorage.setItem('authToken', authToken);
    localStorage.setItem('user', JSON.stringify(userDataToStore));
    
    return { user: userDataToStore, token: authToken };
  } catch (error) {
    console.error('Sign up error:', error);
    throw error;
  }
};

// Sign out
export const signOut = () => {
  localStorage.removeItem('authToken');
  localStorage.removeItem('user');
};

// Update user profile
export const updateProfile = async (updatedData) => {
  try {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    
    const currentUser = getCurrentUser();
    if (!currentUser) {
      throw new Error('No user logged in');
    }
    
    // Update user data
    const updatedUser = { ...currentUser, ...updatedData };
    
    // Update in mock database
    const userIndex = MOCK_USERS.findIndex(u => u.id === currentUser.id);
    if (userIndex !== -1) {
      MOCK_USERS[userIndex] = { ...MOCK_USERS[userIndex], ...updatedData };
    }
    
    // Update localStorage
    localStorage.setItem('user', JSON.stringify(updatedUser));
    
    return updatedUser;
  } catch (error) {
    console.error('Update profile error:', error);
    throw error;
  }
};

// Change password
export const changePassword = async (currentPassword, newPassword) => {
  try {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    
    const currentUser = getCurrentUser();
    if (!currentUser) {
      throw new Error('No user logged in');
    }
    
    // Find user in mock database to verify current password
    const user = MOCK_USERS.find(u => u.id === currentUser.id);
    if (!user || user.password !== currentPassword) {
      throw new Error('Current password is incorrect');
    }
    
    // Update password in mock database
    const userIndex = MOCK_USERS.findIndex(u => u.id === currentUser.id);
    if (userIndex !== -1) {
      MOCK_USERS[userIndex].password = newPassword;
    }
    
    return true;
  } catch (error) {
    console.error('Change password error:', error);
    throw error;
  }
};

// Validate form data
export const validateSignUpData = (userData) => {
  const errors = {};
  
  if (!userData.firstName?.trim()) {
    errors.firstName = 'First name is required';
  }
  
  if (!userData.lastName?.trim()) {
    errors.lastName = 'Last name is required';
  }
  
  if (!userData.username?.trim()) {
    errors.username = 'Username is required';
  } else if (!/^[a-zA-Z0-9_]+$/.test(userData.username)) {
    errors.username = 'Username can only contain letters, numbers, and underscores';
  } else if (userData.username.length < 3) {
    errors.username = 'Username must be at least 3 characters long';
  }
  
  if (!userData.email?.trim()) {
    errors.email = 'Email is required';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userData.email)) {
    errors.email = 'Please enter a valid email address';
  }
  
  if (!userData.password) {
    errors.password = 'Password is required';
  } else if (userData.password.length < 8) {
    errors.password = 'Password must be at least 8 characters long';
  }
  
  if (userData.password !== userData.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match';
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};

// For backwards compatibility - old function name
export const logout = signOut; 