import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { startSession, executeCell, endSession } from '../utils/marimoApi';
import { WidgetStateProvider, useWidgetState } from './WidgetStateContext';

const MarimoSessionContext = createContext();

// Helper function to extract error message from Python traceback
const extractErrorMessage = (tracebackStr) => {
  if (!tracebackStr) {
    return "";
  }
  
  // Split into lines and process in reverse to find the last non-empty line with a colon
  // This will typically be the actual error message like "NameError: name 'c' is not defined"
  const lines = tracebackStr.trim().split('\n').map(line => line.trim());
  
  for (let i = lines.length - 1; i >= 0; i--) {
    const line = lines[i];
    if (line && line.includes(':')) {
      // Return first line having a colon after stripping
      return line;
    }
  }
  
  // Fallback to original message if no colon found
  return tracebackStr;
};

export const useMarimoSession = () => {
  const context = useContext(MarimoSessionContext);
  if (!context) {
    throw new Error('useMarimoSession must be used within a MarimoSessionProvider');
  }
  return context;
};

export const MarimoSessionProvider = ({ children, contentType, contentId }) => {
  const [sharedSessionId, setSharedSessionId] = useState(null);
  const [sessionError, setSessionError] = useState(null);
  const [executionHistory, setExecutionHistory] = useState([]);
  const [sessionInitialized, setSessionInitialized] = useState(false);
  const [firstComponentId, setFirstComponentId] = useState(null);
  const sessionRef = useRef(null);
  const mountedRef = useRef(true);

  // Initialize shared session when first component tries to execute
  const initializeSharedSessionWithComponent = async (componentId) => {
    if (sharedSessionId) {
      return sharedSessionId; // Already initialized
    }

    try {
      console.log(`Initializing shared session with component: ${componentId}`);
      
      const session = await startSession(componentId);
      
      if (mountedRef.current) {
        setSharedSessionId(session.id);
        sessionRef.current = session.id;
        setSessionInitialized(true);
        setFirstComponentId(componentId);
        console.log(`Shared session created: ${session.id}`);
      }
      
      return session.id;
    } catch (err) {
      console.error('Failed to initialize shared session:', err);
      if (mountedRef.current) {
        setSessionError(err.message);
      }
      throw err;
    }
  };

  useEffect(() => {
    return () => {
      mountedRef.current = false;
      const currentSessionId = sessionRef.current;
      if (currentSessionId) {
        endSession(currentSessionId);
      }
    };
  }, []);

  const executeInSharedSession = async (componentId, code) => {
    try {
      console.log(`Executing component ${componentId} in shared session`);
      console.log(`Code to execute:`, code);
      
      // Initialize shared session with the first component that tries to execute
      const sessionId = await initializeSharedSessionWithComponent(componentId);
      
      // Execute the code in the shared session - all components use the same session
      const result = await executeCell(sessionId, componentId, code);
      
      console.log(`Execution result for ${componentId}:`, result);
      
      // Update execution history
      setExecutionHistory(prev => [
        ...prev,
        {
          componentId,
          code,
          timestamp: new Date().toISOString(),
          success: true,
          result
        }
      ]);
      
      return result;
    } catch (error) {
      console.error(`Execution error for component ${componentId}:`, error);
      
      // Extract just the error message from Python traceback
      let errorMessage = error.message;
      if (typeof errorMessage === 'string') {
        errorMessage = extractErrorMessage(errorMessage);
      }
      
      // Update execution history with cleaned error
      setExecutionHistory(prev => [
        ...prev,
        {
          componentId,
          code,
          timestamp: new Date().toISOString(),
          success: false,
          error: errorMessage
        }
      ]);
      
      // Create a new error with just the clean message
      const cleanError = new Error(errorMessage);
      throw cleanError;
    }
  };

  const resetSession = async () => {
    // End the current shared session
    if (sharedSessionId) {
      try {
        await endSession(sharedSessionId);
      } catch (error) {
        console.warn('Error ending session during reset:', error);
      }
    }

    // Clear all state
    setSharedSessionId(null);
    setExecutionHistory([]);
    setSessionError(null);
    setSessionInitialized(false);
    sessionRef.current = null;
    
    // Note: We don't reinitialize here - it will be done on next execution
    console.log('Session reset. Will reinitialize on next execution.');
  };

  const value = {
    sharedSessionId,
    sessionError,
    executionHistory,
    executeInSharedSession,
    resetSession,
    isSessionReady: true // Always ready - session will be created on first execution
  };

  return (
    <MarimoSessionContext.Provider value={value}>
      <WidgetStateProvider sessionId={sharedSessionId}>
        {children}
      </WidgetStateProvider>
    </MarimoSessionContext.Provider>
  );
};
