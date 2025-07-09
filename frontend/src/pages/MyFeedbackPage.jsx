import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { feedbackAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { DocumentTextIcon, ClockIcon, UserIcon } from '@heroicons/react/24/outline';

const FeedbackCard = ({ feedback }) => {
  const { id, submissionId, student, createdAt, content } = feedback;

  if (!feedback.id || typeof feedback.id !== 'string') {
    console.error('Invalid feedback ID:', feedback.id);
    return null;
  }

  return (
    <Link to={`/feedback/view/${feedback.id}`} className="block group">
      <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 border border-gray-200 dark:border-gray-700">
        <div className="flex items-center mb-4">
          <DocumentTextIcon className="w-8 h-8 text-blue-500 dark:text-blue-400 mr-4" />
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              Feedback for submission #{submissionId}
            </h3>
            <div className="flex items-center mt-1">
              <UserIcon className="w-4 h-4 text-gray-500 mr-1" />
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Student: {student.name} {student.surname}
              </p>
            </div>
          </div>
        </div>
        <div className="text-sm text-gray-600 dark:text-gray-300 line-clamp-2 mb-2">
          {content}
        </div>
        <div className="flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
          <div className="flex items-center">
            <ClockIcon className="w-4 h-4 mr-1" />
            <span>Given on: {new Date(createdAt).toLocaleDateString()}</span>
          </div>
        </div>
      </div>
    </Link>
  );
};

const MyFeedbackPage = () => {
  const [feedbacks, setFeedbacks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    page: 1,
    limit: 10,
    totalCount: 0
  });
  const user = useUser();

  useEffect(() => {
    const fetchMyCreatedFeedbacks = async () => {
      if (!user) {
        setLoading(false);
        return;
      }
      try {
        setLoading(true);
        const response = await feedbackAPI.listMyCreatedFeedbacks(user.id, pagination.page, pagination.limit);
        setFeedbacks(response.feedbacks || []);
        setPagination(prev => ({
          ...prev,
          totalCount: response.totalCount || 0
        }));
      } catch (err) {
        setError('Failed to fetch your feedbacks.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchMyCreatedFeedbacks();
  }, [user, pagination.page, pagination.limit]);

  const handlePageChange = (newPage) => {
    setPagination(prev => ({ ...prev, page: newPage }));
  };

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-8">
        My Given Feedbacks
      </h1>
      
      {feedbacks.length > 0 ? (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            {feedbacks.map(feedback => (
              <FeedbackCard key={feedback.id} feedback={feedback} />
            ))}
          </div>
          
          {/* Flip through the pages */}
          {/* <div className="flex justify-center items-center space-x-2">
            <button
              onClick={() => handlePageChange(pagination.page - 1)}
              disabled={pagination.page === 1}
              className="px-4 py-2 border rounded-md disabled:opacity-50 text-gray-600 dark:text-gray-300"
            >
              Previous
            </button>
            <span className="text-gray-600 dark:text-gray-300">
              Page {pagination.page} of {Math.ceil(pagination.totalCount / pagination.limit)}
            </span>
            <button
              onClick={() => handlePageChange(pagination.page + 1)}
              disabled={pagination.page * pagination.limit >= pagination.totalCount}
              className="px-4 py-2 border rounded-md disabled:opacity-50 text-gray-600 dark:text-gray-300"
            >
              Next
            </button>
          </div> */}
        </>
      ) : (
        <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
          <p>You haven't given any feedback yet.</p>
        </div>
      )}
    </div>
  );
};

export default MyFeedbackPage;