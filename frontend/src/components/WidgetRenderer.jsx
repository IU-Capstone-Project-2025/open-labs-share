import React from 'react';

const WidgetRenderer = ({ widgetData, onStateChange }) => {
  if (!widgetData || !widgetData.type) {
    return <div className="text-red-500">Invalid widget data</div>;
  }

  switch (widgetData.type) {
    case 'slider':
      const { label, min, max, step, value, id } = widgetData.params;
      return (
        <div className="my-2 p-2 border rounded-md bg-gray-50">
          <label htmlFor={id} className="block text-sm font-medium text-gray-700">
            {label || 'Slider'}
          </label>
          <div className="flex items-center space-x-4">
            <input
              type="range"
              id={id}
              min={min || 0}
              max={max || 100}
              step={step || 1}
              value={value || 0}
              onChange={(e) => onStateChange(id, e.target.value)}
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            />
            <span className="text-sm font-semibold text-gray-800">{value}</span>
          </div>
        </div>
      );
    default:
      return <div className="text-yellow-500">Unsupported widget type: {widgetData.type}</div>;
  }
};

export default WidgetRenderer; 