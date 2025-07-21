import React from 'react';
import MarimoEditor from './MarimoEditor';
import AssetUploader from './AssetUploader';
import { TrashIcon } from '@heroicons/react/24/outline';
import { Tooltip } from './Tooltip';

const MarimoCreator = ({ index, componentData, onComponentChange, onRemove }) => {

  const handleNameChange = (e) => {
    onComponentChange(index, { ...componentData, name: e.target.value });
  };

  const handleCodeChange = (newCode) => {
    onComponentChange(index, { ...componentData, code: newCode });
  };
  
  const handleAssetUpload = (files) => {
    onComponentChange(index, { ...componentData, assets: files });
  };

  return (
    <div className="bg-white dark:bg-gray-800/50 p-6 border border-gray-200 dark:border-gray-700 rounded-2xl shadow-lg mt-6 relative transition-all hover:shadow-xl">
      <button 
        onClick={onRemove}
        className="absolute top-4 right-4 text-gray-400 hover:text-red-500 transition-colors z-10"
        aria-label="Remove component"
      >
        <Tooltip content="Remove Component">
          <TrashIcon className="h-6 w-6" />
        </Tooltip>
      </button>
      <div className="mb-6">
        <label htmlFor={`marimo-name-${index}`} className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          Component Name
        </label>
        <input
          type="text"
          id={`marimo-name-${index}`}
          value={componentData.name || ''}
          onChange={handleNameChange}
          placeholder="e.g., Pythagorean Theorem Calculator"
          className="w-full px-4 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
        />
      </div>
      
      <div className="mb-6">
        <label htmlFor={`marimo-code-${index}`} className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          Python Code (Marimo)
        </label>
        <MarimoEditor 
          code={componentData.code || ''} 
          setCode={handleCodeChange} 
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          Component-Specific Assets
        </label>
        <AssetUploader onAssetUpload={handleAssetUpload} />
      </div>
    </div>
  );
};

export default MarimoCreator; 