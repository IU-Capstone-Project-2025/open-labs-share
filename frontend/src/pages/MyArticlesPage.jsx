import ArticleCard from "../components/ArticleCard";

export default function MyArticles() {
  // Current user profile (in a real app, this would come from auth context)
  const currentUser = {
    firstName: "Ryan",
    lastName: "Gosling"
  };

  // All available articles from the platform
  const allArticles = [
    {
      id: 1,
      title: "Educational Technology Research",
      description: "Comprehensive study on peer-to-peer learning platforms and their effectiveness in modern educational environments",
      author: { firstName: "Dr. Emma", lastName: "Williams" },
    },
    {
      id: 2,
      title: "Microservices Architecture Patterns",
      description: "Best practices for building scalable educational platforms using microservices, Docker, and gRPC communication",
      author: { firstName: "Prof. Michael", lastName: "Chen" },
    },
    {
      id: 3,
      title: "User Interface Design for Learning",
      description: "Research on effective UI/UX patterns for educational web applications and student engagement optimization",
      author: { firstName: "Dr. Sarah", lastName: "Johnson" },
    },
    {
      id: 4,
      title: "Article Management in Digital Platforms",
      description: "Everyday practice shows that the beginning of daily work on the formation and implementation of content systems",
      author: { firstName: "Ryan", lastName: "Gosling" },
    },
    {
      id: 5,
      title: "Authentication Systems Security",
      description: "Comprehensive analysis of JWT-based authentication, security best practices, and stateless service architecture",
      author: { firstName: "Security", lastName: "Research" },
    },
    {
      id: 6,
      title: "Feedback Systems in Education",
      description: "Study on effective peer review systems, community feedback mechanisms, and their impact on learning outcomes",
      author: { firstName: "Education", lastName: "Research" },
    }
  ];

  // Filter articles to show only those created by the current user
  const myArticles = allArticles.filter(article => 
    article.author.firstName === currentUser.firstName && 
    article.author.lastName === currentUser.lastName
  );

  const loadingCards = Array(2).fill(null);

  return (
    <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="max-w-6xl mx-auto">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
        <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
          My articles
        </h1>

                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {myArticles.map((article) => (
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
