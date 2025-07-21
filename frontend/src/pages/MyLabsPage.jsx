import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import LabCard from "../components/LabCard";
import { labsAPI, marimoAPI } from "../utils/api";
import { getCurrentUser, isAuthenticated } from "../utils/auth";
import { TrashIcon } from '@heroicons/react/24/outline';

export default function MyLabsPage() {
  const [myLabs, setMyLabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [user, setUser] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [labToDelete, setLabToDelete] = useState(null);
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
        const response = await labsAPI.getMyLabs(1, 20);
        setMyLabs(response.labs || []);
      } catch (err) {
        console.error('Error fetching my labs:', err);
        if (err.message.includes('401') || err.message.includes('Unauthorized')) {
          setError('Please log in to view your labs. You may need to sign in again.');
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

  const handleCreateLab = () => {
    navigate('/create-lab');
  };

  const handleDeleteClick = (e, lab) => {
    e.preventDefault();
    e.stopPropagation();
    setLabToDelete(lab);
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = async () => {
    if (!labToDelete) return;

    try {
      const deletedLabId = labToDelete.id || labToDelete.lab_id;
      console.log('Deleting lab with ID:', deletedLabId);
      console.log('Lab to delete:', labToDelete);
      
      // Step 1: Delete marimo components and their assets
      try {
        const marimoComponents = await marimoAPI.getComponentsByContent('lab', deletedLabId);
        console.log('Found marimo components for lab:', marimoComponents);
        
        if (marimoComponents && marimoComponents.length > 0) {
          for (const component of marimoComponents) {
            console.log('Deleting marimo component:', component.id);
            
            try {
              // Delete component assets first (if any)
              if (component.assets && component.assets.length > 0) {
                for (const asset of component.assets) {
                  try {
                    await marimoAPI.deleteAsset(asset.id);
                    console.log('Deleted asset:', asset.id);
                  } catch (assetError) {
                    console.warn('Failed to delete asset:', asset.id, assetError);
                  }
                }
              }
              
              // Then delete the component itself
              await marimoAPI.deleteComponent(component.id);
              console.log('Deleted marimo component:', component.id);
            } catch (componentError) {
              console.error('Error deleting marimo component:', component.id, componentError);
              // Continue with other components even if this one fails
            }
          }
        }
      } catch (marimoError) {
        console.warn('Error cleaning up marimo components:', marimoError);
        // Continue with lab deletion even if marimo cleanup fails
      }
      
      // Step 2: Delete the lab itself
      await labsAPI.deleteLab(deletedLabId);
      console.log('Before filter - labs count:', myLabs.length);
      setMyLabs(prev => {
        const filtered = prev.filter(lab => (lab.id || lab.lab_id) !== deletedLabId);
        console.log('After filter - labs count:', filtered.length);
        console.log('Filtered labs:', filtered);
        return filtered;
      });
      setShowDeleteModal(false);
      setLabToDelete(null);
    } catch (error) {
      console.error('Error deleting lab:', error);
      setError('Failed to delete lab. Please try again.');
    }
  };

  const handleCancelDelete = () => {
    setShowDeleteModal(false);
    setLabToDelete(null);
  };

  const ConfirmationModal = () => {
    if (!showDeleteModal) return null;

    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md w-full animate-fade-in">
          <p className="text-gray-800 dark:text-gray-200 mb-4">
            Are you sure you want to delete the lab "{labToDelete?.title}"?
          </p>
          <div className="flex justify-end space-x-3">
            <button
              onClick={handleCancelDelete}
              className="px-4 py-2 text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmDelete}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    );
  };

  if (!isAuthenticated()) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h1 className="text-4xl font-extrabold font-display text-gray-900 dark:text-white tracking-tight mb-8">My Labs</h1>
            <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-6 max-w-md mx-auto">
              <h2 className="text-lg font-semibold text-yellow-800 dark:text-yellow-200 mb-2">Authentication Required</h2>
              <p className="text-yellow-600 dark:text-yellow-300 mb-4">
                Please sign in to view your labs.
              </p>
              <button 
                onClick={() => navigate('/login')}
                className="px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors"
              >
                Sign In
              </button>
            </div>
          </div>
        </div>
        <ConfirmationModal />
      </div>
    );
  }

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
            <h1 className="text-4xl font-extrabold font-display text-gray-900 dark:text-white tracking-tight mb-8">My Labs</h1>
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
        <ConfirmationModal />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-4xl font-bold font-display text-gray-900 dark:text-white">
            My Labs
          </h1>
          <button
            onClick={handleCreateLab}
            className="inline-flex items-center px-4 py-2 bg-msc text-white dark:bg-white dark:text-msc text-sm font-medium rounded-lg hover:bg-msc-hover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-msc transition-colors"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
            </svg>
            Create Lab
          </button>
        </div>
        
        {myLabs.length === 0 ? (
          <div className="text-center py-12">
            <div className="max-w-md mx-auto">
              <div className="text-6xl mb-4">🔬</div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">No Labs Created Yet</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                You haven't created any labs yet. Start by creating your first one.
              </p>
              <button
                onClick={handleCreateLab}
                className="inline-flex items-center px-6 py-3 bg-msc text-white dark:bg-white dark:text-msc font-medium rounded-lg hover:bg-msc-hover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-msc transition-colors"
              >
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                </svg>
                Create Your First Lab
              </button>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {myLabs.map((lab) => (
              <div key={lab.id || lab.lab_id} className="relative group">
                <LabCard lab={lab} />
                <button
                  onClick={(e) => handleDeleteClick(e, lab)}
                  className="absolute top-2 right-2 p-1.5 bg-gray-200 dark:bg-gray-700 rounded-full text-gray-600 dark:text-gray-300 hover:bg-red-200 dark:hover:bg-red-800 hover:text-red-600 dark:hover:text-red-200 transition-colors opacity-0 group-hover:opacity-100"
                  aria-label="Delete lab"
                >
                  <TrashIcon className="w-5 h-5" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
      <ConfirmationModal />
    </div>
  );
}
