import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { debounce } from '../../utils/widgetUtils';

/**
 * Interactive Plotly Chart Widget
 * Handles interactive charts and visualizations with real-time data updates
 */
const PlotlyWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { updateWidgetValue, getWidget } = useWidgetState();
  const [localData, setLocalData] = useState(widget.value || {});
  const [isInteracting, setIsInteracting] = useState(false);
  const [chartError, setChartError] = useState(null);
  const [plotlyLib, setPlotlyLib] = useState(null);
  const plotRef = useRef(null);
  const mountedRef = useRef(true);
  
  const {
    enableAnalytics = false,
    enableRealTimeUpdates = true,
    enableInteractions = true
  } = optimizations;

  // Get current widget state
  const widgetState = getWidget(widget.id);
  const isLoading = widgetState?.isLoading || false;
  const error = widgetState?.error || null;

  // Extract properties
  const { 
    title = 'Chart',
    chartType = 'scatter', // 'scatter', 'line', 'bar', 'histogram', 'box', 'heatmap', 'surface', 'pie'
    data = [],
    layout = {},
    config = {},
    width = null,
    height = 400,
    responsive = true,
    showControls = true,
    showLegend = true,
    showToolbar = true,
    theme = 'light', // 'light', 'dark', 'auto'
    animations = true,
    interactivity = true,
    exportFormats = ['png', 'svg', 'pdf', 'html'],
    autoResize = true,
    streaming = false,
    maxDataPoints = 1000
  } = widget.properties || {};

  // Sync local data with widget state
  useEffect(() => {
    if (widgetState && widgetState.value !== localData) {
      setLocalData(widgetState.value || {});
    }
  }, [widgetState?.value, localData]);

  // Load Plotly library dynamically
  useEffect(() => {
    const loadPlotly = async () => {
      try {
        // Try to load Plotly from CDN or local installation
        if (typeof window !== 'undefined' && !window.Plotly) {
          const script = document.createElement('script');
          script.src = 'https://cdn.plot.ly/plotly-latest.min.js';
          script.onload = () => {
            setPlotlyLib(window.Plotly);
          };
          script.onerror = () => {
            setChartError('Failed to load Plotly library');
          };
          document.head.appendChild(script);
        } else if (window.Plotly) {
          setPlotlyLib(window.Plotly);
        }
      } catch (err) {
        setChartError('Error loading Plotly: ' + err.message);
      }
    };

    loadPlotly();
  }, []);

  // Process chart data based on type
  const processedData = useMemo(() => {
    if (!data || !Array.isArray(data)) return [];

    return data.map(trace => ({
      ...trace,
      type: trace.type || chartType,
      // Apply data point limits for performance
      x: trace.x && trace.x.length > maxDataPoints ? trace.x.slice(-maxDataPoints) : trace.x,
      y: trace.y && trace.y.length > maxDataPoints ? trace.y.slice(-maxDataPoints) : trace.y,
      z: trace.z && trace.z.length > maxDataPoints ? trace.z.slice(-maxDataPoints) : trace.z
    }));
  }, [data, chartType, maxDataPoints]);

  // Process layout configuration
  const processedLayout = useMemo(() => {
    const baseLayout = {
      title: title,
      width: responsive ? undefined : width,
      height: height,
      showlegend: showLegend,
      paper_bgcolor: theme === 'dark' ? '#1f2937' : '#ffffff',
      plot_bgcolor: theme === 'dark' ? '#111827' : '#ffffff',
      font: {
        color: theme === 'dark' ? '#f3f4f6' : '#1f2937'
      },
      ...layout
    };

    // Apply responsive settings
    if (responsive) {
      baseLayout.autosize = true;
    }

    return baseLayout;
  }, [title, width, height, showLegend, theme, layout, responsive]);

  // Process configuration
  const processedConfig = useMemo(() => {
    const baseConfig = {
      displayModeBar: showToolbar,
      displaylogo: false,
      modeBarButtonsToRemove: interactivity ? [] : ['zoom2d', 'pan2d', 'select2d', 'lasso2d'],
      responsive: responsive,
      toImageButtonOptions: {
        format: exportFormats[0] || 'png',
        filename: `${widget.id}_chart`,
        width: width,
        height: height,
        scale: 2
      },
      ...config
    };

    if (!showControls) {
      baseConfig.displayModeBar = false;
    }

    return baseConfig;
  }, [showToolbar, interactivity, responsive, exportFormats, widget.id, width, height, config, showControls]);

  // Handle chart interactions
  const handleChartClick = useCallback((data) => {
    if (!interactivity) return;

    const clickPayload = {
      action: 'click',
      points: data.points.map(point => ({
        x: point.x,
        y: point.y,
        z: point.z,
        text: point.text,
        pointIndex: point.pointIndex,
        curveNumber: point.curveNumber
      })),
      timestamp: new Date().toISOString()
    };

    updateWidgetValue(widget.id, clickPayload, 100);
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, clickPayload);
    }

    if (enableAnalytics) {
      console.log(`Chart widget ${widget.id} clicked`, clickPayload);
    }
  }, [interactivity, widget.id, updateWidgetValue, onWidgetUpdate, enableAnalytics]);

  const handleChartHover = useCallback((data) => {
    if (!interactivity) return;

    const hoverPayload = {
      action: 'hover',
      points: data.points.map(point => ({
        x: point.x,
        y: point.y,
        z: point.z,
        text: point.text,
        pointIndex: point.pointIndex,
        curveNumber: point.curveNumber
      })),
      timestamp: new Date().toISOString()
    };

    if (enableAnalytics) {
      console.log(`Chart widget ${widget.id} hover`, hoverPayload);
    }
  }, [interactivity, widget.id, enableAnalytics]);

  const handleChartSelection = useCallback((data) => {
    if (!interactivity) return;

    const selectionPayload = {
      action: 'selection',
      points: data.points.map(point => ({
        x: point.x,
        y: point.y,
        z: point.z,
        text: point.text,
        pointIndex: point.pointIndex,
        curveNumber: point.curveNumber
      })),
      range: data.range,
      timestamp: new Date().toISOString()
    };

    updateWidgetValue(widget.id, selectionPayload, 100);
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, selectionPayload);
    }

    if (enableAnalytics) {
      console.log(`Chart widget ${widget.id} selection`, selectionPayload);
    }
  }, [interactivity, widget.id, updateWidgetValue, onWidgetUpdate, enableAnalytics]);

  // Create or update chart
  const createChart = useCallback(() => {
    if (!plotlyLib || !plotRef.current || !mountedRef.current) return;

    try {
      setChartError(null);
      
      plotlyLib.newPlot(
        plotRef.current,
        processedData,
        processedLayout,
        processedConfig
      ).then(() => {
        // Add event listeners
        if (interactivity) {
          plotRef.current.on('plotly_click', handleChartClick);
          plotRef.current.on('plotly_hover', handleChartHover);
          plotRef.current.on('plotly_selected', handleChartSelection);
        }
      });
    } catch (err) {
      setChartError('Error creating chart: ' + err.message);
    }
  }, [plotlyLib, processedData, processedLayout, processedConfig, interactivity, handleChartClick, handleChartHover, handleChartSelection]);

  // Update chart data
  const updateChart = useCallback(() => {
    if (!plotlyLib || !plotRef.current || !mountedRef.current) return;

    try {
      if (streaming) {
        // For streaming data, use react for better performance
        plotlyLib.react(plotRef.current, processedData, processedLayout, processedConfig);
      } else {
        // For static updates, use restyle for better performance
        plotlyLib.restyle(plotRef.current, processedData);
      }
    } catch (err) {
      setChartError('Error updating chart: ' + err.message);
    }
  }, [plotlyLib, processedData, processedLayout, processedConfig, streaming]);

  // Debounced update for performance
  const debouncedUpdate = useCallback(
    debounce(updateChart, 100),
    [updateChart]
  );

  // Handle chart creation and updates
  useEffect(() => {
    if (plotlyLib && plotRef.current) {
      createChart();
    }
  }, [plotlyLib, createChart]);

  useEffect(() => {
    if (plotlyLib && plotRef.current && enableRealTimeUpdates) {
      debouncedUpdate();
    }
  }, [plotlyLib, processedData, enableRealTimeUpdates, debouncedUpdate]);

  // Handle window resize
  useEffect(() => {
    if (!autoResize || !plotlyLib || !plotRef.current) return;

    const handleResize = () => {
      if (plotRef.current) {
        plotlyLib.Plots.resize(plotRef.current);
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [autoResize, plotlyLib]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      mountedRef.current = false;
      if (plotlyLib && plotRef.current) {
        plotlyLib.purge(plotRef.current);
      }
    };
  }, [plotlyLib]);

  // Export chart functionality
  const exportChart = useCallback((format = 'png') => {
    if (!plotlyLib || !plotRef.current) return;

    plotlyLib.downloadImage(plotRef.current, {
      format: format,
      filename: `${widget.id}_chart`,
      width: width || 800,
      height: height || 600,
      scale: 2
    });
  }, [plotlyLib, widget.id, width, height]);

  if (error || chartError) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Chart Error: {error || chartError}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
        {chartError && (
          <button 
            onClick={() => window.location.reload()}
            className="mt-2 px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700"
          >
            Reload Page
          </button>
        )}
      </div>
    );
  }

  if (!plotlyLib) {
    return (
      <div className="widget-loading p-8 border border-gray-300 rounded-lg bg-gray-50 text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p className="text-gray-600">Loading chart library...</p>
      </div>
    );
  }

  return (
    <div className="widget-plotly relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center z-10">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      {/* Chart Controls */}
      {showControls && (
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-medium text-gray-900 dark:text-white">
            {title}
          </h3>
          
          <div className="flex items-center space-x-2">
            {exportFormats.map(format => (
              <button
                key={format}
                onClick={() => exportChart(format)}
                className="px-3 py-1 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700
                          focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                title={`Export as ${format.toUpperCase()}`}
              >
                {format.toUpperCase()}
              </button>
            ))}
          </div>
        </div>
      )}
      
      {/* Chart Container */}
      <div 
        ref={plotRef}
        className="plotly-chart-container"
        style={{
          width: responsive ? '100%' : width,
          height: height,
          minHeight: '300px'
        }}
      />
      
      {/* Chart Info */}
      {processedData.length === 0 && (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">
          No data available for chart
        </div>
      )}
      
      {streaming && (
        <div className="mt-2 text-xs text-gray-500 flex items-center">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse mr-2"></div>
          Live data streaming active
        </div>
      )}
    </div>
  );
};

PlotlyWidget.propTypes = {
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

export default PlotlyWidget;
