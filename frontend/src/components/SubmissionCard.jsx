import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  DocumentTextIcon, 
  ClockIcon, 
  CheckCircleIcon, 
  ExclamationCircleIcon, 
  TrashIcon,
  PaperClipIcon
} from '@heroicons/react/24/outline';


const formatDate = (dateString) => {
  if (!dateString) return 'Unknown date';
  
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }
    return date.toLocaleDateString();
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Invalid date';
  }
};

const SubmissionCard = ({ submission, onDelete }) => {
  const { submissionId, labId, labTitle, text, createdAt, status, assets } = submission;
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const statusInfo = {
    submitted: { icon: <ClockIcon className="w-5 h-5 text-yellow-500" />, text: 'Submitted', color: 'yellow' },
    reviewed: { icon: <CheckCircleIcon className="w-5 h-5 text-green-500" />, text: 'Reviewed', color: 'green' },
    approved: { icon: <CheckCircleIcon className="w-5 h-5 text-green-500" />, text: 'Approved', color: 'green' },
    rejected: { icon: <ExclamationCircleIcon className="w-5 h-5 text-red-500" />, text: 'Rejected', color: 'red' }
  };

  const currentStatus = statusInfo[status.toLowerCase()] || { 
    icon: <ExclamationCircleIcon className="w-5 h-5 text-gray-500" />, 
    text: status, 
    color: 'gray' 
  };

  const handleDeleteClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = () => {
    onDelete(submissionId);
    setShowDeleteModal(false);
  };

  const handleCancelDelete = () => {
    setShowDeleteModal(false);
  };

  const ConfirmationModal = () => {
    if (!showDeleteModal) return null;

    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md w-full animate-fade-in">
          <p className="text-gray-800 dark:text-gray-200 mb-4">
            Are you sure you want to delete this submission?
          </p>
          <div className="flex justify-end space-x-3">
            <button
              onClick={handleCancelDelete}
              className="px-4 py-2 text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmDelete}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="relative group">
      <Link to={`/submissions/${submissionId}`} className="block">
        <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 border border-gray-200 dark:border-gray-700 h-48 flex flex-col">
          <div className="flex items-start mb-4 flex-1">
            <DocumentTextIcon className="w-8 h-8 text-blue-500 dark:text-blue-400 mr-4 mt-1 flex-shrink-0" />
            <div className="flex-1 min-w-0">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors line-clamp-2">
                Submission to "{labTitle || `Lab #${labId}`}"
              </h3>
              {text && (
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 line-clamp-3">
                  {text}
                </p>
              )}
            </div>
          </div>

          <div className="flex justify-between items-center text-sm text-gray-600 dark:text-gray-300 mt-auto">
            <div className="flex items-center">
              {assets?.length > 0 ? (
                <>
                  <PaperClipIcon className="w-5 h-5 mr-1" />
                  <span>{assets.length} file{assets.length !== 1 ? 's' : ''}</span>
                </>
              ) : (
                <span className="text-gray-500 dark:text-gray-400">No files attached</span>
              )}
            </div>
            <div className="flex items-center">
              <ClockIcon className="w-5 h-5 mr-1 text-gray-400" />
              <span>Submitted: {formatDate(createdAt)}</span>
            </div>
          </div>
        </div>
      </Link>
      
      {onDelete && (
        <button
          onClick={handleDeleteClick}
          className="absolute top-2 right-2 p-1.5 bg-gray-200 dark:bg-gray-700 rounded-full text-gray-600 dark:text-gray-300 hover:bg-red-200 dark:hover:bg-red-800 hover:text-red-600 dark:hover:text-red-200 transition-colors opacity-0 group-hover:opacity-100"
          aria-label="Delete submission"
        >
          <TrashIcon className="w-5 h-5" />
        </button>
      )}

      <ConfirmationModal />
    </div>
  );
};

export default SubmissionCard;