import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import LabCard from "../components/LabCard";
import { labsAPI } from "../utils/api";
import { getCurrentUser, isAuthenticated } from "../utils/auth";

export default function MyLabs() {
  const [myLabs, setMyLabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated()) {
      const currentUser = getCurrentUser();
      setUser(currentUser);
    }
  }, []);

  useEffect(() => {
    const fetchMyLabs = async () => {
      try {
        setLoading(true);
        console.log('MyLabsPage: Fetching labs for current user:', user);
        
        // Use the dedicated API endpoint for user's labs
        const response = await labsAPI.getMyLabs();
        console.log('MyLabsPage: My labs response:', response);
        
        const userLabs = response.labs || response || [];
        console.log('MyLabsPage: User labs array:', userLabs);
        
        setMyLabs(userLabs);
      } catch (err) {
        console.error('Error fetching my labs:', err);
        if (err.message.includes('401') || err.message.includes('Unauthorized')) {
          setError('Please log in to view your labs. You may need to sign in again.');
        } else if (err.message.includes('ERR_BLOCKED_BY_CLIENT')) {
          setError('Request blocked by browser. Please disable ad blockers or try a different browser.');
        } else if (err.message.includes('500') || err.message.includes('Internal Server Error')) {
          setError('Backend services are starting up. Please wait a moment and try again.');
        } else {
          setError(`Failed to load your labs: ${err.message}`);
        }
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchMyLabs();
    }
  }, [user]);

  if (!isAuthenticated()) {
    return (
      <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
        <div className="max-w-6xl mx-auto">
          <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="flex justify-between items-center mb-6">
              <h1 className="text-3xl font-bold text-msc dark:text-white">
                My labs
              </h1>
              <button
                onClick={() => navigate('/create-lab')}
                className="px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors flex items-center space-x-2"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                <span>Create Lab</span>
              </button>
            </div>
            <div className="text-center py-8">
              <p className="text-gray-600 dark:text-gray-400">Please sign in to view your labs.</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
        <div className="max-w-6xl mx-auto">
          <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="flex justify-between items-center mb-6">
              <h1 className="text-3xl font-bold text-msc dark:text-white">
                My labs
              </h1>
              <button
                onClick={() => navigate('/create-lab')}
                className="px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors flex items-center space-x-2"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                <span>Create Lab</span>
              </button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
              {Array(3).fill(null).map((_, index) => (
                <div
                  key={`loading-${index}`}
                  className="h-32 bg-light-blue bg-opacity-40 dark:bg-gray-700 animate-pulse rounded-xl"
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
        <div className="max-w-6xl mx-auto">
          <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="flex justify-between items-center mb-6">
              <h1 className="text-3xl font-bold text-msc dark:text-white">
                My labs
              </h1>
              <button
                onClick={() => navigate('/create-lab')}
                className="px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors flex items-center space-x-2"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                <span>Create Lab</span>
              </button>
            </div>
            <div className="text-center py-8">
              <p className="text-red-500 mb-4">{error}</p>
              <button 
                onClick={() => window.location.reload()}
                className="px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors"
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
    <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="max-w-6xl mx-auto">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-msc dark:text-white">
            My labs
          </h1>
          <button
            onClick={() => navigate('/create-lab')}
            className="px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors flex items-center space-x-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            <span>Create Lab</span>
          </button>
        </div>

        {myLabs.length === 0 ? (
          <div className="text-center py-12">
            <div className="mb-4">
              <svg className="mx-auto h-16 w-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 48 48">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7h6m0 0v12m0-12l-6 6m6-6l6 6" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">No labs created yet</h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">Start sharing your knowledge by creating your first lab!</p>
            <button
              onClick={() => navigate('/create-lab')}
              className="px-6 py-3 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors flex items-center mx-auto space-x-2"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              <span>Create Your First Lab</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {myLabs.map((lab) => (
              <LabCard key={lab.id} lab={lab} />
            ))}
          </div>
        )}
        </div>
      </div>
    </div>
  );
}
