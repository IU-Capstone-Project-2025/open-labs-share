import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { MagnifyingGlassIcon } from "@heroicons/react/24/outline";

const Search = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <form onSubmit={handleSearch} className="w-64">
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <MagnifyingGlassIcon className="w-4 h-4 text-msc" />
        </div>
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search labs and articles..."
          className="block w-full pl-10 pr-3 py-2 border border-gray-200 dark:border-gray-700 rounded-md text-sm 
                    placeholder:text-light-blue dark:placeholder-gray-400
                    text-msc dark:text-white
                    bg-light-blue bg-opacity-55 dark:bg-gray-800
                    focus:outline-none focus:ring-1 focus:ring-msc"
        />
      </div>
    </form>
  );
};

export default Search;