import { useState } from 'react';
import { labsAPI } from '../utils/api';
import { getCurrentUser } from '../utils/auth';

export default function LabUpload({ onSuccess, onCancel, isModal = true }) {
  const [labData, setLabData] = useState({
    title: '',
    short_desc: '',
    md_file: null,
    assets: []
  });
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [dragActive, setDragActive] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setLabData(prev => ({
      ...prev,
      [name]: value
    }));
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

  const content = (
    <div className="max-w-2xl mx-auto">
      <h2 className="text-2xl font-bold text-msc dark:text-white mb-6">
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