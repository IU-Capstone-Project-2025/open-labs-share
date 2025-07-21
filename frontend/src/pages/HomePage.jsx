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
import { labsAPI, usersAPI, submissionsAPI, articlesAPI } from "../utils/api";
import { isAuthenticated, getCurrentUser } from "../utils/auth";
import { useUser } from "../hooks/useUser";

export default function Home() {
  
  const user = useUser();
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

  
  const getRandomItems = (array, count) => {
    const shuffled = [...array].sort(() => 0.5 - Math.random());
    return shuffled.slice(0, count);
  };

  useEffect(() => {
    const fetchHomeData = async () => {
      try {
        setLoading(true);
        
        
        const labsResponse = await labsAPI.getLabs();
        const allLabs = labsResponse.labs || [];
        setFeaturedLabs(getRandomItems(allLabs, 3));

        
        const articlesResponse = await articlesAPI.getArticles();
        const allArticles = articlesResponse.articles || [];
        setFeaturedArticles(getRandomItems(allArticles, 3));

        
        // Note: Total users and submissions counts are not available via API
        // These statistics would need to be implemented as separate API endpoints
        let totalUsers = 0;
        let totalSubmissions = 0;

        
        setStats({
          totalLabs: allLabs.length,
          totalArticles: allArticles.length,
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

  
  if (!user) {
    return (
      <div className="flex justify-center items-center h-screen dark:bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-msc"></div>
      </div>
    );
  }

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
            
            <h1 className="text-4xl font-extrabold font-display text-gray-900 dark:text-white tracking-tight sm:text-5xl md:text-6xl">
              Welcome back, <span className="text-blue-600 dark:text-blue-400">
                {user?.firstName || user?.username || 'Student'}!
              </span>
            </h1>
            
            <p className="mt-6 max-w-3xl mx-auto text-lg text-gray-600 dark:text-gray-300">
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
                className="px-8 py-4 bg-white dark:bg-gray-100 text-msc dark:text-gray-900 rounded-lg font-semibold hover:bg-gray-100 dark:hover:bg-white transition-colors border border-gray-200 dark:border-gray-300 flex items-center group"
              >
                Browse Articles
                <ArrowRightIcon className="h-5 w-5 ml-2 group-hover:translate-x-1 transition-transform" />
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
              <h2 className="text-3xl font-bold font-display text-msc dark:text-white">
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
              <h2 className="text-3xl font-bold font-display text-msc dark:text-white">
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
