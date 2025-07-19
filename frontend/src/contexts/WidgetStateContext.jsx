import React, { createContext, useContext, useState, useCallback, useRef, useEffect } from 'react';
import { marimoAPI } from '../utils/api';
import { WidgetCollaborationManager } from '../utils/widgetCollaboration';
import { widgetPersistenceManager } from '../utils/widgetPersistence';
import { widgetTemplateManager } from '../utils/widgetTemplates';
import { validateValue, autoFixValue, extractConstraints, autoFixWidgetValue } from '../utils/widgetUtils';

const WidgetStateContext = createContext();

const useWidgetState = () => {
  const context = useContext(WidgetStateContext);
  if (!context) {
    throw new Error('useWidgetState must be used within a WidgetStateProvider');
  }
  return context;
};

const WidgetStateProvider = ({ children, sessionId, userId = null }) => {
  const [widgets, setWidgets] = useState(new Map());
  const [pendingUpdates, setPendingUpdates] = useState(new Map());
  const [widgetHistory, setWidgetHistory] = useState(new Map());
  const [batchUpdates, setBatchUpdates] = useState(new Set());
  const [widgetVersions, setWidgetVersions] = useState(new Map());
  const [collaborationEnabled, setCollaborationEnabled] = useState(false);
  const [interactiveWidgets, setInteractiveWidgets] = useState(new Set()); // Track widgets currently being interacted with
  const updateTimeouts = useRef(new Map());
  const mountedRef = useRef(true);
  const cacheRef = useRef(new Map());
  const performanceMetrics = useRef(new Map());
  const collaborationManager = useRef(null);
  const lastPersistenceSnapshot = useRef(new Map());

  // Phase 5: Initialize collaboration and persistence
  useEffect(() => {
    if (sessionId && userId) {
      // Initialize collaboration manager
      collaborationManager.current = new WidgetCollaborationManager(sessionId, userId);
      
      // Set up collaboration event handlers
      collaborationManager.current.on('widget_update', handleCollaborativeUpdate);
      collaborationManager.current.on('widget_locked', handleWidgetLocked);
      collaborationManager.current.on('widget_unlocked', handleWidgetUnlocked);
      
      setCollaborationEnabled(true);
      
      // Start auto-save for persistence
      widgetPersistenceManager.startAutoSave(
        sessionId,
        () => getAllWidgetsForPersistence(),
        'local' // Use local storage by default
      );
    }
    
    return () => {
      if (collaborationManager.current) {
        collaborationManager.current.disconnect();
      }
      widgetPersistenceManager.stopAutoSave();
    };
  }, [sessionId, userId]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      mountedRef.current = false;
      // Clear all pending timeouts
      updateTimeouts.current.forEach(timeout => clearTimeout(timeout));
      updateTimeouts.current.clear();
    };
  }, []);

  // Phase 5: Handle collaborative widget updates
  const handleCollaborativeUpdate = useCallback((updateData) => {
    if (!mountedRef.current) return;
    
    const { widgetId, value, userId: updateUserId, timestamp } = updateData;
    
    setWidgets(prev => {
      const newWidgets = new Map(prev);
      const widget = newWidgets.get(widgetId);
      
      if (widget) {
        // Update widget with collaborative information
        newWidgets.set(widgetId, {
          ...widget,
          value: value,
          lastUpdated: timestamp,
          collaborativeUpdate: {
            userId: updateUserId,
            timestamp: timestamp
          }
        });
        
        // Track version history
        trackWidgetVersion(widgetId, value, updateUserId, timestamp);
      }
      
      return newWidgets;
    });
    
    console.log(`Collaborative update applied: ${widgetId} by ${updateUserId}`);
  }, []);

  // Phase 5: Handle widget locking
  const handleWidgetLocked = useCallback((lockData) => {
    if (!mountedRef.current) return;
    
    const { widgetId, userId: lockUserId, isOwnLock } = lockData;
    
    setWidgets(prev => {
      const newWidgets = new Map(prev);
      const widget = newWidgets.get(widgetId);
      
      if (widget) {
        newWidgets.set(widgetId, {
          ...widget,
          isLocked: true,
          lockedBy: lockUserId,
          isOwnLock: isOwnLock
        });
      }
      
      return newWidgets;
    });
    
    console.log(`Widget locked: ${widgetId} by ${lockUserId}`);
  }, []);

  // Phase 5: Handle widget unlocking
  const handleWidgetUnlocked = useCallback((unlockData) => {
    if (!mountedRef.current) return;
    
    const { widgetId } = unlockData;
    
    setWidgets(prev => {
      const newWidgets = new Map(prev);
      const widget = newWidgets.get(widgetId);
      
      if (widget) {
        newWidgets.set(widgetId, {
          ...widget,
          isLocked: false,
          lockedBy: null,
          isOwnLock: false
        });
      }
      
      return newWidgets;
    });
    
    console.log(`Widget unlocked: ${widgetId}`);
  }, []);

  // Phase 5: Track widget version history
  const trackWidgetVersion = useCallback((widgetId, value, userId, timestamp) => {
    setWidgetVersions(prev => {
      const newVersions = new Map(prev);
      const versions = newVersions.get(widgetId) || [];
      
      versions.push({
        value: value,
        userId: userId,
        timestamp: timestamp,
        version: versions.length + 1
      });
      
      // Keep only last 10 versions
      if (versions.length > 10) {
        versions.shift();
      }
      
      newVersions.set(widgetId, versions);
      return newVersions;
    });
  }, []);

  // Phase 5: Get all widgets for persistence
  const getAllWidgetsForPersistence = useCallback(() => {
    const widgetStates = [];
    
    for (const [widgetId, widget] of widgets) {
      // Only save if widget has changed since last snapshot
      const lastValue = lastPersistenceSnapshot.current.get(widgetId);
      if (lastValue !== widget.value) {
        widgetStates.push({
          widgetId: widgetId,
          state: {
            value: widget.value,
            type: widget.type,
            properties: widget.properties,
            lastUpdated: widget.lastUpdated
          }
        });
        lastPersistenceSnapshot.current.set(widgetId, widget.value);
      }
    }
    
    return widgetStates;
  }, [widgets]);
  // Register a widget with its initial state
  const registerWidget = useCallback(async (widgetId, widgetType, initialValue, properties = {}) => {
    if (!mountedRef.current) return;

    // Check if widget already exists to prevent duplicate registration
    if (widgets.has(widgetId)) {
      console.log(`Widget ${widgetId} already registered, skipping registration`);
      return;
    }

    const startTime = performance.now();

    // Phase 5: Try to load persisted state first
    let persistedState = null;
    if (sessionId) {
      persistedState = await widgetPersistenceManager.loadWidgetState(sessionId, widgetId);
    }

    const finalValue = persistedState?.value !== undefined ? persistedState.value : initialValue;

    setWidgets(prev => {
      const newWidgets = new Map(prev);
      const widget = {
        id: widgetId,
        type: widgetType,
        value: finalValue,
        properties: properties,
        constraints: extractConstraints(widgetType, properties),
        isLoading: false,
        error: null,
        lastUpdated: Date.now(),
        updateCount: 0,
        interactionHistory: [],
        // Phase 5: Additional properties
        isLocked: false,
        lockedBy: null,
        isOwnLock: false,
        collaborativeUpdate: null,
        templateId: properties.templateId || null,
        version: 1
      };
      
      newWidgets.set(widgetId, widget);
      
      // Cache widget metadata
      cacheRef.current.set(widgetId, {
        type: widgetType,
        properties: properties,
        constraints: widget.constraints
      });
      
      return newWidgets;
    });

    // Initialize widget history
    setWidgetHistory(prev => {
      const newHistory = new Map(prev);
      newHistory.set(widgetId, [{
        value: finalValue,
        timestamp: Date.now(),
        action: 'register'
      }]);
      return newHistory;
    });

    // Phase 5: Initialize version tracking
    trackWidgetVersion(widgetId, finalValue, userId || 'system', Date.now());

    const endTime = performance.now();
    performanceMetrics.current.set(`register_${widgetId}`, endTime - startTime);

    console.log(`Widget registered: ${widgetId} (${widgetType}) with value:`, finalValue);
  }, [sessionId, userId, trackWidgetVersion]);

  // Start widget interaction (when user begins interacting)
  const startWidgetInteraction = useCallback((widgetId) => {
    if (!mountedRef.current) return;
    
    setInteractiveWidgets(prev => new Set([...prev, widgetId]));
    console.log(`Widget interaction started: ${widgetId}`);
  }, []);

  // End widget interaction (when user stops interacting)
  const endWidgetInteraction = useCallback((widgetId) => {
    if (!mountedRef.current) return;
    
    setInteractiveWidgets(prev => {
      const newSet = new Set(prev);
      newSet.delete(widgetId);
      return newSet;
    });
    console.log(`Widget interaction ended: ${widgetId}`);
  }, []);

  // Update widget value immediately (for UI responsiveness) but defer backend update
  const updateWidgetValueImmediate = useCallback((widgetId, newValue) => {
    if (!mountedRef.current) return;

    const widget = widgets.get(widgetId);
    if (!widget) return;

    // Update local state immediately for responsive UI
    setWidgets(prev => {
      const newWidgets = new Map(prev);
      const currentWidget = newWidgets.get(widgetId);
      if (currentWidget) {
        newWidgets.set(widgetId, {
          ...currentWidget,
          value: newValue,
          lastUpdated: Date.now()
        });
      }
      return newWidgets;
    });
  }, [widgets]);

  // Update widget value with debouncing, validation, and batching
  const updateWidgetValue = useCallback(async (widgetId, newValue, debounceMs = 300, options = {}) => {
    if (!mountedRef.current || !sessionId) return;

    const startTime = performance.now();
    const { batchUpdate = false, priority = 'normal', commitImmediately = false } = options;

    // Get cached widget metadata for faster validation
    const cachedWidget = cacheRef.current.get(widgetId);
    const widget = widgets.get(widgetId);
    
    if (cachedWidget && cachedWidget.constraints) {
      const validationResult = validateValue(cachedWidget.constraints, newValue);
      if (!validationResult.isValid) {
        console.warn(`Invalid value for widget ${widgetId}:`, validationResult.error);
        // Auto-fix the value
        newValue = autoFixWidgetValue(cachedWidget.constraints, newValue);
      }
    }

    // Check if value actually changed to avoid unnecessary updates
    if (widget && widget.value === newValue) {
      console.log(`Widget ${widgetId} value unchanged, skipping update`);
      return;
    }

    // If widget is currently being interacted with and not forcing immediate commit, only update local state
    if (interactiveWidgets.has(widgetId) && !commitImmediately) {
      updateWidgetValueImmediate(widgetId, newValue);
      return;
    }

    // Update widget history
    setWidgetHistory(prev => {
      const newHistory = new Map(prev);
      const history = newHistory.get(widgetId) || [];
      history.push({
        value: newValue,
        timestamp: Date.now(),
        action: 'update',
        priority: priority
      });
      
      // Keep only last 10 history entries
      if (history.length > 10) {
        history.shift();
      }
      
      newHistory.set(widgetId, history);
      return newHistory;
    });

    // Update local state immediately for responsive UI
    setWidgets(prev => {
      const newWidgets = new Map(prev);
      const widget = newWidgets.get(widgetId);
      if (widget) {
        newWidgets.set(widgetId, {
          ...widget,
          value: newValue,
          isLoading: true,
          error: null,
          updateCount: widget.updateCount + 1,
          interactionHistory: [...widget.interactionHistory, {
            timestamp: Date.now(),
            action: 'value_change',
            value: newValue
          }].slice(-5) // Keep last 5 interactions
        });
      }
      return newWidgets;
    });

    // Handle batch updates
    if (batchUpdate) {
      setBatchUpdates(prev => new Set([...prev, widgetId]));
      
      // Store pending update
      setPendingUpdates(prev => {
        const newPending = new Map(prev);
        newPending.set(widgetId, { value: newValue, priority, timestamp: Date.now() });
        return newPending;
      });
      
      return; // Don't process immediately in batch mode
    }

    // Store pending update
    setPendingUpdates(prev => {
      const newPending = new Map(prev);
      newPending.set(widgetId, { value: newValue, priority, timestamp: Date.now() });
      return newPending;
    });

    // Clear existing timeout for this widget
    if (updateTimeouts.current.has(widgetId)) {
      clearTimeout(updateTimeouts.current.get(widgetId));
    }

    // Set new debounced update with priority handling
    const timeoutId = setTimeout(async () => {
      if (!mountedRef.current) return;

      try {
        console.log(`Updating widget ${widgetId} with value:`, newValue);
        
        // Prepare value for API call - convert arrays and objects to JSON strings
        let apiValue = newValue;
        if (Array.isArray(newValue) || (typeof newValue === 'object' && newValue !== null && !(newValue instanceof Date))) {
          apiValue = JSON.stringify(newValue);
        }
        
        // Make API call to update widget value
        const updateStartTime = performance.now();
        await marimoAPI.updateWidgetValue(sessionId, widgetId, apiValue);
        const updateEndTime = performance.now();
        
        // Track performance
        performanceMetrics.current.set(`update_${widgetId}`, updateEndTime - updateStartTime);

        // Phase 5: Broadcast collaborative update
        if (collaborationEnabled && collaborationManager.current) {
          collaborationManager.current.broadcastWidgetUpdate(widgetId, newValue, {
            updateType: 'user_interaction',
            priority: priority
          });
        }

        // Phase 5: Track version
        trackWidgetVersion(widgetId, newValue, userId || 'user', Date.now());

        if (mountedRef.current) {
          // Update success state
          setWidgets(prev => {
            const newWidgets = new Map(prev);
            const widget = newWidgets.get(widgetId);
            if (widget) {
              newWidgets.set(widgetId, {
                ...widget,
                value: newValue,
                isLoading: false,
                error: null,
                lastUpdated: Date.now()
              });
            }
            return newWidgets;
          });

          // Remove from pending updates
          setPendingUpdates(prev => {
            const newPending = new Map(prev);
            newPending.delete(widgetId);
            return newPending;
          });

          console.log(`Widget ${widgetId} updated successfully`);
        }
      } catch (error) {
        console.error(`Failed to update widget ${widgetId}:`, error);
        
        if (mountedRef.current) {
          // Update error state
          setWidgets(prev => {
            const newWidgets = new Map(prev);
            const widget = newWidgets.get(widgetId);
            if (widget) {
              newWidgets.set(widgetId, {
                ...widget,
                isLoading: false,
                error: error.message || 'Failed to update widget'
              });
            }
            return newWidgets;
          });

          // Remove from pending updates
          setPendingUpdates(prev => {
            const newPending = new Map(prev);
            newPending.delete(widgetId);
            return newPending;
          });
        }
      }

      // Clean up timeout reference
      updateTimeouts.current.delete(widgetId);
    }, priority === 'high' ? Math.min(debounceMs, 100) : debounceMs);

    updateTimeouts.current.set(widgetId, timeoutId);
    
    const endTime = performance.now();
    performanceMetrics.current.set(`updateWidgetValue_${widgetId}`, endTime - startTime);
  }, [sessionId, widgets, interactiveWidgets, updateWidgetValueImmediate]);

  // Commit widget value to backend (called when interaction ends)
  const commitWidgetValue = useCallback(async (widgetId) => {
    if (!mountedRef.current) return;
    
    const widget = widgets.get(widgetId);
    if (!widget) return;
    
    console.log(`Committing widget value: ${widgetId} = ${widget.value}`);
    
    try {
      // Prepare value for API call - convert arrays and objects to JSON strings
      let apiValue = widget.value;
      if (Array.isArray(widget.value) || (typeof widget.value === 'object' && widget.value !== null && !(widget.value instanceof Date))) {
        apiValue = JSON.stringify(widget.value);
      }
      
      // Directly call the API to update the backend session
      await marimoAPI.updateWidgetValue(sessionId, widgetId, apiValue);
      console.log(`Successfully committed widget ${widgetId} value to backend`);
    } catch (error) {
      console.error(`Failed to commit widget ${widgetId} value:`, error);
      
      // Update error state
      setWidgets(prev => {
        const newWidgets = new Map(prev);
        const widget = newWidgets.get(widgetId);
        if (widget) {
          widget.error = error.message || 'Failed to commit widget value';
        }
        return newWidgets;
      });
    }
  }, [widgets, sessionId]);

  // Get widget state
  const getWidget = useCallback((widgetId) => {
    return widgets.get(widgetId) || null;
  }, [widgets]);

  // Check if widget exists
  const hasWidget = useCallback((widgetId) => {
    return widgets.has(widgetId);
  }, [widgets]);

  // Get all widgets
  const getAllWidgets = useCallback(() => {
    return Array.from(widgets.values());
  }, [widgets]);

  // Clear all widgets (useful for session cleanup)
  const clearWidgets = useCallback(() => {
    if (!mountedRef.current) return;

    // Clear all pending timeouts
    updateTimeouts.current.forEach(timeout => clearTimeout(timeout));
    updateTimeouts.current.clear();

    setWidgets(new Map());
    setPendingUpdates(new Map());
    
    console.log('All widgets cleared');
  }, []);

  // Remove specific widget
  const removeWidget = useCallback((widgetId) => {
    if (!mountedRef.current) return;

    // Clear timeout for this widget
    if (updateTimeouts.current.has(widgetId)) {
      clearTimeout(updateTimeouts.current.get(widgetId));
      updateTimeouts.current.delete(widgetId);
    }

    setWidgets(prev => {
      const newWidgets = new Map(prev);
      newWidgets.delete(widgetId);
      return newWidgets;
    });

    setPendingUpdates(prev => {
      const newPending = new Map(prev);
      newPending.delete(widgetId);
      return newPending;
    });

    // Clean up cache and history
    cacheRef.current.delete(widgetId);
    setWidgetHistory(prev => {
      const newHistory = new Map(prev);
      newHistory.delete(widgetId);
      return newHistory;
    });

    console.log(`Widget ${widgetId} removed`);
  }, []);

  // Batch update multiple widgets
  const batchUpdateWidgets = useCallback(async (updates) => {
    if (!mountedRef.current || !sessionId || !updates.length) return;

    console.log('Processing batch update for widgets:', updates.map(u => u.widgetId));
    
    try {
      // Sort updates by priority
      const sortedUpdates = updates.sort((a, b) => {
        const priorityOrder = { high: 3, normal: 2, low: 1 };
        return priorityOrder[b.priority || 'normal'] - priorityOrder[a.priority || 'normal'];
      });

      // Process batch update
      await marimoAPI.batchUpdateWidgets(sessionId, sortedUpdates);

      // Update all widgets in batch
      setWidgets(prev => {
        const newWidgets = new Map(prev);
        sortedUpdates.forEach(({ widgetId, value }) => {
          const widget = newWidgets.get(widgetId);
          if (widget) {
            newWidgets.set(widgetId, {
              ...widget,
              value: value,
              isLoading: false,
              error: null,
              lastUpdated: Date.now()
            });
          }
        });
        return newWidgets;
      });

      // Clear batch updates
      setBatchUpdates(new Set());
      
      console.log('Batch update completed successfully');
    } catch (error) {
      console.error('Batch update failed:', error);
      
      // Update error state for all widgets in batch
      setWidgets(prev => {
        const newWidgets = new Map(prev);
        updates.forEach(({ widgetId }) => {
          const widget = newWidgets.get(widgetId);
          if (widget) {
            newWidgets.set(widgetId, {
              ...widget,
              isLoading: false,
              error: 'Batch update failed'
            });
          }
        });
        return newWidgets;
      });
    }
  }, [sessionId]);

  // Process pending batch updates
  const processBatchUpdates = useCallback(() => {
    if (batchUpdates.size === 0) return;

    const updates = Array.from(batchUpdates).map(widgetId => {
      const pendingUpdate = pendingUpdates.get(widgetId);
      return {
        widgetId,
        value: pendingUpdate?.value,
        priority: pendingUpdate?.priority || 'normal'
      };
    }).filter(update => update.value !== undefined);

    if (updates.length > 0) {
      batchUpdateWidgets(updates);
    }
  }, [batchUpdates, pendingUpdates, batchUpdateWidgets]);

  // Get widget performance metrics
  const getPerformanceMetrics = useCallback(() => {
    return Object.fromEntries(performanceMetrics.current);
  }, []);

  // Get widget history
  const getWidgetHistory = useCallback((widgetId) => {
    return widgetHistory.get(widgetId) || [];
  }, [widgetHistory]);

  // Optimized widget search
  const searchWidgets = useCallback((query) => {
    const results = [];
    for (const [widgetId, widget] of widgets.entries()) {
      if (
        widgetId.toLowerCase().includes(query.toLowerCase()) ||
        widget.type.toLowerCase().includes(query.toLowerCase()) ||
        (widget.properties.label && widget.properties.label.toLowerCase().includes(query.toLowerCase()))
      ) {
        results.push(widget);
      }
    }
    return results;
  }, [widgets]);

  // Widget analytics
  const getWidgetAnalytics = useCallback(() => {
    const analytics = {
      totalWidgets: widgets.size,
      widgetTypes: {},
      totalUpdates: 0,
      averageUpdateTime: 0,
      errorRate: 0
    };

    let totalErrors = 0;
    let totalUpdateTime = 0;
    let updateCount = 0;

    widgets.forEach((widget) => {
      // Count widget types
      analytics.widgetTypes[widget.type] = (analytics.widgetTypes[widget.type] || 0) + 1;
      
      // Count updates and errors
      analytics.totalUpdates += widget.updateCount || 0;
      if (widget.error) totalErrors++;
      
      // Performance metrics
      const updateMetric = performanceMetrics.current.get(`update_${widget.id}`);
      if (updateMetric) {
        totalUpdateTime += updateMetric;
        updateCount++;
      }
    });

    analytics.errorRate = widgets.size > 0 ? (totalErrors / widgets.size) * 100 : 0;
    analytics.averageUpdateTime = updateCount > 0 ? totalUpdateTime / updateCount : 0;

    return analytics;
  }, [widgets]);

  // Phase 5: Widget template management
  const createWidgetFromTemplate = useCallback((templateId, overrides = {}) => {
    try {
      const widget = widgetTemplateManager.createWidgetFromTemplate(templateId, overrides);
      registerWidget(widget.id, widget.type, widget.value, widget.properties);
      return widget;
    } catch (error) {
      console.error('Failed to create widget from template:', error);
      return null;
    }
  }, [registerWidget]);

  const saveWidgetAsTemplate = useCallback((widgetId, templateId, metadata = {}) => {
    const widget = widgets.get(widgetId);
    if (!widget) {
      console.error('Widget not found:', widgetId);
      return false;
    }

    try {
      const template = widgetTemplateManager.saveTemplate(templateId, widget, metadata);
      console.log('Widget saved as template:', template);
      return true;
    } catch (error) {
      console.error('Failed to save widget as template:', error);
      return false;
    }
  }, [widgets]);

  // Phase 5: Widget collaboration management
  const requestWidgetLock = useCallback((widgetId) => {
    if (collaborationEnabled && collaborationManager.current) {
      return collaborationManager.current.requestWidgetLock(widgetId);
    }
    return false;
  }, [collaborationEnabled]);

  const releaseWidgetLock = useCallback((widgetId) => {
    if (collaborationEnabled && collaborationManager.current) {
      return collaborationManager.current.releaseWidgetLock(widgetId);
    }
    return false;
  }, [collaborationEnabled]);

  const getCollaborators = useCallback(() => {
    if (collaborationEnabled && collaborationManager.current) {
      return collaborationManager.current.getCollaborators();
    }
    return [];
  }, [collaborationEnabled]);

  // Phase 5: Widget versioning
  const getWidgetVersions = useCallback((widgetId) => {
    return widgetVersions.get(widgetId) || [];
  }, [widgetVersions]);

  const revertWidgetToVersion = useCallback(async (widgetId, version) => {
    const versions = widgetVersions.get(widgetId) || [];
    const targetVersion = versions.find(v => v.version === version);
    
    if (!targetVersion) {
      console.error('Version not found:', version);
      return false;
    }

    try {
      await updateWidgetValue(widgetId, targetVersion.value, 0, { priority: 'high' });
      console.log(`Widget ${widgetId} reverted to version ${version}`);
      return true;
    } catch (error) {
      console.error('Failed to revert widget:', error);
      return false;
    }
  }, [widgetVersions, updateWidgetValue]);

  // Phase 5: Widget persistence management
  const saveWidgetState = useCallback(async (widgetId, strategy = 'local') => {
    const widget = widgets.get(widgetId);
    if (!widget) {
      console.error('Widget not found:', widgetId);
      return false;
    }

    return await widgetPersistenceManager.saveWidgetState(sessionId, widgetId, {
      value: widget.value,
      type: widget.type,
      properties: widget.properties,
      lastUpdated: widget.lastUpdated
    }, strategy);
  }, [widgets, sessionId]);

  const loadWidgetState = useCallback(async (widgetId, strategy = 'local') => {
    return await widgetPersistenceManager.loadWidgetState(sessionId, widgetId, strategy);
  }, [sessionId]);

  const value = {
    widgets,
    pendingUpdates,
    registerWidget,
    updateWidgetValue,
    updateWidgetValueImmediate,
    commitWidgetValue,
    startWidgetInteraction,
    endWidgetInteraction,
    getWidget,
    hasWidget,
    getAllWidgets,
    clearWidgets,
    removeWidget,
    batchUpdateWidgets,
    processBatchUpdates,
    getPerformanceMetrics,
    getWidgetHistory,
    searchWidgets,
    getWidgetAnalytics,
    // Phase 5: New features
    createWidgetFromTemplate,
    saveWidgetAsTemplate,
    requestWidgetLock,
    releaseWidgetLock,
    getCollaborators,
    getWidgetVersions,
    revertWidgetToVersion,
    saveWidgetState,
    loadWidgetState,
    collaborationEnabled,
    widgetVersions,
    interactiveWidgets
  };

  return (
    <WidgetStateContext.Provider value={value}>
      {children}
    </WidgetStateContext.Provider>
  );
};

// Export the provider and utilities
export { WidgetStateProvider, useWidgetState };
