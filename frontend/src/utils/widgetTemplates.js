/**
 * Phase 5: Widget Templates and Presets System
 * Provides functionality to save, load, and manage widget configurations
 */

export const WIDGET_TEMPLATES = {
  // Data visualization widgets
  DATA_VISUALIZATION: {
    slider: {
      type: 'slider',
      properties: {
        label: 'Data Range',
        min: 0,
        max: 100,
        step: 1,
        orientation: 'horizontal',
        showValue: true
      },
      value: 50,
      category: 'data-viz',
      description: 'Standard data range slider'
    },
    dropdown: {
      type: 'dropdown',
      properties: {
        label: 'Chart Type',
        options: [
          { value: 'line', label: 'Line Chart' },
          { value: 'bar', label: 'Bar Chart' },
          { value: 'scatter', label: 'Scatter Plot' },
          { value: 'histogram', label: 'Histogram' }
        ],
        placeholder: 'Select chart type...',
        clearable: true,
        searchPlaceholder: 'Search chart types...'
      },
      value: 'line',
      category: 'data-viz',
      description: 'Chart type dropdown selector'
    },
    radio: {
      type: 'radio',
      properties: {
        label: 'Visualization Style',
        options: [
          { value: 'modern', label: 'Modern Style' },
          { value: 'classic', label: 'Classic Style' },
          { value: 'minimal', label: 'Minimal Style' }
        ],
        layout: 'vertical',
        required: false
      },
      value: 'modern',
      category: 'data-viz',
      description: 'Visualization style radio buttons'
    },
    multiselect: {
      type: 'multiselect',
      properties: {
        label: 'Data Columns',
        options: [
          { value: 'x_axis', label: 'X-Axis Data' },
          { value: 'y_axis', label: 'Y-Axis Data' },
          { value: 'color', label: 'Color Mapping' },
          { value: 'size', label: 'Size Mapping' },
          { value: 'category', label: 'Category Labels' }
        ],
        placeholder: 'Select data columns...',
        maxSelections: 3,
        showSelectAll: true,
        showClearAll: true,
        tagLimit: 2
      },
      value: ['x_axis', 'y_axis'],
      category: 'data-viz',
      description: 'Multiple data column selector'
    }
  },

  // Machine learning widgets
  MACHINE_LEARNING: {
    parameterSlider: {
      type: 'slider',
      properties: {
        label: 'Learning Rate',
        min: 0.0001,
        max: 1,
        step: 0.0001,
        orientation: 'horizontal',
        showValue: true
      },
      value: 0.01,
      category: 'ml',
      description: 'ML parameter slider'
    },
    epochSelector: {
      type: 'number',
      properties: {
        label: 'Epochs',
        min: 1,
        max: 1000,
        step: 1
      },
      value: 100,
      category: 'ml',
      description: 'Training epochs selector'
    },
    algorithmDropdown: {
      type: 'dropdown',
      properties: {
        label: 'Algorithm',
        options: [
          { value: 'svm', label: 'Support Vector Machine' },
          { value: 'rf', label: 'Random Forest' },
          { value: 'lr', label: 'Logistic Regression' },
          { value: 'nb', label: 'Naive Bayes' },
          { value: 'knn', label: 'K-Nearest Neighbors' }
        ],
        placeholder: 'Choose algorithm...',
        clearable: false
      },
      value: 'svm',
      category: 'ml',
      description: 'Machine learning algorithm selector'
    },
    optimizerRadio: {
      type: 'radio',
      properties: {
        label: 'Optimizer',
        options: [
          { value: 'adam', label: 'Adam' },
          { value: 'sgd', label: 'SGD' },
          { value: 'rmsprop', label: 'RMSprop' },
          { value: 'adagrad', label: 'Adagrad' }
        ],
        layout: 'horizontal',
        required: true
      },
      value: 'adam',
      category: 'ml',
      description: 'Optimizer selection via radio buttons'
    },
    featureMultiselect: {
      type: 'multiselect',
      properties: {
        label: 'Feature Selection',
        options: [
          { value: 'age', label: 'Age' },
          { value: 'income', label: 'Income Level' },
          { value: 'education', label: 'Education' },
          { value: 'experience', label: 'Work Experience' },
          { value: 'location', label: 'Geographic Location' },
          { value: 'skills', label: 'Technical Skills' }
        ],
        placeholder: 'Select features for training...',
        maxSelections: 4,
        showSelectAll: true,
        showSelectedCount: true
      },
      value: ['age', 'income', 'education'],
      category: 'ml',
      description: 'Multiple feature selection for model training'
    }
  },

  // UI control widgets
  UI_CONTROLS: {
    confirmButton: {
      type: 'button',
      properties: {
        label: 'Confirm',
        variant: 'primary',
        disabled: false
      },
      value: false,
      category: 'ui',
      description: 'Confirmation button'
    },
    toggleSwitch: {
      type: 'checkbox',
      properties: {
        label: 'Enable Feature',
        disabled: false
      },
      value: false,
      category: 'ui',
      description: 'Feature toggle switch'
    },
    priorityDropdown: {
      type: 'dropdown',
      properties: {
        label: 'Priority Level',
        options: [
          { value: 'low', label: 'Low Priority' },
          { value: 'medium', label: 'Medium Priority' },
          { value: 'high', label: 'High Priority' },
          { value: 'urgent', label: 'Urgent' }
        ],
        placeholder: 'Select priority...',
        clearable: false
      },
      value: 'medium',
      category: 'ui',
      description: 'Priority level dropdown'
    },
    statusRadio: {
      type: 'radio',
      properties: {
        label: 'Status',
        options: [
          { value: 'active', label: 'Active' },
          { value: 'inactive', label: 'Inactive' },
          { value: 'pending', label: 'Pending' }
        ],
        layout: 'horizontal'
      },
      value: 'active',
      category: 'ui',
      description: 'Status radio button group'
    },
    permissionsMultiselect: {
      type: 'multiselect',
      properties: {
        label: 'User Permissions',
        options: [
          { value: 'read', label: 'Read Access' },
          { value: 'write', label: 'Write Access' },
          { value: 'delete', label: 'Delete Access' },
          { value: 'admin', label: 'Admin Access' },
          { value: 'share', label: 'Share Access' }
        ],
        placeholder: 'Select permissions...',
        showSelectAll: false,
        showClearAll: true,
        maxSelections: null
      },
      value: ['read'],
      category: 'ui',
      description: 'Multiple permissions selector'
    }
  }
};

