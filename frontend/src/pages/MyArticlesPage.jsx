import { useState, useEffect } from "react";
import ArticleCard from "../components/ArticleCard";
import { getCurrentUser, isAuthenticated } from "../utils/auth";
// Note: articlesAPI is currently commented out in api.js
// This code is prepared for when articles service is connected
import { articlesAPI } from "../utils/api";

export default function MyArticles() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [user, setUser] = useState(null);

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
        const response = await articlesAPI.getMyArticles();
        setArticles(response.articles || []);
        
      } catch (err) {
        console.error('Error fetching my articles:', err);
        setError('Failed to load your articles');
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchMyArticles();
    }
  }, [user]);

  if (!isAuthenticated()) {
    return (
      <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
        <div className="max-w-6xl mx-auto">
          <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
              My articles
            </h1>
            <div className="text-center py-8">
              <p className="text-gray-600 dark:text-gray-400">Please sign in to view your articles.</p>
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
            <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
              My articles
            </h1>
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
            <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
              My articles
            </h1>
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
        <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
          My articles
        </h1>

        {articles.length === 0 ? (
          <div className="text-center py-8">
            <p className="text-gray-600 dark:text-gray-400">You haven't created any articles yet.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {articles.map((article) => (
              <ArticleCard key={article.id} article={article} />
            ))}
          </div>
        )}
        </div>
      </div>
    </div>
  );
}
