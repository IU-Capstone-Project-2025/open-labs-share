import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { submissionsAPI, labsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { DocumentTextIcon, ClockIcon, UserIcon } from '@heroicons/react/24/outline';


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

const SubmissionCard = ({ submission }) => {

  const { submissionId, labId, labTitle, owner, createdAt } = submission;

  return (
    <Link to={`/feedback/${submissionId}`} className="block group">
      <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 border border-gray-200 dark:border-gray-700">
        <div className="flex items-center mb-4">
          <DocumentTextIcon className="w-8 h-8 text-blue-500 dark:text-blue-400 mr-4" />
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              {`Submission to: ${labTitle}` || `Submission to Lab #${labId}`}
            </h3>
          </div>
        </div>
        <div className="flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
          <div className="flex items-center">
            <ClockIcon className="w-4 h-4 mr-1" />
            <span>Submitted: {formatDate(createdAt)}</span>
          </div>
          <div className="flex items-center">
            <UserIcon className="w-4 h-4 text-gray-500 mr-1" />
            <p className="text-sm text-gray-500 dark:text-gray-400">
              By: {owner?.name} {owner?.surname}
            </p>
          </div>
        </div>
      </div>
    </Link>
  );
};

const ReviewQueuePage = () => {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalCount, setTotalCount] = useState(0);
  const user = useUser();

  useEffect(() => {
    const fetchSubmissionsForReview = async () => {
      if (!user) {
        setLoading(false);
        return;
      }
      
      try {
        setLoading(true);

        const response = await submissionsAPI.getSubmissionsForReview();
        const submissionsData = response.submissions || [];
        const total = response.totalCount || 0;
        
        
        const labIds = [...new Set(submissionsData.map(s => s.labId))];
        const labsResponse = await Promise.all(
          labIds.map(id => labsAPI.getLabById(id).catch(() => null))
        );
        
        const labsMap = labsResponse.reduce((acc, lab) => {
          if (lab) acc[lab.id] = lab;
          return acc;
        }, {});

        
        const enrichedSubmissions = submissionsData.map(sub => ({
          ...sub,
          labTitle: labsMap[sub.labId]?.title
        }));
        
        setSubmissions(enrichedSubmissions);
        setTotalCount(total);

      } catch (err) {
        setError('Failed to fetch submissions for review.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchSubmissionsForReview();
  }, [user]);

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-8">
        Review Queue
      </h1>
      
      {totalCount > 0 && (
        <p className="text-gray-600 dark:text-gray-400 mb-6">
          {totalCount} submission{totalCount !== 1 ? 's' : ''} waiting for review
        </p>
      )}
      
      {submissions.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {submissions.map(submission => (
            <SubmissionCard key={submission.submissionId} submission={submission} />
          ))}
        </div>
      ) : (
        <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
          <p>No submissions waiting for review at the moment.</p>
        </div>
      )}
    </div>
  );
};

export default ReviewQueuePage;