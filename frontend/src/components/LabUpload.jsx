import { useState, useEffect } from 'react';
import { labsAPI, tagsAPI } from '../utils/api';
import { getCurrentUser } from '../utils/auth';
import { TagsInput } from "../components/Tags";
import { Tooltip } from '../components/Tooltip';

export default function LabUpload({ onSuccess, onCancel, isModal = true }) {
  const [labData, setLabData] = useState({
    title: '',
    short_desc: '',
    md_file: null,
    assets: [],
    tags: []
  });
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [availableTags, setAvailableTags] = useState([]);
  const [loadingTags, setLoadingTags] = useState(false);
  const [showCreateTag, setShowCreateTag] = useState(false);
  const [newTagData, setNewTagData] = useState({ name: '', description: '' });
  const [creatingTag, setCreatingTag] = useState(false);

  useEffect(() => {
    const fetchTags = async () => {
      try {
        setLoadingTags(true);
        const response = await tagsAPI.getTags(1, 100); // Получаем больше тегов
        setAvailableTags(response.tags || []);
      } catch (err) {
        console.error('Error fetching tags:', err);
        setError('Failed to load tags');
      } finally {
        setLoadingTags(false);
      }
    };

    fetchTags();
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setLabData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleAddTag = (tagId) => {
    // Добавляем тег только если его еще нет в списке
    if (!labData.tags.includes(tagId)) {
      setLabData(prev => ({
        ...prev,
        tags: [...prev.tags, tagId]
      }));
    }
  };

  const handleRemoveTag = (tagToRemove) => {
    setLabData(prev => ({
      ...prev,
      tags: prev.tags.filter(tag => tag !== tagToRemove)
    }));
  };

  const handleCreateTag = async (e) => {
    e.preventDefault();
    if (!newTagData.name.trim()) {
      setError('Tag name is required');
      return;
    }

    setCreatingTag(true);
    try {
      const createdTag = await tagsAPI.createTag({
        name: newTagData.name.trim(),
        description: newTagData.description.trim()
      });
      
      // Добавляем новый тег к списку доступных
      setAvailableTags(prev => [...prev, createdTag]);
      
      // Автоматически добавляем новый тег к лабе
      handleAddTag(createdTag.id);
      
      // Очищаем форму и закрываем модальное окно
      setNewTagData({ name: '', description: '' });
      setShowCreateTag(false);
      setError(null);
    } catch (err) {
      console.error('Error creating tag:', err);
      setError(err.message || 'Failed to create tag');
    } finally {
      setCreatingTag(false);
    }
  };

  const handleFileChange = (e) => {
    const { name, files } = e.target;
    if (name === 'md_file') {
      setLabData(prev => ({
        ...prev,
        md_file: files[0]
      }));
    } else if (name === 'assets') {
      setLabData(prev => ({
        ...prev,
        assets: Array.from(files)
      }));
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const files = Array.from(e.dataTransfer.files);
      const mdFile = files.find(file => file.name.endsWith('.md'));
      const assetFiles = files.filter(file => !file.name.endsWith('.md'));
      
      setLabData(prev => ({
        ...prev,
        md_file: mdFile || prev.md_file,
        assets: [...prev.assets, ...assetFiles]
      }));
    }
  };

  const removeAsset = (index) => {
    setLabData(prev => ({
      ...prev,
      assets: prev.assets.filter((_, i) => i !== index)
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    
    if (!labData.title.trim()) {
      setError('Title is required');
      return;
    }
    
    if (!labData.short_desc.trim()) {
      setError('Short description is required');
      return;
    }
    
    if (!labData.md_file) {
      setError('Markdown file is required');
      return;
    }

    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('title', labData.title);
      formData.append('short_desc', labData.short_desc);
      formData.append('md_file', labData.md_file);

      if (labData.tags.length > 0) {
        formData.append('tags', labData.tags.join(','));
      }
      
      if (labData.assets && labData.assets.length > 0) {
        for (const asset of labData.assets) {
          formData.append('assets', asset);
        }
      }
      
      const result = await labsAPI.createLab(formData);
      onSuccess && onSuccess(result);
    } catch (err) {
      console.error('Error creating lab:', err);
      setError(err.message || 'Failed to create lab');
    } finally {
      setUploading(false);
    }
  };

  const getSelectedTags = () => {
    return labData.tags.map(tagId => 
      availableTags.find(tag => tag.id === tagId)
    ).filter(Boolean);
  };

  const content = (
    <div className="max-w-2xl mx-auto">
      <h2 className="text-3xl font-bold font-display text-gray-900 dark:text-white mb-6">
        Create New Lab
      </h2>
      
      {error && (
        <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Title */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Lab Title *
          </label>
          <input
            type="text"
            name="title"
            value={labData.title}
            onChange={handleInputChange}
            placeholder="Enter lab title..."
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white"
            required
          />
        </div>

        {/* Short Description */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Short Description *
          </label>
          <textarea
            name="short_desc"
            value={labData.short_desc}
            onChange={handleInputChange}
            placeholder="Brief description of the lab..."
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white"
            required
          />
        </div>

        {/* Tags Section */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Tags
            </label>
            <button
              type="button"
              onClick={() => setShowCreateTag(true)}
              className="text-sm text-msc hover:text-msc-hover font-medium"
            >
              + Create New Tag
            </button>
          </div>
          
          {loadingTags ? (
            <div className="text-sm text-gray-500">Loading tags...</div>
          ) : (
            <div className="space-y-3">
              {/* Available Tags */}
              <div>
                <p className="text-xs text-gray-500 mb-2">Available Tags:</p>
                <div className="flex flex-wrap gap-2">
                  {availableTags.map(tag => (
                    tag.description && tag.description.length > 1 ? (
                      <Tooltip 
                        key={tag.id}
                        content={tag.description}
                        position="top"
                        delay={200}
                      >
                        <button
                          type="button"
                          onClick={() => handleAddTag(tag.id)}
                          disabled={labData.tags.includes(tag.id)}
                          className={`px-3 py-1 rounded-full text-xs transition-colors ${
                            labData.tags.includes(tag.id)
                              ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                              : 'bg-blue-100 text-blue-800 hover:bg-blue-200 dark:bg-blue-900 dark:text-blue-200'
                          }`}
                        >
                          {tag.name}
                        </button>
                      </Tooltip>
                    ) : (
                      <button
                        key={tag.id}
                        type="button"
                        onClick={() => handleAddTag(tag.id)}
                        disabled={labData.tags.includes(tag.id)}
                        className={`px-3 py-1 rounded-full text-xs transition-colors ${
                          labData.tags.includes(tag.id)
                            ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                            : 'bg-blue-100 text-blue-800 hover:bg-blue-200 dark:bg-blue-900 dark:text-blue-200'
                        }`}
                      >
                        {tag.name}
                      </button>
                    )
                  ))}
                  {availableTags.length === 0 && (
                    <span className="text-gray-500 text-sm">No tags available</span>
                  )}
                </div>
              </div>

              {/* Selected Tags */}
              {labData.tags.length > 0 && (
                <div>
                  <p className="text-xs text-gray-500 mb-2">Selected Tags:</p>
                  <div className="flex flex-wrap gap-2">
                    {getSelectedTags().map(tag => (
                      tag.description && tag.description.length > 1 ? (
                        <Tooltip 
                          key={tag.id}
                          content={tag.description}
                          position="top"
                          delay={200}
                        >
                          <span className="inline-flex items-center px-3 py-1 rounded-full text-xs bg-msc text-white">
                            {tag.name}
                            <button
                              type="button"
                              onClick={() => handleRemoveTag(tag.id)}
                              className="ml-1 text-white hover:text-gray-200"
                            >
                              ×
                            </button>
                          </span>
                        </Tooltip>
                      ) : (
                        <span key={tag.id} className="inline-flex items-center px-3 py-1 rounded-full text-xs bg-msc text-white">
                          {tag.name}
                          <button
                            type="button"
                            onClick={() => handleRemoveTag(tag.id)}
                            className="ml-1 text-white hover:text-gray-200"
                          >
                            ×
                          </button>
                        </span>
                      )
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Markdown File Upload */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Lab Markdown File * (.md)
          </label>
          <input
            type="file"
            name="md_file"
            accept=".md,.markdown"
            onChange={handleFileChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white"
            required
          />
          {labData.md_file && (
            <p className="mt-1 text-sm text-green-600">
              Selected: {labData.md_file.name}
            </p>
          )}
        </div>

        {/* Asset Files Upload with Drag & Drop */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Additional Assets (optional)
          </label>
          <div
            className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
              dragActive 
                ? 'border-msc bg-msc bg-opacity-10' 
                : 'border-gray-300 dark:border-gray-600'
            }`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <div className="space-y-2">
              <svg className="mx-auto h-12 w-12 text-gray-400" stroke="currentColor" fill="none" viewBox="0 0 48 48">
                <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <div className="text-gray-600 dark:text-gray-400">
                <p>Drag and drop files here, or</p>
                <label className="cursor-pointer text-msc hover:text-msc-hover">
                  <span>browse to select files</span>
                  <input
                    type="file"
                    name="assets"
                    multiple
                    onChange={handleFileChange}
                    className="hidden"
                  />
                </label>
              </div>
              <p className="text-xs text-gray-500">
                Images, PDFs, code files, etc.
              </p>
            </div>
          </div>

          {/* Display selected assets */}
          {labData.assets.length > 0 && (
            <div className="mt-4 space-y-2">
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Selected Assets:
              </p>
              {labData.assets.map((file, index) => (
                <div key={index} className="flex items-center justify-between bg-gray-50 dark:bg-gray-700 rounded p-2">
                  <span className="text-sm text-gray-700 dark:text-gray-300">{file.name}</span>
                  <button
                    type="button"
                    onClick={() => removeAsset(index)}
                    className="text-red-500 hover:text-red-700 text-sm"
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex justify-end space-x-4 pt-4">
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
              disabled={uploading}
            >
              Cancel
            </button>
          )}
          <button
            type="submit"
            disabled={uploading}
            className="px-6 py-2 bg-msc text-white rounded-md hover:bg-msc-hover disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
          >
            {uploading ? (
              <>
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Creating Lab...
              </>
            ) : (
              'Create Lab'
            )}
          </button>
        </div>
      </form>

      {/* Create Tag Modal */}
      {showCreateTag && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-md">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Create New Tag
            </h3>
            <form onSubmit={handleCreateTag} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Tag Name *
                </label>
                <input
                  type="text"
                  value={newTagData.name}
                  onChange={(e) => setNewTagData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="Enter tag name..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Description (optional)
                </label>
                <textarea
                  value={newTagData.description}
                  onChange={(e) => setNewTagData(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="Enter tag description..."
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                />
              </div>
              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateTag(false);
                    setNewTagData({ name: '', description: '' });
                    setError(null);
                  }}
                  className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                  disabled={creatingTag}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creatingTag}
                  className="px-4 py-2 bg-msc text-white rounded-md hover:bg-msc-hover disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                >
                  {creatingTag ? (
                    <>
                      <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      Creating...
                    </>
                  ) : (
                    'Create Tag'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );

  if (isModal) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto p-6">
          {content}
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg p-6">
      {content}
    </div>
  );
}