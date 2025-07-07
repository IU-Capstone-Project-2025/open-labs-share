import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { labsAPI, submissionsAPI } from '../utils/api';
import Spinner from '../components/Spinner';

const SubmissionPage = () => {
  const { id } = useParams();
  const [submission, setSubmission] = useState([null]);
  const [lab, setLab] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
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
      
      {/* Placeholder for review functionality */}
      <div className="mt-8 bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
        <h2 className="text-2xl font-semibold mb-4">Review</h2>
        <p className="text-gray-500">Review functionality will be implemented here.</p>
      </div>
    </div>
  );
};

export default SubmissionPage; 