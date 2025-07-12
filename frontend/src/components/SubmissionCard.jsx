import React from 'react';
import { Link } from 'react-router-dom';
import { 
  DocumentTextIcon, 
  ClockIcon, 
  CheckCircleIcon, 
  ExclamationCircleIcon, 
  TrashIcon,
  PaperClipIcon
} from '@heroicons/react/24/outline';

const SubmissionCard = ({ submission, onDelete }) => {
  const { submissionId, labId, labTitle, text, createdAt, status, assets } = submission;

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

  const handleDelete = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (window.confirm('Are you sure you want to delete this submission?')) {
      onDelete(submissionId);
    }
  };

  return (
    <div className="relative group">
      <Link to={`/submissions/${submissionId}`} className="block">
        <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center mb-4">
            <DocumentTextIcon className="w-8 h-8 text-blue-500 dark:text-blue-400 mr-4" />
            <div>
              <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                {labTitle || `Lab #${labId}`}
              </h3>
              {text && (
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">
                  {text}
                </p>
              )}
            </div>
          </div>

          {assets?.length > 0 && (
            <div className="flex justify-between items-center text-sm text-gray-600 dark:text-gray-300">
              <div className="flex items-center">
                <PaperClipIcon className="w-5 h-5 mr-1" />
                <span>{assets.length} file{assets.length !== 1 ? 's' : ''}</span>
              </div>
              <div className="flex items-center">
                <ClockIcon className="w-5 h-5 mr-1 text-gray-400" />
                <span>{new Date(createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          )}
        </div>
      </Link>
      
      {onDelete && (
        <button
          onClick={handleDelete}
          className="absolute top-2 right-2 p-1.5 bg-gray-200 dark:bg-gray-700 rounded-full text-gray-600 dark:text-gray-300 hover:bg-red-200 dark:hover:bg-red-800 hover:text-red-600 dark:hover:text-red-200 transition-colors opacity-0 group-hover:opacity-100"
          aria-label="Delete submission"
        >
          <TrashIcon className="w-5 h-5" />
        </button>
      )}
    </div>
  );
};

export default SubmissionCard;