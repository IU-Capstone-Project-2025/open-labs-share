import React from 'react';
import PropTypes from 'prop-types';
import WidgetRenderer from './WidgetRenderer';

/**
 * Output Renderer Component
 * Supports STDOUT, STDERR, EXPRESSION_RESULT with proper data type classification
 */
const OutputRenderer = ({ output, index = 0, className = '', sessionId, onWidgetUpdate }) => {
  if (!output) return null;

  const renderByType = () => {
    switch (output.type) {
      case 'STDOUT':
        return (
          <pre className="whitespace-pre-wrap font-mono text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">
            {output.content}
          </pre>
        );

      case 'STDERR':
        return (
          <pre className="whitespace-pre-wrap font-mono text-sm text-orange-700 dark:text-orange-300 overflow-x-auto">
            {output.content}
          </pre>
        );

      case 'EXPRESSION_RESULT':
        return renderExpressionResult(output);

      case 'ERROR':
        return (
          <pre className="text-red-600 dark:text-red-400 whitespace-pre-wrap font-mono text-sm overflow-x-auto">
            {output.content}
          </pre>
        );

      // Legacy output types
      case 'TEXT':
        return (
          <pre className="whitespace-pre-wrap font-mono text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">
            {output.content}
          </pre>
        );

      case 'HTML':
        return (
          <div className="html-output" dangerouslySetInnerHTML={{ __html: output.content }} />
        );

      case 'PLOT':
        return renderPlot(output);

      case 'WIDGET':
        try {
          const widgetData = JSON.parse(output.content);
          if (sessionId && widgetData.id) {
            return (
              <WidgetRenderer
                widget={widgetData}
                sessionId={sessionId}
                onWidgetUpdate={onWidgetUpdate}
              />
            );
          } else {
            // Fallback for when sessionId is not available
            return (
              <div className="widget-fallback p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
                <p className="text-sm text-blue-800 dark:text-blue-200 font-medium">
                  Interactive Widget
                </p>
                <pre className="text-xs text-blue-700 dark:text-blue-300 mt-1">
                  {JSON.stringify(widgetData, null, 2)}
                </pre>
              </div>
            );
          }
        } catch (e) {
          return (
            <div className="text-red-500">
              Failed to parse widget data: {e.message}
            </div>
          );
        }

      default:
        return (
          <pre className="whitespace-pre-wrap font-mono text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">
            {output.content}
          </pre>
        );
    }
  };

  const renderExpressionResult = (output) => {
    switch (output.dataType) {
      case 'HTML_DATA':
        return (
          <div dangerouslySetInnerHTML={{ __html: output.content }} />
        );

      case 'JSON_DATA':
        try {
          const jsonData = JSON.parse(output.content);
          return (
            <pre className="text-sm overflow-x-auto text-gray-800 dark:text-gray-200 font-mono">
              {JSON.stringify(jsonData, null, 2)}
            </pre>
          );
        } catch (e) {
          return (
            <pre className="whitespace-pre-wrap font-mono text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">
              {output.content}
            </pre>
          );
        }

      case 'IMAGE_DATA':
        return renderImage(output);

      case 'TEXT_DATA':
      default:
        return (
          <pre className="whitespace-pre-wrap font-mono text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">
            {output.content}
          </pre>
        );
    }
  };

  const renderImage = (output) => {
    let imgSrc = output.content;
    
    // Handle base64 data
    if (output.data && output.mimeType?.startsWith('image/')) {
      imgSrc = `data:${output.mimeType};base64,${output.data}`;
    }
    
    return (
      <div className="image-output">
        <img 
          src={imgSrc} 
          alt={`output-${index}`} 
          className="max-w-full h-auto border rounded shadow-sm"
          style={{ maxHeight: '400px' }}
          onError={(e) => {
            e.target.style.display = 'none';
            e.target.nextSibling.style.display = 'block';
          }}
        />
        <div className="text-red-500 bg-red-50 dark:bg-red-900/20 p-2 rounded border border-red-200 dark:border-red-800" style={{ display: 'none' }}>
          Failed to load image
        </div>
      </div>
    );
  };

  const renderPlot = (output) => {
    return renderImage(output);
  };

  return (
    <div className={`output-item ${className}`}>
      {renderByType()}
    </div>
  );
};

OutputRenderer.propTypes = {
  output: PropTypes.shape({
    type: PropTypes.string.isRequired,
    content: PropTypes.string,
    data: PropTypes.oneOfType([PropTypes.string, PropTypes.arrayOf(PropTypes.number)]),
    mimeType: PropTypes.string,
    dataType: PropTypes.string,
    metadata: PropTypes.object,
  }).isRequired,
  index: PropTypes.number,
  className: PropTypes.string,
  sessionId: PropTypes.string,
  onWidgetUpdate: PropTypes.func,
};

export default OutputRenderer;
