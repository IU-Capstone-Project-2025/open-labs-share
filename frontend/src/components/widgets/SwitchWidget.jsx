import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { validateValue } from '../../utils/widgetUtils';

/**
 * Switch Widget
 * Handles boolean toggle with visual feedback and accessibility
 */
const SwitchWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { updateWidgetValue, getWidget } = useWidgetState();
  const [localValue, setLocalValue] = useState(Boolean(widget.value));
  const [isAnimating, setIsAnimating] = useState(false);
  const switchRef = useRef(null);
  const mountedRef = useRef(true);
  
  const {
    enableAnalytics = false
  } = optimizations;

  // Get current widget state
  const widgetState = getWidget(widget.id);
  const isLoading = widgetState?.isLoading || false;
  const error = widgetState?.error || null;
  const constraints = widgetState?.constraints || null;

  // Extract properties
  const { 
    label = widget.type === 'checkbox' ? 'Checkbox' : 'Switch',
    onLabel = 'On',
    offLabel = 'Off',
    description = '',
    disabled = false,
    required = false,
    size = 'medium', // 'small' | 'medium' | 'large'
    color = 'blue', // 'blue' | 'green' | 'red' | 'purple' | 'indigo'
    showLabels = true
  } = widget.properties || {};

  // Sync local value with widget state
  useEffect(() => {
    if (widgetState && Boolean(widgetState.value) !== localValue) {
      setLocalValue(Boolean(widgetState.value));
    }
  }, [widgetState?.value, localValue]);

  // Handle value changes with validation
  const handleValueChange = useCallback((newValue) => {
    if (!mountedRef.current) return;

    // Validate value
    if (constraints) {
      const validationResult = validateValue(constraints, newValue);
      if (!validationResult.isValid) {
        console.warn(`Invalid switch value for widget ${widget.id}:`, validationResult.error);
        return;
      }
    }

    setLocalValue(newValue);
    updateWidgetValue(widget.id, newValue, 100);
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, newValue);
    }
  }, [widget.id, updateWidgetValue, onWidgetUpdate, constraints]);

  // Handle switch toggle
  const handleToggle = useCallback(() => {
    if (disabled || isLoading) return;
    
    setIsAnimating(true);
    const newValue = !localValue;
    
    handleValueChange(newValue);
    
    if (enableAnalytics) {
      console.log(`Switch widget ${widget.id} toggled to:`, newValue);
    }
    
    // Reset animation state
    setTimeout(() => {
      setIsAnimating(false);
    }, 200);
  }, [disabled, isLoading, localValue, handleValueChange, widget.id, enableAnalytics]);

  // Handle keyboard interaction
  const handleKeyDown = useCallback((e) => {
    if (disabled || isLoading) return;
    
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault();
      handleToggle();
    }
  }, [disabled, isLoading, handleToggle]);

  // Get size classes
  const getSizeClasses = useCallback(() => {
    switch (size) {
      case 'small':
        return {
          track: 'w-8 h-4',
          thumb: 'w-3 h-3',
          translate: 'translate-x-3.5' // Adjusted for padding
        };
      case 'large':
        return {
          track: 'w-14 h-7',
          thumb: 'w-6 h-6',
          translate: 'translate-x-6' // Adjusted for padding
        };
      default: // medium
        return {
          track: 'w-11 h-6',
          thumb: 'w-5 h-5',
          translate: 'translate-x-4' // Adjusted for padding
        };
    }
  }, [size]);

  // Get color classes
  const getColorClasses = useCallback(() => {
    const baseClasses = 'focus:ring-2 focus:ring-offset-2';
    
    switch (color) {
      case 'green':
        return `${baseClasses} bg-green-600 focus:ring-green-500`;
      case 'red':
        return `${baseClasses} bg-red-600 focus:ring-red-500`;
      case 'purple':
        return `${baseClasses} bg-purple-600 focus:ring-purple-500`;
      case 'indigo':
        return `${baseClasses} bg-indigo-600 focus:ring-indigo-500`;
      default: // blue
        return `${baseClasses} bg-blue-600 focus:ring-blue-500`;
    }
  }, [color]);

  const sizeClasses = getSizeClasses();
  const colorClasses = getColorClasses();

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Switch Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-switch relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center z-10">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      <div className="flex items-center justify-between">
        <div className="flex-1 min-w-0">
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
            {label}
            {required && <span className="text-red-500 ml-1">*</span>}
          </label>
          
          {description && (
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {description}
            </p>
          )}
        </div>
        
        <div className="flex items-center ml-4">
          {showLabels && (
            <span className={`mr-3 text-sm font-medium select-none
                             ${localValue ? 'text-gray-400' : 'text-gray-900 dark:text-white'}`}>
              {offLabel}
            </span>
          )}
          
          <button
            ref={switchRef}
            onClick={handleToggle}
            onKeyDown={handleKeyDown}
            disabled={disabled || isLoading}
            className={`relative inline-flex items-center justify-start rounded-full border-2 border-transparent 
                       focus:outline-none transition-colors duration-200 ease-in-out p-0.5
                       ${disabled || isLoading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                       ${localValue ? colorClasses : 'bg-gray-200 dark:bg-gray-700 focus:ring-2 focus:ring-gray-500 focus:ring-offset-2'}
                       ${sizeClasses.track}
                       ${isAnimating ? 'animate-pulse' : ''}`}
            role="switch"
            aria-checked={localValue}
            aria-disabled={disabled || isLoading}
            aria-labelledby={`${widget.id}-label`}
          >
            <span className="sr-only">
              {localValue ? `Turn off ${label}` : `Turn on ${label}`}
            </span>
            
            <span
              className={`pointer-events-none relative inline-block rounded-full bg-white shadow transform 
                         ring-0 transition duration-200 ease-in-out
                         ${localValue ? sizeClasses.translate : 'translate-x-0'}
                         ${sizeClasses.thumb}`}
            >
              {/* Inner indicator - Off state (X) */}
              <span
                className={`absolute inset-0 flex items-center justify-center transition-opacity duration-200 ease-in-out
                           ${localValue ? 'opacity-0' : 'opacity-100'}`}
              >
                <svg className={`text-gray-400 ${size === 'small' ? 'w-2 h-2' : size === 'large' ? 'w-3 h-3' : 'w-2.5 h-2.5'}`} 
                     fill="none" stroke="currentColor" viewBox="0 0 8 8" strokeWidth="1.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M1.5 1.5l5 5m0-5l-5 5" />
                </svg>
              </span>
              
              {/* Inner indicator - On state (Checkmark) */}
              <span
                className={`absolute inset-0 flex items-center justify-center transition-opacity duration-200 ease-in-out
                           ${localValue ? 'opacity-100' : 'opacity-0'}`}
              >
                <svg className={`text-green-400 ${size === 'small' ? 'w-2 h-2' : size === 'large' ? 'w-3 h-3' : 'w-2.5 h-2.5'}`} 
                     fill="none" stroke="currentColor" viewBox="0 0 8 8" strokeWidth="1.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M1.5 4l2 2 3.5-3.5" />
                </svg>
              </span>
            </span>
          </button>
          
          {showLabels && (
            <span className={`ml-3 text-sm font-medium select-none
                             ${localValue ? 'text-gray-900 dark:text-white' : 'text-gray-400'}`}>
              {onLabel}
            </span>
          )}
        </div>
      </div>
      
      {required && !localValue && (
        <div className="mt-2 text-sm text-red-600">
          This field is required
        </div>
      )}
    </div>
  );
};

SwitchWidget.propTypes = {
  widget: PropTypes.shape({
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    value: PropTypes.any,
    properties: PropTypes.object
  }).isRequired,
  sessionId: PropTypes.string.isRequired,
  onWidgetUpdate: PropTypes.func,
  optimizations: PropTypes.object
};

export default SwitchWidget;
