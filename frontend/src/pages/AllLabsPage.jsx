import LabCard from "../components/LabCard";

export default function AllLabs() {
  // All available labs from the platform
  const availableLabs = [
    {
      id: 1,
      title: "Open Labs Share Platform",
      description: "A peer-to-peer educational platform connecting experts with learners through hands-on, practical learning experiences",
      author: { firstName: "Platform", lastName: "Team" },
    },
    {
      id: 2,
      title: "Frontend Development Guide",
      description: "Complete guide to the Open Labs Share frontend structure, setup, and development workflow with React and Vite",
      author: { firstName: "Frontend", lastName: "Team" },
    },
    {
      id: 3,
      title: "API Gateway Documentation",
      description: "Comprehensive REST API documentation for frontend requests, authentication, and service endpoints",
      author: { firstName: "Backend", lastName: "Team" },
    },
    {
      id: 4,
      title: "Articles Service Architecture",
      description: "Central repository for scientific articles management with gRPC contracts, entities, and technical implementation",
      author: { firstName: "Backend", lastName: "Team" },
    },
    {
      id: 5,
      title: "Authentication Service Lab",
      description: "Stateless microservice for JWT-based authentication and authorization in distributed systems",
      author: { firstName: "Backend", lastName: "Team" },
    },
    {
      id: 6,
      title: "Feedback Service Implementation",
      description: "Lab feedback and comments system with PostgreSQL storage, MinIO assets, and gRPC integration",
      author: { firstName: "Ryan", lastName: "Gosling" },
    }
  ];

  const loadingCards = Array(2).fill(null);

  return (
    <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="max-w-6xl mx-auto">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
          <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
            All labs
          </h1>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {availableLabs.map((lab) => (
              <LabCard key={lab.id} lab={lab} />
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