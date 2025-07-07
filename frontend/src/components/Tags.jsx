import { useState,useEffect } from "react";

const tagColors = [
  'bg-blue-500',
  'bg-green-500',
  'bg-red-500',
  'bg-purple-500',
  'bg-yellow-500',
  'bg-pink-500',
  'bg-indigo-500',
  'bg-teal-500',
  'bg-orange-500',
  'bg-cyan-500',
];

const getTagColor = (index) => {
  return tagColors[index % tagColors.length];
};

export const Tag = ({ children, index = 0, className = '' }) => {
  const color = getTagColor(index);
  
  return (
    <span 
      className={`${color} text-white text-xs font-medium px-2.5 py-0.5 rounded-full ${className}`}
    >
      {children}
    </span>
  );
};

export const TagsInput = ({ tags = [], availableTags = [], onAddTag, onRemoveTag }) => {
  const [inputValue, setInputValue] = useState('');
  const [filteredTags, setFilteredTags] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    if (inputValue.trim() === '') {
      setFilteredTags(availableTags.filter(tag => !tags.includes(tag)));
    } else {
      setFilteredTags(
        availableTags.filter(tag => 
          tag.toLowerCase().includes(inputValue.toLowerCase()) && 
          !tags.includes(tag))
      );
    }
  }, [inputValue, tags, availableTags]);

  const handleAddTag = (tag) => {
    onAddTag(tag);
    setInputValue('');
    setShowDropdown(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && inputValue.trim() && !tags.includes(inputValue.trim())) {
      handleAddTag(inputValue.trim());
    }
  };

  return (
    <div className="relative">
      <div className="flex flex-wrap gap-2 mb-2">
        {tags.map((tag, index) => (
          <div key={tag} className="flex items-center">
            <Tag index={index}>{tag}</Tag>
            <button 
              type="button"
              onClick={() => onRemoveTag(tag)}
              className="ml-1 text-gray-400 hover:text-gray-600"
            >
              ×
            </button>
          </div>
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
        className="text-sm border border-gray-300 rounded-md px-3 py-2 w-full"
      />
      
      {showDropdown && filteredTags.length > 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white dark:bg-gray-800 shadow-lg rounded-md border border-gray-200 dark:border-gray-700 max-h-60 overflow-y-auto">
          {filteredTags.map((tag) => (
            <div
              key={tag}
              className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
              onMouseDown={() => handleAddTag(tag)}
            >
              {tag}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export const TagsList = ({ tags }) => {
  if (!tags || tags.length === 0) {
    return <span className="text-gray-500 text-sm">No tags</span>;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {tags.map((tag, index) => (
        <Tag key={tag} index={index}>
          {tag}
        </Tag>
      ))}
    </div>
  );
};