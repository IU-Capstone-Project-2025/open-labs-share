/**
 * Phase 5: Widget Persistence System
 * Manages saving and loading widget states across sessions
 */

import { marimoAPI } from './api';

export class WidgetPersistenceManager {
  constructor() {
    this.cache = new Map();
    this.persistenceStrategies = {
      'session': new SessionPersistenceStrategy(),
      'local': new LocalStoragePersistenceStrategy(),
      'server': new ServerPersistenceStrategy()
    };
    this.defaultStrategy = 'session';
    this.autoSaveInterval = null;
    this.autoSaveDelay = 5000; // 5 seconds
  }

  /**
   * Save widget state with specified strategy
   */
  async saveWidgetState(sessionId, widgetId, state, strategy = this.defaultStrategy) {
    try {
      const persistenceStrategy = this.persistenceStrategies[strategy];
      if (!persistenceStrategy) {
        throw new Error(`Unknown persistence strategy: ${strategy}`);
      }

      const stateData = {
        sessionId: sessionId,
        widgetId: widgetId,
        state: state,
        timestamp: Date.now(),
        version: '1.0.0'
      };

      await persistenceStrategy.save(stateData);
      
      // Update cache
      const cacheKey = `${sessionId}:${widgetId}`;
      this.cache.set(cacheKey, stateData);
      
      console.log(`Widget state saved: ${widgetId} (${strategy})`);
      return true;
    } catch (error) {
      console.error(`Failed to save widget state: ${error.message}`);
      return false;
    }
  }

  /**
   * Load widget state with specified strategy
   */
  async loadWidgetState(sessionId, widgetId, strategy = this.defaultStrategy) {
    try {
      const cacheKey = `${sessionId}:${widgetId}`;
      
      // Check cache first
      if (this.cache.has(cacheKey)) {
        const cached = this.cache.get(cacheKey);
        if (Date.now() - cached.timestamp < 300000) { // 5 minutes cache
          return cached.state;
        }
      }

      const persistenceStrategy = this.persistenceStrategies[strategy];
      if (!persistenceStrategy) {
        throw new Error(`Unknown persistence strategy: ${strategy}`);
      }

      const stateData = await persistenceStrategy.load(sessionId, widgetId);
      
      if (stateData) {
        // Update cache
        this.cache.set(cacheKey, stateData);
        return stateData.state;
      }
      
      return null;
    } catch (error) {
      console.error(`Failed to load widget state: ${error.message}`);
      return null;
    }
  }

  /**
   * Save multiple widget states in batch
   */
  async batchSaveWidgetStates(sessionId, widgetStates, strategy = this.defaultStrategy) {
    try {
      const persistenceStrategy = this.persistenceStrategies[strategy];
      if (!persistenceStrategy) {
        throw new Error(`Unknown persistence strategy: ${strategy}`);
      }

      const batchData = widgetStates.map(({ widgetId, state }) => ({
        sessionId: sessionId,
        widgetId: widgetId,
        state: state,
        timestamp: Date.now(),
        version: '1.0.0'
      }));

      if (persistenceStrategy.batchSave) {
        await persistenceStrategy.batchSave(batchData);
      } else {
        // Fallback to individual saves
        await Promise.all(batchData.map(data => persistenceStrategy.save(data)));
      }

      // Update cache
      batchData.forEach(data => {
        const cacheKey = `${sessionId}:${data.widgetId}`;
        this.cache.set(cacheKey, data);
      });

      console.log(`Batch saved ${batchData.length} widget states (${strategy})`);
      return true;
    } catch (error) {
      console.error(`Failed to batch save widget states: ${error.message}`);
      return false;
    }
  }

  /**
   * Load multiple widget states in batch
   */
  async batchLoadWidgetStates(sessionId, widgetIds, strategy = this.defaultStrategy) {
    try {
      const persistenceStrategy = this.persistenceStrategies[strategy];
      if (!persistenceStrategy) {
        throw new Error(`Unknown persistence strategy: ${strategy}`);
      }

      const results = new Map();

      // Check cache first
      const uncachedIds = [];
      for (const widgetId of widgetIds) {
        const cacheKey = `${sessionId}:${widgetId}`;
        if (this.cache.has(cacheKey)) {
          const cached = this.cache.get(cacheKey);
          if (Date.now() - cached.timestamp < 300000) { // 5 minutes cache
            results.set(widgetId, cached.state);
            continue;
          }
        }
        uncachedIds.push(widgetId);
      }

      // Load uncached states
      if (uncachedIds.length > 0) {
        if (persistenceStrategy.batchLoad) {
          const batchData = await persistenceStrategy.batchLoad(sessionId, uncachedIds);
          batchData.forEach(data => {
            if (data) {
              results.set(data.widgetId, data.state);
              const cacheKey = `${sessionId}:${data.widgetId}`;
              this.cache.set(cacheKey, data);
            }
          });
        } else {
          // Fallback to individual loads
          await Promise.all(uncachedIds.map(async widgetId => {
            const data = await persistenceStrategy.load(sessionId, widgetId);
            if (data) {
              results.set(widgetId, data.state);
              const cacheKey = `${sessionId}:${widgetId}`;
              this.cache.set(cacheKey, data);
            }
          }));
        }
      }

      return results;
    } catch (error) {
      console.error(`Failed to batch load widget states: ${error.message}`);
      return new Map();
    }
  }

