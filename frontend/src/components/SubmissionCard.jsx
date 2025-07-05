import React from 'react';
import { Link } from 'react-router-dom';
import { DocumentTextIcon, ClockIcon, CheckCircleIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline';

const SubmissionCard = ({ submission }) => {
  // Assuming submission object has lab title, submission date, and status.
  const { id, lab, createdAt, status } = submission;

  const statusInfo = {
    submitted: { icon: <ClockIcon className="w-5 h-5 text-yellow-500" />, text: 'Submitted', color: 'yellow' },
    reviewed: { icon: <CheckCircleIcon className="w-5 h-5 text-green-500" />, text: 'Reviewed', color: 'green' },
  };

  const currentStatus = statusInfo[status] || { icon: <ExclamationCircleIcon className="w-5 h-5 text-gray-500" />, text: status, color: 'gray' };

  return (
    <Link to={`/submissions/${id}`} className="block group">
      <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 border border-gray-200 dark:border-gray-700">
        <div className="flex items-center mb-4">
          <DocumentTextIcon className="w-8 h-8 text-blue-500 dark:text-blue-400 mr-4" />
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              {lab?.title || 'Unknown Lab'}
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Submission
            </p>
          </div>
        </div>
        <div className="flex justify-between items-center text-sm text-gray-600 dark:text-gray-300">
          <div className="flex items-center">
            {currentStatus.icon}
            <span className={`ml-2 font-medium text-${currentStatus.color}-600 dark:text-${currentStatus.color}-400`}>{currentStatus.text}</span>
          </div>
          <div className="flex items-center">
            <ClockIcon className="w-5 h-5 mr-1 text-gray-400" />
            <span>{new Date(createdAt).toLocaleDateString()}</span>
          </div>
        </div>
      </div>
    </Link>
  );
};

export default SubmissionCard; 