import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { validateValue } from '../../utils/widgetUtils';

/**
 * Radio Widget
 * Handles single selection from predefined options with keyboard navigation
 */
const RadioWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { updateWidgetValue, getWidget } = useWidgetState();
  const [localValue, setLocalValue] = useState(widget.value);
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const radioGroupRef = useRef(null);
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
    label = 'Radio Group',
    options = [],
    layout = 'vertical', // 'vertical' | 'horizontal' | 'grid'
    disabled = false,
    required = false,
    columns = 2 // for grid layout
  } = widget.properties || {};

  // Normalize options to handle both string arrays and object arrays
  const normalizedOptions = React.useMemo(() => {
    if (!Array.isArray(options)) {
      console.warn(`Radio widget ${widget.id}: options is not an array`, options);
      return [];
    }
    
    return options.map(option => {
      if (typeof option === 'string') {
        return { label: option, value: option, disabled: false };
      } else if (typeof option === 'object' && option !== null) {
        return {
          label: option.label || option.value || String(option),
          value: option.value !== undefined ? option.value : option.label || String(option),
          disabled: option.disabled || false
        };
      } else {
        return { label: String(option), value: option, disabled: false };
      }
    });
  }, [options, widget.id]);

  // Sync local value with widget state
  useEffect(() => {
    if (widgetState && widgetState.value !== localValue) {
      setLocalValue(widgetState.value);
    }
  }, [widgetState?.value, localValue]);

  // Handle value changes with validation
  const handleValueChange = useCallback((newValue) => {
    if (!mountedRef.current) return;

    // Validate value
    if (constraints) {
      const validationResult = validateValue(constraints, newValue);
      if (!validationResult.isValid) {
        console.warn(`Invalid radio value for widget ${widget.id}:`, validationResult.error);
        return;
      }
    }

    setLocalValue(newValue);
    updateWidgetValue(widget.id, newValue, 100);
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, newValue);
    }
  }, [widget.id, updateWidgetValue, onWidgetUpdate, constraints]);

  // Handle option selection
  const handleOptionSelect = useCallback((option) => {
    if (option.disabled || disabled || isLoading) return;
    
    handleValueChange(option.value);
    
    if (enableAnalytics) {
      console.log(`Radio widget ${widget.id} selected:`, option.value);
    }
  }, [handleValueChange, disabled, isLoading, widget.id, enableAnalytics]);

  // Handle keyboard navigation
  const handleKeyDown = useCallback((e) => {
    if (disabled || isLoading) return;
    
    const enabledOptions = normalizedOptions.filter(option => !option.disabled);
    const currentIndex = enabledOptions.findIndex(option => option.value === localValue);
    
    switch (e.key) {
      case 'ArrowDown':
      case 'ArrowRight':
        e.preventDefault();
        const nextIndex = currentIndex < enabledOptions.length - 1 ? currentIndex + 1 : 0;
        handleOptionSelect(enabledOptions[nextIndex]);
        setFocusedIndex(nextIndex);
        break;
      case 'ArrowUp':
      case 'ArrowLeft':
        e.preventDefault();
        const prevIndex = currentIndex > 0 ? currentIndex - 1 : enabledOptions.length - 1;
        handleOptionSelect(enabledOptions[prevIndex]);
        setFocusedIndex(prevIndex);
        break;
      case ' ':
        e.preventDefault();
        if (focusedIndex >= 0 && focusedIndex < enabledOptions.length) {
          handleOptionSelect(enabledOptions[focusedIndex]);
        }
        break;
    }
  }, [disabled, isLoading, options, localValue, focusedIndex, handleOptionSelect]);

  // Handle focus management
  const handleFocus = useCallback((index) => {
    setFocusedIndex(index);
  }, []);

  const handleBlur = useCallback(() => {
    setFocusedIndex(-1);
  }, []);

  // Get layout classes
  const getLayoutClasses = useCallback(() => {
    switch (layout) {
      case 'horizontal':
        return 'flex flex-wrap gap-4';
      case 'grid':
        return `grid gap-3 grid-cols-${Math.min(columns, normalizedOptions.length)}`;
      default:
        return 'space-y-3';
    }
  }, [layout, columns, normalizedOptions.length]);

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Radio Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-radio relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center z-10">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      <fieldset className="w-full" disabled={disabled || isLoading}>
        <legend className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
          {normalizedOptions.length > 0 && (
            <span className="ml-2 text-xs text-gray-500">
              ({normalizedOptions.length} options)
            </span>
          )}
        </legend>
        
        <div 
          ref={radioGroupRef}
          className={getLayoutClasses()}
          onKeyDown={handleKeyDown}
          role="radiogroup"
          aria-labelledby={`${widget.id}-label`}
        >
          {normalizedOptions.map((option, index) => {
            const isSelected = option.value === localValue;
            const isDisabled = option.disabled || disabled || isLoading;
            const isFocused = focusedIndex === index;
            
            return (
              <div
                key={option.value}
                className={`flex items-start space-x-3 p-2 rounded-lg transition-colors
                           ${isDisabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700'}
                           ${isFocused ? 'ring-2 ring-blue-500 ring-opacity-50' : ''}
                           ${isSelected ? 'bg-blue-50 dark:bg-blue-900' : ''}`}
                onClick={() => handleOptionSelect(option)}
                onFocus={() => handleFocus(index)}
                onBlur={handleBlur}
                tabIndex={isDisabled ? -1 : (isSelected ? 0 : -1)}
                role="radio"
                aria-checked={isSelected}
                aria-disabled={isDisabled}
              >
                <div className="flex items-center h-5">
                  <input
                    type="radio"
                    name={widget.id}
                    value={option.value}
                    checked={isSelected}
                    onChange={() => handleOptionSelect(option)}
                    disabled={isDisabled}
                    className="w-4 h-4 text-blue-600 border-gray-300 dark:border-gray-600 focus:ring-blue-500 
                              focus:ring-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    tabIndex={-1} // Handled by parent div
                  />
                </div>
                
                <div className="flex-1 min-w-0">
                  <label
                    className={`block text-sm font-medium cursor-pointer select-none
                               ${isDisabled ? 'text-gray-400' : 'text-gray-900 dark:text-white'}
                               ${isSelected ? 'font-semibold' : ''}`}
                    htmlFor={`${widget.id}-${option.value}`}
                  >
                    {option.label}
                  </label>
                  
                  {option.description && (
                    <p className={`mt-1 text-xs
                                  ${isDisabled ? 'text-gray-300' : 'text-gray-500 dark:text-gray-400'}`}>
                      {option.description}
                    </p>
                  )}
                  
                  {option.hint && (
                    <p className={`mt-1 text-xs italic
                                  ${isDisabled ? 'text-gray-300' : 'text-gray-400 dark:text-gray-500'}`}>
                      {option.hint}
                    </p>
                  )}
                </div>
                
                {option.badge && (
                  <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium
                                   ${option.badge.color === 'blue' ? 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200' : ''}
                                   ${option.badge.color === 'green' ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' : ''}
                                   ${option.badge.color === 'red' ? 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200' : ''}
                                   ${option.badge.color === 'yellow' ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200' : ''}
                                   ${!option.badge.color ? 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200' : ''}`}>
                    {option.badge.text}
                  </span>
                )}
              </div>
            );
          })}
        </div>
        
        {normalizedOptions.length === 0 && (
          <div className="text-sm text-gray-500 dark:text-gray-400 italic">
            No options available
          </div>
        )}
      </fieldset>
      
      {required && !localValue && (
        <div className="mt-2 text-sm text-red-600">
          This field is required
        </div>
      )}
    </div>
  );
};

RadioWidget.propTypes = {
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

export default RadioWidget;