  /**
   * Delete widget state
   */
  async deleteWidgetState(sessionId, widgetId, strategy = this.defaultStrategy) {
    try {
      const persistenceStrategy = this.persistenceStrategies[strategy];
      if (!persistenceStrategy) {
        throw new Error(`Unknown persistence strategy: ${strategy}`);
      }

      await persistenceStrategy.delete(sessionId, widgetId);
      
      // Remove from cache
      const cacheKey = `${sessionId}:${widgetId}`;
      this.cache.delete(cacheKey);
      
      console.log(`Widget state deleted: ${widgetId} (${strategy})`);
      return true;
    } catch (error) {
      console.error(`Failed to delete widget state: ${error.message}`);
      return false;
    }
  }

  /**
   * Clear all widget states for a session
   */
  async clearSessionStates(sessionId, strategy = this.defaultStrategy) {
    try {
      const persistenceStrategy = this.persistenceStrategies[strategy];
      if (!persistenceStrategy) {
        throw new Error(`Unknown persistence strategy: ${strategy}`);
      }

      if (persistenceStrategy.clearSession) {
        await persistenceStrategy.clearSession(sessionId);
      }
      
      // Clear cache for this session
      for (const [cacheKey] of this.cache) {
        if (cacheKey.startsWith(`${sessionId}:`)) {
          this.cache.delete(cacheKey);
        }
      }
      
      console.log(`Session states cleared: ${sessionId} (${strategy})`);
      return true;
    } catch (error) {
      console.error(`Failed to clear session states: ${error.message}`);
      return false;
    }
  }

  /**
   * Start auto-save for a session
   */
  startAutoSave(sessionId, getWidgetStates, strategy = this.defaultStrategy) {
    this.stopAutoSave();
    
    this.autoSaveInterval = setInterval(async () => {
      try {
        const widgetStates = getWidgetStates();
        if (widgetStates.length > 0) {
          await this.batchSaveWidgetStates(sessionId, widgetStates, strategy);
        }
      } catch (error) {
        console.error('Auto-save failed:', error);
      }
    }, this.autoSaveDelay);
    
    console.log(`Auto-save started for session: ${sessionId}`);
  }

  /**
   * Stop auto-save
   */
  stopAutoSave() {
    if (this.autoSaveInterval) {
      clearInterval(this.autoSaveInterval);
      this.autoSaveInterval = null;
      console.log('Auto-save stopped');
    }
  }

  /**
   * Get persistence statistics
   */
  getPersistenceStats() {
    return {
      cacheSize: this.cache.size,
      availableStrategies: Object.keys(this.persistenceStrategies),
      defaultStrategy: this.defaultStrategy,
      autoSaveActive: this.autoSaveInterval !== null,
      autoSaveDelay: this.autoSaveDelay
    };
  }
}

/**
 * Session-based persistence strategy (in-memory for the session)
 */
class SessionPersistenceStrategy {
  constructor() {
    this.sessionData = new Map();
  }

  async save(stateData) {
    const key = `${stateData.sessionId}:${stateData.widgetId}`;
    this.sessionData.set(key, stateData);
  }

  async load(sessionId, widgetId) {
    const key = `${sessionId}:${widgetId}`;
    return this.sessionData.get(key) || null;
  }

  async delete(sessionId, widgetId) {
    const key = `${sessionId}:${widgetId}`;
    this.sessionData.delete(key);
  }

  async clearSession(sessionId) {
    for (const [key] of this.sessionData) {
      if (key.startsWith(`${sessionId}:`)) {
        this.sessionData.delete(key);
      }
    }
  }

  async batchSave(batchData) {
    batchData.forEach(data => {
      const key = `${data.sessionId}:${data.widgetId}`;
      this.sessionData.set(key, data);
    });
  }

