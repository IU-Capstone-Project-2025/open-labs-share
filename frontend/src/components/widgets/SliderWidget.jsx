import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { throttle, validateValue, autoFixValue } from '../../utils/widgetUtils';

/**
 * Slider Widget Component
 * Handles numeric input with range constraints and visual feedback
 */
const SliderWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { 
    updateWidgetValueImmediate, 
    commitWidgetValue, 
    startWidgetInteraction, 
    endWidgetInteraction, 
    getWidget 
  } = useWidgetState();
  const [localValue, setLocalValue] = useState(widget.value);
  const [isInteracting, setIsInteracting] = useState(false);
  const mountedRef = useRef(true);
  
  const {
    enableThrottling = true,
    enableAnalytics = false
  } = optimizations;

  // Get current widget state
  const widgetState = getWidget(widget.id);
  const isLoading = widgetState?.isLoading || false;
  const error = widgetState?.error || null;
  const constraints = widgetState?.constraints || null;

  // Extract properties
  const { 
    label = 'Slider',
    min = 0,
    max = 100,
    step = 1,
    showValue = true,
    orientation = 'horizontal'
  } = widget.properties || {};

  // Sync local value with widget state
  useEffect(() => {
    if (!isInteracting && widgetState && widgetState.value !== localValue) {
      setLocalValue(widgetState.value);
    }
  }, [widgetState?.value, isInteracting, localValue]);

  // Handle value changes during interaction (immediate UI update only)
  const handleValueChange = useCallback((newValue) => {
    if (!mountedRef.current) return;

    // Validate and fix value
    if (constraints) {
      const validationResult = validateValue(constraints, newValue);
      if (!validationResult.isValid) {
        console.warn(`Invalid slider value for widget ${widget.id}:`, validationResult.error);
        newValue = autoFixValue(constraints, newValue);
      }
    }

    setLocalValue(newValue);
    
    // Only update UI immediately, don't commit to backend while interacting
    if (isInteracting) {
      updateWidgetValueImmediate(widget.id, newValue);
    } else {
      // If not interacting (e.g., programmatic change), commit immediately
      commitWidgetValue(widget.id);
    }
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, newValue);
    }
  }, [widget.id, updateWidgetValueImmediate, commitWidgetValue, onWidgetUpdate, constraints, isInteracting]);

  // Throttled value change for smooth interaction
  const throttledValueChange = useCallback(
    enableThrottling 
      ? throttle(handleValueChange, 100) 
      : handleValueChange,
    [handleValueChange, enableThrottling]
  );

  // Handle interaction events
  const handleInteractionStart = useCallback(() => {
    setIsInteracting(true);
    startWidgetInteraction(widget.id);
    if (enableAnalytics) {
      console.log(`Slider widget ${widget.id} interaction started`);
    }
  }, [widget.id, enableAnalytics, startWidgetInteraction]);

  const handleInteractionEnd = useCallback(async () => {
    setIsInteracting(false);
    endWidgetInteraction(widget.id);
    
    // Commit the current value to backend when interaction ends
    await commitWidgetValue(widget.id);
    
    if (enableAnalytics) {
      console.log(`Slider widget ${widget.id} interaction ended, value committed: ${localValue}`);
    }
  }, [widget.id, enableAnalytics, endWidgetInteraction, commitWidgetValue, localValue]);

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Slider Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-slider relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        {label}
        {showValue && (
          <span className="ml-2 text-xs text-gray-500 font-mono">
            ({localValue})
          </span>
        )}
        {isInteracting && (
          <span className="ml-2 text-xs text-blue-500 font-medium">
            • interacting
          </span>
        )}
      </label>
      
      <div className={`flex items-center space-x-3 ${orientation === 'vertical' ? 'flex-col space-y-3 space-x-0' : ''}`}>
        <span className="text-xs text-gray-500 select-none">{min}</span>
        
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={localValue}
          onChange={(e) => {
            const newValue = parseFloat(e.target.value);
            setLocalValue(newValue);
            if (isInteracting) {
              updateWidgetValueImmediate(widget.id, newValue);
            } else {
              handleValueChange(newValue);
            }
          }}
          onMouseDown={handleInteractionStart}
          onMouseUp={handleInteractionEnd}
          onTouchStart={handleInteractionStart}
          onTouchEnd={handleInteractionEnd}
          onKeyDown={(e) => {
            // Handle keyboard interaction
            if (e.key === 'ArrowLeft' || e.key === 'ArrowRight' || e.key === 'ArrowUp' || e.key === 'ArrowDown') {
              if (!isInteracting) {
                handleInteractionStart();
              }
            }
          }}
          onKeyUp={(e) => {
            // Commit value after keyboard interaction
            if (e.key === 'ArrowLeft' || e.key === 'ArrowRight' || e.key === 'ArrowUp' || e.key === 'ArrowDown') {
              setTimeout(handleInteractionEnd, 100); // Small delay to ensure value is set
            }
          }}
          className={`flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer
                     focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50
                     ${orientation === 'vertical' ? 'writing-mode-vertical-lr' : ''}
                     [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:w-4 
                     [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-blue-600 [&::-webkit-slider-thumb]:cursor-pointer
                     [&::-webkit-slider-thumb]:border-0 [&::-webkit-slider-thumb]:shadow-md
                     [&::-moz-range-thumb]:h-4 [&::-moz-range-thumb]:w-4 [&::-moz-range-thumb]:rounded-full 
                     [&::-moz-range-thumb]:bg-blue-600 [&::-moz-range-thumb]:cursor-pointer [&::-moz-range-thumb]:border-0`}
          disabled={isLoading}
          style={{ 
            background: `linear-gradient(to right, #3B82F6 0%, #3B82F6 ${((localValue - min) / (max - min)) * 100}%, #E5E7EB ${((localValue - min) / (max - min)) * 100}%, #E5E7EB 100%)` 
          }}
        />
        
        <span className="text-xs text-gray-500 select-none">{max}</span>
      </div>
      
      {step && step < 1 && (
        <div className="mt-1 text-xs text-gray-400">
          Step: {step}
        </div>
      )}
    </div>
  );
};

SliderWidget.propTypes = {
  widget: PropTypes.shape({
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    value: PropTypes.number.isRequired,
    properties: PropTypes.object
  }).isRequired,
  sessionId: PropTypes.string.isRequired,
  onWidgetUpdate: PropTypes.func,
  optimizations: PropTypes.object
};

export default SliderWidget;
