import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate  } from 'react-router-dom';
import { feedbackAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';

const FeedbackViewPage = () => {
  const { feedbackId } = useParams();
  const navigate = useNavigate();
  const [feedback, setFeedback] = useState(null);
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
          throw new Error('You are not authorized to view this feedback');
        }
        
        setFeedback(response);
      } catch (err) {
        setError(err.message || 'Failed to fetch feedback details.');
        console.error(err);
        navigate('/feedback/my', { replace: true });
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
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }

  if (!feedback) {
    return <div className="text-center text-gray-500 mt-8">Feedback not found.</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <Link to="/feedback/my" className="text-blue-600 hover:underline">
          ← Back to My Feedbacks
        </Link>
      </div>

      <h1 className="text-3xl font-bold mb-4">
        Feedback for: {feedback.submission?.lab?.title || 'Unknown Lab'}
      </h1>

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-8">
        <h2 className="text-xl font-semibold mb-2">Submission Details</h2>
        <p className="mb-4">Submitted by: {feedback.student.name} {feedback.student.surname}</p>
        
        <h2 className="text-xl font-semibold mb-2 mt-6">Your Feedback</h2>
        <div className="prose dark:prose-invert max-w-none">
          <p>{feedback.content}</p>
        </div>

        {feedback.attachments && feedback.attachments.length > 0 && (
          <div className="mt-6">
            <h3 className="text-lg font-semibold mb-2">Attachments:</h3>
            <ul className="list-disc list-inside mt-2">
              {feedback.attachments.map(attachment => (
                <li key={attachment.feedback_id}>
                  <a 
                    href={`/api/feedback/attachments/${attachment.feedback_id}`} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline"
                  >
                    {attachment.filename} ({Math.round(attachment.total_size / 1024)} KB)
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};

export default FeedbackViewPage;