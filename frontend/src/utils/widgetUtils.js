/**
 * Phase 5: Utility functions for widget management
 */

/**
 * Throttle function to limit how often a function can be called
 */
export const throttle = (func, limit) => {
  let inThrottle;
  return function(...args) {
    const context = this;
    if (!inThrottle) {
      func.apply(context, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
};

/**
 * Debounce function to delay execution until after a certain time has passed
 */
export const debounce = (func, wait) => {
  let timeout;
  return function(...args) {
    const context = this;
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(context, args), wait);
  };
};

/**
 * Validate widget value against constraints
 */
export const validateValue = (constraints, value) => {
  if (!constraints || !constraints.type) {
    return { isValid: true };
  }

  const { type } = constraints;

  try {
    switch (type) {
      case 'slider':
        if (typeof value !== 'number') {
          return { isValid: false, error: 'Slider value must be numeric' };
        }
        if (constraints.min !== undefined && value < constraints.min) {
          return { isValid: false, error: `Value ${value} is below minimum ${constraints.min}` };
        }
        if (constraints.max !== undefined && value > constraints.max) {
          return { isValid: false, error: `Value ${value} is above maximum ${constraints.max}` };
        }
        if (constraints.step !== undefined && constraints.step > 0) {
          const min = constraints.min || 0;
          if ((value - min) % constraints.step !== 0) {
            return { isValid: false, error: `Value ${value} is not a valid step increment` };
          }
        }
        break;

      case 'text':
        if (typeof value !== 'string') {
          return { isValid: false, error: 'Text value must be a string' };
        }
        if (constraints.maxLength !== undefined && value.length > constraints.maxLength) {
          return { isValid: false, error: `Text length ${value.length} exceeds maximum ${constraints.maxLength}` };
        }
        break;

      case 'select':
        if (constraints.options && constraints.options.length > 0) {
          const validValues = constraints.options.map(opt => 
            typeof opt === 'object' ? opt.value : opt
          );
          if (!validValues.includes(value)) {
            return { isValid: false, error: `Value ${value} is not in valid options` };
          }
        }
        break;

      case 'checkbox':
        if (typeof value !== 'boolean') {
          return { isValid: false, error: 'Checkbox value must be boolean' };
        }
        break;

      case 'number':
        if (typeof value !== 'number') {
          return { isValid: false, error: 'Number value must be numeric' };
        }
        if (constraints.min !== undefined && value < constraints.min) {
          return { isValid: false, error: `Value ${value} is below minimum ${constraints.min}` };
        }
        if (constraints.max !== undefined && value > constraints.max) {
          return { isValid: false, error: `Value ${value} is above maximum ${constraints.max}` };
        }
        break;

      default:
        return { isValid: true };
    }

    return { isValid: true };
  } catch (error) {
    return { isValid: false, error: `Validation error: ${error.message}` };
  }
};

/**
 * Auto-fix widget value to conform to constraints
 */
export const autoFixValue = (constraints, value) => {
  if (!constraints || !constraints.type) {
    return value;
  }

  const { type } = constraints;

  try {
    switch (type) {
      case 'slider':
        // Ensure numeric
        if (typeof value !== 'number') {
          try {
            value = Number(value);
          } catch (e) {
            value = constraints.min || 0;
          }
        }
        
        // Clamp to bounds
        if (constraints.min !== undefined) {
          value = Math.max(value, constraints.min);
        }
        if (constraints.max !== undefined) {
          value = Math.min(value, constraints.max);
        }
        
        // Snap to step
        if (constraints.step !== undefined && constraints.step > 0) {
          const min = constraints.min || 0;
          value = min + Math.round((value - min) / constraints.step) * constraints.step;
        }
        break;

      case 'text':
        // Ensure string
        if (typeof value !== 'string') {
          value = String(value);
        }
        
        // Truncate if too long
        if (constraints.maxLength !== undefined) {
          value = value.substring(0, constraints.maxLength);
        }
        break;

      case 'select':
        // Ensure valid option
        if (constraints.options && constraints.options.length > 0) {
          const validValues = constraints.options.map(opt => 
            typeof opt === 'object' ? opt.value : opt
          );
          if (!validValues.includes(value)) {
            value = validValues[0]; // Default to first option
          }
        }
        break;

      case 'checkbox':
        // Ensure boolean
        if (typeof value !== 'boolean') {
          value = Boolean(value);
        }
        break;

      case 'number':
        // Ensure numeric
        if (typeof value !== 'number') {
          try {
            value = Number(value);
          } catch (e) {
            value = constraints.min || 0;
          }
        }
        
        // Clamp to bounds
        if (constraints.min !== undefined) {
          value = Math.max(value, constraints.min);
        }
        if (constraints.max !== undefined) {
          value = Math.min(value, constraints.max);
        }
        break;

      default:
        // No changes for unknown types
        break;
    }

    return value;
  } catch (error) {
    console.error('Auto-fix failed:', error);
    return value;
  }
};

/**
 * Extract constraints from widget type and properties
 */
export const extractConstraints = (widgetType, properties) => {
  const constraints = { type: widgetType };

  switch (widgetType) {
    case 'slider':
      if (properties.min !== undefined) constraints.min = properties.min;
      if (properties.max !== undefined) constraints.max = properties.max;
      if (properties.step !== undefined) constraints.step = properties.step;
      break;

    case 'text':
      if (properties.maxLength !== undefined) constraints.maxLength = properties.maxLength;
      break;

    case 'select':
      if (properties.options) constraints.options = properties.options;
      break;

    case 'number':
      if (properties.min !== undefined) constraints.min = properties.min;
      if (properties.max !== undefined) constraints.max = properties.max;
      if (properties.step !== undefined) constraints.step = properties.step;
      break;

    default:
      break;
  }

  return constraints;
};

/**
 * Auto-fix widget value based on constraints
 */
export const autoFixWidgetValue = (constraints, value) => {
  return autoFixValue(constraints, value);
};

/**
 * Generate a unique ID for widgets
 */
export const generateWidgetId = (prefix = 'widget') => {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Format timestamp for display
 */
export const formatTimestamp = (timestamp) => {
  return new Date(timestamp).toLocaleString();
};

/**
 * Deep clone an object
 */
export const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') {
    return obj;
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime());
  }
  
  if (Array.isArray(obj)) {
    return obj.map(item => deepClone(item));
  }
  
  if (typeof obj === 'object') {
    const cloned = {};
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        cloned[key] = deepClone(obj[key]);
      }
    }
    return cloned;
  }
  
  return obj;
};
