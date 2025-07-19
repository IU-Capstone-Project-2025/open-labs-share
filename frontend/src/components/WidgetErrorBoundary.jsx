import React from 'react';
import PropTypes from 'prop-types';

/**
 * Widget Error Boundary Component
 * Catches and handles errors in widget components with graceful fallbacks
 */
class WidgetErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      retryCount: 0,
      lastErrorTime: null
    };
  }

  static getDerivedStateFromError(error) {
    // Update state to trigger error UI
    return { 
      hasError: true, 
      error,
      lastErrorTime: new Date().toISOString()
    };
  }

  componentDidCatch(error, errorInfo) {
    // Log error details
    console.error('Widget Error Boundary caught an error:', error, errorInfo);
    
    this.setState({
      error,
      errorInfo,
      retryCount: this.state.retryCount + 1
    });

    // Send error to analytics/logging service
    if (this.props.onError) {
      this.props.onError({
        error: error.message,
        stack: error.stack,
        componentStack: errorInfo.componentStack,
        widgetId: this.props.widgetId,
        widgetType: this.props.widgetType,
        timestamp: new Date().toISOString(),
        retryCount: this.state.retryCount,
        sessionId: this.props.sessionId
      });
    }

    // Auto-retry for certain error types
    if (this.shouldAutoRetry(error) && this.state.retryCount < this.props.maxRetries) {
      setTimeout(() => {
        this.handleRetry();
      }, this.props.retryDelay * Math.pow(2, this.state.retryCount)); // Exponential backoff
    }
  }

  shouldAutoRetry = (error) => {
    const retryableErrors = [
      'ChunkLoadError',
      'Loading chunk',
      'Network error',
      'Failed to fetch',
      'TypeError: Failed to fetch'
    ];

    return retryableErrors.some(pattern => 
      error.message.includes(pattern) || error.name.includes(pattern)
    );
  };

  handleRetry = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null
    });
  };

  handleReload = () => {
    if (this.props.onReload) {
      this.props.onReload();
    } else {
      window.location.reload();
    }
  };

  getErrorSeverity = () => {
    const { error } = this.state;
    if (!error) return 'low';

    // Critical errors that require immediate attention
    if (error.name === 'ChunkLoadError' || error.message.includes('Loading chunk')) {
      return 'critical';
    }

    // High priority errors
    if (error.name === 'TypeError' || error.name === 'ReferenceError') {
      return 'high';
    }

    // Medium priority errors
    if (error.name === 'RangeError' || error.name === 'SyntaxError') {
      return 'medium';
    }

    return 'low';
  };

  getErrorIcon = () => {
    const severity = this.getErrorSeverity();
    switch (severity) {
      case 'critical':
        return '🚨';
      case 'high':
        return '⚠️';
      case 'medium':
        return '⚡';
      default:
        return '🔧';
    }
  };

  getErrorColor = () => {
    const severity = this.getErrorSeverity();
    switch (severity) {
      case 'critical':
        return 'border-red-500 bg-red-50';
      case 'high':
        return 'border-orange-500 bg-orange-50';
      case 'medium':
        return 'border-yellow-500 bg-yellow-50';
      default:
        return 'border-gray-500 bg-gray-50';
    }
  };

  renderErrorDetails = () => {
    const { error, errorInfo } = this.state;
    const { showDetails = false } = this.props;

    if (!showDetails) return null;

    return (
      <details className="mt-4 text-sm">
        <summary className="cursor-pointer font-medium text-gray-700 hover:text-gray-900">
          Technical Details
        </summary>
        <div className="mt-2 space-y-2 text-xs text-gray-600">
          <div>
            <strong>Error:</strong> {error.message}
          </div>
          <div>
            <strong>Type:</strong> {error.name}
          </div>
          <div>
            <strong>Widget:</strong> {this.props.widgetType} ({this.props.widgetId})
          </div>
          <div>
            <strong>Retry Count:</strong> {this.state.retryCount}
          </div>
          <div>
            <strong>Timestamp:</strong> {this.state.lastErrorTime}
          </div>
          {error.stack && (
            <div>
              <strong>Stack Trace:</strong>
              <pre className="mt-1 p-2 bg-gray-100 rounded text-xs overflow-x-auto">
                {error.stack}
              </pre>
            </div>
          )}
          {errorInfo && errorInfo.componentStack && (
            <div>
              <strong>Component Stack:</strong>
              <pre className="mt-1 p-2 bg-gray-100 rounded text-xs overflow-x-auto">
                {errorInfo.componentStack}
              </pre>
            </div>
          )}
        </div>
      </details>
    );
  };

  renderFallback = () => {
    const { fallback: CustomFallback, widgetType, widgetId } = this.props;
    const { error, retryCount } = this.state;

    // Use custom fallback if provided
    if (CustomFallback) {
      return (
        <CustomFallback 
          error={error}
          retryCount={retryCount}
          onRetry={this.handleRetry}
          onReload={this.handleReload}
          widgetType={widgetType}
          widgetId={widgetId}
        />
      );
    }

    // Default fallback UI
    return (
      <div className={`p-4 border rounded-lg ${this.getErrorColor()}`}>
        <div className="flex items-start space-x-3">
          <div className="flex-shrink-0 text-2xl">
            {this.getErrorIcon()}
          </div>
          
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-medium text-gray-900">
              Widget Error
            </h3>
            
            <p className="mt-1 text-sm text-gray-700">
              The <strong>{widgetType}</strong> widget encountered an error and couldn't render properly.
            </p>
            
            {error && (
              <p className="mt-2 text-sm text-gray-600 font-mono">
                {error.message}
              </p>
            )}
            
            <div className="mt-4 flex items-center space-x-3">
              <button
                onClick={this.handleRetry}
                disabled={retryCount >= this.props.maxRetries}
                className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md
                          text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500
                          disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {retryCount >= this.props.maxRetries ? 'Max Retries Reached' : 'Retry'}
              </button>
              
              <button
                onClick={this.handleReload}
                className="inline-flex items-center px-3 py-2 border border-gray-300 text-sm font-medium rounded-md
                          text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                Reload Page
              </button>
            </div>
            
            {this.renderErrorDetails()}
          </div>
        </div>
      </div>
    );
  };

  render() {
    if (this.state.hasError) {
      return this.renderFallback();
    }

    return this.props.children;
  }
}

WidgetErrorBoundary.propTypes = {
  children: PropTypes.node.isRequired,
  widgetId: PropTypes.string.isRequired,
  widgetType: PropTypes.string.isRequired,
  sessionId: PropTypes.string,
  onError: PropTypes.func,
  onReload: PropTypes.func,
  fallback: PropTypes.elementType,
  maxRetries: PropTypes.number,
  retryDelay: PropTypes.number,
  showDetails: PropTypes.bool
};

WidgetErrorBoundary.defaultProps = {
  maxRetries: 3,
  retryDelay: 1000,
  showDetails: false
};

export default WidgetErrorBoundary;
