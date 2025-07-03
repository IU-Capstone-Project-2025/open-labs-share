import { useState } from 'react';
import { articlesAPI } from '../utils/api';

export default function ArticleUpload({ onSuccess, onCancel, isModal = true }) {
  const [articleData, setArticleData] = useState({
    title: '',
    short_desc: '',
    pdf_file: null,
  });
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setArticleData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleFileChange = (e) => {
    const { name, files } = e.target;
    if (name === 'pdf_file') {
      setArticleData(prev => ({
        ...prev,
        pdf_file: files[0]
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    
    if (!articleData.title.trim()) {
      setError('Title is required');
      return;
    }
    
    if (!articleData.short_desc.trim()) {
      setError('Short description is required');
      return;
    }
    
    if (!articleData.pdf_file) {
      setError('PDF file is required');
      return;
    }

    const formData = new FormData();
    formData.append('title', articleData.title);
    formData.append('short_desc', articleData.short_desc);
    formData.append('pdf_file', articleData.pdf_file);

    try {
      setUploading(true);
      const result = await articlesAPI.createArticle(formData);
      onSuccess && onSuccess(result);
    } catch (err) {
      console.error('Error creating article:', err);
      setError(err.message || 'Failed to create article');
    } finally {
      setUploading(false);
    }
  };

  const content = (
    <div className="max-w-2xl mx-auto">
      <h2 className="text-2xl font-bold text-msc dark:text-white mb-6">
        Create New Article
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
            Article Title *
          </label>
          <input
            type="text"
            name="title"
            value={articleData.title}
            onChange={handleInputChange}
            placeholder="Enter article title..."
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
            value={articleData.short_desc}
            onChange={handleInputChange}
            placeholder="Brief description of the article..."
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white"
            required
          />
        </div>

        {/* PDF File Upload */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Article PDF File * (.pdf)
          </label>
          <input
            type="file"
            name="pdf_file"
            accept=".pdf"
            onChange={handleFileChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white"
            required
          />
          {articleData.pdf_file && (
            <p className="mt-1 text-sm text-green-600">
              Selected: {articleData.pdf_file.name}
            </p>
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
                  <path clipRule="evenodd" d="M12 2.75A9.25 9.25 0 1021.25 12 1.25 1.25 0 1123.75 12 11.75 11.75 0 1112 .25a1.25 1.25 0 110 2.5z" fill="currentColor" fillRule="evenodd"/>
                </svg>
                <span>Uploading...</span>
              </>
            ) : (
              'Create Article'
            )}
          </button>
        </div>
      </form>
    </div>
  );

  return isModal ? (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl p-8 w-full max-w-2xl">
        <button onClick={onCancel} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
          &times;
        </button>
        {content}
      </div>
    </div>
  ) : content;
} 