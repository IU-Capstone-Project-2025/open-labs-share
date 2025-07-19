import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { debounce, validateValue, autoFixValue } from '../../utils/widgetUtils';

/**
 * Num          <input
            ref={inputRef}
            type="number"
            value={inputValue}
            onChange={handleInputChange}
            onFocus={handleInputFocus}
            onBlur={handleInputBlur}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            min={min}
            max={max}
            step={step}
            className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                       ${hasError ? 'border-red-500 bg-red-50' : 'border-gray-300 dark:border-gray-600'}
                       ${isLoading ? 'opacity-50' : ''}
                       dark:bg-gray-700 dark:text-white`}
            disabled={isLoading}
          />onent
 * Handles numeric input with precision, validation, and step controls
 */
const NumberWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { 
    updateWidgetValueImmediate, 
    commitWidgetValue, 
    startWidgetInteraction, 
    endWidgetInteraction, 
    getWidget 
  } = useWidgetState();
  const [localValue, setLocalValue] = useState(widget.value);
  const [inputValue, setInputValue] = useState(String(widget.value));
  const [isInputFocused, setIsInputFocused] = useState(false);
  const [hasError, setHasError] = useState(false);
  const inputRef = useRef(null);
  const mountedRef = useRef(true);
  
  const {
    enableDebouncing = true,
    enableAnalytics = false
  } = optimizations;

  // Get current widget state
  const widgetState = getWidget(widget.id);
  const isLoading = widgetState?.isLoading || false;
  const error = widgetState?.error || null;
  const constraints = widgetState?.constraints || null;

  // Extract properties
  const { 
    label = 'Number',
    min = null,
    max = null,
    step = 1,
    precision = null,
    placeholder = 'Enter number...',
    format = 'default'
  } = widget.properties || {};

  // Sync local value with widget state
  useEffect(() => {
    if (!isInputFocused && widgetState && widgetState.value !== localValue) {
      setLocalValue(widgetState.value);
      setInputValue(String(widgetState.value));
    }
  }, [widgetState?.value, isInputFocused, localValue]);

  // Format number based on properties
  const formatNumber = useCallback((value) => {
    if (value === null || value === undefined || isNaN(value)) return '';
    
    switch (format) {
      case 'integer':
        return Math.round(value).toString();
      case 'currency':
        return new Intl.NumberFormat('en-US', {
          style: 'currency',
          currency: 'USD'
        }).format(value);
      case 'percentage':
        return (value * 100).toFixed(precision || 1) + '%';
      default:
        return precision !== null ? value.toFixed(precision) : value.toString();
    }
  }, [format, precision]);

  // Parse input value with strict validation
  const parseNumber = useCallback((value) => {
    if (value === '' || value === null || value === undefined) return null;
    
    // Remove currency symbols and percentage for parsing
    let cleanValue = value.replace(/[$,%]/g, '').trim();
    
    // Check for completely non-numeric values first
    if (cleanValue === '' || !/^[-+]?(\d+\.?\d*|\.\d+)([eE][-+]?\d+)?$/.test(cleanValue)) {
      return NaN; // Return NaN for invalid numbers
    }
    
    if (format === 'percentage') {
      cleanValue = cleanValue.replace('%', '');
      const parsed = parseFloat(cleanValue);
      return isNaN(parsed) ? NaN : parsed / 100;
    }
    
    const parsed = parseFloat(cleanValue);
    return isNaN(parsed) ? NaN : parsed;
  }, [format]);

  // Validate number within constraints
  const validateNumber = useCallback((value) => {
    if (value === null || value === undefined) return { isValid: true, error: null };
    
    if (isNaN(value)) {
      return { isValid: false, error: 'Must be a valid number' };
    }
    
    if (min !== null && value < min) {
      return { isValid: false, error: `Must be at least ${min}` };
    }
    
    if (max !== null && value > max) {
      return { isValid: false, error: `Must be at most ${max}` };
    }
    
    if (constraints) {
      return validateValue(constraints, value);
    }
    
    return { isValid: true, error: null };
  }, [min, max, constraints]);

  // Handle value changes during interaction (immediate UI update only)
  const handleValueChange = useCallback((newValue, shouldFormat = true) => {
    if (!mountedRef.current) return;

    // Prevent sending invalid values to backend
    if (isNaN(newValue) && newValue !== null) {
      setHasError(true);
      console.warn(`Invalid number value rejected for widget ${widget.id}:`, newValue);
      return; // Don't update anything for invalid values
    }

    const validation = validateNumber(newValue);
    
    if (!validation.isValid) {
      setHasError(true);
      if (constraints && newValue !== null && !isNaN(newValue)) {
        newValue = autoFixValue(constraints, newValue);
      } else if (isNaN(newValue)) {
        // For NaN values, don't attempt to fix, just reject
        return;
      }
    } else {
      setHasError(false);
    }

    setLocalValue(newValue);
    
    if (shouldFormat && !isInputFocused) {
      setInputValue(formatNumber(newValue));
    }
    
    // Only update UI immediately during focus, don't commit to backend
    if (isInputFocused && newValue !== null && !isNaN(newValue)) {
      updateWidgetValueImmediate(widget.id, newValue);
    } else if (!isInputFocused && newValue !== null && !isNaN(newValue)) {
      // If not focused (e.g., step controls), commit immediately
      commitWidgetValue(widget.id);
    }
    
    if (onWidgetUpdate && newValue !== null && !isNaN(newValue)) {
      onWidgetUpdate(widget.id, newValue);
    }
  }, [widget.id, updateWidgetValueImmediate, commitWidgetValue, onWidgetUpdate, validateNumber, formatNumber, isInputFocused, constraints]);

  // Debounced value change for input field
  const debouncedValueChange = useCallback(
    enableDebouncing 
      ? debounce(handleValueChange, 300) 
      : handleValueChange,
    [handleValueChange, enableDebouncing]
  );

  // Handle input field changes with validation
  const handleInputChange = useCallback((e) => {
    const value = e.target.value;
    
    // Only allow numeric characters, decimal point, minus sign, and 'e' for scientific notation
    const numericRegex = /^-?(\d+\.?\d*|\.\d*)([eE][-+]?\d*)?$/;
    
    // Allow empty value for clearing
    if (value === '' || value === '-' || value === '.' || value === '-.' || numericRegex.test(value)) {
      setInputValue(value);
      setHasError(false);
      
      // Parse and validate the numeric value
      const parsedValue = parseNumber(value);
      
      // Don't send invalid values to backend
      if (!isNaN(parsedValue) || parsedValue === null) {
        debouncedValueChange(parsedValue, false);
      }
    }
    // If the input doesn't match the regex, simply don't update the input value
    // This prevents invalid characters from appearing in the field
  }, [parseNumber, debouncedValueChange]);

  // Handle input field focus
  const handleInputFocus = useCallback(() => {
    setIsInputFocused(true);
    setInputValue(String(localValue));
    startWidgetInteraction(widget.id);
    
    if (enableAnalytics) {
      console.log(`Number widget ${widget.id} focused`);
    }
  }, [localValue, widget.id, enableAnalytics, startWidgetInteraction]);

  // Handle input field blur
  const handleInputBlur = useCallback(async () => {
    setIsInputFocused(false);
    endWidgetInteraction(widget.id);
    
    const parsedValue = parseNumber(inputValue);
    
    // Only commit valid numbers
    if (!isNaN(parsedValue) || parsedValue === null) {
      setLocalValue(parsedValue);
      setInputValue(formatNumber(parsedValue));
      setHasError(false);
      
      // Commit the final value when focus is lost
      await commitWidgetValue(widget.id);
      
      if (enableAnalytics) {
        console.log(`Number widget ${widget.id} blurred and value committed: ${parsedValue}`);
      }
    } else {
      // Invalid value - revert to last valid value
      setInputValue(formatNumber(localValue));
      setHasError(true);
      console.warn(`Invalid number value reverted for widget ${widget.id}: ${inputValue}`);
    }
  }, [inputValue, parseNumber, formatNumber, localValue, widget.id, enableAnalytics, endWidgetInteraction, commitWidgetValue]);

  // Handle keyboard shortcuts and validation
  const handleKeyDown = useCallback((e) => {
    // Allow browser default behavior for arrow keys with number inputs
    // The browser will handle up/down arrows automatically with step controls
  }, []);

  // Prevent invalid characters from being typed
  const handleKeyPress = useCallback((e) => {
    const char = e.key;
    const currentValue = e.target.value;
    const selectionStart = e.target.selectionStart;
    
    // Allow control keys (backspace, delete, arrow keys, etc.)
    if (e.ctrlKey || e.metaKey || char.length > 1) {
      return;
    }
    
    // Allow numeric digits
    if (/\d/.test(char)) {
      return;
    }
    
    // Allow decimal point if not already present
    if (char === '.' && !currentValue.includes('.')) {
      return;
    }
    
    // Allow minus sign only at the beginning
    if (char === '-' && selectionStart === 0 && !currentValue.includes('-')) {
      return;
    }
    
    // Allow 'e' or 'E' for scientific notation (but only once and not at the beginning)
    if ((char === 'e' || char === 'E') && selectionStart > 0 && !currentValue.toLowerCase().includes('e')) {
      return;
    }
    
    // Allow '+' or '-' after 'e' for scientific notation
    if ((char === '+' || char === '-') && selectionStart > 0) {
      const beforeCursor = currentValue.substring(0, selectionStart);
      if (beforeCursor.toLowerCase().endsWith('e')) {
        return;
      }
    }
    
    // Block all other characters
    e.preventDefault();
  }, []);

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Number Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-number relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        {label}
        {(min !== null || max !== null) && (
          <span className="ml-2 text-xs text-gray-500">
            ({min !== null && max !== null ? `${min} - ${max}` : 
              min !== null ? `≥ ${min}` : `≤ ${max}`})
          </span>
        )}
      </label>
      
      <div className="flex items-center space-x-2">
        <div className="flex-1 relative">
          <input
            ref={inputRef}
            type="number"
            inputMode="numeric"
            value={inputValue}
            onChange={handleInputChange}
            onFocus={handleInputFocus}
            onBlur={handleInputBlur}
            onKeyDown={handleKeyDown}
            onKeyPress={handleKeyPress}
            min={min}
            max={max}
            step={step}
            placeholder={placeholder}
            className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                       ${hasError ? 'border-red-500 bg-red-50' : 'border-gray-300 dark:border-gray-600'}
                       ${isLoading ? 'opacity-50' : ''}
                       dark:bg-gray-700 dark:text-white`}
            disabled={isLoading}
            onPaste={(e) => {
              // Validate pasted content
              const pastedText = e.clipboardData.getData('text');
              const parsed = parseNumber(pastedText);
              if (isNaN(parsed) && parsed !== null) {
                e.preventDefault();
                console.warn('Invalid number format pasted:', pastedText);
              }
            }}
          />
          
          {hasError && (
            <div className="absolute right-3 top-2.5">
              <svg className="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
            </div>
          )}
        </div>
      </div>
      
      {step && step !== 1 && (
        <div className="mt-1 text-xs text-gray-400">
          Step: {step}
        </div>
      )}
    </div>
  );
};

NumberWidget.propTypes = {
  widget: PropTypes.shape({
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    value: PropTypes.number,
    properties: PropTypes.object
  }).isRequired,
  sessionId: PropTypes.string.isRequired,
  onWidgetUpdate: PropTypes.func,
  optimizations: PropTypes.object
};

export default NumberWidget;
