import React, { useState, useCallback, useRef, useEffect } from 'react';

const ResizablePanel = ({ leftComponent, rightComponent, initialLeftWidth = 33.33, minLeftWidth = 25, minRightWidth = 50 }) => {
  const [leftWidth, setLeftWidth] = useState(initialLeftWidth);
  const isResizing = useRef(false);
  const containerRef = useRef(null);

  const handleMouseDown = (e) => {
    e.preventDefault();
    isResizing.current = true;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  };

  const handleMouseUp = useCallback(() => {
    isResizing.current = false;
    document.body.style.cursor = 'default';
    document.body.style.userSelect = 'auto';
  }, []);

  const handleMouseMove = useCallback((e) => {
    if (!isResizing.current || !containerRef.current) return;

    const containerRect = containerRef.current.getBoundingClientRect();
    const newLeftWidth = ((e.clientX - containerRect.left) / containerRect.width) * 100;

    const rightWidth = 100 - newLeftWidth;

    if (newLeftWidth >= minLeftWidth && rightWidth >= minRightWidth) {
      setLeftWidth(newLeftWidth);
    }
  }, [minLeftWidth, minRightWidth]);

  useEffect(() => {
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [handleMouseMove, handleMouseUp]);

  return (
    <div ref={containerRef} className="flex h-full w-full">
      <div style={{ width: `${leftWidth}%` }} className="h-full overflow-y-auto pr-6">
        {leftComponent}
      </div>
      <div
        onMouseDown={handleMouseDown}
        className="flex-shrink-0 w-2 cursor-col-resize bg-gray-300 dark:bg-gray-700 hover:bg-msc dark:hover:bg-msc-light transition-colors duration-200 mx-2"
        style={{ zIndex: 10 }}
      />
      <div style={{ width: `calc(100% - ${leftWidth}% - 16px)` }} className="h-full overflow-y-auto pl-6">
        {rightComponent}
      </div>
    </div>
  );
};

export default ResizablePanel; 