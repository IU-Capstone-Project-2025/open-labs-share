import { useState, useEffect } from 'react';
import { submissionsAPI } from '../utils/api';
import SubmissionCard from '../components/SubmissionCard';
import Spinner from '../components/Spinner';

const mockReviewSubmissions = [
    {
        id: 3,
        lab: { title: 'Introduction to AI !MOCK!' },
        createdAt: new Date().toISOString(),
        status: 'submitted',
    },
    {
        id: 4,
        lab: { title: 'Machine Learning Basics !MOCK!' },
        createdAt: new Date().toISOString(),
        status: 'submitted',
    },
    {
        id: 5,
        lab: { title: 'Web Development 101 !MOCK!' },
        createdAt: new Date().toISOString(),
        status: 'submitted',
    },
];

const ReviewSubmissionsPage = () => {
  const [submissions, setSubmissions] = useState(mockReviewSubmissions);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  /*
  useEffect(() => {
    const fetchSubmissions = async () => {
      try {
        setLoading(true);
        const response = await submissionsAPI.getAllSubmissions();
        const allSubmissions = response.submissions || response || [];
        // Later, we might filter this to show only submissions needing review
        setSubmissions(allSubmissions);
      } catch (err) {
        setError('Failed to fetch submissions for review.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchSubmissions();
  }, []);
  */

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-8">Review Submissions</h1>
      {submissions.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {submissions.map(submission => (
            <SubmissionCard key={submission.id} submission={submission} />
          ))}
        </div>
      ) : (
        <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
          <p>There are no submissions available for review right now.</p>
        </div>
      )}
    </div>
  );
};

export default ReviewSubmissionsPage; 