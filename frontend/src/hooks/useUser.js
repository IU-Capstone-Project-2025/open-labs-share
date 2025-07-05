import { createContext, useContext } from 'react';

// Create a context for user data
export const UserContext = createContext(null);

/**
 * Custom hook to access user data from the UserContext
 * 
 * @returns {Object} user - Current user object or null
 */
export const useUser = () => {
  return useContext(UserContext);
}; 