import { useState, useEffect } from "react";

export const Tag = ({ children, index = 0, className = '', onRemove }) => {
  const colors = [
    'bg-blue-500', 'bg-green-500', 'bg-red-500',
    'bg-purple-500', 'bg-yellow-500', 'bg-pink-500',
    'bg-indigo-500', 'bg-teal-500', 'bg-orange-500'
  ];
  
  const color = colors[index % colors.length];

  return (
    <span 
      className={`${color} text-white text-xs font-medium px-2.5 py-0.5 rounded-full ${className}`}
    >
      {children}
      {onRemove && (
        <button 
          onClick={onRemove}
          className="ml-1.5 text-white hover:text-gray-200 focus:outline-none"
        >
          ×
        </button>
      )}
    </span>
  );
};

export const TagsInput = ({ 
  availableTags = [], 
  selectedTags = [], 
  onChange, 
  onCreateTag 
}) => {
  const [inputValue, setInputValue] = useState('');
  const [filteredTags, setFilteredTags] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    if (inputValue.trim() === '') {
      setFilteredTags(
        availableTags.filter(tag => 
          !selectedTags.some(selected => selected.id === tag.id)
        )
      );
    } else {
      setFilteredTags(
        availableTags.filter(tag => 
          tag.name.toLowerCase().includes(inputValue.toLowerCase()) &&
          !selectedTags.some(selected => selected.id === tag.id)
        )
      );
    }
  }, [inputValue, availableTags, selectedTags]);

  const handleAddTag = (tag) => {
    onChange([...selectedTags, tag]);
    setInputValue('');
    setShowDropdown(false);
  };

  const handleCreateTag = async () => {
    if (!inputValue.trim()) return;
    
    try {
      const newTag = await onCreateTag(inputValue.trim());
      handleAddTag(newTag);
    } catch (err) {
      console.error('Error creating tag:', err);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (filteredTags.length > 0) {
        handleAddTag(filteredTags[0]);
      } else if (inputValue.trim() && onCreateTag) {
        handleCreateTag();
      }
    }
  };

  return (
    <div className="relative">
      <div className="flex flex-wrap gap-2 mb-2">
        {selectedTags.map((tag, index) => (
          <Tag 
            key={tag.id} 
            index={index}
            onRemove={() => onChange(selectedTags.filter(t => t.id !== tag.id))}
          >
            {tag.name}
          </Tag>
        ))}
      </div>
      
      <input
        type="text"
        value={inputValue}
        onChange={(e) => setInputValue(e.target.value)}
        onFocus={() => setShowDropdown(true)}
        onBlur={() => setTimeout(() => setShowDropdown(false), 200)}
        onKeyDown={handleKeyDown}
        placeholder="Add tags..."
        className="text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 w-full bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400"
      />
      
      {showDropdown && (filteredTags.length > 0 || (inputValue.trim() && onCreateTag)) && (
        <div className="absolute z-10 mt-1 w-full bg-white dark:bg-gray-800 shadow-lg rounded-md border border-gray-200 dark:border-gray-700 max-h-60 overflow-y-auto">
          {filteredTags.map(tag => (
            <div
              key={tag.id}
              className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
              onMouseDown={() => handleAddTag(tag)}
            >
              {tag.name}
            </div>
          ))}
          {inputValue.trim() && onCreateTag && !availableTags.some(tag => 
            tag.name.toLowerCase() === inputValue.toLowerCase()
          ) && (
            <div
              className="px-4 py-2 text-sm text-blue-600 dark:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
              onMouseDown={handleCreateTag}
            >
              Create new tag: "{inputValue}"
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export const TagsList = ({ tags = [], onTagClick }) => {
  if (tags.length === 0) {
    return <span className="text-gray-500 text-sm">No tags</span>;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {tags.map((tag, index) => (
        <Tag 
          key={tag.id} 
          index={index}
          className={onTagClick ? 'cursor-pointer hover:opacity-80' : ''}
          onClick={() => onTagClick?.(tag)}
        >
          {tag.name}
        </Tag>
      ))}
    </div>
  );
};