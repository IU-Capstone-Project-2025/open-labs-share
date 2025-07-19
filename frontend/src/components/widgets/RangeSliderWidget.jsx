import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { throttle, validateValue, autoFixValue } from '../../utils/widgetUtils';

/**
 * Range Slider Widget
 * Handles dual-thumb range selection with visual feedback
 */
const RangeSliderWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { 
    updateWidgetValueImmediate, 
    commitWidgetValue, 
    startWidgetInteraction, 
    endWidgetInteraction, 
    getWidget 
  } = useWidgetState();
  const [localValue, setLocalValue] = useState(widget.value || [0, 100]);
  const [activeThumb, setActiveThumb] = useState('min'); // Default to min thumb
  const [isInteracting, setIsInteracting] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const sliderRef = useRef(null);
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
    label = 'Range Slider',
    min = 0,
    max = 100,
    step = 1,
    showValues = true,
    showLabels = true,
    orientation = 'horizontal',
    formatValue = null,
    disabled = false,
    minDistance = 0
  } = widget.properties || {};

  // Clean up component on unmount
  useEffect(() => {
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Global mouse move and mouse up handlers for dragging
  useEffect(() => {
    const handleGlobalMouseMove = (e) => {
      if (!isDragging || !sliderRef.current || disabled || isLoading) return;
      
      const rect = sliderRef.current.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const sliderWidth = rect.width;
      const clickPercent = Math.max(0, Math.min(100, (clickX / sliderWidth) * 100));
      const newValue = Math.round(min + (clickPercent / 100) * (max - min));
      
      // Update the appropriate thumb
      const [currentMin, currentMax] = localValue;
      let updatedValue;
      if (activeThumb === 'min') {
        const constrainedValue = Math.min(newValue, currentMax - minDistance);
        updatedValue = [Math.round(Math.max(min, constrainedValue)), currentMax];
      } else {
        const constrainedValue = Math.max(newValue, currentMin + minDistance);
        updatedValue = [currentMin, Math.round(Math.min(max, constrainedValue))];
      }
      
      setLocalValue(updatedValue);
      updateWidgetValueImmediate(widget.id, updatedValue);
    };

    const handleGlobalMouseUp = async () => {
      if (isDragging) {
        setIsDragging(false);
        setIsInteracting(false);
        endWidgetInteraction(widget.id);
        await commitWidgetValue(widget.id);
      }
    };

    if (isDragging) {
      document.addEventListener('mousemove', handleGlobalMouseMove);
      document.addEventListener('mouseup', handleGlobalMouseUp);
      document.addEventListener('touchmove', handleGlobalMouseMove);
      document.addEventListener('touchend', handleGlobalMouseUp);
    }

    return () => {
      document.removeEventListener('mousemove', handleGlobalMouseMove);
      document.removeEventListener('mouseup', handleGlobalMouseUp);
      document.removeEventListener('touchmove', handleGlobalMouseMove);
      document.removeEventListener('touchend', handleGlobalMouseUp);
    };
  }, [isDragging, activeThumb, localValue, min, max, minDistance, disabled, isLoading, widget.id, updateWidgetValueImmediate, endWidgetInteraction, commitWidgetValue]);

  // Sync local value with widget state
  useEffect(() => {
    if (!isInteracting && widgetState && widgetState.value !== localValue) {
      setLocalValue(widgetState.value || [min, max]);
    }
  }, [widgetState?.value, isInteracting, localValue, min, max]);

  // Format value for display
  const formatDisplayValue = useCallback((value) => {
    if (formatValue) {
      switch (formatValue) {
        case 'currency':
          return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
          }).format(value);
        case 'percentage':
          return `${value}%`;
        case 'decimal':
          return value.toFixed(2);
        default:
          return value.toString();
      }
    }
    return value.toString();
  }, [formatValue]);

  // Validate and constrain values
  const validateRange = useCallback((newValue) => {
    let [minVal, maxVal] = newValue;
    
    // Round to integers first
    minVal = Math.round(minVal);
    maxVal = Math.round(maxVal);
    
    // Ensure values are within bounds
    minVal = Math.max(min, Math.min(max, minVal));
    maxVal = Math.max(min, Math.min(max, maxVal));
    
    // Ensure min <= max (but don't force them to swap unless necessary)
    if (minVal > maxVal) {
      // Only swap if the difference is significant to avoid jitter
      if (minVal - maxVal > step) {
        [minVal, maxVal] = [maxVal, minVal];
      } else {
        // Prefer keeping the active thumb's position when values are close
        if (activeThumb === 'min') {
          maxVal = minVal;
        } else {
          minVal = maxVal;
        }
      }
    }
    
    // Ensure minimum distance only after basic validation
    if (maxVal - minVal < minDistance) {
      if (activeThumb === 'min') {
        // If moving min thumb, push max up
        maxVal = Math.min(max, minVal + minDistance);
        // If max hit the boundary, pull min down
        if (maxVal === max) {
          minVal = Math.max(min, max - minDistance);
        }
      } else {
        // If moving max thumb, push min down
        minVal = Math.max(min, maxVal - minDistance);
        // If min hit the boundary, push max up
        if (minVal === min) {
          maxVal = Math.min(max, min + minDistance);
        }
      }
    }
    
    return [Math.round(minVal), Math.round(maxVal)];
  }, [min, max, minDistance, activeThumb, step]);

  // Handle value changes during interaction (immediate UI update only)
  const handleValueChange = useCallback((newValue) => {
    if (!mountedRef.current) return;

    const validatedValue = validateRange(newValue);
    
    // Additional constraint validation
    if (constraints) {
      const validationResult = validateValue(constraints, validatedValue);
      if (!validationResult.isValid) {
        console.warn(`Invalid range slider value for widget ${widget.id}:`, validationResult.error);
        const fixedValue = autoFixValue(constraints, validatedValue);
        if (fixedValue !== validatedValue) {
          setLocalValue(fixedValue);
          // If constraints require fixing, commit immediately
          commitWidgetValue(widget.id);
          return;
        }
      }
    }

    setLocalValue(validatedValue);
    
    // Only update UI immediately during interaction, don't commit to backend
    if (isInteracting) {
      updateWidgetValueImmediate(widget.id, validatedValue);
    } else {
      // If not interacting (e.g., programmatic change), commit immediately
      commitWidgetValue(widget.id);
    }
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, validatedValue);
    }
  }, [widget.id, updateWidgetValueImmediate, commitWidgetValue, onWidgetUpdate, validateRange, constraints, isInteracting]);

  // Throttled value change for smooth interaction
  const throttledValueChange = useCallback(
    enableThrottling 
      ? throttle(handleValueChange, 100) // Match single slider throttling
      : handleValueChange,
    [handleValueChange, enableThrottling]
  );

  // Calculate which thumb is closest to click position
  const getClosestThumb = useCallback((e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const sliderWidth = rect.width;
    const clickPercent = (clickX / sliderWidth) * 100;
    
    // Calculate positions of both thumbs
    const minThumbPercent = ((localValue[0] - min) / (max - min)) * 100;
    const maxThumbPercent = ((localValue[1] - min) / (max - min)) * 100;
    
    // Return the closest thumb
    const distanceToMin = Math.abs(clickPercent - minThumbPercent);
    const distanceToMax = Math.abs(clickPercent - maxThumbPercent);
    
    return distanceToMin <= distanceToMax ? 'min' : 'max';
  }, [localValue, min, max]);

  // Handle slider interactions with intelligent thumb detection
  const handleSliderMouseDown = useCallback((e) => {
    if (disabled || isLoading) return;
    
    // Only handle clicks on the track, not on thumbs (those have their own handlers)
    if (e.target.tagName === 'DIV' && e.target.classList.contains('cursor-pointer')) {
      // Determine which thumb should be active based on click position
      const closestThumb = getClosestThumb(e);
      setActiveThumb(closestThumb);
      setIsInteracting(true);
      startWidgetInteraction(widget.id);
      
      // Calculate new value based on click position
      const rect = e.currentTarget.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const sliderWidth = rect.width;
      const clickPercent = (clickX / sliderWidth) * 100;
      const newValue = Math.round(min + (clickPercent / 100) * (max - min));
      
      // Update the appropriate thumb
      const [currentMin, currentMax] = localValue;
      let updatedValue;
      if (closestThumb === 'min') {
        const constrainedValue = Math.min(newValue, currentMax - minDistance);
        updatedValue = [Math.round(Math.max(min, constrainedValue)), currentMax];
      } else {
        const constrainedValue = Math.max(newValue, currentMin + minDistance);
        updatedValue = [currentMin, Math.round(Math.min(max, constrainedValue))];
      }
      
      setLocalValue(updatedValue);
      updateWidgetValueImmediate(widget.id, updatedValue);
    }
    
    if (enableAnalytics) {
      console.log(`Range slider widget ${widget.id} interaction started`);
    }
  }, [disabled, isLoading, widget.id, enableAnalytics, startWidgetInteraction, getClosestThumb, localValue, min, max, minDistance, updateWidgetValueImmediate]);

  const handleSliderMouseUp = useCallback(async () => {
    if (isInteracting) {
      setIsInteracting(false);
      endWidgetInteraction(widget.id);
      
      // Commit the current value to backend when interaction ends
      await commitWidgetValue(widget.id);
      
      if (enableAnalytics) {
        console.log(`Range slider widget ${widget.id} interaction ended, value committed: [${localValue.join(', ')}]`);
      }
    }
  }, [widget.id, enableAnalytics, endWidgetInteraction, commitWidgetValue, localValue, isInteracting]);

  // Handle slider interactions with intelligent value update
  const handleSliderChange = useCallback((e) => {
    if (disabled || isLoading) return;
    
    const newValue = Math.round(parseFloat(e.target.value));
    const [currentMin, currentMax] = localValue;
    
    let updatedValue;
    if (activeThumb === 'min') {
      // When moving min thumb, ensure it doesn't go above max - minDistance
      const constrainedValue = Math.min(newValue, currentMax - minDistance);
      updatedValue = [Math.round(constrainedValue), currentMax];
    } else {
      // When moving max thumb, ensure it doesn't go below min + minDistance  
      const constrainedValue = Math.max(newValue, currentMin + minDistance);
      updatedValue = [currentMin, Math.round(constrainedValue)];
    }
    
    setLocalValue(updatedValue);
    
    if (isInteracting) {
      updateWidgetValueImmediate(widget.id, updatedValue);
    } else {
      handleValueChange(updatedValue);
    }
  }, [disabled, isLoading, activeThumb, localValue, minDistance, isInteracting, updateWidgetValueImmediate, widget.id, handleValueChange]);

  // Handle keyboard navigation with active thumb
  const handleKeyDown = useCallback((e) => {
    if (disabled || isLoading) return;
    
    const [minVal, maxVal] = localValue;
    let newValue = [...localValue];
    
    switch (e.key) {
      case 'ArrowRight':
      case 'ArrowUp':
        e.preventDefault();
        if (activeThumb === 'min') {
          newValue[0] = Math.min(maxVal, minVal + step);
        } else {
          newValue[1] = Math.min(max, maxVal + step);
        }
        break;
      case 'ArrowLeft':
      case 'ArrowDown':
        e.preventDefault();
        if (activeThumb === 'min') {
          newValue[0] = Math.max(min, minVal - step);
        } else {
          newValue[1] = Math.max(minVal, maxVal - step);
        }
        break;
      case 'Home':
        e.preventDefault();
        if (activeThumb === 'min') {
          newValue[0] = min;
        } else {
          newValue[1] = min;
        }
        break;
      case 'End':
        e.preventDefault();
        if (activeThumb === 'min') {
          newValue[0] = max;
        } else {
          newValue[1] = max;
        }
        break;
      case 'Tab':
        // Switch between thumbs on tab
        e.preventDefault();
        setActiveThumb(activeThumb === 'min' ? 'max' : 'min');
        return;
      default:
        return;
    }
    
    handleValueChange(newValue);
  }, [disabled, isLoading, activeThumb, localValue, step, min, max, handleValueChange]);

  // Calculate percentages for styling
  const minPercent = ((localValue[0] - min) / (max - min)) * 100;
  const maxPercent = ((localValue[1] - min) / (max - min)) * 100;

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Range Slider Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-range-slider relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center z-10">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        {label}
        {showValues && (
          <span className="ml-2 text-xs text-gray-500 font-mono">
            ({formatDisplayValue(localValue[0])} - {formatDisplayValue(localValue[1])})
          </span>
        )}
        {isInteracting && (
          <span className="ml-2 text-xs text-blue-500 font-medium">
            • interacting
          </span>
        )}
      </label>
      
      <div className="flex items-center space-x-3">
        <span className="text-xs text-gray-500 select-none">{formatDisplayValue(min)}</span>
        
        <div className="relative flex-1">
          {/* Background track with gradient - matches SliderWidget exactly */}
          <div 
            ref={sliderRef}
            className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg relative cursor-pointer"
            style={{ 
              background: `linear-gradient(to right, #E5E7EB 0%, #E5E7EB ${minPercent}%, #3B82F6 ${minPercent}%, #3B82F6 ${maxPercent}%, #E5E7EB ${maxPercent}%, #E5E7EB 100%)` 
            }}
            onMouseDown={handleSliderMouseDown}
            onTouchStart={handleSliderMouseDown}
          >
            {/* Hidden input for accessibility and keyboard navigation */}
            <input
              type="range"
              min={min}
              max={max}
              step={step}
              value={activeThumb === 'min' ? localValue[0] : localValue[1]}
              onChange={handleSliderChange}
              onKeyDown={handleKeyDown}
              disabled={disabled || isLoading}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer focus:outline-none"
              aria-label={`${activeThumb === 'min' ? 'Minimum' : 'Maximum'} value: ${activeThumb === 'min' ? formatDisplayValue(localValue[0]) : formatDisplayValue(localValue[1])}`}
            />
            
            {/* Min thumb */}
            <div 
              className="absolute w-4 h-4 bg-blue-600 rounded-full shadow-md border-0 cursor-pointer select-none"
              style={{
                left: `calc(${minPercent}% - 8px)`,
                top: '50%',
                transform: 'translateY(-50%)',
                zIndex: activeThumb === 'min' ? 10 : 8
              }}
              onMouseDown={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setActiveThumb('min');
                setIsInteracting(true);
                setIsDragging(true);
                startWidgetInteraction(widget.id);
              }}
              onTouchStart={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setActiveThumb('min');
                setIsInteracting(true);
                setIsDragging(true);
                startWidgetInteraction(widget.id);
              }}
            />
            
            {/* Max thumb */}
            <div 
              className="absolute w-4 h-4 bg-blue-600 rounded-full shadow-md border-0 cursor-pointer select-none"
              style={{
                left: `calc(${maxPercent}% - 8px)`,
                top: '50%',
                transform: 'translateY(-50%)',
                zIndex: activeThumb === 'max' ? 10 : 8
              }}
              onMouseDown={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setActiveThumb('max');
                setIsInteracting(true);
                setIsDragging(true);
                startWidgetInteraction(widget.id);
              }}
              onTouchStart={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setActiveThumb('max');
                setIsInteracting(true);
                setIsDragging(true);
                startWidgetInteraction(widget.id);
              }}
            />
          </div>
        </div>
        
        <span className="text-xs text-gray-500 select-none">{formatDisplayValue(max)}</span>
      </div>
        
      {/* Value indicators */}
      {showValues && (
        <div className="flex justify-between items-center mt-2">
          <div className="flex items-center space-x-2">
            <span className="text-xs text-gray-500">Min:</span>
            <span className="text-sm font-mono text-gray-900 dark:text-white">
              {formatDisplayValue(localValue[0])}
            </span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-xs text-gray-500">Max:</span>
            <span className="text-sm font-mono text-gray-900 dark:text-white">
              {formatDisplayValue(localValue[1])}
            </span>
          </div>
        </div>
      )}
      
      {step && step !== 1 && (
        <div className="mt-1 text-xs text-gray-400">
          Step: {step}
        </div>
      )}
      
      {minDistance > 0 && (
        <div className="mt-1 text-xs text-gray-400">
          Min distance: {minDistance}
        </div>
      )}
    </div>
  );
};

RangeSliderWidget.propTypes = {
  widget: PropTypes.shape({
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    value: PropTypes.arrayOf(PropTypes.number),
    properties: PropTypes.object
  }).isRequired,
  sessionId: PropTypes.string.isRequired,
  onWidgetUpdate: PropTypes.func,
  optimizations: PropTypes.object
};

export default RangeSliderWidget;
