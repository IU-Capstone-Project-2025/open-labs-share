import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { 
  BookOpenIcon, 
  BeakerIcon, 
  UserGroupIcon, 
  AcademicCapIcon,
  ArrowRightIcon,
  ChartBarIcon,
  SparklesIcon,
  RocketLaunchIcon
} from "@heroicons/react/24/outline";
import ArticleCard from "../components/ArticleCard";
import LabCard from "../components/LabCard";

export default function Home() {
  const [stats, setStats] = useState({
    totalLabs: 156,
    totalArticles: 342,
    totalUsers: 1250,
    completedSubmissions: 8400
  });

  // All available labs from the platform
  const allLabs = [
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

  // All available articles from the platform
  const allArticles = [
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

  // Function to get random items from an array
  const getRandomItems = (array, count) => {
    const shuffled = [...array].sort(() => 0.5 - Math.random());
    return shuffled.slice(0, count);
  };

  // Get 3 random featured labs and articles
  const featuredLabs = getRandomItems(allLabs, 3);
  const featuredArticles = getRandomItems(allArticles, 3);

  const features = [
    {
      icon: BeakerIcon,
      title: "Interactive Labs",
      description: "Hands-on learning experiences designed by subject-matter experts",
      color: "bg-blue-500"
    },
    {
      icon: BookOpenIcon,
      title: "Scientific Articles",
      description: "Access cutting-edge research and educational content",
      color: "bg-green-500"
    },
    {
      icon: UserGroupIcon,
      title: "Peer Review",
      description: "Get feedback from community members and improve your skills",
      color: "bg-purple-500"
    },
    {
      icon: AcademicCapIcon,
      title: "Expert Guidance",
      description: "Learn from industry professionals and academic experts",
      color: "bg-orange-500"
    }
  ];

  return (
    <div className="relative min-h-screen dark:bg-gray-900 bg-transparent">
      {/* Hero Section */}
      <section className="relative py-20 px-6">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg max-w-6xl mx-auto">
          <div className="text-center">
            <div className="flex justify-center mb-6">
              <div className="p-4 bg-msc/10 rounded-full">
                <RocketLaunchIcon className="h-16 w-16 text-msc" />
              </div>
            </div>
            
            <h1 className="text-5xl font-bold text-msc dark:text-white mb-6">
              Welcome to <span className="text-blue-blue">Open Labs Share</span>
            </h1>
            
            <p className="text-xl text-msc/80 dark:text-gray-300 mb-8 max-w-3xl mx-auto leading-relaxed">
              A peer-to-peer educational platform connecting experts with learners through 
              hands-on, practical learning experiences. Discover interactive labs, access 
              scientific articles, and join a collaborative community of knowledge sharing.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <Link 
                to="/my-labs" 
                className="px-8 py-4 bg-msc text-white rounded-lg font-semibold hover:bg-msc-hover transition-colors flex items-center group"
              >
                Explore Labs
                <ArrowRightIcon className="h-5 w-5 ml-2 group-hover:translate-x-1 transition-transform" />
              </Link>
              
              <Link 
                to="/my-articles" 
                className="px-8 py-4 bg-light-blue text-msc rounded-lg font-semibold hover:bg-light-blue-hover transition-colors"
              >
                Browse Articles
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <h2 className="text-3xl font-bold text-msc dark:text-white text-center mb-12">
              Platform Statistics
            </h2>
            
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
              <div className="text-center">
                <div className="flex justify-center mb-3">
                  <BeakerIcon className="h-8 w-8 text-blue-blue" />
                </div>
                <div className="text-3xl font-bold text-msc dark:text-white">{stats.totalLabs}</div>
                <div className="text-light-blue dark:text-gray-400">Active Labs</div>
              </div>
              
              <div className="text-center">
                <div className="flex justify-center mb-3">
                  <BookOpenIcon className="h-8 w-8 text-blue-blue" />
                </div>
                <div className="text-3xl font-bold text-msc dark:text-white">{stats.totalArticles}</div>
                <div className="text-light-blue dark:text-gray-400">Articles</div>
              </div>
              
              <div className="text-center">
                <div className="flex justify-center mb-3">
                  <UserGroupIcon className="h-8 w-8 text-blue-blue" />
                </div>
                <div className="text-3xl font-bold text-msc dark:text-white">{stats.totalUsers}</div>
                <div className="text-light-blue dark:text-gray-400">Community Members</div>
              </div>
              
              <div className="text-center">
                <div className="flex justify-center mb-3">
                  <ChartBarIcon className="h-8 w-8 text-blue-blue" />
                </div>
                <div className="text-3xl font-bold text-msc dark:text-white">{stats.completedSubmissions}</div>
                <div className="text-light-blue dark:text-gray-400">Submissions</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="text-center mb-12">
              <h2 className="text-3xl font-bold text-msc dark:text-white mb-4">
                Why Choose Open Labs Share?
              </h2>
              <p className="text-lg text-msc/80 dark:text-gray-300 max-w-2xl mx-auto">
                Experience the future of collaborative learning with our innovative platform features
              </p>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
              {features.map((feature, index) => (
                <div key={index} className="text-center group">
                  <div className="flex justify-center mb-4">
                    <div className="p-4 bg-light-blue/20 dark:bg-gray-700 rounded-xl group-hover:scale-110 transition-transform">
                      <feature.icon className="h-8 w-8 text-msc dark:text-blue-blue" />
                    </div>
                  </div>
                  <h3 className="text-xl font-semibold text-msc dark:text-white mb-2">
                    {feature.title}
                  </h3>
                  <p className="text-msc/70 dark:text-gray-400">
                    {feature.description}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Featured Labs Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="flex justify-between items-center mb-8">
              <div>
                <h2 className="text-3xl font-bold text-msc dark:text-white mb-2">
                  Featured Labs
                </h2>
                <p className="text-msc/80 dark:text-gray-300">
                  Discover hands-on learning experiences created by experts
                </p>
              </div>
              <Link 
                to="/all-labs" 
                className="text-blue-blue hover:text-msc transition-colors font-semibold flex items-center"
              >
                View All
                <ArrowRightIcon className="h-4 w-4 ml-1" />
              </Link>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {featuredLabs.map((lab) => (
                <LabCard key={lab.id} lab={lab} />
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Featured Articles Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="flex justify-between items-center mb-8">
              <div>
                <h2 className="text-3xl font-bold text-msc dark:text-white mb-2">
                  Featured Articles
                </h2>
                <p className="text-msc/80 dark:text-gray-300">
                  Explore cutting-edge research and educational content
                </p>
              </div>
              <Link 
                to="/all-articles" 
                className="text-blue-blue hover:text-msc transition-colors font-semibold flex items-center"
              >
                View All
                <ArrowRightIcon className="h-4 w-4 ml-1" />
              </Link>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {featuredArticles.map((article) => (
                <ArticleCard key={article.id} article={article} />
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Call to Action Section */}
      <section className="py-20 px-6">
        <div className="max-w-4xl mx-auto">
          <div className="bg-gradient-to-r from-msc to-blue-blue rounded-xl p-8 shadow-lg text-center text-white">
            <div className="flex justify-center mb-6">
              <SparklesIcon className="h-12 w-12" />
            </div>
            
            <h2 className="text-3xl font-bold mb-4">
              Ready to Start Your Learning Journey?
            </h2>
            
            <p className="text-xl mb-8 opacity-90">
              Join thousands of learners and experts in our collaborative educational community
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link 
                to="/signup" 
                className="px-8 py-4 bg-white text-msc rounded-lg font-semibold hover:bg-gray-100 transition-colors"
              >
                Create Your Profile
              </Link>
              <Link 
                to="/all-labs" 
                className="px-8 py-4 border-2 border-white text-white rounded-lg font-semibold hover:bg-white hover:text-msc transition-colors"
              >
                Start Learning
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
