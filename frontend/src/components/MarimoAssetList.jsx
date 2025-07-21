import React, { useState, useEffect } from 'react';
import { marimoAPI } from '../utils/api';

/**
 * MarimoAssetList - A modern, minimalistic component for displaying marimo component assets
 * 
 * Features:
 * - File type icons based on extension and MIME type
 * - File size formatting (B, KB, MB, GB)
 * - Asset download functionality
 * - Error handling with user-friendly messages
 * - Dark mode support
 * - Professional UI/UX following modern design principles
 * - Integrated toggle UI for better asset count management
 * 
 * @param {Object} props
 * @param {string} props.componentId - The ID of the marimo component to fetch assets for
 * @param {function} props.onAssetCountChange - Callback to notify parent of asset count changes
 * @param {boolean} props.showUI - Whether to show the UI (dropdown button and assets)
 * @param {boolean} props.showAssets - Whether the assets list is expanded
 * @param {function} props.onToggleAssets - Callback to toggle assets visibility
 */
const MarimoAssetList = ({ componentId, onAssetCountChange, showUI = true, showAssets = false, onToggleAssets }) => {
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (componentId) {
      console.log('MarimoAssetList: Fetching assets for componentId:', componentId); // Debug log
      fetchAssets();
    } else {
      // If no componentId, reset assets and notify parent
      setAssets([]);
      if (onAssetCountChange) {
        onAssetCountChange(0);
      }
    }
  }, [componentId]);

  useEffect(() => {
    if (onAssetCountChange) {
      console.log('MarimoAssetList: Calling onAssetCountChange with:', assets.length); // Debug log
      onAssetCountChange(assets.length);
    }
  }, [assets.length]);

  const fetchAssets = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
      const bucketName = 'marimo';
      const prefix = `components/${componentId}/assets/`;
      
      // Try to use MinIO REST API to list objects directly
      const listUrl = `${minioEndpoint}/${bucketName}?list-type=2&prefix=${encodeURIComponent(prefix)}`;
      
      try {
        const response = await fetch(listUrl, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/xml',
          },
        });
        
        if (response.ok) {
          const xmlText = await response.text();
          const parsedAssets = parseMinioListResponse(xmlText, prefix);
          
          // Convert to our asset format
          const assetList = parsedAssets.map(asset => ({
            id: asset.name,
            path: asset.name,
            name: asset.name.split('/').pop(),
            size: asset.size,
            mimeType: guessMimeType(asset.name),
            url: `${minioEndpoint}/${bucketName}/${prefix}${asset.name}`
          }));
          
          setAssets(assetList);
          console.log('MarimoAssetList: MinIO assets loaded:', assetList.length); // Debug log
          return;
        } else if (response.status === 404 || response.status === 403) {
          // No assets found, this is normal
          setAssets([]);
          return;
        }
      } catch (minioError) {
        console.warn('Direct MinIO access failed, falling back to backend:', minioError);
      }
      
      // Fallback: Use the correct backend endpoint with authentication
      try {
        const assetInfos = await marimoAPI.listAssets(componentId);
        const assetList = assetInfos.map(assetInfo => ({
          id: assetInfo.id,
          name: assetInfo.fileName.split('/').pop(),
          path: assetInfo.fileName,
          size: assetInfo.fileSize,
          mimeType: assetInfo.mimeType,
          url: assetInfo.downloadUrl
        }));
        
        setAssets(assetList);
        console.log('MarimoAssetList: Backend assets loaded:', assetList.length); // Debug log
      } catch (backendError) {
        if (backendError.status === 404) {
          setAssets([]);
          return;
        }
        throw backendError;
      }
      
    } catch (err) {
      console.error('Failed to fetch assets:', err);
      setError('Failed to load assets');
      setAssets([]);
    } finally {
      setLoading(false);
    }
  };

  const parseMinioListResponse = (xmlText, prefix) => {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
    const contents = xmlDoc.querySelectorAll('Contents');
    
    const assets = [];
    contents.forEach(content => {
      const key = content.querySelector('Key')?.textContent;
      if (key && key.startsWith(prefix)) {
        const fileName = key.substring(prefix.length);
        const size = content.querySelector('Size')?.textContent || '0';
        // Only include actual files, not directories
        if (fileName && !fileName.endsWith('/')) {
          assets.push({ name: fileName, size: parseInt(size, 10) });
        }
      }
    });
    
    return assets;
  };

  const guessMimeType = (fileName) => {
    const ext = fileName.split('.').pop()?.toLowerCase();
    const mimeTypes = {
      'png': 'image/png',
      'jpg': 'image/jpeg',
      'jpeg': 'image/jpeg',
      'gif': 'image/gif',
      'svg': 'image/svg+xml',
      'pdf': 'application/pdf',
      'txt': 'text/plain',
      'csv': 'text/csv',
      'json': 'application/json',
      'xml': 'application/xml',
      'zip': 'application/zip',
      'py': 'text/x-python',
      'js': 'text/javascript',
      'html': 'text/html',
      'css': 'text/css',
      'md': 'text/markdown',
    };
    return mimeTypes[ext] || 'application/octet-stream';
  };

  const getFileIcon = (fileName, mimeType) => {
    if (!fileName) return '📄';
    
    const extension = fileName.split('.').pop()?.toLowerCase();
    
    // Image files
    if (mimeType?.startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension)) {
      return '🖼️';
    }
    
    // Data files
    if (['csv', 'tsv', 'json', 'xml', 'yaml', 'yml'].includes(extension)) {
      return '📊';
    }
    
    // Text files
    if (mimeType?.startsWith('text/') || ['txt', 'md', 'readme'].includes(extension)) {
      return '📝';
    }
    
    // Python files
    if (extension === 'py') {
      return '🐍';
    }
    
    // Archive files
    if (['zip', 'rar', '7z', 'tar', 'gz'].includes(extension)) {
      return '📦';
    }
    
    // Document files
    if (['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'].includes(extension)) {
      return '📄';
    }
    
    return '📄';
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const handleDownload = async (asset) => {
    try {
      const response = await fetch(asset.url);
      if (!response.ok) {
        let errorMessage = `Failed to download. Status: ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          // Not a json error, stick with the status
        }
        throw new Error(errorMessage);
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = url;
      a.download = asset.name;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to download asset:', err);
      setError(err.message || 'Could not download the file.');
    }
  };

  if (!componentId) {
    return null;
  }

  return (
    <div className="w-full">
      {/* Assets Section - Only show if there are assets */}
      {showUI && assets.length > 0 && (
        <>
          <button
            onClick={onToggleAssets}
            className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 transition-colors"
          >
            <svg 
              className={`w-4 h-4 transform transition-transform ${showAssets ? 'rotate-90' : ''}`}
              fill="currentColor" 
              viewBox="0 0 24 24"
            >
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
            <span className="font-medium">Assets</span>
            <span className="text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 px-2 py-1 rounded-full">
              {assets.length}
            </span>
          </button>
          
          {showAssets && (
            <div className="mt-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg overflow-hidden">
              {loading && (
                <div className="p-4 text-center text-gray-500 dark:text-gray-400">
                  <div className="inline-flex items-center gap-2">
                    <div className="w-4 h-4 border-2 border-gray-300 dark:border-gray-600 border-t-blue-500 rounded-full animate-spin"></div>
                    <span>Loading assets...</span>
                  </div>
                </div>
              )}

              {error && (
                <div className="p-4 text-center text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 rounded-lg">
                  <div className="inline-flex items-center gap-2">
                    <span>⚠️</span>
                    <span>{error}</span>
                  </div>
                </div>
              )}

              {!loading && !error && assets.length > 0 && (
                <div className="divide-y divide-gray-200 dark:divide-gray-700">
                  {assets.map((asset) => (
                    <div key={asset.id} className="p-3 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3 flex-1 min-w-0">
                          <span className="text-lg flex-shrink-0">
                            {getFileIcon(asset.name, asset.mimeType)}
                          </span>
                          <div className="flex-1 min-w-0">
                            <div className="text-sm font-medium text-gray-900 dark:text-white truncate" title={asset.name}>
                              {asset.name}
                            </div>
                            <div className="flex items-center gap-3 text-xs text-gray-500 dark:text-gray-400 mt-1">
                              <span>{formatFileSize(asset.size)}</span>
                              {asset.mimeType && (
                                <span className="bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">
                                  {asset.mimeType.split('/')[0]}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                        <button
                          className="p-2 text-gray-500 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-colors"
                          onClick={() => handleDownload(asset)}
                          title="Download asset"
                        >
                          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clipRule="evenodd" />
                          </svg>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default MarimoAssetList;
