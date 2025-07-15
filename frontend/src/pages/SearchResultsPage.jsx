import { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { MagnifyingGlassIcon } from "@heroicons/react/24/outline";
import { labsAPI, articlesAPI } from "../utils/api";
import LabCard from "../components/LabCard";
import ArticleCard from "../components/ArticleCard";

export default function SearchResultsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [labsResults, setLabsResults] = useState([]);
  const [articlesResults, setArticlesResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState("all");

  useEffect(() => {
    const queryParams = new URLSearchParams(location.search);
    const query = queryParams.get("q") || "";
    setSearchQuery(query);
    
    if (query) {
      performSearch(query);
    }
  }, [location.search]);

  const performSearch = async (query) => {
    try {
      setLoading(true);
      setError(null);
      
      const [labsResponse, articlesResponse] = await Promise.all([
        labsAPI.searchLabs(query),
        articlesAPI.searchArticles(query)
      ]);

      setLabsResults(labsResponse.labs || []);
      setArticlesResults(articlesResponse.articles || []);
    } catch (err) {
      console.error("Search error:", err);
      setError("Failed to perform search. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Search Bar */}
        <div className="mb-8">
          <form onSubmit={handleSearch} className="flex gap-2">
            <div className="relative flex-grow">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <MagnifyingGlassIcon className="h-5 w-5 text-msc" />
              </div>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search labs and articles..."
                className="block w-full pl-10 pr-3 py-2 border border-light-blue border-opacity-55 rounded-md leading-5 bg-light-blue bg-opacity-55 dark:bg-gray-800 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-msc focus:border-msc sm:text-sm"
              />
            </div>
            <button
              type="submit"
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-msc dark:bg-white dark:text-msc hover:bg-msc-hover focus:outline-none focus:ring-2 focus:ring-offset-2"
            >
              Search
            </button>
          </form>
        </div>

        {/* Results */}
        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-msc"></div>
          </div>
        ) : error ? (
          <div className="bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800 rounded-lg p-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        ) : (
          <>
            {/* Tabs */}
            <div className="flex border-b border-gray-200 dark:border-gray-700 mb-6">
              <button
                className={`py-4 px-6 font-medium text-sm border-b-2 ${activeTab === "all" ? "border-msc-hover text-msc dark:text-blue-400" : "border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300"}`}
                onClick={() => setActiveTab("all")}
              >
                All Results
              </button>
              <button
                className={`py-4 px-6 font-medium text-sm border-b-2 ${activeTab === "labs" ? "border-msc-hover text-msc dark:text-blue-400" : "border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300"}`}
                onClick={() => setActiveTab("labs")}
              >
                Labs ({labsResults.length})
              </button>
              <button
                className={`py-4 px-6 font-medium text-sm border-b-2 ${activeTab === "articles" ? "border-msc-hover text-msc dark:text-blue-400" : "border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300"}`}
                onClick={() => setActiveTab("articles")}
              >
                Articles ({articlesResults.length})
              </button>
            </div>

            {/* Results Content */}
            {(activeTab === "all" || activeTab === "labs") && labsResults.length > 0 && (
              <div className="mb-8">
                <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Labs</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {labsResults.map((lab) => (
                    <LabCard key={lab.id} lab={lab} />
                  ))}
                </div>
              </div>
            )}

            {(activeTab === "all" || activeTab === "articles") && articlesResults.length > 0 && (
              <div className="mb-8">
                <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Articles</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {articlesResults.map((article) => (
                    <ArticleCard key={article.id} article={article} />
                  ))}
                </div>
              </div>
            )}

            {labsResults.length === 0 && articlesResults.length === 0 && (
              <div className="text-center py-12">
                <div className="text-gray-500 dark:text-gray-400">
                  No results found for "{searchQuery}"
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}