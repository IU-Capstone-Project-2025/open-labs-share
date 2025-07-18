import React, { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';

const AssetUploader = ({ onAssetUpload }) => {
  const [uploadedFiles, setUploadedFiles] = useState([]);

  const onDrop = useCallback((acceptedFiles) => {
    const newFiles = [...uploadedFiles, ...acceptedFiles];
    setUploadedFiles(newFiles);
    if (onAssetUpload) {
      onAssetUpload(newFiles);
    }
  }, [uploadedFiles, onAssetUpload]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop });

  const removeFile = (fileName) => {
    const filteredFiles = uploadedFiles.filter(file => file.name !== fileName);
    setUploadedFiles(filteredFiles);

    if (onAssetUpload) {
      onAssetUpload(filteredFiles);
    }
  };

  return (
    <div className="mt-4">
      <div
        {...getRootProps()}
        className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors ${
          isDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'
        }`}
      >
        <input {...getInputProps()} />
        {isDragActive ? (
          <p className="text-blue-600">Drop the files here ...</p>
        ) : (
          <p className="text-gray-500">Drag 'n' drop some files here, or click to select files</p>
        )}
      </div>
      {uploadedFiles.length > 0 && (
        <div className="mt-4">
          <h4 className="font-semibold text-gray-700">Uploaded Assets:</h4>
          <ul className="list-disc list-inside mt-2">
            {uploadedFiles.map((file, index) => (
              <li key={index} className="text-gray-600 flex justify-between items-center">
                <span>{file.name} - {(file.size / 1024).toFixed(2)} KB</span>
                <button
                  onClick={() => removeFile(file.name)}
                  className="text-red-500 hover:text-red-700 font-bold"
                >
                  &times;
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default AssetUploader; 