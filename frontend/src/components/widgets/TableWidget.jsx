import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { debounce } from '../../utils/widgetUtils';

/**
 * Table Widget
 * Handles tabular data display with sorting, filtering, pagination, and selection
 */
const TableWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { updateWidgetValue, getWidget } = useWidgetState();
  const [localData, setLocalData] = useState(widget.value || []);
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });
  const [filterText, setFilterText] = useState('');
  const [selectedRows, setSelectedRows] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const tableRef = useRef(null);
  const mountedRef = useRef(true);
  
  const {
    enableAnalytics = false,
    enableVirtualization = false
  } = optimizations;

  // Get current widget state
  const widgetState = getWidget(widget.id);
  const isLoading = widgetState?.isLoading || false;
  const error = widgetState?.error || null;

  // Extract properties
  const { 
    label = 'Table',
    columns = [],
    selectable = false,
    sortable = true,
    filterable = true,
    paginated = true,
    striped = true,
    bordered = true,
    hover = true,
    compact = false,
    showHeader = true,
    showFooter = true,
    allowColumnReorder = false,
    allowColumnResize = false,
    exportable = false,
    maxHeight = null,
    stickyHeader = false,
    rowActions = [],
    onRowClick = null,
    onRowDoubleClick = null
  } = widget.properties || {};

  // Sync local data with widget state
  useEffect(() => {
    if (widgetState && widgetState.value !== localData) {
      setLocalData(widgetState.value || []);
    }
  }, [widgetState?.value, localData]);

  // Process and filter data
  const processedData = useMemo(() => {
    let data = [...localData];
    
    // Apply filtering
    if (filterText && filterable) {
      data = data.filter(row => 
        Object.values(row).some(value => 
          value?.toString().toLowerCase().includes(filterText.toLowerCase())
        )
      );
    }
    
    // Apply sorting
    if (sortConfig.key && sortable) {
      data.sort((a, b) => {
        const aValue = a[sortConfig.key];
        const bValue = b[sortConfig.key];
        
        if (aValue < bValue) {
          return sortConfig.direction === 'asc' ? -1 : 1;
        }
        if (aValue > bValue) {
          return sortConfig.direction === 'asc' ? 1 : -1;
        }
        return 0;
      });
    }
    
    return data;
  }, [localData, filterText, sortConfig, filterable, sortable]);

  // Paginate data
  const paginatedData = useMemo(() => {
    if (!paginated) return processedData;
    
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return processedData.slice(startIndex, endIndex);
  }, [processedData, currentPage, pageSize, paginated]);

  // Calculate pagination info
  const totalPages = useMemo(() => {
    if (!paginated) return 1;
    return Math.ceil(processedData.length / pageSize);
  }, [processedData.length, pageSize, paginated]);

  // Handle sorting
  const handleSort = useCallback((columnKey) => {
    if (!sortable) return;
    
    setSortConfig(prev => ({
      key: columnKey,
      direction: prev.key === columnKey && prev.direction === 'asc' ? 'desc' : 'asc'
    }));
    
    if (enableAnalytics) {
      console.log(`Table widget ${widget.id} sorted by ${columnKey}`);
    }
  }, [sortable, widget.id, enableAnalytics]);

  // Handle filtering
  const handleFilter = useCallback((text) => {
    setFilterText(text);
    setCurrentPage(1); // Reset to first page
    
    if (enableAnalytics) {
      console.log(`Table widget ${widget.id} filtered with: ${text}`);
    }
  }, [widget.id, enableAnalytics]);

  // Debounced filter handler
  const debouncedFilter = useCallback(
    debounce(handleFilter, 300),
    [handleFilter]
  );

  // Handle row selection
  const handleRowSelection = useCallback((rowIndex, isSelected) => {
    if (!selectable) return;
    
    setSelectedRows(prev => {
      const newSelection = isSelected
        ? [...prev, rowIndex]
        : prev.filter(index => index !== rowIndex);
      
      // Update widget value with selection
      const selectionPayload = {
        selectedRows: newSelection,
        selectedData: newSelection.map(index => paginatedData[index])
      };
      
      updateWidgetValue(widget.id, selectionPayload, 100);
      
      if (onWidgetUpdate) {
        onWidgetUpdate(widget.id, selectionPayload);
      }
      
      return newSelection;
    });
  }, [selectable, paginatedData, widget.id, updateWidgetValue, onWidgetUpdate]);

  // Handle select all
  const handleSelectAll = useCallback((isSelected) => {
    if (!selectable) return;
    
    const newSelection = isSelected 
      ? Array.from({ length: paginatedData.length }, (_, i) => i)
      : [];
    
    setSelectedRows(newSelection);
    
    const selectionPayload = {
      selectedRows: newSelection,
      selectedData: newSelection.map(index => paginatedData[index])
    };
    
    updateWidgetValue(widget.id, selectionPayload, 100);
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, selectionPayload);
    }
  }, [selectable, paginatedData, widget.id, updateWidgetValue, onWidgetUpdate]);

  // Handle row click
  const handleRowClick = useCallback((row, index, event) => {
    if (onRowClick) {
      const clickPayload = {
        action: 'rowClick',
        row,
        index,
        timestamp: new Date().toISOString(),
        ctrlKey: event.ctrlKey,
        shiftKey: event.shiftKey
      };
      
      onRowClick(clickPayload);
      
      if (enableAnalytics) {
        console.log(`Table widget ${widget.id} row clicked`, clickPayload);
      }
    }
  }, [onRowClick, widget.id, enableAnalytics]);

  // Handle row double click
  const handleRowDoubleClick = useCallback((row, index, event) => {
    if (onRowDoubleClick) {
      const doubleClickPayload = {
        action: 'rowDoubleClick',
        row,
        index,
        timestamp: new Date().toISOString()
      };
      
      onRowDoubleClick(doubleClickPayload);
      
      if (enableAnalytics) {
        console.log(`Table widget ${widget.id} row double clicked`, doubleClickPayload);
      }
    }
  }, [onRowDoubleClick, widget.id, enableAnalytics]);

  // Handle pagination
  const handlePageChange = useCallback((newPage) => {
    setCurrentPage(newPage);
    setSelectedRows([]); // Clear selection when changing pages
  }, []);

  // Handle page size change
  const handlePageSizeChange = useCallback((newPageSize) => {
    setPageSize(newPageSize);
    setCurrentPage(1);
    setSelectedRows([]);
  }, []);

  // Render cell content
  const renderCell = useCallback((value, column, row, rowIndex) => {
    if (column.render) {
      return column.render(value, row, rowIndex);
    }
    
    if (column.type === 'boolean') {
      return (
        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium
                         ${value ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
          {value ? 'Yes' : 'No'}
        </span>
      );
    }
    
    if (column.type === 'number') {
      return (
        <span className="font-mono text-right">
          {typeof value === 'number' ? value.toLocaleString() : value}
        </span>
      );
    }
    
    if (column.type === 'date') {
      return new Date(value).toLocaleDateString();
    }
    
    return value?.toString() || '';
  }, []);

  // Get table classes
  const getTableClasses = useCallback(() => {
    return [
      'w-full divide-y divide-gray-200 dark:divide-gray-700',
      bordered ? 'border border-gray-200 dark:border-gray-700' : '',
      compact ? 'text-sm' : '',
      'table-auto'
    ].filter(Boolean).join(' ');
  }, [bordered, compact]);

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Table Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-table relative">
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center z-10">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-medium text-gray-900 dark:text-white">
          {label}
          <span className="ml-2 text-sm text-gray-500">
            ({processedData.length} {processedData.length === 1 ? 'row' : 'rows'})
          </span>
        </h3>
        
        <div className="flex items-center space-x-2">
          {filterable && (
            <input
              type="text"
              placeholder="Filter table..."
              onChange={(e) => debouncedFilter(e.target.value)}
              className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md text-sm
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                        dark:bg-gray-700 dark:text-white"
            />
          )}
          
          {exportable && (
            <button
              onClick={() => {
                // Export functionality would go here
                console.log('Export table data', processedData);
              }}
              className="px-3 py-1 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Export
            </button>
          )}
        </div>
      </div>
      
      {/* Table Container */}
      <div className={`overflow-auto ${maxHeight ? `max-h-[${maxHeight}px]` : ''}`}>
        <table ref={tableRef} className={getTableClasses()}>
          {/* Table Head */}
          {showHeader && (
            <thead className={`bg-gray-50 dark:bg-gray-800 ${stickyHeader ? 'sticky top-0 z-10' : ''}`}>
              <tr>
                {selectable && (
                  <th className="w-12 px-3 py-3 text-left">
                    <input
                      type="checkbox"
                      checked={selectedRows.length === paginatedData.length && paginatedData.length > 0}
                      onChange={(e) => handleSelectAll(e.target.checked)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                  </th>
                )}
                
                {columns.map((column) => (
                  <th
                    key={column.key}
                    className={`px-3 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider
                               ${sortable && column.sortable !== false ? 'cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700' : ''}`}
                    onClick={() => sortable && column.sortable !== false && handleSort(column.key)}
                  >
                    <div className="flex items-center space-x-1">
                      <span>{column.label || column.key}</span>
                      {sortable && column.sortable !== false && (
                        <span className="flex flex-col">
                          <svg className={`w-3 h-3 ${sortConfig.key === column.key && sortConfig.direction === 'asc' ? 'text-blue-600' : 'text-gray-400'}`} fill="currentColor" viewBox="0 0 20 20">
                            <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" />
                          </svg>
                        </span>
                      )}
                    </div>
                  </th>
                ))}
                
                {rowActions.length > 0 && (
                  <th className="w-20 px-3 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Actions
                  </th>
                )}
              </tr>
            </thead>
          )}
          
          {/* Table Body */}
          <tbody className={`divide-y divide-gray-200 dark:divide-gray-700 ${striped ? 'bg-white dark:bg-gray-900' : ''}`}>
            {paginatedData.map((row, rowIndex) => (
              <tr
                key={rowIndex}
                className={`
                  ${striped && rowIndex % 2 === 1 ? 'bg-gray-50 dark:bg-gray-800' : ''}
                  ${hover ? 'hover:bg-gray-100 dark:hover:bg-gray-700' : ''}
                  ${selectedRows.includes(rowIndex) ? 'bg-blue-50 dark:bg-blue-900' : ''}
                  ${onRowClick ? 'cursor-pointer' : ''}
                `}
                onClick={(e) => handleRowClick(row, rowIndex, e)}
                onDoubleClick={(e) => handleRowDoubleClick(row, rowIndex, e)}
              >
                {selectable && (
                  <td className="w-12 px-3 py-2">
                    <input
                      type="checkbox"
                      checked={selectedRows.includes(rowIndex)}
                      onChange={(e) => handleRowSelection(rowIndex, e.target.checked)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                  </td>
                )}
                
                {columns.map((column) => (
                  <td
                    key={column.key}
                    className={`px-3 py-2 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100
                               ${column.align === 'center' ? 'text-center' : column.align === 'right' ? 'text-right' : 'text-left'}`}
                  >
                    {renderCell(row[column.key], column, row, rowIndex)}
                  </td>
                ))}
                
                {rowActions.length > 0 && (
                  <td className="w-20 px-3 py-2 text-sm">
                    <div className="flex items-center space-x-2">
                      {rowActions.map((action, actionIndex) => (
                        <button
                          key={actionIndex}
                          onClick={(e) => {
                            e.stopPropagation();
                            action.onClick(row, rowIndex);
                          }}
                          className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-200"
                          title={action.label}
                        >
                          {action.icon || action.label}
                        </button>
                      ))}
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
        
        {paginatedData.length === 0 && (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400">
            {filterText ? 'No matching records found' : 'No data available'}
          </div>
        )}
      </div>
      
      {/* Pagination */}
      {paginated && showFooter && totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <div className="flex items-center space-x-2">
            <span className="text-sm text-gray-700 dark:text-gray-300">
              Showing {((currentPage - 1) * pageSize) + 1} to {Math.min(currentPage * pageSize, processedData.length)} of {processedData.length} results
            </span>
            
            <select
              value={pageSize}
              onChange={(e) => handlePageSizeChange(parseInt(e.target.value))}
              className="px-2 py-1 border border-gray-300 dark:border-gray-600 rounded text-sm
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                        dark:bg-gray-700 dark:text-white"
            >
              <option value={5}>5 per page</option>
              <option value={10}>10 per page</option>
              <option value={25}>25 per page</option>
              <option value={50}>50 per page</option>
              <option value={100}>100 per page</option>
            </select>
          </div>
          
          <div className="flex items-center space-x-2">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1}
              className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-sm
                        hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Previous
            </button>
            
            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              const pageNumber = i + 1;
              return (
                <button
                  key={pageNumber}
                  onClick={() => handlePageChange(pageNumber)}
                  className={`px-3 py-1 border rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
                             ${currentPage === pageNumber 
                               ? 'bg-blue-600 text-white border-blue-600' 
                               : 'border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'}`}
                >
                  {pageNumber}
                </button>
              );
            })}
            
            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-sm
                        hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

TableWidget.propTypes = {
  widget: PropTypes.shape({
    id: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    value: PropTypes.array,
    properties: PropTypes.object
  }).isRequired,
  sessionId: PropTypes.string.isRequired,
  onWidgetUpdate: PropTypes.func,
  optimizations: PropTypes.object
};

export default TableWidget;
