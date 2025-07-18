import { marimoAPI } from './api';

// Session cleanup queue to handle multiple concurrent cleanup requests
const sessionCleanupQueue = new Set();
let cleanupTimeout = null;

const processCleanupQueue = async () => {
  if (sessionCleanupQueue.size === 0) return;
  
  const sessions = Array.from(sessionCleanupQueue);
  sessionCleanupQueue.clear();
  
  // Process sessions in batches to avoid overwhelming the server
  for (const sessionId of sessions) {
    try {
      await marimoAPI.closeSession(sessionId);
    } catch (error) {
      // Silently handle cleanup errors during navigation
      console.debug('Session cleanup error (likely during navigation):', error);
    }
  }
};

export const startSession = async (componentId, sessionName = 'FrontendSession') => {
  return marimoAPI.createSession(componentId);
};

export const executeCell = async (sessionId, cellId, code) => {
  return marimoAPI.runCode(sessionId, code, cellId);
};

export const endSession = async (sessionId) => {
  if (!sessionId) return;
  
  // Add to cleanup queue instead of calling directly
  sessionCleanupQueue.add(sessionId);
  
  // Debounce cleanup to avoid overwhelming the server
  if (cleanupTimeout) {
    clearTimeout(cleanupTimeout);
  }
  
  cleanupTimeout = setTimeout(processCleanupQueue, 200);
};

export const getComponentsByContent = async (contentType, contentId) => {
  return marimoAPI.getComponentsByContent(contentType, contentId);
};
