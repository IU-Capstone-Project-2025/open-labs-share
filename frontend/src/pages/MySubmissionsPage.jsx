import { useState, useEffect } from 'react';
import { submissionsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import SubmissionCard from '../components/SubmissionCard';
import Spinner from '../components/Spinner';

const MySubmissionsPage = () => {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const user = useUser();

  useEffect(() => {
    const fetchSubmissions = async () => {
      if (!user) {
        setLoading(false);
        return;
      }
      try {
        setLoading(true);
        const response = await submissionsAPI.getAllSubmissions();
        const allSubmissions = response.submissions || response.data?.submissions || response.data || response || [];
        const mySubmissions = allSubmissions.filter(sub => sub.ownerId === user.id);
        setSubmissions(mySubmissions);
      } catch (err) {
        setError('Failed to fetch submissions.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchSubmissions();
  }, [user]);

  const handleDelete = async (submissionId) => {
    const originalSubmissions = [...submissions];
    setSubmissions(submissions.filter(s => s.id !== submissionId));

    try {
      await submissionsAPI.deleteSubmission(submissionId);
    } catch (err) {
      setSubmissions(originalSubmissions);
      setError('Failed to delete submission. Please try again.');
      console.error(err);
    }
  };

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-8">My Submissions</h1>
      {submissions.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {submissions.map(submission => (
            <SubmissionCard key={submission.id} submission={submission} onDelete={() => handleDelete(submission.id)} />
          ))}
        </div>
      ) : (
        <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
          <p>You haven't submitted any solutions yet.</p>
        </div>
      )}
    </div>
  );
};

export default MySubmissionsPage; 