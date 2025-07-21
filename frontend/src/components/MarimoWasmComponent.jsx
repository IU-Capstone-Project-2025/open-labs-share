import React, { useEffect, useRef, useState } from 'react';

export default function MarimoWasmComponent({ code }) {
  const containerRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!code || !containerRef.current) {
      return;
    }

    // Clear previous instance
    containerRef.current.innerHTML = '';

    const initializeMarimo = () => {
      if (!window.MarimoApp) {
        console.error("MarimoApp script not loaded or available.");
        setError("Interactive environment failed to load. Please refresh the page.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        
        // Programmatically create a new Marimo app instance
        new window.MarimoApp({
          target: containerRef.current, // The DOM element to render into
          notebook: {
            code: code, // The python code for the notebook
          },
          pyodide: {
            // This will use the public CDN for pyodide
          },
          onLoad: () => {
            // This callback is fired when the notebook is ready
            setLoading(false);
          }
        });

      } catch (err) {
        console.error("Failed to initialize Marimo App:", err);
        setError("Could not load the interactive notebook. " + err.message);
        setLoading(false);
      }
    };

    initializeMarimo();

    return () => {
      if (containerRef.current) {
        containerRef.current.innerHTML = '';
      }
    };
  }, [code]);

  return (
    <div className="marimo-wasm-container relative min-h-[200px]">
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-white dark:bg-gray-800 bg-opacity-80 z-10 rounded-lg">
          <div className="text-center">
            <p className="text-lg font-semibold text-gray-800 dark:text-gray-200 animate-pulse">
              Loading Interactive Notebook...
            </p>
            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
              This may take a moment.
            </p>
          </div>
        </div>
      )}
      {error && (
        <div className="p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg">
          <p className="font-bold">Error</p>
          <p>{error}</p>
        </div>
      )}
      <div ref={containerRef} className={loading ? 'opacity-0' : 'opacity-100 transition-opacity duration-300'} />
    </div>
  );
}