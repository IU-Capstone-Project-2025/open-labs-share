import { Link } from "react-router-dom";

export default function LabCard({ lab }) {
  return (
    <Link
      to={`/lab/${lab.id}`}
      className="block bg-light-blue bg-opacity-50 dark:bg-gray-800 rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 p-4"
    >
      <div className="space-y-2">
        <h3 className="text-lg font-bold text-msc dark:text-white line-clamp-1">
          {lab.title}
        </h3>

        <p className="text-msc dark:text-gray-300 text-sm line-clamp-2">
          {lab.abstract || lab.description || 'No description available'}
        </p>

        <div className="flex items-center justify-between pt-2">
          <div className="flex items-center">
            <div className="w-6 h-6 rounded-full bg-msc flex items-center justify-center text-white text-xs font-medium mr-2">
              {lab.author?.firstName?.[0] || 'U'}
              {lab.author?.lastName?.[0] || ''}
            </div>
            <span className="text-base text-msc font-semibold dark:text-gray-300">
              {lab.author?.firstName && lab.author?.lastName 
                ? `${lab.author.firstName} ${lab.author.lastName}`
                : 'Unknown Author'
              }
            </span>
          </div>
          <div className="flex items-center space-x-4 text-xs text-gray-500 dark:text-gray-400">
            {lab.views !== undefined && (
              <span className="flex items-center">
                <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                  <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd"/>
                </svg>
                {lab.views}
              </span>
            )}
            {lab.submissions !== undefined && (
              <span className="flex items-center">
                <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 4a1 1 0 011-1h12a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1V8z" clipRule="evenodd"/>
                </svg>
                {lab.submissions}
              </span>
            )}
          </div>
        </div>
      </div>
    </Link>
  );
}
