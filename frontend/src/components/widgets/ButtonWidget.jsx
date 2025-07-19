import React, { useState, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';

/**
 * Button Widget
 * Handles clickable actions with loading states, icons, and accessibility
 */
const ButtonWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { updateWidgetValue, getWidget } = useWidgetState();
  const [isClicked, setIsClicked] = useState(false);
  const [clickCount, setClickCount] = useState(0);
  const buttonRef = useRef(null);
  const mountedRef = useRef(true);
  
  const {
    enableAnalytics = false,
    enableRippleEffect = true
  } = optimizations;

  // Get current widget state
  const widgetState = getWidget(widget.id);
  const isLoading = widgetState?.isLoading || false;
  const error = widgetState?.error || null;

  // Extract properties
  const { 
    label = 'Button',
    variant = 'primary', // 'primary' | 'secondary' | 'danger' | 'success' | 'warning' | 'ghost'
    size = 'medium', // 'small' | 'medium' | 'large'
    disabled = false,
    loading = false,
    icon = null,
    iconPosition = 'left', // 'left' | 'right' | 'only'
    fullWidth = false,
    rounded = true,
    tooltip = null,
    confirmAction = false,
    confirmMessage = 'Are you sure?',
    debounceMs = 300,
    maxClicks = null,
    resetClicksAfter = null // milliseconds
  } = widget.properties || {};

  // Handle button click
  const handleClick = useCallback(async (e) => {
    if (disabled || loading || isLoading) return;
    
    // Check max clicks limit
    if (maxClicks && clickCount >= maxClicks) {
      console.warn(`Button ${widget.id} has reached maximum clicks (${maxClicks})`);
      return;
    }

    // Confirmation dialog
    if (confirmAction && !window.confirm(confirmMessage)) {
      return;
    }

    // Visual feedback
    setIsClicked(true);
    setTimeout(() => setIsClicked(false), 200);

    // Update click count
    const newClickCount = clickCount + 1;
    setClickCount(newClickCount);

    // Reset click count after specified time
    if (resetClicksAfter) {
      setTimeout(() => setClickCount(0), resetClicksAfter);
    }

    // Create action payload
    const actionPayload = {
      action: 'click',
      timestamp: new Date().toISOString(),
      clickCount: newClickCount,
      sessionId,
      metadata: {
        x: e.clientX,
        y: e.clientY,
        ctrlKey: e.ctrlKey,
        shiftKey: e.shiftKey,
        altKey: e.altKey
      }
    };

    // Update widget value with action payload
    updateWidgetValue(widget.id, actionPayload, debounceMs);
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, actionPayload);
    }

    if (enableAnalytics) {
      console.log(`Button widget ${widget.id} clicked`, actionPayload);
    }
  }, [
    disabled, loading, isLoading, clickCount, maxClicks, confirmAction, confirmMessage,
    widget.id, sessionId, updateWidgetValue, onWidgetUpdate, debounceMs, enableAnalytics,
    resetClicksAfter
  ]);

  // Handle keyboard interaction
  const handleKeyDown = useCallback((e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick(e);
    }
  }, [handleClick]);

  // Get variant classes
  const getVariantClasses = useCallback(() => {
    const baseClasses = 'font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2';
    
    switch (variant) {
      case 'primary':
        return `${baseClasses} bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500 disabled:bg-blue-300`;
      case 'secondary':
        return `${baseClasses} bg-gray-600 text-white hover:bg-gray-700 focus:ring-gray-500 disabled:bg-gray-300`;
      case 'danger':
        return `${baseClasses} bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 disabled:bg-red-300`;
      case 'success':
        return `${baseClasses} bg-green-600 text-white hover:bg-green-700 focus:ring-green-500 disabled:bg-green-300`;
      case 'warning':
        return `${baseClasses} bg-yellow-600 text-white hover:bg-yellow-700 focus:ring-yellow-500 disabled:bg-yellow-300`;
      case 'ghost':
        return `${baseClasses} bg-transparent text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 focus:ring-gray-500 disabled:text-gray-400`;
      default:
        return `${baseClasses} bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500 disabled:bg-blue-300`;
    }
  }, [variant]);

  // Get size classes
  const getSizeClasses = useCallback(() => {
    switch (size) {
      case 'small':
        return 'px-3 py-1.5 text-sm';
      case 'large':
        return 'px-6 py-3 text-lg';
      default: // medium
        return 'px-4 py-2 text-base';
    }
  }, [size]);

  // Get icon element
  const getIcon = useCallback(() => {
    if (!icon) return null;
    
    // Handle different icon types
    if (typeof icon === 'string') {
      // Assume it's an emoji or text icon
      return <span className="inline-block">{icon}</span>;
    } else if (React.isValidElement(icon)) {
      // React component icon
      return icon;
    }
    
    return null;
  }, [icon]);

  const isDisabled = disabled || loading || isLoading;
  const showLoading = loading || isLoading;
  const IconComponent = getIcon();

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Button Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-button relative">
      <button
        ref={buttonRef}
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        disabled={isDisabled}
        title={tooltip}
        className={`
          relative inline-flex items-center justify-center overflow-hidden
          ${getVariantClasses()}
          ${getSizeClasses()}
          ${rounded ? 'rounded-lg' : 'rounded-none'}
          ${fullWidth ? 'w-full' : ''}
          ${isDisabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
          ${isClicked ? 'transform scale-95' : ''}
          ${enableRippleEffect ? 'ripple' : ''}
        `}
      >
        {showLoading && (
          <div className="absolute inset-0 flex items-center justify-center bg-inherit">
            <div className="animate-spin rounded-full h-4 w-4 border-2 border-current border-t-transparent"></div>
          </div>
        )}
        
        <div className={`flex items-center space-x-2 ${showLoading ? 'opacity-0' : 'opacity-100'}`}>
          {IconComponent && iconPosition === 'left' && (
            <span className="flex-shrink-0">{IconComponent}</span>
          )}
          
          {iconPosition !== 'only' && (
            <span className="whitespace-nowrap">{label}</span>
          )}
          
          {IconComponent && iconPosition === 'right' && (
            <span className="flex-shrink-0">{IconComponent}</span>
          )}
          
          {IconComponent && iconPosition === 'only' && (
            <span className="flex-shrink-0">{IconComponent}</span>
          )}
        </div>
        
        {/* Click count indicator */}
        {maxClicks && clickCount > 0 && (
          <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
            {clickCount}
          </span>
        )}
      </button>
      
      {/* Click count info */}
      {maxClicks && (
        <div className="mt-1 text-xs text-gray-500">
          {clickCount}/{maxClicks} clicks
        </div>
      )}
    </div>
  );
};

ButtonWidget.propTypes = {
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

export default ButtonWidget;
