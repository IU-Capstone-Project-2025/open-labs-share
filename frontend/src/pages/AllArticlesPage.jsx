import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ArticleCard from "../components/ArticleCard";

import { articlesAPI } from "../utils/api";

export default function AllArticlesPage() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchArticles = async () => {
      try {
        setLoading(true);
        const response = await articlesAPI.getArticles(1, 100);
        setArticles(response.articles || []);
        
      } catch (err) {
        console.error('Error fetching articles:', err);
        setError('Failed to load articles. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchArticles();
  }, []);

  const handleCreateArticle = () => {
    navigate('/create-article');
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
            <h1 className="text-4xl font-extrabold font-display text-gray-900 dark:text-white tracking-tight mb-8">All Articles</h1>
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
            All Articles
          </h1>
          <button
            onClick={handleCreateArticle}
            className="inline-flex items-center px-4 py-2 bg-msc text-white dark:bg-white dark:text-msc text-sm font-medium rounded-lg hover:bg-msc-hover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-msc transition-colors"
          >
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
            Create Article
          </button>
        </div>
        
        {articles.length === 0 ? (
          <div className="text-center py-12">
            <div className="max-w-md mx-auto">
              <div className="text-6xl mb-4">📄</div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">No Articles Available</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                There are no articles to display at the moment. Why not be the first to create one?
              </p>
              <button
                onClick={handleCreateArticle}
                className="inline-flex items-center px-6 py-3 bg-msc text-white dark:bg-white dark:text-msc font-medium rounded-lg hover:bg-msc-hover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-msc transition-colors"
              >
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                Create Your First Article
              </button>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {articles.map((article) => (
              <ArticleCard key={article.id || article.article_id} article={article} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
} 