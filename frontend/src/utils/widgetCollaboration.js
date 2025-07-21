/**
 * Phase 5: Widget Collaboration System
 * Enables real-time collaborative widget updates across multiple users
 */

// Simple EventEmitter implementation for browser compatibility
class EventEmitter {
  constructor() {
    this.events = {};
  }

  on(event, listener) {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(listener);
  }

  emit(event, ...args) {
    if (this.events[event]) {
      this.events[event].forEach(listener => {
        listener(...args);
      });
    }
  }

  off(event, listener) {
    if (this.events[event]) {
      this.events[event] = this.events[event].filter(l => l !== listener);
    }
  }
}

export class WidgetCollaborationManager extends EventEmitter {
  constructor(sessionId, userId) {
    super();
    this.sessionId = sessionId;
    this.userId = userId;
    this.collaborators = new Map();
    this.widgetLocks = new Map();
    this.pendingCollaborativeUpdates = new Map();
    this.websocket = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.isConnected = false;
    
    this.initializeWebSocket();
  }

  /**
   * Initialize WebSocket connection for real-time collaboration
   */
  initializeWebSocket() {
    try {
      // Use WebSocket for real-time communication
      const wsUrl = `ws://localhost:8084/ws/collaboration/${this.sessionId}`;
      this.websocket = new WebSocket(wsUrl);
      
      this.websocket.onopen = () => {
        console.log('Widget collaboration WebSocket connected');
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.emit('connected');
        
        // Send initial presence
        this.sendPresence();
      };
      
      this.websocket.onmessage = (event) => {
        this.handleWebSocketMessage(event);
      };
      
      this.websocket.onclose = () => {
        console.log('Widget collaboration WebSocket disconnected');
        this.isConnected = false;
        this.emit('disconnected');
        this.attemptReconnect();
      };
      
      this.websocket.onerror = (error) => {
        console.error('Widget collaboration WebSocket error:', error);
        this.emit('error', error);
      };
    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
      this.emit('error', error);
    }
  }

  /**
   * Handle incoming WebSocket messages
   */
  handleWebSocketMessage(event) {
    try {
      const message = JSON.parse(event.data);
      
      switch (message.type) {
        case 'widget_update':
          this.handleRemoteWidgetUpdate(message);
          break;
        case 'widget_lock':
          this.handleWidgetLock(message);
          break;
        case 'widget_unlock':
          this.handleWidgetUnlock(message);
          break;
        case 'collaborator_join':
          this.handleCollaboratorJoin(message);
          break;
        case 'collaborator_leave':
          this.handleCollaboratorLeave(message);
          break;
        case 'cursor_update':
          this.handleCursorUpdate(message);
          break;
        case 'presence_update':
          this.handlePresenceUpdate(message);
          break;
        default:
          console.warn('Unknown collaboration message type:', message.type);
      }
    } catch (error) {
      console.error('Failed to handle WebSocket message:', error);
    }
  }

  /**
   * Send presence information to other collaborators
   */
  sendPresence() {
    if (!this.isConnected) return;
    
    const presenceData = {
      type: 'presence_update',
      userId: this.userId,
      sessionId: this.sessionId,
      timestamp: Date.now(),
      userInfo: {
        name: localStorage.getItem('userName') || 'Anonymous',
        avatar: localStorage.getItem('userAvatar') || null
      }
    };
    
    this.websocket.send(JSON.stringify(presenceData));
  }

  /**
   * Broadcast widget update to other collaborators
   */
  broadcastWidgetUpdate(widgetId, value, metadata = {}) {
    if (!this.isConnected) {
      console.warn('Cannot broadcast widget update - not connected');
      return;
    }
    
    const updateData = {
      type: 'widget_update',
      widgetId: widgetId,
      value: value,
      userId: this.userId,
      sessionId: this.sessionId,
      timestamp: Date.now(),
      metadata: metadata
    };
    
    this.websocket.send(JSON.stringify(updateData));
  }

  /**
   * Handle remote widget updates from other collaborators
   */
  handleRemoteWidgetUpdate(message) {
    if (message.userId === this.userId) return; // Ignore own updates
    
    const { widgetId, value, userId, timestamp, metadata } = message;
    
    // Store the update
    this.pendingCollaborativeUpdates.set(widgetId, {
      value: value,
      userId: userId,
      timestamp: timestamp,
      metadata: metadata
    });
    
    // Emit event for the widget state context to handle
    this.emit('widget_update', {
      widgetId: widgetId,
      value: value,
      userId: userId,
      timestamp: timestamp,
      metadata: metadata
    });
    
    console.log(`Received widget update from ${userId}: ${widgetId} = ${value}`);
  }

  /**
   * Request exclusive lock on a widget
   */
  requestWidgetLock(widgetId) {
    if (!this.isConnected) return false;
    
    const lockRequest = {
      type: 'widget_lock_request',
      widgetId: widgetId,
      userId: this.userId,
      sessionId: this.sessionId,
      timestamp: Date.now()
    };
    
    this.websocket.send(JSON.stringify(lockRequest));
    return true;
  }

