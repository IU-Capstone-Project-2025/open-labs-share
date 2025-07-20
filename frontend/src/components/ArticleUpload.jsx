import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { articlesAPI, marimoAPI } from '../utils/api';
import { getCurrentUser } from '../utils/auth';
import MarimoCreator from './MarimoCreator';
import { PlusCircleIcon } from '@heroicons/react/24/solid';


export default function ArticleUpload({ onSuccess, onCancel, isModal = true }) {
  const [articleData, setArticleData] = useState({
    title: '',
    short_desc: '',
    pdf_file: null,
  });
  const [marimoComponents, setMarimoComponents] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const navigate = useNavigate();

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setArticleData(prev => ({
      ...prev,
      [name]: value
    }));
    setFieldErrors(prev => ({ ...prev, [name]: undefined }));
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

  const handleAddComponent = () => {
    const newComponent = {
      id: `temp-${Date.now()}-${Math.random()}`, // Unique ID for key prop
      name: '',
      code: '',
      assets: []
    };
    setMarimoComponents([...marimoComponents, newComponent]);
  };
  
  const handleRemoveComponent = (idToRemove) => {
    setMarimoComponents(marimoComponents.filter((component) => component.id !== idToRemove));
  };

  const handleComponentChange = (index, data) => {
    setMarimoComponents(prevComponents => 
      prevComponents.map((component, i) => 
        i === index ? data : component
      )
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    
    if (!articleData.title.trim()) {
      setFieldErrors(prev => ({ ...prev, title: 'Title is required' }));
      return;
    }
    
    if (!articleData.short_desc.trim()) {
      setFieldErrors(prev => ({ ...prev, short_desc: 'Short description is required' }));
      return;
    }
    
    if (!articleData.pdf_file) {
      setFieldErrors(prev => ({ ...prev, pdf_file: 'PDF file is required' }));
      return;
    }

    // Validate Marimo components before creating the article
    if (marimoComponents.length > 0) {
      const invalidComponents = marimoComponents.filter(
        component => !component.name || component.name.trim() === '' || !component.code || component.code.trim() === ''
      );
      
      if (invalidComponents.length > 0) {
        setError('All interactive components must have a name and code filled in. Please complete or remove empty components.');
        return;
      }
    }

    const formData = new FormData();
    formData.append('title', articleData.title);
    formData.append('short_desc', articleData.short_desc);
    formData.append('pdf_file', articleData.pdf_file);

    try {
      setUploading(true);
      const result = await articlesAPI.createArticle(formData);

      // Step 2: Create Marimo components if any
      const user = getCurrentUser();

      // --- DEBUGGING LOG ---
      console.log("--- Marimo Component Creation Check ---");
      console.log("User object:", user);
      console.log("Article creation API result:", result);
      console.log("Marimo components in state:", JSON.stringify(marimoComponents, null, 2));
      console.log("marimoComponents.length > 0:", marimoComponents.length > 0);
      // --- END DEBUGGING LOG ---

      if (user && result && result.id && marimoComponents.length > 0) {
        console.log("Condition PASSED. Creating Marimo components...");
        const articleId = result.id;
        for (const component of marimoComponents) {
          const componentResponse = await marimoAPI.createComponent({
            name: component.name,
            contentType: 'article',
            contentId: String(articleId),
            ownerId: String(user.id),
            initialCode: component.code,
          });
          const componentId = componentResponse.id;

          if (component.assets && component.assets.length > 0) {
            for (const assetFile of component.assets) {
              const assetFormData = new FormData();
              assetFormData.append('file', assetFile);
              assetFormData.append('componentId', String(componentId));
              assetFormData.append('assetType', 'DATA');
              await marimoAPI.uploadAsset(assetFormData);
            }
          }
        }
      } else {
        console.log("Condition FAILED. Skipping Marimo component creation.");
      }

      if (onSuccess) {
        onSuccess(result);
      } else {
        const user = getCurrentUser();
        if (user) {
          navigate(`/users/${user.id}/articles`);
        } else {
          navigate('/'); // Fallback to home if user is not found
        }
      }

    } catch (err) {
      console.error('Error creating article:', err);
      if (err.data && typeof err.data === 'object') {
        const { message, ...fields } = err.data;
        setFieldErrors(fields);
        setError(message || 'Failed to create article');
      } else {
        setError(err.message || 'Failed to create article');
      }
    } finally {
      setUploading(false);
    }
  };

  const content = (
    <div className="max-w-2xl mx-auto">
      <h2 className="text-3xl font-bold font-display text-gray-900 dark:text-white mb-6">
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
            className={`w-full px-3 py-2 border ${fieldErrors.title ? 'border-red-500' : 'border-gray-300'} rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white`}
            required
          />
          {fieldErrors.title && (
            <p className="mt-1 text-xs text-red-600">{fieldErrors.title}</p>
          )}
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
            className={`w-full px-3 py-2 border ${fieldErrors.short_desc ? 'border-red-500' : 'border-gray-300'} rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white`}
            required
          />
          {fieldErrors.short_desc && (
            <p className="mt-1 text-xs text-red-600">{fieldErrors.short_desc}</p>
          )}
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
            className={`w-full px-3 py-2 border ${fieldErrors.pdf_file ? 'border-red-500' : 'border-gray-300'} rounded-md focus:outline-none focus:ring-2 focus:ring-msc dark:bg-gray-700 dark:border-gray-600 dark:text-white`}
            required
          />
          {articleData.pdf_file && (
            <p className="mt-1 text-sm text-green-600">
              Selected: {articleData.pdf_file.name}
            </p>
          )}
          {fieldErrors.pdf_file && (
            <p className="mt-1 text-xs text-red-600">{fieldErrors.pdf_file}</p>
          )}
        </div>

        {/* Marimo Components Section */}
        <div className="bg-gray-50 dark:bg-gray-800/50 p-6 rounded-2xl border border-gray-200 dark:border-gray-700 mt-6">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Interactive Components (Optional)</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
                Enhance your article by adding interactive components with Python code. These allow for live code execution directly within the lab page.
            </p>

            {marimoComponents.length > 0 && (
              <div className="space-y-6">
                {marimoComponents.map((component, index) => (
                  <MarimoCreator
                    key={component.id}
                    index={index}
                    componentData={component}
                    onComponentChange={handleComponentChange}
                    onRemove={() => handleRemoveComponent(component.id)}
                  />
                ))}
              </div>
            )}
            
            <button
                type="button"
                onClick={handleAddComponent}
                className="mt-6 flex items-center justify-center w-full px-4 py-3 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg text-gray-500 dark:text-gray-400 hover:border-blue-500 hover:text-blue-500 dark:hover:border-blue-400 dark:hover:text-blue-400 transition-all"
            >
                <PlusCircleIcon className="h-6 w-6 mr-2" />
            </button>
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
            className="px-6 py-2 bg-msc text-white dark:bg-white dark:text-msc rounded-md hover:bg-msc-hover disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
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