  async batchLoad(sessionId, widgetIds) {
    return widgetIds.map(widgetId => {
      const key = `${sessionId}:${widgetId}`;
      return this.sessionData.get(key) || null;
    });
  }
}

/**
 * Local storage persistence strategy
 */
class LocalStoragePersistenceStrategy {
  constructor() {
    this.storageKey = 'marimo_widget_states';
  }

  async save(stateData) {
    try {
      const existing = this.getStoredData();
      const key = `${stateData.sessionId}:${stateData.widgetId}`;
      existing[key] = stateData;
      localStorage.setItem(this.storageKey, JSON.stringify(existing));
    } catch (error) {
      console.error('LocalStorage save failed:', error);
    }
  }

  async load(sessionId, widgetId) {
    try {
      const existing = this.getStoredData();
      const key = `${sessionId}:${widgetId}`;
      return existing[key] || null;
    } catch (error) {
      console.error('LocalStorage load failed:', error);
      return null;
    }
  }

  async delete(sessionId, widgetId) {
    try {
      const existing = this.getStoredData();
      const key = `${sessionId}:${widgetId}`;
      delete existing[key];
      localStorage.setItem(this.storageKey, JSON.stringify(existing));
    } catch (error) {
      console.error('LocalStorage delete failed:', error);
    }
  }

  async clearSession(sessionId) {
    try {
      const existing = this.getStoredData();
      const filtered = {};
      for (const [key, value] of Object.entries(existing)) {
        if (!key.startsWith(`${sessionId}:`)) {
          filtered[key] = value;
        }
      }
      localStorage.setItem(this.storageKey, JSON.stringify(filtered));
    } catch (error) {
      console.error('LocalStorage clearSession failed:', error);
    }
  }

  async batchSave(batchData) {
    try {
      const existing = this.getStoredData();
      batchData.forEach(data => {
        const key = `${data.sessionId}:${data.widgetId}`;
        existing[key] = data;
      });
      localStorage.setItem(this.storageKey, JSON.stringify(existing));
    } catch (error) {
      console.error('LocalStorage batchSave failed:', error);
    }
  }

  async batchLoad(sessionId, widgetIds) {
    try {
      const existing = this.getStoredData();
      return widgetIds.map(widgetId => {
        const key = `${sessionId}:${widgetId}`;
        return existing[key] || null;
      });
    } catch (error) {
      console.error('LocalStorage batchLoad failed:', error);
      return [];
    }
  }

  getStoredData() {
    try {
      const stored = localStorage.getItem(this.storageKey);
      return stored ? JSON.parse(stored) : {};
    } catch (error) {
      console.error('Failed to parse stored data:', error);
      return {};
    }
  }
}

/**
 * Server-based persistence strategy
 */
class ServerPersistenceStrategy {
  async save(stateData) {
    try {
      await marimoAPI.saveWidgetState(stateData.sessionId, stateData.widgetId, stateData.state);
    } catch (error) {
      console.error('Server save failed:', error);
      throw error;
    }
  }

  async load(sessionId, widgetId) {
    try {
      const state = await marimoAPI.loadWidgetState(sessionId, widgetId);
      return state ? { sessionId, widgetId, state, timestamp: Date.now() } : null;
    } catch (error) {
      console.error('Server load failed:', error);
      return null;
    }
  }

  async delete(sessionId, widgetId) {
    try {
      await marimoAPI.deleteWidgetState(sessionId, widgetId);
    } catch (error) {
      console.error('Server delete failed:', error);
      throw error;
    }
  }

  async clearSession(sessionId) {
    try {
      await marimoAPI.clearSessionWidgetStates(sessionId);
    } catch (error) {
      console.error('Server clearSession failed:', error);
      throw error;
    }
  }

  async batchSave(batchData) {
    try {
      await marimoAPI.batchSaveWidgetStates(batchData);
    } catch (error) {
      console.error('Server batchSave failed:', error);
      throw error;
    }
  }

  async batchLoad(sessionId, widgetIds) {
    try {
      const states = await marimoAPI.batchLoadWidgetStates(sessionId, widgetIds);
      return widgetIds.map(widgetId => {
        const state = states[widgetId];
        return state ? { sessionId, widgetId, state, timestamp: Date.now() } : null;
      });
    } catch (error) {
      console.error('Server batchLoad failed:', error);
      return [];
    }
  }
}

// Global persistence manager instance
export const widgetPersistenceManager = new WidgetPersistenceManager();
