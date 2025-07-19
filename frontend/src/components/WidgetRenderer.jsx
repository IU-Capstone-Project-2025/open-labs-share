import React, { useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../contexts/WidgetStateContext';

// Import individual widget components
import SliderWidget from './widgets/SliderWidget';
import NumberWidget from './widgets/NumberWidget';
import DropdownWidget from './widgets/DropdownWidget';
import RadioWidget from './widgets/RadioWidget';
import SwitchWidget from './widgets/SwitchWidget';
import TextAreaWidget from './widgets/TextAreaWidget';
import RangeSliderWidget from './widgets/RangeSliderWidget';
import MultiselectWidget from './widgets/MultiselectWidget';
import ButtonWidget from './widgets/ButtonWidget';
import TableWidget from './widgets/TableWidget';
import PlotlyWidget from './widgets/PlotlyWidget';

// Import error boundary
import WidgetErrorBoundary from './WidgetErrorBoundary';

/**
 * Widget renderer dispatcher that delegates to individual widget components
 * Routes to appropriate widget component based on type and wraps with error boundaries
 */
const WidgetRenderer = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { registerWidget, hasWidget } = useWidgetState();
  const mountedRef = useRef(true);

  // Register widget on mount - only if not already registered
  useEffect(() => {
    // Only register if the widget doesn't exist yet
    if (!hasWidget(widget.id)) {
      registerWidget(widget.id, widget.type, widget.value, widget.properties);
    }
    
    return () => {
      mountedRef.current = false;
    };
  }, [registerWidget, hasWidget, widget.id, widget.type]); // Remove widget.value and widget.properties from deps

  // Route to appropriate widget component based on type
  const renderWidget = () => {
    const commonProps = {
      widget,
      sessionId,
      onWidgetUpdate,
      optimizations
    };

    switch (widget.type) {
      // Basic Input Widgets
      case 'slider':
        return <SliderWidget {...commonProps} />;
      case 'number':
        return <NumberWidget {...commonProps} />;
      
      // Advanced Input Controls
      case 'dropdown':
      case 'select':
        return <DropdownWidget {...commonProps} />;
      case 'radio':
        return <RadioWidget {...commonProps} />;
      case 'switch':
      case 'checkbox':
        return <SwitchWidget {...commonProps} />;
      case 'text':
      case 'textarea':
        return <TextAreaWidget {...commonProps} />;
      case 'range':
      case 'range_slider':
        return <RangeSliderWidget {...commonProps} />;
      case 'multiselect':
        return <MultiselectWidget {...commonProps} />;
      
      // Action Controls
      case 'button':
        return <ButtonWidget {...commonProps} />;
      
      // Data Display & Interaction
      case 'table':
      case 'dataframe':
        return <TableWidget {...commonProps} />;
      
      // Chart Integration
      case 'plotly':
      case 'chart':
      case 'plot':
        return <PlotlyWidget {...commonProps} />;
      
      default:
        return (
          <div className="widget-unknown p-4 border border-red-300 rounded-lg bg-red-50">
            <p className="text-red-700">Unknown widget type: {widget.type}</p>
          </div>
        );
    }
  };

  return (
    <WidgetErrorBoundary
      widgetId={widget.id}
      widgetType={widget.type}
      sessionId={sessionId}
      onError={(errorInfo) => {
        console.error('Widget error:', errorInfo);
        // You can send this to an error tracking service
      }}
      showDetails={optimizations.showErrorDetails || false}
    >
      <div className="widget-container">
        {renderWidget()}
      </div>
    </WidgetErrorBoundary>
  );
};

WidgetRenderer.propTypes = {
  widget: PropTypes.shape({
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    value: PropTypes.any.isRequired,
    properties: PropTypes.object
  }).isRequired,
  sessionId: PropTypes.string.isRequired,
  onWidgetUpdate: PropTypes.func,
  optimizations: PropTypes.shape({
    enableMemoization: PropTypes.bool,
    enableThrottling: PropTypes.bool,
    enableAnalytics: PropTypes.bool,
    batchUpdates: PropTypes.bool,
    showErrorDetails: PropTypes.bool,
    enableRealTimeUpdates: PropTypes.bool,
    enableInteractions: PropTypes.bool
  })
};

export default WidgetRenderer;
