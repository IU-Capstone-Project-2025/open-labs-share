import { useState, useEffect } from 'react';
import { Link } from "react-router-dom";
import { TagsList } from "../components/Tags";
import { tagsAPI } from '../utils/api';

export default function LabCard({ lab }) {
  const [labTags, setLabTags] = useState([]);
  const [loadingTags, setLoadingTags] = useState(false);

  useEffect(() => {
    const fetchLabTags = async () => {
      if (lab.tags && lab.tags.length > 0) {
        const hasTagObjects = lab.tags.some(tag => typeof tag === 'object' && tag.name);
        
        if (hasTagObjects) {
          setLabTags(lab.tags);
        } else {
          const tagIds = lab.tags.filter(tag => typeof tag === 'number' || !isNaN(tag));
          
          if (tagIds.length > 0) {
            try {
              setLoadingTags(true);
              const response = await tagsAPI.getTagsByIds(tagIds);
              setLabTags(response.tags || []);
            } catch (error) {
              console.error('Error fetching tags for lab:', error);
              setLabTags([]);
            } finally {
              setLoadingTags(false);
            }
          }
        }
      }
    };

    fetchLabTags();
  }, [lab.tags]);

  const renderTags = () => {
    if (loadingTags) {
      return (
        <div className="flex items-center space-x-2">
          <div className="animate-pulse bg-gray-300 h-4 w-12 rounded"></div>
          <div className="animate-pulse bg-gray-300 h-4 w-16 rounded"></div>
        </div>
      );
    }

    if (labTags && labTags.length > 0) {
      return (
        <div className="flex flex-wrap gap-2">
          {labTags.map((tag, index) => (
            <span
              key={tag.id || tag}
              className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
              title={typeof tag === 'object' ? tag.description : ''}
            >
              {typeof tag === 'object' ? tag.name : `Tag ${tag}`}
            </span>
          ))}
        </div>
      );
    }

    return <span className="text-gray-500 text-sm">No tags</span>;
  };

  return (
    <Link
      to={`/lab/${lab.id}`}
      className="block bg-light-blue bg-opacity-50 dark:bg-gray-800 rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 p-4 h-full flex flex-col"
    >
      <div className="flex-grow space-y-2">
        <h3 className="text-lg font-bold text-msc dark:text-white line-clamp-1">
          {lab.title}
        </h3>

        <p className="text-msc dark:text-gray-300 text-sm line-clamp-2">
          {lab.shortDesc || lab.abstract || lab.description || 'No description available'}
        </p>
      </div>

      <div className="mt-auto pt-4 space-y-3">
        <div className="py-1">
          {renderTags()}
        </div>

        <div className="flex items-center justify-between border-gray-200 dark:border-gray-700 pt-2">
          <div className="flex items-center">
            <div className="w-6 h-6 rounded-full bg-msc flex items-center justify-center text-white text-xs font-medium mr-2">
              {lab.authorName?.[0] || 'U'}
              {lab.authorSurname?.[0] || ''}
            </div>
            <span className="text-base text-msc font-semibold dark:text-gray-300">
              {lab.authorName && lab.authorSurname 
                ? `${lab.authorName} ${lab.authorSurname}`
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