/**
 * Widget Template Manager
 */
export class WidgetTemplateManager {
  constructor() {
    this.customTemplates = new Map();
    this.loadCustomTemplates();
  }

  /**
   * Save a widget configuration as a template
   */
  saveTemplate(templateId, widget, metadata = {}) {
    const template = {
      id: templateId,
      type: widget.type,
      properties: { ...widget.properties },
      value: widget.value,
      category: metadata.category || 'custom',
      description: metadata.description || '',
      createdAt: new Date().toISOString(),
      version: metadata.version || '1.0.0'
    };

    this.customTemplates.set(templateId, template);
    this.saveCustomTemplates();
    
    console.log(`Template saved: ${templateId}`);
    return template;
  }

  /**
   * Load a widget template
   */
  loadTemplate(templateId) {
    // Check custom templates first
    if (this.customTemplates.has(templateId)) {
      return this.customTemplates.get(templateId);
    }

    // Check built-in templates
    for (const category of Object.values(WIDGET_TEMPLATES)) {
      if (category[templateId]) {
        return { id: templateId, ...category[templateId] };
      }
    }

    return null;
  }

  /**
   * Get all available templates
   */
  getAllTemplates() {
    const templates = new Map();
    
    // Add built-in templates
    for (const [categoryName, category] of Object.entries(WIDGET_TEMPLATES)) {
      for (const [templateId, template] of Object.entries(category)) {
        templates.set(templateId, {
          id: templateId,
          ...template,
          builtIn: true
        });
      }
    }
    
    // Add custom templates
    for (const [templateId, template] of this.customTemplates) {
      templates.set(templateId, {
        ...template,
        builtIn: false
      });
    }
    
    return templates;
  }

  /**
   * Delete a custom template
   */
  deleteTemplate(templateId) {
    if (this.customTemplates.has(templateId)) {
      this.customTemplates.delete(templateId);
      this.saveCustomTemplates();
      console.log(`Template deleted: ${templateId}`);
      return true;
    }
    return false;
  }

  /**
   * Get templates by category
   */
  getTemplatesByCategory(category) {
    const templates = new Map();
    
    for (const [templateId, template] of this.getAllTemplates()) {
      if (template.category === category) {
        templates.set(templateId, template);
      }
    }
    
    return templates;
  }

  /**
   * Create widget from template
   */
  createWidgetFromTemplate(templateId, overrides = {}) {
    const template = this.loadTemplate(templateId);
    if (!template) {
      throw new Error(`Template not found: ${templateId}`);
    }

    return {
      id: overrides.id || `widget_${Date.now()}`,
      type: template.type,
      properties: { ...template.properties, ...overrides.properties },
      value: overrides.value !== undefined ? overrides.value : template.value,
      templateId: templateId,
      createdAt: new Date().toISOString()
    };
  }

  /**
   * Save custom templates to localStorage
   */
  saveCustomTemplates() {
    try {
      const templates = Object.fromEntries(this.customTemplates);
      localStorage.setItem('marimo_widget_templates', JSON.stringify(templates));
    } catch (error) {
      console.error('Failed to save custom templates:', error);
    }
  }

  /**
   * Load custom templates from localStorage
   */
  loadCustomTemplates() {
    try {
      const saved = localStorage.getItem('marimo_widget_templates');
      if (saved) {
        const templates = JSON.parse(saved);
        this.customTemplates = new Map(Object.entries(templates));
      }
    } catch (error) {
      console.error('Failed to load custom templates:', error);
      this.customTemplates = new Map();
    }
  }

  /**
   * Export templates as JSON
   */
  exportTemplates() {
    return {
      customTemplates: Object.fromEntries(this.customTemplates),
      builtInTemplates: WIDGET_TEMPLATES,
      exportedAt: new Date().toISOString()
    };
  }

  /**
   * Import templates from JSON
   */
  importTemplates(data) {
    try {
      if (data.customTemplates) {
        for (const [templateId, template] of Object.entries(data.customTemplates)) {
          this.customTemplates.set(templateId, template);
        }
        this.saveCustomTemplates();
      }
      return true;
    } catch (error) {
      console.error('Failed to import templates:', error);
      return false;
    }
  }
}

// Global template manager instance
export const widgetTemplateManager = new WidgetTemplateManager();
