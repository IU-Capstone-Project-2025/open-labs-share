import { useState } from 'react';

export const Tooltip = ({ children, content, position = 'top', delay = 0 }) => {
  const [visible, setVisible] = useState(false);
  const [timeoutId, setTimeoutId] = useState(null);

  const handleMouseEnter = () => {
    const id = setTimeout(() => {
      setVisible(true);
    }, delay);
    setTimeoutId(id);
  };

  const handleMouseLeave = () => {
    clearTimeout(timeoutId);
    setVisible(false);
  };

  const positionClasses = {
    top: 'bottom-full left-1/2 transform -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 transform -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 transform -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 transform -translate-y-1/2 ml-2',
  };

  return (
    <div 
      className="relative inline-block"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {children}
      {visible && (
        <div 
          className={`absolute z-50 w-max max-w-xs px-3 py-2 text-sm text-white dark:text-gray-100 bg-blue-hover dark:bg-gray-800 rounded-md shadow-lg ${positionClasses[position]}`}
        >
          {content}
          <div 
            className={`absolute w-2 h-2 bg-blue-hover dark:bg-gray-800 transform rotate-45 ${
              position === 'top' ? 'bottom-0 left-1/2 -mb-1 -translate-x-1/2' :
              position === 'bottom' ? 'top-0 left-1/2 -mt-1 -translate-x-1/2' :
              position === 'left' ? 'right-0 top-1/2 -mr-1 -translate-y-1/2' :
              'left-0 top-1/2 -ml-1 -translate-y-1/2'
            }`}
          />
        </div>
      )}
    </div>
  );
};