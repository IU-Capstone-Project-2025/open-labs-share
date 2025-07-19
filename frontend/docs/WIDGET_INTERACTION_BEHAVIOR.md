# Widget Interaction Behavior

## Overview

The widget system now implements a "commit on interaction end" approach for better performance and user experience. This means that components that depend on widget values are updated only when the user finishes interacting with a widget, rather than continuously during the interaction.

## How It Works

### Before (Real-time Updates)
- User drags slider → Value updates immediately → All dependent components re-render → Backend API called
- This happened continuously while dragging, causing:
  - Performance issues with many dependent components
  - Visual flickering
  - Excessive API calls
  - Poor user experience on slow connections

### After (Commit on Interaction End)
- User starts dragging slider → UI updates immediately (for responsiveness)
- User continues dragging → Only local UI updates, no backend calls
- User releases slider → Final value committed to backend → Dependent components update
- This provides:
  - Better performance
  - Smoother interactions
  - Reduced API calls
  - Better user experience

## Implementation Details

### Widget State Context
The `WidgetStateContext` now provides:

- `updateWidgetValueImmediate(widgetId, value)` - Updates only local UI state
- `commitWidgetValue(widgetId)` - Commits current value to backend
- `startWidgetInteraction(widgetId)` - Marks widget as being interacted with
- `endWidgetInteraction(widgetId)` - Marks interaction as ended
- `interactiveWidgets` - Set of widget IDs currently being interacted with

### Widget Components
Widgets like `SliderWidget` and `RangeSliderWidget` now:

1. Track interaction state with `isInteracting`
2. Use mouse/touch/keyboard events to detect interaction start/end
3. Update local UI immediately during interaction
4. Commit final value when interaction ends
5. Show visual feedback during interaction (e.g., "• interacting" indicator)

### Supported Widgets
Currently implemented for:
- ✅ SliderWidget - Single-value slider
- ✅ RangeSliderWidget - Dual-thumb range slider
- 🔄 NumberWidget - Text input for numbers (planned)
- 🔄 TextAreaWidget - Text input (planned)

### Event Handling

#### Mouse/Touch Events
```javascript
onMouseDown={handleInteractionStart}
onMouseUp={handleInteractionEnd}
onTouchStart={handleInteractionStart}
onTouchEnd={handleInteractionEnd}
```

#### Keyboard Events
```javascript
onKeyDown={(e) => {
  if (isArrowKey(e.key) && !isInteracting) {
    handleInteractionStart();
  }
}}
onKeyUp={(e) => {
  if (isArrowKey(e.key)) {
    setTimeout(handleInteractionEnd, 100);
  }
}}
```

## Benefits

1. **Performance**: Reduced API calls and component re-renders
2. **User Experience**: Smoother interactions without lag
3. **Network Efficiency**: Fewer requests to backend services
4. **Visual Feedback**: Clear indication when widgets are being modified
5. **Debouncing**: Natural debouncing through interaction patterns

## Migration Guide

### For Widget Developers
If creating new widgets that should use this pattern:

1. Import the new functions from `useWidgetState`:
```javascript
const { 
  updateWidgetValueImmediate, 
  commitWidgetValue, 
  startWidgetInteraction, 
  endWidgetInteraction 
} = useWidgetState();
```

2. Track interaction state:
```javascript
const [isInteracting, setIsInteracting] = useState(false);
```

3. Handle interaction events:
```javascript
const handleInteractionStart = () => {
  setIsInteracting(true);
  startWidgetInteraction(widget.id);
};

const handleInteractionEnd = async () => {
  setIsInteracting(false);
  endWidgetInteraction(widget.id);
  await commitWidgetValue(widget.id);
};
```

4. Update values appropriately:
```javascript
const handleValueChange = (newValue) => {
  setLocalValue(newValue);
  if (isInteracting) {
    updateWidgetValueImmediate(widget.id, newValue);
  } else {
    commitWidgetValue(widget.id);
  }
};
```

### For Component Developers
Components that depend on widget values will automatically benefit from this change with no modifications needed. They will simply receive updates less frequently but with the final, committed values.

## Configuration

The behavior can be controlled through widget options:

```javascript
updateWidgetValue(widgetId, value, debounceMs, { 
  commitImmediately: true  // Force immediate commit even during interaction
});
```

## Future Enhancements

- Configurable commit strategies (time-based, distance-based, etc.)
- Undo/redo support for widget interactions
- Batch commit for multiple widget interactions
- Offline interaction queuing
