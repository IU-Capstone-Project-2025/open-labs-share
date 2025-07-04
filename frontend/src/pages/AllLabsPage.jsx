import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import LabCard from "../components/LabCard";
import { labsAPI } from "../utils/api";

export default function AllLabsPage() {
  const [labsData, setLabsData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchLabs = async () => {
      try {
        setLoading(true);
        console.log('AllLabsPage: Fetching labs from backend API...');
        const response = await labsAPI.getLabs();
        console.log('AllLabsPage: Received response:', response);
        console.log('AllLabsPage: Response type:', typeof response);
        console.log('AllLabsPage: Response.labs:', response.labs);
        console.log('AllLabsPage: Response.labs type:', typeof response.labs);
        console.log('AllLabsPage: Response.labs length:', response.labs ? response.labs.length : 'null/undefined');
        
        // Handle different possible response structures
        const labsArray = response.labs || response || [];
        console.log('AllLabsPage: Processed labs array:', labsArray);
        console.log('AllLabsPage: Processed labs array length:', labsArray.length);
        setLabsData(labsArray);
        
      } catch (err) {
        console.error('AllLabsPage: Error fetching labs:', err);
        if (err.message.includes('401') || err.message.includes('Unauthorized')) {
          setError('Please log in to view labs. You may need to sign in again.');
        } else if (err.message.includes('ERR_BLOCKED_BY_CLIENT')) {
          setError('Request blocked by browser. Please disable ad blockers or try a different browser.');
        } else if (err.message.includes('500') || err.message.includes('Internal Server Error')) {
          setError('Backend services are starting up. Please wait a moment and try again.');
        } else {
          setError(`Failed to load labs: ${err.message}`);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchLabs();
  }, []);

  const handleCreateLab = () => {
    navigate('/create-lab');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen dark:bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-msc"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h1 className="text-4xl font-extrabold font-display text-gray-900 dark:text-white tracking-tight mb-8">All Labs</h1>
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-6 max-w-md mx-auto">
              <h2 className="text-lg font-semibold text-red-800 dark:text-red-200 mb-2">Error Loading Labs</h2>
              <p className="text-red-600 dark:text-red-300 mb-4">{error}</p>
              <button 
                onClick={() => window.location.reload()}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
              >
                Retry
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-4xl font-bold font-display text-gray-900 dark:text-white">
            All Labs
          </h1>
          <button
            onClick={handleCreateLab}
            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            <span className="mr-2">+</span>
            Create Lab
          </button>
        </div>
        
        {labsData.length === 0 ? (
          <div className="text-center py-12">
            <div className="max-w-md mx-auto">
              <div className="text-6xl mb-4">📚</div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">No Labs Available</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                No labs have been created yet. Be the first to share your knowledge!
              </p>
              <button
                onClick={handleCreateLab}
                className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
              >
                <span className="mr-2">+</span>
                Create Your First Lab
              </button>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {labsData.map((lab) => (
              <LabCard key={lab.id || lab.lab_id} lab={lab} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
} 
