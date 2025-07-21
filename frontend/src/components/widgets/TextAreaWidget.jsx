import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { debounce, validateValue } from '../../utils/widgetUtils';

/**
 * TextArea Widget
 * Handles multi-line text input with auto-resize, validation, and character counting
 */
const TextAreaWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { 
    updateWidgetValueImmediate, 
    commitWidgetValue, 
    startWidgetInteraction, 
    endWidgetInteraction, 
    getWidget 
  } = useWidgetState();
  const [localValue, setLocalValue] = useState(widget.value || '');
  const [isFocused, setIsFocused] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const textareaRef = useRef(null);
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
    label = 'Text Area',
    placeholder = 'Enter text...',
    disabled = false,
    required = false,
    rows = 4,
    maxRows = 10,
    minLength = null,
    maxLength = null,
    showCharCount = true,
    autoResize = true,
    spellCheck = true,
    wrap = 'soft' // 'soft' | 'hard' | 'off'
  } = widget.properties || {};

  // Sync local value with widget state
  useEffect(() => {
    if (!isFocused && widgetState && widgetState.value !== localValue) {
      setLocalValue(widgetState.value || '');
    }
  }, [widgetState?.value, isFocused, localValue]);

  // Auto-resize textarea
  useEffect(() => {
    if (autoResize && textareaRef.current) {
      const textarea = textareaRef.current;
      textarea.style.height = 'auto';
      
      const lineHeight = parseInt(window.getComputedStyle(textarea).lineHeight);
      const maxHeight = lineHeight * maxRows;
      const newHeight = Math.min(textarea.scrollHeight, maxHeight);
      
      textarea.style.height = `${newHeight}px`;
    }
  }, [localValue, autoResize, maxRows]);

  // Validate text input
  const validateText = useCallback((value) => {
    if (required && (!value || value.trim() === '')) {
      return { isValid: false, error: 'This field is required' };
    }
    
    if (minLength !== null && value.length < minLength) {
      return { isValid: false, error: `Must be at least ${minLength} characters` };
    }
    
    if (maxLength !== null && value.length > maxLength) {
      return { isValid: false, error: `Must be no more than ${maxLength} characters` };
    }
    
    if (constraints) {
      return validateValue(constraints, value);
    }
    
    return { isValid: true, error: null };
  }, [required, minLength, maxLength, constraints]);

  // Handle value changes with validation
  const handleValueChange = useCallback((newValue, shouldValidate = true) => {
    if (!mountedRef.current) return;

    const validation = shouldValidate ? validateText(newValue) : { isValid: true, error: null };
    
    setHasError(!validation.isValid);
    setErrorMessage(validation.error || '');
    
    setLocalValue(newValue);
    
    // Only update UI immediately during focus, don't commit to backend
    if (isFocused) {
      updateWidgetValueImmediate(widget.id, newValue);
    } else {
      // If not focused (e.g., programmatic change), commit immediately
      commitWidgetValue(widget.id);
    }
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, newValue);
    }
  }, [widget.id, updateWidgetValueImmediate, commitWidgetValue, onWidgetUpdate, validateText, isFocused]);

  // Debounced value change
  const debouncedValueChange = useCallback(
    enableDebouncing 
      ? debounce(handleValueChange, 300) 
      : handleValueChange,
    [handleValueChange, enableDebouncing]
  );

  // Handle textarea changes
  const handleTextareaChange = useCallback((e) => {
    const value = e.target.value;
    
    // Immediate local state update for responsive typing
    setLocalValue(value);
    
    // Debounced validation and state update
    debouncedValueChange(value);
  }, [debouncedValueChange]);

  // Handle focus events
  const handleFocus = useCallback(() => {
    setIsFocused(true);
    startWidgetInteraction(widget.id);
    
    if (enableAnalytics) {
      console.log(`TextArea widget ${widget.id} focused`);
    }
  }, [widget.id, enableAnalytics, startWidgetInteraction]);

  const handleBlur = useCallback(async () => {
    setIsFocused(false);
    endWidgetInteraction(widget.id);
    
    // Final validation on blur
    const validation = validateText(localValue);
    setHasError(!validation.isValid);
    setErrorMessage(validation.error || '');
    
    // Commit the final value when focus is lost
    await commitWidgetValue(widget.id);
    
    if (enableAnalytics) {
      console.log(`TextArea widget ${widget.id} blurred and value committed`);
    }
  }, [localValue, validateText, widget.id, enableAnalytics, endWidgetInteraction, commitWidgetValue]);

  // Handle keyboard shortcuts
  const handleKeyDown = useCallback((e) => {
    // Handle common shortcuts
    if (e.ctrlKey || e.metaKey) {
      switch (e.key) {
        case 'a':
          // Select all - allow default behavior
          break;
        case 'z':
          // Undo - allow default behavior
          break;
        case 'y':
          // Redo - allow default behavior
          break;
        default:
          // Allow other shortcuts
          break;
      }
    }
    
    // Handle tab key (optional: insert tab character)
    if (e.key === 'Tab' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
      // Allow default tab behavior (focus next element)
      // Uncomment below to insert tab character instead:
      // e.preventDefault();
      // const start = e.target.selectionStart;
      // const end = e.target.selectionEnd;
      // const value = localValue.substring(0, start) + '\t' + localValue.substring(end);
      // setLocalValue(value);
      // debouncedValueChange(value);
      // 
      // setTimeout(() => {
      //   e.target.selectionStart = e.target.selectionEnd = start + 1;
      // }, 0);
    }
  }, [localValue, debouncedValueChange]);

  // Get character count info
  const getCharacterInfo = useCallback(() => {
    const currentLength = localValue.length;
    const remaining = maxLength !== null ? maxLength - currentLength : null;
    const isApproachingLimit = maxLength !== null && remaining !== null && remaining <= 20;
    const isOverLimit = maxLength !== null && currentLength > maxLength;
    
    return {
      currentLength,
      remaining,
      isApproachingLimit,
      isOverLimit
    };
  }, [localValue, maxLength]);

  const characterInfo = getCharacterInfo();

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">TextArea Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-textarea relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center z-10">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
        {(minLength !== null || maxLength !== null) && (
          <span className="ml-2 text-xs text-gray-500">
            ({minLength !== null && maxLength !== null 
              ? `${minLength} - ${maxLength} chars` 
              : minLength !== null 
                ? `≥ ${minLength} chars`
                : `≤ ${maxLength} chars`})
          </span>
        )}
      </label>
      
      <div className="relative">
        <textarea
          ref={textareaRef}
          value={localValue}
          onChange={handleTextareaChange}
          onFocus={handleFocus}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled || isLoading}
          required={required}
          rows={rows}
          spellCheck={spellCheck}
          wrap={wrap}
          className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                     resize-${autoResize ? 'none' : 'vertical'} transition-colors duration-200
                     ${hasError ? 'border-red-500 bg-red-50 focus:ring-red-500 focus:border-red-500' : 'border-gray-300 dark:border-gray-600'}
                     ${disabled || isLoading ? 'opacity-50 cursor-not-allowed' : ''}
                     ${isFocused ? 'bg-white dark:bg-gray-800' : 'bg-gray-50 dark:bg-gray-700'}
                     dark:text-white`}
          style={{
            minHeight: `${rows * 1.5}rem`,
            maxHeight: autoResize ? `${maxRows * 1.5}rem` : 'none'
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
      
      <div className="flex items-center justify-between mt-2">
        <div className="flex-1">
          {hasError && errorMessage && (
            <p className="text-sm text-red-600">{errorMessage}</p>
          )}
        </div>
        
        {showCharCount && (
          <div className={`text-sm font-mono
                          ${characterInfo.isOverLimit ? 'text-red-600' : 
                            characterInfo.isApproachingLimit ? 'text-yellow-600' : 'text-gray-500'}`}>
            {characterInfo.currentLength}
            {maxLength !== null && (
              <span>/{maxLength}</span>
            )}
          </div>
        )}
      </div>
      
      {maxLength !== null && characterInfo.remaining !== null && characterInfo.remaining < 0 && (
        <div className="mt-1 text-sm text-red-600">
          {Math.abs(characterInfo.remaining)} characters over limit
        </div>
      )}
    </div>
  );
};

TextAreaWidget.propTypes = {
  widget: PropTypes.shape({
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    value: PropTypes.string,
    properties: PropTypes.object
  }).isRequired,
  sessionId: PropTypes.string.isRequired,
  onWidgetUpdate: PropTypes.func,
  optimizations: PropTypes.object
};

export default TextAreaWidget;
