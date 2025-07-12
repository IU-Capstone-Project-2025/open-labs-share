import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { feedbackAPI, submissionsAPI, labsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';

// Helper function to safely format dates
const formatDateTime = (dateString) => {
  if (!dateString) return 'Unknown date';
  
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }
    return date.toLocaleString();
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Invalid date';
  }
};

const FeedbackViewPage = () => {
  const { feedbackId } = useParams();
  const navigate = useNavigate();
  const [feedback, setFeedback] = useState(null);
  const [labTitle, setLabTitle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const user = useUser();

  useEffect(() => {
    const fetchFeedback = async () => {
      try {
        setLoading(true);
        
        if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(feedbackId)) {
          throw new Error('Invalid feedback ID format');
        }
        
        const response = await feedbackAPI.getFeedbackById(feedbackId);
        
        if (response.reviewer.id !== user?.id) {
          throw new Error('You are not logged in to view this feedback.');
        }
        
        setFeedback(response);
        
        // Fetch lab title
        try {
          const submission = await submissionsAPI.getSubmissionById(response.submissionId);
          const labId = submission.labId || submission.data?.labId;
          
          if (labId) {
            const lab = await labsAPI.getLabById(labId);
            setLabTitle(lab?.title);
          }
        } catch (labErr) {
          console.error('Failed to fetch lab data:', labErr);
          // Continue without lab title
        }
      } catch (err) {
        setError(err.message || 'Feedback details could not be uploaded.');
        console.error(err);
        setTimeout(() => navigate('/feedback/my', { replace: true }), 2000);
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchFeedback();
    }
  }, [feedbackId, user, navigate]);

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return (
      <div className="text-center text-red-500 mt-8">
        <p>{error}</p>
        <p className="text-sm mt-2">Redirection to the feedback page...</p>
      </div>
    );
  }

  if (!feedback) {
    return <div className="text-center text-gray-500 mt-8">Feedback not found.</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <Link to="/feedback/my" className="text-blue-600 hover:underline">
          ←  Back to my feedbacks
        </Link>
      </div>

      <h1 className="text-3xl font-bold mb-4">
        Feedback for submission to "{labTitle || `Lab #${feedback.submissionId}`}"
      </h1>

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-8">
        <h2 className="text-xl font-semibold mb-2">Feedback details</h2>
        <div className="mb-4">
          <strong>Student:</strong> {feedback.student.name} {feedback.student.surname} ({feedback.student.username})
        </div>
        <div className="mb-4">
          <strong>Date of creation:</strong> {formatDateTime(feedback.createdAt)}
        </div>
        
        <h2 className="text-xl font-semibold mb-2 mt-6">Your feedback</h2>
        <div className="prose dark:prose-invert max-w-none bg-gray-50 dark:bg-gray-700 p-4 rounded-md">
          <p className="whitespace-pre-wrap">{feedback.content}</p>
        </div>

        {feedback.attachments && feedback.attachments.length > 0 && (
          <div className="mt-6">
            <h3 className="text-lg font-semibold mb-2">Attached files:</h3>
            <ul className="list-disc list-inside mt-2 space-y-1">
              {feedback.attachments.map((attachment, index) => (
                <li key={index} className="flex items-center">
                  <a 
                    href={`/api/feedback/attachments/${attachment.feedback_id}`} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline flex items-center"
                  >
                    <span className="mr-1">📎</span>
                    {attachment.filename} ({Math.round(attachment.total_size / 1024)} KB)
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}
        
        <div className="mt-6 pt-4 border-t border-gray-200 dark:border-gray-600">
          <div className="text-sm text-gray-500 dark:text-gray-400">
            <p>Last update: {formatDateTime(feedback.updatedAt)}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FeedbackViewPage;