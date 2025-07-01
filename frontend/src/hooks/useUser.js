import { useState, useEffect } from 'react';
import { getCurrentUser, isAuthenticated } from '../utils/auth';

/**
 * Custom hook to manage user state across components
 * Automatically syncs with global user data updates
 * 
 * @returns {Object} user - Current user object or null
 */
export const useUser = () => {
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Initialize user state
    if (isAuthenticated()) {
      const currentUser = getCurrentUser();
      setUser(currentUser);
    } else {
      setUser(null);
    }
  }, []);

  useEffect(() => {
    const handleUserDataUpdate = () => {
      if (isAuthenticated()) {
        const updatedUser = getCurrentUser();
        setUser(updatedUser);
      } else {
        setUser(null);
      }
    };

    // Listen for global user data updates
    window.addEventListener('userDataUpdated', handleUserDataUpdate);

    // Cleanup event listener on component unmount
    return () => {
      window.removeEventListener('userDataUpdated', handleUserDataUpdate);
    };
  }, []);

  return user;
}; 