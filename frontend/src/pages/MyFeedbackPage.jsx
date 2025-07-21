import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { feedbackAPI, submissionsAPI, labsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { DocumentTextIcon, ClockIcon, UserIcon, TrashIcon } from '@heroicons/react/24/outline';


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

const FeedbackCard = ({ feedback, onDelete }) => {
  const { id, submissionId, labTitle, student, createdAt, content } = feedback;

  if (!feedback.id || typeof feedback.id !== 'string') {
    console.error('Invalid feedback ID:', feedback.id);
    return null;
  }

  const handleDeleteClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    onDelete(feedback);
  };

  return (
    <div className="relative group">
      <Link to={`/feedback/view/${feedback.id}`} className="block group">
        <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 border border-gray-200 dark:border-gray-700 h-64 flex flex-col">
          <div className="flex mt-2 items-start mb-4 flex-1">
            <DocumentTextIcon className="w-8 h-8 text-blue-500 dark:text-blue-400 mr-4 mt-1 flex-shrink-0" />
            <div className="flex-1 min-w-0">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors line-clamp-2">
                Feedback for submission to "{labTitle || `Lab #${submissionId}`}"
              </h3>
              <div className="flex items-center mt-2">
                <UserIcon className="w-4 h-4 text-gray-500 mr-1 flex-shrink-0" />
                <p className="text-sm text-gray-500 dark:text-gray-400 line-clamp-1">
                  Student: {student.name} {student.surname}
                </p>
              </div>
            </div>
          </div>
          <div className="text-sm text-gray-600 dark:text-gray-300 line-clamp-2 mb-2 flex-1">
            {content}
          </div>
          <div className="flex justify-between items-center text-sm text-gray-500 dark:text-gray-400 mt-auto">
            <div className="flex items-center">
              <ClockIcon className="w-4 h-4 mr-1" />
              <span>Given on: {formatDate(createdAt)}</span>
            </div>
          </div>
        </div>
      </Link>
      
      <button
        onClick={handleDeleteClick}
        className="absolute top-2 right-2 p-1.5 bg-gray-200 dark:bg-gray-700 rounded-full text-gray-600 dark:text-gray-300 hover:bg-red-200 dark:hover:bg-red-800 hover:text-red-600 dark:hover:text-red-200 transition-colors opacity-0 group-hover:opacity-100"
        aria-label="Delete feedback"
      >
        <TrashIcon className="w-5 h-5" />
      </button>
    </div>
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
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [feedbackToDelete, setFeedbackToDelete] = useState(null);
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
        const feedbacksData = response.feedbacks || [];
        
        
        const enrichedFeedbacks = await Promise.all(
          feedbacksData.map(async (feedback) => {
            try {
              
              const submission = await submissionsAPI.getSubmissionById(feedback.submissionId);
              const labId = submission.labId || submission.data?.labId;
              
              if (labId) {
                
                const lab = await labsAPI.getLabById(labId);
                return {
                  ...feedback,
                  labTitle: lab?.title
                };
              }
              return feedback;
            } catch (err) {
              console.error(`Failed to fetch lab data for submission ${feedback.submissionId}:`, err);
              return feedback;
            }
          })
        );
        
        setFeedbacks(enrichedFeedbacks);
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

  const handleDeleteClick = (feedback) => {
    setFeedbackToDelete(feedback);
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = async () => {
    if (!feedbackToDelete) return;

    try {
      await feedbackAPI.deleteFeedback(feedbackToDelete.id);
      setFeedbacks(prev => prev.filter(feedback => feedback.id !== feedbackToDelete.id));
      setPagination(prev => ({
        ...prev,
        totalCount: prev.totalCount - 1
      }));
      setShowDeleteModal(false);
      setFeedbackToDelete(null);
    } catch (error) {
      console.error('Error deleting feedback:', error);
      setError('Failed to delete feedback. Please try again.');
    }
  };

  const handleCancelDelete = () => {
    setShowDeleteModal(false);
    setFeedbackToDelete(null);
  };

  const ConfirmationModal = () => {
    if (!showDeleteModal) return null;

    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md w-full animate-fade-in">
          <p className="text-gray-800 dark:text-gray-200 mb-4">
            Are you sure you want to delete this feedback for submission to "{feedbackToDelete?.labTitle || `Lab #${feedbackToDelete?.submissionId}`}"?
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
              <FeedbackCard key={feedback.id} feedback={feedback} onDelete={handleDeleteClick} />
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
      
      <ConfirmationModal />
    </div>
  );
};

export default MyFeedbackPage;