import { Link } from "react-router-dom";

export default function ArticleCard({ article }) {
  return (
    <Link
      to={`/article/${article.id}`}
      className="block bg-light-blue bg-opacity-50 dark:bg-gray-800 rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 p-4 h-full flex flex-col justify-between"
    >
      <div className="space-y-2 flex-1">
        <h3 className="text-lg font-bold text-msc dark:text-white line-clamp-1">
          {article.title}
        </h3>
        <p className="text-msc dark:text-gray-300 text-sm line-clamp-2">
          {article.shortDesc}
        </p>
      </div>
      <div className="flex items-center pt-2 mt-2">
        <div className="w-6 h-6 rounded-full bg-msc flex items-center justify-center text-white text-xs font-medium mr-2">
          {article.authorName?.[0]}
          {article.authorSurname?.[0]}
        </div>
        <span className="text-base text-msc font-semibold dark:text-gray-300">
          {article.authorName} {article.authorSurname}
        </span>
      </div>
    </Link>
  );
}
