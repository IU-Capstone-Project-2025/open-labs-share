import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ArticleCard from "../components/ArticleCard";
import { articlesAPI } from "../utils/api";
import { getCurrentUser, isAuthenticated } from "../utils/auth";

export default function MyArticlesPage() {
  const [myArticles, setMyArticles] = useState([]);
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
    const fetchMyArticles = async () => {
      try {
        setLoading(true);
        const response = await articlesAPI.getMyArticles(1, 20); // page=1, limit=20
        setMyArticles(response.articles || []);
      } catch (err) {
        console.error('Error fetching my articles:', err);
        if (err.message.includes('401') || err.message.includes('Unauthorized')) {
          setError('Please log in to view your articles. You may need to sign in again.');
        } else {
          setError(`Failed to load your articles: ${err.message}`);
        }
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchMyArticles();
    }
  }, [user]);

  const handleCreateArticle = () => {
    navigate('/create-article');
  };

  if (!isAuthenticated()) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h1 className="text-4xl font-extrabold font-display text-gray-900 dark:text-white tracking-tight mb-8">My Articles</h1>
            <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-6 max-w-md mx-auto">
              <h2 className="text-lg font-semibold text-yellow-800 dark:text-yellow-200 mb-2">Authentication Required</h2>
              <p className="text-yellow-600 dark:text-yellow-300 mb-4">
                Please sign in to view your articles.
              </p>
              <button 
                onClick={() => navigate('/login')}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Sign In
              </button>
            </div>
          </div>
        </div>
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
            <h1 className="text-4xl font-extrabold font-display text-gray-900 dark:text-white tracking-tight mb-8">My Articles</h1>
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-6 max-w-md mx-auto">
              <h2 className="text-lg font-semibold text-red-800 dark:text-red-200 mb-2">Error Loading Articles</h2>
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
            My Articles
          </h1>
          <button
            onClick={handleCreateArticle}
            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
            </svg>
            Create Article
          </button>
        </div>
        
        {myArticles.length === 0 ? (
          <div className="text-center py-12">
            <div className="max-w-md mx-auto">
              <div className="text-6xl mb-4">📄</div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">No Articles Created Yet</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                You haven't created any articles yet. Start by creating your first one.
              </p>
              <button
                onClick={handleCreateArticle}
                className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
              >
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                </svg>
                Create Your First Article
              </button>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {myArticles.map((article) => (
              <ArticleCard key={article.id || article.article_id} article={article} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
