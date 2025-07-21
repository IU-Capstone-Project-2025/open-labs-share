import React, { useState, useEffect, useRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import { useMarimoSession } from '../contexts/MarimoSessionContext';
import { useWidgetState } from '../contexts/WidgetStateContext';
import MarimoAssetList from './MarimoAssetList';
import OutputRenderer from './OutputRenderer';
import CodeMirror from '@uiw/react-codemirror';
import { python } from '@codemirror/lang-python';
import { oneDark } from '@codemirror/theme-one-dark';

// Helper function to extract error message from Python traceback
const extractErrorMessage = (tracebackStr) => {
  if (!tracebackStr) {
    return "";
  }
  
  // Split into lines and process in reverse to find the last non-empty line with a colon
  // This will typically be the actual error message like "NameError: name 'c' is not defined"
  const lines = tracebackStr.trim().split('\n').map(line => line.trim());
  
  for (let i = lines.length - 1; i >= 0; i--) {
    const line = lines[i];
    if (line && line.includes(':')) {
      // Return first line having a colon after stripping
      return line;
    }
  }
  
  // Fallback to original message if no colon found
  return tracebackStr;
};

const MarimoCell = ({ component }) => {
  const [output, setOutput] = useState(null);
  const [error, setError] = useState(null);
  const [isRunning, setIsRunning] = useState(false);
  const [code, setCode] = useState(component.code || '');
  const [showAssets, setShowAssets] = useState(false);
  const [assetCount, setAssetCount] = useState(0);
  const mountedRef = useRef(true);
  
  // Use shared session context
  const { sharedSessionId, sessionError, executeInSharedSession, isSessionReady } = useMarimoSession();
  
  // Use widget state context
  const { clearWidgets } = useWidgetState();

  // Detect current theme
  const [isDarkMode, setIsDarkMode] = useState(false);

  useEffect(() => {
    // Add custom styles for CodeMirror cursor
    const style = document.createElement('style');
    style.textContent = `
      .codemirror-wrapper .cm-editor {
        cursor: text !important;
      }
      .codemirror-wrapper .cm-editor .cm-content {
        cursor: text !important;
      }
      .codemirror-wrapper .cm-editor .cm-line {
        cursor: text !important;
      }
      .codemirror-wrapper .cm-editor:hover {
        cursor: text !important;
      }
    `;
    document.head.appendChild(style);

    return () => {
      document.head.removeChild(style);
    };
  }, []);

  useEffect(() => {
    // Check if dark mode is active
    const checkDarkMode = () => {
      const isDark = document.documentElement.classList.contains('dark');
      setIsDarkMode(isDark);
    };

    // Initial check
    checkDarkMode();

    // Listen for theme changes
    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, { 
      attributes: true, 
      attributeFilter: ['class'] 
    });

    return () => observer.disconnect();
  }, []);

  // Set initial error from session context
  useEffect(() => {
    if (sessionError) {
      const cleanError = extractErrorMessage(sessionError);
      setError(cleanError);
    }
  }, [sessionError]);

  // Stable callback for asset count changes
  const handleAssetCountChange = useCallback((count) => {
    console.log('Asset count changed:', count); // Debug log
    setAssetCount(count);
  }, []);

  const handleRun = async () => {
    if (!isSessionReady || !mountedRef.current) {
      setError('Session not ready.');
      return;
    }
    
    setIsRunning(true);
    setError(null);
    
    try {
      const result = await executeInSharedSession(component.id, code);
      if (mountedRef.current) {
        setOutput(result);
      }
    } catch (err) {
      if (mountedRef.current) {
        const cleanError = extractErrorMessage(err.message);
        setError(cleanError);
        setOutput(null);
      }
    } finally {
      if (mountedRef.current) {
        setIsRunning(false);
      }
    }
  };

  const renderOutput = () => {
    if (!output) return null;

    // Handle different output formats from the API
    if (output.outputs && Array.isArray(output.outputs)) {
      return output.outputs.map((out, index) => (
        <OutputRenderer 
          key={index} 
          output={out} 
          index={index} 
          sessionId={sharedSessionId}
          onWidgetUpdate={handleWidgetUpdate}
        />
      ));
    }

    // Fallback for other response formats
    return <pre className="text-gray-800 dark:text-gray-200 whitespace-pre-wrap font-mono text-sm bg-gray-50 dark:bg-gray-900 p-2 rounded border">{JSON.stringify(output, null, 2)}</pre>;
  };

  // Handle widget updates
  const handleWidgetUpdate = useCallback((widgetId, newValue) => {
    console.log(`Widget ${widgetId} updated to:`, newValue);
    // Widget state is managed by WidgetStateContext, no additional handling needed here
  }, []);

  return (
    <div className="marimo-cell bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg my-4 shadow-sm">
      <div className="flex items-start gap-3 p-4">
        <div className="flex-shrink-0 mt-1">
          <button
            onClick={handleRun}
            disabled={isRunning || !isSessionReady}
            className="w-8 h-8 flex items-center justify-center rounded-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 dark:disabled:bg-gray-600 transition-colors"
            title={isRunning ? 'Running...' : 'Run Cell'}
          >
            {isRunning ? (
              <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            ) : (
              <svg className="w-3 h-3 text-white ml-0.5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z"/>
              </svg>
            )}
          </button>
        </div>
        <div className="flex-1 min-w-0">
          <div className="code-editor mb-3">
            <div className="codemirror-wrapper">
              <CodeMirror
                value={code}
                height="auto"
                extensions={[python()]}
                theme={isDarkMode ? oneDark : 'light'}
                onChange={(value) => setCode(value)}
                className="text-sm border border-gray-200 dark:border-gray-600 rounded"
              />
            </div>
          </div>

          {/* Always mount MarimoAssetList to get asset count */}
          <div className="mb-3">
            <MarimoAssetList 
              componentId={component.id} 
              onAssetCountChange={handleAssetCountChange}
              showUI={assetCount > 0}
              showAssets={showAssets}
              onToggleAssets={() => setShowAssets(!showAssets)}
            />
          </div>
          {error && (
            <div className="text-red-600 dark:text-red-400 text-sm mb-3 p-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded">
              {error}
            </div>
          )}
          {(output || isRunning) && (
            <div className="output bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded p-3">
              <div className="text-xs text-gray-500 dark:text-gray-400 mb-2 font-medium">Output:</div>
              {isRunning ? (
                <div className="text-gray-600 dark:text-gray-400 text-sm italic">Running...</div>
              ) : (
                <div className="text-sm">{renderOutput()}</div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

MarimoCell.propTypes = {
  component: PropTypes.shape({
    id: PropTypes.string.isRequired,
    code: PropTypes.string,
  }).isRequired,
};

export default MarimoCell;
