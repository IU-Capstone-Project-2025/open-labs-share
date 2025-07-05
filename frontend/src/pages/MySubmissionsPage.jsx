import { useState, useEffect } from 'react';
import { submissionsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import SubmissionCard from '../components/SubmissionCard';
import Spinner from '../components/Spinner';

const mockSubmissions = [
  {
    id: 1,
    lab: { title: 'Data Structures Fundamentals !MOCK!' },
    createdAt: new Date().toISOString(),
    status: 'reviewed',
  },
  {
    id: 2,
    lab: { title: 'Advanced Algorithms !MOCK!' },
    createdAt: new Date().toISOString(),
    status: 'submitted',
  },
];

const MySubmissionsPage = () => {
  const [submissions, setSubmissions] = useState(mockSubmissions);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const user = useUser();

  /*
  useEffect(() => {
    const fetchSubmissions = async () => {
      if (!user) {
          setLoading(false);
          return;
      }
      try {
        setLoading(true);
        // This is inefficient. Ideally, we'd have a /api/v1/submissions/my endpoint
        const response = await submissionsAPI.getAllSubmissions();
        const allSubmissions = response.submissions || response || [];
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
  */

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
            <SubmissionCard key={submission.id} submission={submission} />
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