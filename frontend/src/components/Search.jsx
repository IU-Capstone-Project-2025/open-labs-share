import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { MagnifyingGlassIcon } from "@heroicons/react/24/outline";
import { labsAPI, articlesAPI } from "../utils/api";

const Search = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const [labsResults, setLabsResults] = useState([]);
  const [articlesResults, setArticlesResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (searchQuery.trim().length > 2) {
      const timer = setTimeout(() => {
        searchContent();
      }, 300);

      return () => clearTimeout(timer);
    } else {
      setLabsResults([]);
      setArticlesResults([]);
    }
  }, [searchQuery]);

  const searchContent = async () => {
    try {
      setIsLoading(true);
      setShowResults(true);
      
      const query = searchQuery.trim().toLowerCase();
      
      const [labsResponse, articlesResponse] = await Promise.all([
        labsAPI.getLabs(1, 100),
        articlesAPI.getArticles(1, 100)
      ]);

      const filteredLabs = (labsResponse.labs || []).filter(lab => 
        lab.title.toLowerCase().includes(query) ||
        (lab.shortDesc && lab.shortDesc.toLowerCase().includes(query)) ||
        (lab.tags && lab.tags.some(tag => tag.toLowerCase().includes(query)))
      );

      const filteredArticles = (articlesResponse.articles || []).filter(article => 
        article.title.toLowerCase().includes(query) ||
        (article.shortDesc && article.shortDesc.toLowerCase().includes(query)) ||
        (article.tags && article.tags.some(tag => tag.toLowerCase().includes(query)))
      );

      setLabsResults(filteredLabs.slice(0, 5));
      setArticlesResults(filteredArticles.slice(0, 5));
    } catch (error) {
      console.error("Search error:", error);
      setLabsResults([]);
      setArticlesResults([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResultClick = (type, id) => {
    setSearchQuery("");
    setShowResults(false);
    navigate(`/${type}/${id}`);
  };

  return (
    <div className="relative w-64">
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <MagnifyingGlassIcon className="w-4 h-4 text-msc" />
        </div>
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onFocus={() => searchQuery.trim().length > 0 && setShowResults(true)}
          onBlur={() => setTimeout(() => setShowResults(false), 200)}
          placeholder="Search labs and articles..."
          className="block w-full pl-10 pr-3 py-2 border border-gray-200 rounded-md text-sm 
                    placeholder:text-light-blue
                    text-msc
                    bg-light-blue bg-opacity-55
                    focus:outline-none focus:ring-1 focus:ring-msc"
        />
      </div>

      {showResults && searchQuery && (
        <div className="absolute z-50 mt-1 w-full bg-white dark:bg-gray-800 shadow-lg rounded-md border border-gray-200 dark:border-gray-700 max-h-96 overflow-y-auto">
          {isLoading ? (
            <div className="p-4 text-center text-gray-500 dark:text-gray-400">
              Searching...
            </div>
          ) : (
            <>
              {/* Labs results */}
              <div className="border-b border-gray-200 dark:border-gray-700">
                <div className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700">
                  Labs ({labsResults.length})
                </div>
                {labsResults.length > 0 ? (
                  labsResults.map((lab) => (
                    <div
                      key={lab.id}
                      className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
                      onClick={() => handleResultClick("lab", lab.id)}
                    >
                      <div className="font-medium">{lab.title}</div>
                      {lab.shortDesc && (
                        <div className="text-xs text-gray-500 dark:text-gray-400 truncate">
                          {lab.shortDesc}
                        </div>
                      )}
                      {lab.tags && lab.tags.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-1">
                          {lab.tags.slice(0, 3).map((tag, i) => (
                            <span 
                              key={i} 
                              className="text-xs px-1.5 py-0.5 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200"
                            >
                              {tag}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))
                ) : (
                  <div className="px-4 py-2 text-sm text-gray-500 dark:text-gray-400">
                    No matching labs found
                  </div>
                )}
              </div>

              {/* Articles results */}
              <div className="border-b border-gray-200 dark:border-gray-700">
                <div className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700">
                  Articles ({articlesResults.length})
                </div>
                {articlesResults.length > 0 ? (
                  articlesResults.map((article) => (
                    <div
                      key={article.id}
                      className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
                      onClick={() => handleResultClick("article", article.id)}
                    >
                      <div className="font-medium">{article.title}</div>
                      {article.shortDesc && (
                        <div className="text-xs text-gray-500 dark:text-gray-400 truncate">
                          {article.shortDesc}
                        </div>
                      )}
                      {article.tags && article.tags.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-1">
                          {article.tags.slice(0, 3).map((tag, i) => (
                            <span 
                              key={i} 
                              className="text-xs px-1.5 py-0.5 rounded-full bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200"
                            >
                              {tag}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))
                ) : (
                  <div className="px-4 py-2 text-sm text-gray-500 dark:text-gray-400">
                    No matching articles found
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default Search;