import React from 'react';
import WidgetRenderer from './WidgetRenderer';
import OutputRenderer from './OutputRenderer';

const MarimoOutput = ({ outputs, onStateChange }) => {
  if (!outputs || outputs.length === 0) {
    return null;
  }

  return (
    <div className="mt-4 p-4 border rounded-md bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 min-h-[100px]">
      <h3 className="text-lg font-semibold border-b pb-2 mb-2 text-gray-900 dark:text-gray-100">Output</h3>
      <div className="space-y-4">
        {outputs.map((output, index) => (
          <OutputRenderer key={index} output={output} index={index} />
        ))}
      </div>
    </div>
  );
};

export default MarimoOutput; 