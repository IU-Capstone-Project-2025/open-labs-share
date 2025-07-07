import { useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { labsAPI, submissionsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import SubmissionCard from '../components/SubmissionCard';
import Spinner from '../components/Spinner';

const MySubmissionsPage = () => {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    page: 1,
    limit: 20,
    totalCount: 0
  });
  const user = useUser();

  useEffect(() => {
    const fetchSubmissionsWithLabs = async () => {
      if (!user) {
        setLoading(false);
        return;
      }
      
      try {
        setLoading(true);
        const response = await submissionsAPI.getMySubmissions(pagination.page, pagination.limit);
        const submissionsData = response.submissions || [];
        
        const labIds = [...new Set(submissionsData.map(s => s.labId))];
        const labsResponse = await Promise.all(
          labIds.map(id => labsAPI.getLabById(id).catch(() => null))
        );
        
        const labsMap = labsResponse.reduce((acc, lab) => {
          if (lab) acc[lab.id] = lab;
          return acc;
        }, {});

        const enrichedSubmissions = submissionsData.map(sub => ({
          ...sub,
          labTitle: labsMap[sub.labId]?.title
        }));

        setSubmissions(enrichedSubmissions);
        setPagination(prev => ({
          ...prev,
          totalCount: response.totalCount || 0
        }));
      } catch (err) {
        setError('Failed to fetch submissions.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchSubmissionsWithLabs();
  }, [user, pagination.page, pagination.limit]);

  const handleDelete = async (submissionId) => {
    try {
      await submissionsAPI.deleteSubmission(submissionId);
      setSubmissions(prev => prev.filter(sub => sub.submissionId !== submissionId));
      setPagination(prev => ({
        ...prev,
        totalCount: prev.totalCount - 1
      }));
    } catch (err) {
      setError('Failed to delete submission. Please try again.');
      console.error('Error deleting submission:', err);
    }
  };

  const handlePageChange = (newPage) => {
    setPagination(prev => ({
      ...prev,
      page: newPage
    }));
  };

  if (loading && pagination.page === 1) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center text-red-500 mt-8">{error}</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-8">My Submissions</h1>
      
      {submissions.length > 0 ? (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            {submissions.map(submission => (
              <SubmissionCard 
                key={submission.submissionId} 
                submission={submission} 
                onDelete={() => handleDelete(submission.submissionId)} 
              />
            ))}
          </div>
        </>
      ) : (
        <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
          <p>You haven't submitted any solutions yet.</p>
        </div>
      )}
    </div>
  );
};

export default MySubmissionsPage;