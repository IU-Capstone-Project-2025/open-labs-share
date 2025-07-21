import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useWidgetState } from '../../contexts/WidgetStateContext';
import { debounce, validateValue } from '../../utils/widgetUtils';

/**
 * Multiselect Widget
 * Handles multiple selection from predefined options with search and tagging
 */
const MultiselectWidget = ({ widget, sessionId, onWidgetUpdate, optimizations = {} }) => {
  const { updateWidgetValue, getWidget } = useWidgetState();
  const [localValue, setLocalValue] = useState(widget.value || []);
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredOptions, setFilteredOptions] = useState([]);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const dropdownRef = useRef(null);
  const searchInputRef = useRef(null);
  const mountedRef = useRef(true);
  
  const {
    enableSearch = true,
    enableAnalytics = false
  } = optimizations;

  // Get current widget state
  const widgetState = getWidget(widget.id);
  const isLoading = widgetState?.isLoading || false;
  const error = widgetState?.error || null;
  const constraints = widgetState?.constraints || null;

  // Extract properties
  const { 
    label = 'Multiselect',
    options = [],
    placeholder = 'Select options...',
    searchPlaceholder = 'Search options...',
    disabled = false,
    maxSelections = null,
    showSelectAll = true,
    showClearAll = true,
    maxHeight = 200,
    tagLimit = 3,
    showSelectedCount = true
  } = widget.properties || {};

  // Normalize options to handle both string arrays and object arrays
  const normalizedOptions = React.useMemo(() => {
    if (!Array.isArray(options)) {
      console.warn(`Multiselect widget ${widget.id}: options is not an array`, options);
      return [];
    }
    
    return options.map(option => {
      if (typeof option === 'string') {
        return { label: option, value: option, disabled: false };
      } else if (typeof option === 'object' && option !== null) {
        return {
          label: option.label || option.value || String(option),
          value: option.value !== undefined ? option.value : option.label || String(option),
          disabled: option.disabled || false
        };
      } else {
        return { label: String(option), value: option, disabled: false };
      }
    });
  }, [options, widget.id]);

  // Sync local value with widget state
  useEffect(() => {
    if (widgetState && widgetState.value !== localValue) {
      setLocalValue(widgetState.value || []);
    }
  }, [widgetState?.value, localValue]);

  // Filter options based on search term
  useEffect(() => {
    if (!searchTerm) {
      setFilteredOptions(normalizedOptions);
    } else {
      const filtered = normalizedOptions.filter(option => 
        option.label.toLowerCase().includes(searchTerm.toLowerCase()) ||
        option.value.toString().toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredOptions(filtered);
    }
    setHighlightedIndex(-1);
  }, [searchTerm, normalizedOptions]);

  // Handle clicks outside dropdown
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
        setSearchTerm('');
        setHighlightedIndex(-1);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Handle value changes with validation
  const handleValueChange = useCallback((newValue) => {
    if (!mountedRef.current) return;

    // Validate value
    if (constraints) {
      const validationResult = validateValue(constraints, newValue);
      if (!validationResult.isValid) {
        console.warn(`Invalid multiselect value for widget ${widget.id}:`, validationResult.error);
        return;
      }
    }

    // Check max selections
    if (maxSelections && newValue.length > maxSelections) {
      console.warn(`Cannot select more than ${maxSelections} options`);
      return;
    }

    setLocalValue(newValue);
    updateWidgetValue(widget.id, newValue, 100);
    
    if (onWidgetUpdate) {
      onWidgetUpdate(widget.id, newValue);
    }
  }, [widget.id, updateWidgetValue, onWidgetUpdate, constraints, maxSelections]);

  // Handle option selection/deselection
  const handleOptionToggle = useCallback((option) => {
    if (option.disabled || disabled || isLoading) return;
    
    const isSelected = localValue.includes(option.value);
    
    let newValue;
    if (isSelected) {
      // Remove from selection
      newValue = localValue.filter(val => val !== option.value);
    } else {
      // Add to selection
      if (maxSelections && localValue.length >= maxSelections) {
        console.warn(`Cannot select more than ${maxSelections} options`);
        return;
      }
      newValue = [...localValue, option.value];
    }
    
    handleValueChange(newValue);
    
    if (enableAnalytics) {
      console.log(`Multiselect widget ${widget.id} ${isSelected ? 'deselected' : 'selected'}:`, option.value);
    }
  }, [localValue, disabled, isLoading, maxSelections, handleValueChange, widget.id, enableAnalytics]);

  // Handle select all
  const handleSelectAll = useCallback(() => {
    const availableOptions = normalizedOptions.filter(option => !option.disabled);
    const allValues = availableOptions.map(option => option.value);
    
    if (maxSelections && allValues.length > maxSelections) {
      const limitedValues = allValues.slice(0, maxSelections);
      handleValueChange(limitedValues);
    } else {
      handleValueChange(allValues);
    }
    
    if (enableAnalytics) {
      console.log(`Multiselect widget ${widget.id} select all`);
    }
  }, [options, maxSelections, handleValueChange, widget.id, enableAnalytics]);

  // Handle clear all
  const handleClearAll = useCallback(() => {
    handleValueChange([]);
    
    if (enableAnalytics) {
      console.log(`Multiselect widget ${widget.id} clear all`);
    }
  }, [handleValueChange, widget.id, enableAnalytics]);

  // Handle remove tag
  const handleRemoveTag = useCallback((valueToRemove, e) => {
    e.stopPropagation();
    const newValue = localValue.filter(val => val !== valueToRemove);
    handleValueChange(newValue);
    
    if (enableAnalytics) {
      console.log(`Multiselect widget ${widget.id} removed tag:`, valueToRemove);
    }
  }, [localValue, handleValueChange, widget.id, enableAnalytics]);

  // Handle dropdown toggle
  const handleToggle = useCallback(() => {
    if (disabled || isLoading) return;
    
    setIsOpen(!isOpen);
    setSearchTerm('');
    setHighlightedIndex(-1);
    
    // Focus search input when opening
    if (!isOpen && enableSearch) {
      setTimeout(() => {
        if (searchInputRef.current) {
          searchInputRef.current.focus();
        }
      }, 50);
    }
  }, [disabled, isLoading, isOpen, enableSearch]);

  // Handle keyboard navigation
  const handleKeyDown = useCallback((e) => {
    if (!isOpen) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleToggle();
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex(prev => 
          prev < filteredOptions.length - 1 ? prev + 1 : 0
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex(prev => 
          prev > 0 ? prev - 1 : filteredOptions.length - 1
        );
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < filteredOptions.length) {
          handleOptionToggle(filteredOptions[highlightedIndex]);
        }
        break;
      case 'Escape':
        setIsOpen(false);
        setSearchTerm('');
        setHighlightedIndex(-1);
        break;
    }
  }, [isOpen, filteredOptions, highlightedIndex, handleToggle, handleOptionToggle]);

  // Handle search input change
  const handleSearchChange = useCallback((e) => {
    setSearchTerm(e.target.value);
  }, []);

  // Get selected options
  const selectedOptions = normalizedOptions.filter(option => localValue.includes(option.value));
  const hasSelections = selectedOptions.length > 0;
  const displayedTags = selectedOptions.slice(0, tagLimit);
  const remainingCount = selectedOptions.length - tagLimit;

  if (error) {
    return (
      <div className="widget-error p-4 border border-red-300 rounded-lg bg-red-50">
        <p className="text-red-700">Multiselect Error: {error}</p>
        <p className="text-sm text-red-600">Widget ID: {widget.id}</p>
      </div>
    );
  }

  return (
    <div className="widget-multiselect relative" ref={dropdownRef}>
      {isLoading && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center z-10">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
        </div>
      )}
      
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        {label}
        {showSelectedCount && hasSelections && (
          <span className="ml-2 text-xs text-blue-600 font-semibold">
            ({selectedOptions.length} selected)
          </span>
        )}
        {maxSelections && (
          <span className="ml-2 text-xs text-gray-500">
            (max {maxSelections})
          </span>
        )}
      </label>
      
      <div className="relative">
        <button
          onClick={handleToggle}
          onKeyDown={handleKeyDown}
          disabled={disabled || isLoading}
          className={`w-full min-h-[2.5rem] px-3 py-2 text-left border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                     ${disabled || isLoading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                     ${isOpen ? 'border-blue-500 ring-2 ring-blue-500 ring-opacity-50' : 'border-gray-300 dark:border-gray-600'}
                     dark:bg-gray-700 dark:text-white hover:bg-gray-50 dark:hover:bg-gray-600`}
        >
          <div className="flex items-center justify-between">
            <div className="flex-1 min-w-0">
              {hasSelections ? (
                <div className="flex flex-wrap gap-1">
                  {displayedTags.map((option) => (
                    <span
                      key={option.value}
                      className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                    >
                      {option.label}
                      <button
                        onClick={(e) => handleRemoveTag(option.value, e)}
                        className="ml-1 hover:bg-blue-200 dark:hover:bg-blue-800 rounded-full p-0.5"
                        title={`Remove ${option.label}`}
                      >
                        <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                        </svg>
                      </button>
                    </span>
                  ))}
                  {remainingCount > 0 && (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200">
                      +{remainingCount} more
                    </span>
                  )}
                </div>
              ) : (
                <span className="text-gray-500 dark:text-gray-400">
                  {placeholder}
                </span>
              )}
            </div>
            
            <div className="flex items-center space-x-2 ml-2">
              {showClearAll && hasSelections && (
                <button
                  onClick={(e) => { e.stopPropagation(); handleClearAll(); }}
                  className="p-1 hover:bg-gray-200 dark:hover:bg-gray-600 rounded"
                  title="Clear all selections"
                >
                  <svg className="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </button>
              )}
              
              <svg 
                className={`w-4 h-4 text-gray-400 transform transition-transform ${isOpen ? 'rotate-180' : ''}`} 
                fill="currentColor" 
                viewBox="0 0 20 20"
              >
                <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </div>
          </div>
        </button>
        
        {isOpen && (
          <div className="absolute z-20 w-full mt-1 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg shadow-lg">
            {enableSearch && (
              <div className="p-2 border-b border-gray-200 dark:border-gray-600">
                <input
                  ref={searchInputRef}
                  type="text"
                  value={searchTerm}
                  onChange={handleSearchChange}
                  placeholder={searchPlaceholder}
                  className="w-full px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded 
                            focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500
                            dark:bg-gray-600 dark:text-white"
                />
              </div>
            )}
            
            {(showSelectAll || showClearAll) && (
              <div className="p-2 border-b border-gray-200 dark:border-gray-600 flex justify-between">
                {showSelectAll && (
                  <button
                    onClick={handleSelectAll}
                    disabled={maxSelections && selectedOptions.length >= maxSelections}
                    className="text-sm text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-200 
                              disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Select All
                  </button>
                )}
                {showClearAll && hasSelections && (
                  <button
                    onClick={handleClearAll}
                    className="text-sm text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-200"
                  >
                    Clear All
                  </button>
                )}
              </div>
            )}
            
            <div className="max-h-48 overflow-y-auto" style={{ maxHeight: `${maxHeight}px` }}>
              {filteredOptions.length === 0 ? (
                <div className="px-3 py-2 text-sm text-gray-500 dark:text-gray-400">
                  {searchTerm ? 'No options match your search' : 'No options available'}
                </div>
              ) : (
                filteredOptions.map((option, index) => {
                  const isSelected = localValue.includes(option.value);
                  const isHighlighted = index === highlightedIndex;
                  
                  return (
                    <button
                      key={option.value}
                      onClick={() => handleOptionToggle(option)}
                      className={`w-full px-3 py-2 text-left text-sm hover:bg-gray-100 dark:hover:bg-gray-600 
                                 ${isHighlighted ? 'bg-blue-100 dark:bg-blue-900' : ''}
                                 ${isSelected ? 'bg-blue-50 dark:bg-blue-800' : ''}
                                 ${option.disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                      disabled={option.disabled}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2">
                          <div className={`w-4 h-4 border-2 rounded flex items-center justify-center
                                          ${isSelected ? 'bg-blue-600 border-blue-600' : 'border-gray-300 dark:border-gray-600'}`}>
                            {isSelected && (
                              <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                              </svg>
                            )}
                          </div>
                          <span className="dark:text-white">{option.label}</span>
                        </div>
                      </div>
                      {option.description && (
                        <div className="text-xs text-gray-500 dark:text-gray-400 mt-1 ml-6">
                          {option.description}
                        </div>
                      )}
                    </button>
                  );
                })
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

MultiselectWidget.propTypes = {
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

export default MultiselectWidget;
