import ArticleCard from "../components/ArticleCard";

export default function MyArticles() {
  const sampleArticle = {
    id: 1,
    title: "Article 4: Scheduling",
    description:
      "Everyday practice shows that the beginning of daily work on the formation",
    author: {
      firstName: "Ryan",
      lastName: "Gosling",
    },
  };

  const loadingCards = Array(5).fill(null);

  return (
    <div className="relative min-h-screen bg-white dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-6 shadow-lg">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-6">
          My articles
        </h1>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          <ArticleCard article={sampleArticle} />
          {loadingCards.map((_, index) => (
            <div
              key={index}
              className="h-32 bg-light-blue bg-opacity-40 dark:bg-gray-700 animate-pulse rounded-xl"
            />
          ))}
        </div>
      </div>
    </div>
  );
}
