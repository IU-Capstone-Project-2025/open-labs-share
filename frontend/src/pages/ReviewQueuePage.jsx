import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { submissionsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { DocumentTextIcon, ClockIcon } from '@heroicons/react/24/outline';

const SubmissionCard = ({ submission }) => {
  const { submissionId, lab, owner, createdAt } = submission;
  const labTitle = lab?.title || `Lab ${submission.labId}`;

  return (
    // исправить 2 на id
    <Link to={`/feedback/${2}`} className="block group">
      <div className="p-6 bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 border border-gray-200 dark:border-gray-700">
        <div className="flex items-center mb-4">
          <DocumentTextIcon className="w-8 h-8 text-blue-500 dark:text-blue-400 mr-4" />
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              {labTitle}
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Submitted by: {owner?.name} {owner?.surname}
            </p>
          </div>
        </div>
        <div className="flex justify-between items-center text-sm text-gray-600 dark:text-gray-300">
          <div className="flex items-center">
            <ClockIcon className="w-5 h-5 mr-1 text-gray-400" />
            <span>Submitted on: {new Date(createdAt).toLocaleDateString()}</span>
          </div>
        </div>
      </div>
    </Link>
  );
};

const ReviewQueuePage = () => {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const user = useUser();

  useEffect(() => {
    const fetchSubmissionsForReview = async () => {
      if (!user) {
        setLoading(false);
        return;
      }
      try {
        setLoading(true);
        //сабмишены для лабы 2
        const response = await submissionsAPI.getLabSubmissions(2);
        
        const submissionsData = response.submissions || response.data?.submissions || response.data || response || [];
        
        const reviewableSubmissions = submissionsData.filter(
          sub => sub.status.toLowerCase() === 'submitted' && sub.owner?.id !== user.id
        );
        
        setSubmissions(submissionsData);
      } catch (err) {
        setError('Failed to fetch submissions for review.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchSubmissionsForReview();
  }, [user]);

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-8">
        Review Queue for Lab 2
      </h1>
      {submissions.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {submissions.map(submission => (
            <SubmissionCard key={submission.submissionId} submission={submission} />
          ))}
        </div>
      ) : (
        <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
          <p>There are no submissions waiting for review for this lab.</p>
        </div>
      )}
    </div>
  );
};

export default ReviewQueuePage;