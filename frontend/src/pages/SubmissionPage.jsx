import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { labsAPI, submissionsAPI, feedbackAPI } from '../utils/api';
import { mlAPI } from '../utils/api';
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
  const [downloadingFiles, setDownloadingFiles] = useState(new Set());
  const [toast, setToast] = useState({ show: false, message: "", type: "" });
  const [gradingStatus, setGradingStatus] = useState(null);
  const [gradingResult, setGradingResult] = useState(null);
  const [gradingError, setGradingError] = useState(null);
  const [gradingLoading, setGradingLoading] = useState(false);

  const getSubmissionFileUrl = (submissionId, filename) => {
    const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
    return `${minioEndpoint}/submissions/${submissionId}/${filename}`;
  };

  const downloadFile = async (filename) => {
    if (!id || !filename) return;

    try {
      setDownloadingFiles(prev => new Set(prev).add(filename));
      
      const url = getSubmissionFileUrl(id, filename);
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error(`Failed to download file: HTTP ${response.status}`);
      }
      
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      window.URL.revokeObjectURL(downloadUrl);
      
      setToast({ show: true, message: `Downloaded ${filename} successfully!`, type: 'success' });
    } catch (error) {
      console.error(`Error downloading file ${filename}:`, error);
      setToast({ show: true, message: `Failed to download ${filename}: ${error.message}`, type: 'error' });
    } finally {
      setDownloadingFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete(filename);
        return newSet;
      });
    }
  };

  const downloadAllFiles = async () => {
    if (!submission?.assets || submission.assets.length === 0) return;

    try {
      setDownloadingFiles(prev => new Set([...prev, 'all']));
      
      if (submission.assets.length === 1) {
        await downloadFile(submission.assets[0].filename);
        return;
      }

      for (const asset of submission.assets) {
        await downloadFile(asset.filename);
        await new Promise(resolve => setTimeout(resolve, 500));
      }
      
      setToast({ show: true, message: 'All files downloaded successfully!', type: 'success' });
    } catch (error) {
      console.error('Error downloading all files:', error);
      setToast({ show: true, message: `Failed to download all files: ${error.message}`, type: 'error' });
    } finally {
      setDownloadingFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete('all');
        return newSet;
      });
    }
  };

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

  // Fetch autograding status/result
  useEffect(() => {
    let polling = null;
    const fetchGrading = async () => {
      if (!submission || !user?.id || !submission.labId || (!submission.submissionId && !submission.id)) return;
      const submissionId = submission.submissionId || submission.id;
      const params = {
        uuid: String(user.id),
        assignment_id: String(submission.labId),
        submission_id: String(submissionId),
        webhook_url: 'http://localhost:8080/webhook', // TODO: replace with real webhook if needed
      };
      setGradingLoading(true);
      setGradingError(null);
      try {
        // Get status
        const statusResp = await mlAPI.getGradingStatus(params);
        let statusData = null;
        if (statusResp.ok) {
          statusData = await statusResp.json();
          setGradingStatus(statusData.status || statusData);
        } else {
          setGradingStatus(null);
          setGradingError('Failed to fetch grading status');
          setGradingLoading(false);
          return;
        }
        // If completed, get result
        if (statusData.status === 'completed' || statusData.status === 'COMPLETED' || statusData.status === 'done' || statusData === 'completed') {
          const resultResp = await mlAPI.getGradingResult(params);
          if (resultResp.ok) {
            const resultData = await resultResp.json();
            setGradingResult(resultData);
          } else {
            setGradingResult(null);
            setGradingError('Failed to fetch grading result');
          }
          setGradingLoading(false);
        } else if (statusData.status === 'failed' || statusData.status === 'FAILED' || statusData === 'failed') {
          setGradingError('Autograding failed');
          setGradingLoading(false);
        } else {
          // Poll every 5s if not completed
          polling = setTimeout(fetchGrading, 5000);
          setGradingLoading(false);
        }
      } catch (err) {
        setGradingError('Error fetching grading status/result');
        setGradingLoading(false);
      }
    };
    if (submission && user && submission.labId) {
      fetchGrading();
    }
    return () => { if (polling) clearTimeout(polling); };
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
        Submission for: "{lab?.title || `Lab #${submission.labId}`}"
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

      {/* Autograding Results Section */}
      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow mb-8">
        <h2 className="text-2xl font-semibold mb-4">Autograding Results</h2>
        {gradingLoading ? (
          <div className="flex items-center space-x-2 text-blue-500"><Spinner className="w-5 h-5" /> <span>Checking autograding status...</span></div>
        ) : gradingError ? (
          <div className="text-red-500">{gradingError}</div>
        ) : gradingStatus ? (
          <div className="mb-2">
            <span className="font-medium">Status:</span> {gradingStatus}
          </div>
        ) : (
          <div className="text-gray-500">No autograding information available.</div>
        )}
        {gradingResult && (
          <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-700 rounded">
            <pre className="whitespace-pre-wrap text-sm">{JSON.stringify(gradingResult, null, 2)}</pre>
          </div>
        )}
      </div>

      {toast.show && (
        <ToastNotification 
          message={toast.message}
          type={toast.type}
          onClose={() => setToast({ show: false, message: "", type: "" })}
        />
      )}
    </div>
  );
};

export default SubmissionPage;