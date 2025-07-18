import React from 'react';
import WidgetRenderer from './WidgetRenderer';

const MarimoOutput = ({ outputs, onStateChange }) => {
  if (!outputs || outputs.length === 0) {
    return null;
  }

  const renderOutput = (output, index) => {
    switch (output.type) {
      case 'TEXT':
        return <pre key={index} className="whitespace-pre-wrap font-mono text-sm">{output.content}</pre>;
      case 'HTML':
        return <div key={index} dangerouslySetInnerHTML={{ __html: output.content }} />;
      case 'PLOT':
        // Assuming plot is returned as a base64 encoded image in content
        return <img key={index} src={output.content} alt={`plot-${index}`} className="max-w-full h-auto" />;
      case 'WIDGET':
        // Assuming widget data is in JSON format in the content field
        try {
          const widgetData = JSON.parse(output.content);
          return <WidgetRenderer key={index} widgetData={widgetData} onStateChange={onStateChange} />;
        } catch (e) {
          return <div key={index} className="text-red-500">Failed to parse widget data.</div>;
        }
      case 'ERROR':
        return <pre key={index} className="text-red-500 whitespace-pre-wrap font-mono text-sm">{output.content}</pre>;
      default:
        return <div key={index} className="text-yellow-500">Unknown output type: {output.type}</div>;
    }
  };

  return (
    <div className="mt-4 p-4 border rounded-md bg-white min-h-[100px]">
      <h3 className="text-lg font-semibold border-b pb-2 mb-2">Output</h3>
      <div className="space-y-4">
        {outputs.map(renderOutput)}
      </div>
    </div>
  );
};

export default MarimoOutput; 