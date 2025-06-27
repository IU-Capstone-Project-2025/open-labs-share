import { useState, useEffect } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { 
  BookOpenIcon, 
  BeakerIcon, 
  UserGroupIcon, 
  AcademicCapIcon,
  ArrowRightIcon,
  ChartBarIcon,
  SparklesIcon,
  RocketLaunchIcon,
  PlusIcon
} from "@heroicons/react/24/outline";
import ArticleCard from "../components/ArticleCard";
import LabCard from "../components/LabCard";
import { labsAPI, usersAPI, submissionsAPI } from "../utils/api";
import { isAuthenticated, getCurrentUser } from "../utils/auth";

export default function Home() {
  // Check authentication - if not authenticated, redirect will be handled by ProtectedRoute
  // This component should only render for authenticated users
  const user = getCurrentUser();
  const navigate = useNavigate();
  
  const [stats, setStats] = useState({
    totalLabs: 0,
    totalArticles: 0,
    totalUsers: 0,
    completedSubmissions: 0
  });
  const [featuredLabs, setFeaturedLabs] = useState([]);
  const [featuredArticles, setFeaturedArticles] = useState([]);
  const [loading, setLoading] = useState(true);

  // Function to get random items from an array
  const getRandomItems = (array, count) => {
    const shuffled = [...array].sort(() => 0.5 - Math.random());
    return shuffled.slice(0, count);
  };

  useEffect(() => {
    const fetchHomeData = async () => {
      try {
        setLoading(true);
        
        // Fetch labs data
        const labsResponse = await labsAPI.getLabs();
        const allLabs = labsResponse.labs || [];
        setFeaturedLabs(getRandomItems(allLabs, 3));

        // Fetch users count
        let totalUsers = 0;
        try {
          const usersResponse = await usersAPI.getAllUsers();
          totalUsers = (usersResponse.data || []).length;
        } catch (err) {
          console.warn('Could not fetch users count:', err);
        }

        // Fetch submissions count
        let totalSubmissions = 0;
        try {
          const submissionsResponse = await submissionsAPI.getAllSubmissions();
          totalSubmissions = (submissionsResponse.data || []).length;
        } catch (err) {
          console.warn('Could not fetch submissions count:', err);
        }

        // For articles - use mock data since articles service is not connected
        const mockArticles = [
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
        setFeaturedArticles(getRandomItems(mockArticles, 3));

        // Update stats
        setStats({
          totalLabs: allLabs.length,
          totalArticles: mockArticles.length,
          totalUsers: totalUsers,
          completedSubmissions: totalSubmissions
        });

      } catch (err) {
        console.error('Error fetching home data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchHomeData();
  }, []);

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
              Welcome back, <span className="text-blue-blue">{user?.firstName || user?.username || 'Student'}!</span>
            </h1>
            
            <p className="text-xl text-msc/80 dark:text-gray-300 mb-8 max-w-3xl mx-auto leading-relaxed">
              Continue your learning journey with hands-on labs, engage with the latest research articles, 
              and connect with fellow learners in our collaborative community.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <Link 
                to="/all-labs" 
                className="px-8 py-4 bg-msc text-white rounded-lg font-semibold hover:bg-msc-hover transition-colors flex items-center group"
              >
                Explore Labs
                <ArrowRightIcon className="h-5 w-5 ml-2 group-hover:translate-x-1 transition-transform" />
              </Link>
              
              <Link 
                to="/all-articles" 
                className="px-8 py-4 bg-light-blue text-msc rounded-lg font-semibold hover:bg-light-blue-hover transition-colors"
              >
                Browse Articles
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <h2 className="text-3xl font-bold text-msc dark:text-white text-center mb-12">
              Platform Features
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
              {features.map((feature, index) => (
                <div 
                  key={index}
                  className="text-center p-6 rounded-lg border border-gray-200 dark:border-gray-700 hover:shadow-md transition-shadow"
                >
                  <div className="flex justify-center mb-4">
                    <div className={`p-3 rounded-full ${feature.color}`}>
                      <feature.icon className="h-6 w-6 text-white" />
                    </div>
                  </div>
                  <h3 className="text-lg font-semibold text-msc dark:text-white mb-2">
                    {feature.title}
                  </h3>
                  <p className="text-light-blue dark:text-gray-400">
                    {feature.description}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Quick Actions Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <h2 className="text-3xl font-bold text-msc dark:text-white text-center mb-8">
              Quick Actions
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Create Lab Card */}
              <div 
                onClick={() => navigate('/create-lab')}
                className="p-6 border-2 border-dashed border-msc/30 rounded-lg hover:border-msc/60 hover:bg-msc/5 transition-all cursor-pointer group"
              >
                <div className="text-center">
                  <div className="flex justify-center mb-4">
                    <div className="p-3 bg-msc/10 rounded-full group-hover:bg-msc/20 transition-colors">
                      <PlusIcon className="h-8 w-8 text-msc" />
                    </div>
                  </div>
                  <h3 className="text-lg font-semibold text-msc dark:text-white mb-2">
                    Create New Lab
                  </h3>
                  <p className="text-light-blue dark:text-gray-400">
                    Share your knowledge by creating interactive lab exercises
                  </p>
                </div>
              </div>

              {/* My Labs Card */}
              <div 
                onClick={() => navigate('/my-labs')}
                className="p-6 bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-gray-800 dark:to-gray-700 rounded-lg hover:shadow-md transition-all cursor-pointer border border-blue-100 dark:border-gray-600"
              >
                <div className="text-center">
                  <div className="flex justify-center mb-4">
                    <div className="p-3 bg-blue-500/10 rounded-full">
                      <BeakerIcon className="h-8 w-8 text-blue-500" />
                    </div>
                  </div>
                  <h3 className="text-lg font-semibold text-msc dark:text-white mb-2">
                    My Labs
                  </h3>
                  <p className="text-light-blue dark:text-gray-400">
                    Manage and review your created lab exercises
                  </p>
                </div>
              </div>

              {/* My Profile Card */}
              <div 
                onClick={() => navigate('/profile')}
                className="p-6 bg-gradient-to-br from-green-50 to-emerald-50 dark:from-gray-800 dark:to-gray-700 rounded-lg hover:shadow-md transition-all cursor-pointer border border-green-100 dark:border-gray-600"
              >
                <div className="text-center">
                  <div className="flex justify-center mb-4">
                    <div className="p-3 bg-green-500/10 rounded-full">
                      <UserGroupIcon className="h-8 w-8 text-green-500" />
                    </div>
                  </div>
                  <h3 className="text-lg font-semibold text-msc dark:text-white mb-2">
                    My Profile
                  </h3>
                  <p className="text-light-blue dark:text-gray-400">
                    Update your profile and view your activity
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Featured Labs Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="flex justify-between items-center mb-8">
              <h2 className="text-3xl font-bold text-msc dark:text-white">
                Featured Labs
              </h2>
              <Link 
                to="/all-labs"
                className="text-blue-blue hover:text-blue-blue-hover font-semibold flex items-center group"
              >
                View All Labs
                <ArrowRightIcon className="h-4 w-4 ml-1 group-hover:translate-x-1 transition-transform" />
              </Link>
            </div>
            
            {loading ? (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {Array(3).fill(null).map((_, index) => (
                  <div
                    key={`loading-lab-${index}`}
                    className="h-32 bg-light-blue bg-opacity-40 dark:bg-gray-700 animate-pulse rounded-xl"
                  />
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {featuredLabs.map((lab) => (
                  <LabCard key={lab.id} lab={lab} />
                ))}
              </div>
            )}
          </div>
        </div>
      </section>

      {/* Featured Articles Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="flex justify-between items-center mb-8">
              <h2 className="text-3xl font-bold text-msc dark:text-white">
                Featured Articles
              </h2>
              <Link 
                to="/all-articles"
                className="text-blue-blue hover:text-blue-blue-hover font-semibold flex items-center group"
              >
                View All Articles
                <ArrowRightIcon className="h-4 w-4 ml-1 group-hover:translate-x-1 transition-transform" />
              </Link>
            </div>
            
            {loading ? (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {Array(3).fill(null).map((_, index) => (
                  <div
                    key={`loading-article-${index}`}
                    className="h-32 bg-light-blue bg-opacity-40 dark:bg-gray-700 animate-pulse rounded-xl"
                  />
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {featuredArticles.map((article) => (
                  <ArticleCard key={article.id} article={article} />
                ))}
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
