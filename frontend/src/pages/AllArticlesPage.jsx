import ArticleCard from "../components/ArticleCard";

export default function AllArticles() {
  // All available articles from the platform
  const availableArticles = [
    {
      id: 1,
      title: "Educational Technology Research",
      description: "Comprehensive study on peer-to-peer learning platforms and their effectiveness in modern education",
      author: { firstName: "Dr. Sarah", lastName: "Johnson" },
    },
    {
      id: 2,
      title: "Best Practices in Lab Design",
      description: "Guidelines for creating engaging hands-on learning experiences with community feedback systems",
      author: { firstName: "Prof. Michael", lastName: "Chen" },
    },
    {
      id: 3,
      title: "Microservices Architecture Patterns",
      description: "Design patterns and best practices for building scalable distributed systems",
      author: { firstName: "Backend", lastName: "Team" },
    },
    {
      id: 4,
      title: "Article 4: Scheduling",
      description: "Everyday practice shows that the beginning of daily work on the formation",
      author: { firstName: "Ryan", lastName: "Gosling" },
    },
    {
      id: 5,
      title: "Modern Frontend Development",
      description: "Latest trends and technologies in frontend development with React and modern tooling",
      author: { firstName: "Frontend", lastName: "Team" },
    },
    {
      id: 6,
      title: "Database Optimization Techniques",
      description: "Advanced database optimization strategies for high-performance applications",
      author: { firstName: "Platform", lastName: "Team" },
    }
  ];

  const loadingCards = Array(2).fill(null);

  return (
    <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="max-w-6xl mx-auto">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
          <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
            All articles
          </h1>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {availableArticles.map((article) => (
              <ArticleCard key={article.id} article={article} />
            ))}
            {loadingCards.map((_, index) => (
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