  /**
   * Release exclusive lock on a widget
   */
  releaseWidgetLock(widgetId) {
    if (!this.isConnected) return false;
    
    const unlockRequest = {
      type: 'widget_unlock_request',
      widgetId: widgetId,
      userId: this.userId,
      sessionId: this.sessionId,
      timestamp: Date.now()
    };
    
    this.websocket.send(JSON.stringify(unlockRequest));
    return true;
  }

  /**
   * Handle widget lock notifications
   */
  handleWidgetLock(message) {
    const { widgetId, userId, timestamp } = message;
    
    this.widgetLocks.set(widgetId, {
      userId: userId,
      timestamp: timestamp
    });
    
    this.emit('widget_locked', {
      widgetId: widgetId,
      userId: userId,
      timestamp: timestamp,
      isOwnLock: userId === this.userId
    });
  }

  /**
   * Handle widget unlock notifications
   */
  handleWidgetUnlock(message) {
    const { widgetId, userId } = message;
    
    this.widgetLocks.delete(widgetId);
    
    this.emit('widget_unlocked', {
      widgetId: widgetId,
      userId: userId,
      wasOwnLock: userId === this.userId
    });
  }

  /**
   * Handle collaborator join
   */
  handleCollaboratorJoin(message) {
    const { userId, userInfo } = message;
    
    this.collaborators.set(userId, {
      ...userInfo,
      joinedAt: Date.now()
    });
    
    this.emit('collaborator_joined', {
      userId: userId,
      userInfo: userInfo
    });
  }

  /**
   * Handle collaborator leave
   */
  handleCollaboratorLeave(message) {
    const { userId } = message;
    
    this.collaborators.delete(userId);
    
    // Remove any locks held by this user
    for (const [widgetId, lock] of this.widgetLocks) {
      if (lock.userId === userId) {
        this.widgetLocks.delete(widgetId);
        this.emit('widget_unlocked', {
          widgetId: widgetId,
          userId: userId,
          wasOwnLock: false
        });
      }
    }
    
    this.emit('collaborator_left', {
      userId: userId
    });
  }

  /**
   * Handle cursor/interaction updates
   */
  handleCursorUpdate(message) {
    const { userId, widgetId, action, position } = message;
    
    this.emit('cursor_update', {
      userId: userId,
      widgetId: widgetId,
      action: action,
      position: position
    });
  }

  /**
   * Handle presence updates
   */
  handlePresenceUpdate(message) {
    const { userId, userInfo } = message;
    
    if (userId !== this.userId) {
      this.collaborators.set(userId, {
        ...userInfo,
        lastSeen: Date.now()
      });
      
      this.emit('presence_update', {
        userId: userId,
        userInfo: userInfo
      });
    }
  }

  /**
   * Check if a widget is locked by another user
   */
  isWidgetLocked(widgetId) {
    const lock = this.widgetLocks.get(widgetId);
    return lock && lock.userId !== this.userId;
  }

  /**
   * Get information about who has locked a widget
   */
  getWidgetLockInfo(widgetId) {
    return this.widgetLocks.get(widgetId) || null;
  }

  /**
   * Get all current collaborators
   */
  getCollaborators() {
    return Array.from(this.collaborators.entries()).map(([userId, userInfo]) => ({
      userId: userId,
      ...userInfo
    }));
  }

  /**
   * Attempt to reconnect WebSocket
   */
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      this.emit('max_reconnect_attempts_reached');
      return;
    }
    
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;
    
    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);
    
    setTimeout(() => {
      this.initializeWebSocket();
    }, delay);
  }

  /**
   * Disconnect and cleanup
   */
  disconnect() {
    if (this.websocket) {
      this.websocket.close();
      this.websocket = null;
    }
    
    this.isConnected = false;
    this.collaborators.clear();
    this.widgetLocks.clear();
    this.pendingCollaborativeUpdates.clear();
    
    this.emit('disconnected');
  }

  /**
   * Get statistics about collaboration
   */
  getCollaborationStats() {
    return {
      isConnected: this.isConnected,
      collaboratorCount: this.collaborators.size,
      lockedWidgets: this.widgetLocks.size,
      pendingUpdates: this.pendingCollaborativeUpdates.size,
      reconnectAttempts: this.reconnectAttempts
    };
  }
}

// Collaboration utilities
export const collaborationUtils = {
  /**
   * Generate a unique color for a user
   */
  generateUserColor(userId) {
    const colors = [
      '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
      '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9'
    ];
    
    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
      hash = userId.charCodeAt(i) + ((hash << 5) - hash);
    }
    
    return colors[Math.abs(hash) % colors.length];
  },

  /**
   * Create a user avatar element
   */
  createUserAvatar(userInfo) {
    const color = this.generateUserColor(userInfo.userId);
    const initials = userInfo.name 
      ? userInfo.name.split(' ').map(n => n[0]).join('').toUpperCase()
      : 'U';
    
    return {
      initials: initials,
      color: color,
      avatar: userInfo.avatar || null
    };
  },

  /**
   * Format time since last activity
   */
  formatLastSeen(timestamp) {
    const now = Date.now();
    const diff = now - timestamp;
    
    if (diff < 60000) return 'just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return `${Math.floor(diff / 86400000)}d ago`;
  }
};
