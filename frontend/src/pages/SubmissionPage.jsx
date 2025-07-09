import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { labsAPI, submissionsAPI, feedbackAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { DocumentTextIcon, ClockIcon, UserIcon, PaperClipIcon } from '@heroicons/react/24/outline';

const FeedbackCard = ({ feedback }) => {
  return (
    <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg mb-4">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center space-x-2">
          <UserIcon className="w-5 h-5 text-blue-500" />
          <span className="font-medium text-gray-900 dark:text-white">
            {feedback.reviewer.name} {feedback.reviewer.surname}
          </span>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            (@{feedback.reviewer.username})
          </span>
        </div>
        <div className="flex items-center space-x-1 text-sm text-gray-500 dark:text-gray-400">
          <ClockIcon className="w-4 h-4" />
          <span>{new Date(feedback.createdAt).toLocaleDateString()}</span>
        </div>
      </div>
      
      <div className="prose dark:prose-invert max-w-none">
        <p className="whitespace-pre-wrap text-gray-700 dark:text-gray-300">
          {feedback.content}
        </p>
      </div>
      
      {feedback.attachments && feedback.attachments.length > 0 && (
        <div className="mt-4">
          <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-2">
            Attached Files:
          </h4>
          <ul className="space-y-1">
            {feedback.attachments.map((attachment, index) => (
              <li key={index} className="flex items-center space-x-2">
                <PaperClipIcon className="w-4 h-4 text-gray-500" />
                <a 
                  href={`/api/feedback/attachments/${attachment.feedback_id}`} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:underline text-sm"
                >
                  {attachment.filename} ({Math.round(attachment.total_size / 1024)} KB)
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

const SubmissionPage = () => {
  const { id } = useParams();
  const user = useUser();
  const [submission, setSubmission] = useState(null);
  const [lab, setLab] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading] = useState(true);
  const [feedbackLoading, setFeedbackLoading] = useState(true);
  const [error, setError] = useState(null);
  const [feedbackError, setFeedbackError] = useState(null);

  useEffect(() => {
    const fetchSubmission = async () => {
      try {
        setLoading(true);
        const sub = await submissionsAPI.getSubmissionById(id);
        setSubmission(sub);

        if (sub?.labId) {
          const labData = await labsAPI.getLabById(sub.labId);
          setLab(labData);
        }
      } catch (err) {
        setError('Failed to fetch submission details.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchSubmission();
    }
  }, [id]);

  useEffect(() => {
    const fetchFeedback = async () => {
      if (!submission || !user) return;
      
      try {
        setFeedbackLoading(true);
        // Получаем фидбек для конкретного сабмишена
        const response = await feedbackAPI.getMyFeedbackForSubmission(submission.submissionId);
        setFeedback(response);
      } catch (err) {
        // Если фидбека нет, это нормально (404 ошибка)
        if (err.message?.includes('404') || err.message?.includes('not found')) {
          setFeedback(null);
        } else {
          setFeedbackError('Failed to fetch feedback.');
          console.error('Error fetching feedback:', err);
        }
      } finally {
        setFeedbackLoading(false);
      }
    };

    fetchFeedback();
  }, [submission, user]);

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }
  
  if (!submission) {
    return <div className="text-center text-gray-500 mt-8">Submission not found.</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-2">
        Submission for: {lab?.title || `Lab #${submission.labId}`}
      </h1>
      <p className="text-gray-600 dark:text-gray-400 mb-8">
        Submitted on: {new Date(submission.createdAt).toLocaleString()} by {submission.owner?.username || 'Unknown User'}
      </p>

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow mb-8">
        <h2 className="text-2xl font-semibold mb-4">Submission Details</h2>
        <div className="prose dark:prose-invert max-w-none">
          <p><strong>Status:</strong> {submission.status}</p>
          <p><strong>Comment/Solution:</strong></p>
          <pre className="bg-gray-100 dark:bg-gray-700 p-4 rounded-md">
            <code>{submission.text}</code>
          </pre>
          
          <h3 className="mt-6">Attached Files:</h3>
          {submission.assets && submission.assets.length > 0 ? (
            <ul>
              {submission.assets.map(asset => (
                <li key={asset.assetId}>
                  <a href={asset.url} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline">
                    {asset.filename}
                  </a>
                </li>
              ))}
            </ul>
          ) : (
            <p>No files were attached.</p>
          )}
        </div>
      </div>
      
      {/* Review section with feedback */}
      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
        <div className="flex items-center space-x-2 mb-4">
          <DocumentTextIcon className="w-6 h-6 text-blue-500" />
          <h2 className="text-2xl font-semibold">Reviews & Feedback</h2>
        </div>
        
        {feedbackLoading ? (
          <div className="flex justify-center items-center h-24">
            <Spinner />
          </div>
        ) : feedbackError ? (
          <div className="text-center text-red-500 py-4">
            {feedbackError}
          </div>
        ) : feedback ? (
          <FeedbackCard feedback={feedback} />
        ) : (
          <div className="text-center text-gray-500 py-8">
            <DocumentTextIcon className="w-12 h-12 mx-auto mb-2 text-gray-400" />
            <p>No feedback has been provided for this submission yet.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default SubmissionPage;