import { Link } from "react-router-dom";

export default function LabCard({ lab }) {
  return (
    <Link
      to={`/lab/${lab.id}`}
      className="block bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 p-4"
    >
      <div className="space-y-2">
        <h3 className="text-lg font-bold text-gray-800 dark:text-white line-clamp-1">
          {lab.title}
        </h3>

        <p className="text-gray-600 dark:text-gray-300 text-sm line-clamp-2">
          {lab.description}
        </p>

        <div className="flex items-center pt-2">
          <div className="w-6 h-6 rounded-full bg-indigo-500 flex items-center justify-center text-white text-xs font-medium mr-2">
            {lab.author?.firstName?.[0]}
            {lab.author?.lastName?.[0]}
          </div>
          <span className="text-xs text-gray-700 dark:text-gray-300">
            {lab.author?.firstName} {lab.author?.lastName}
          </span>
        </div>
      </div>
    </Link>
  );
}
