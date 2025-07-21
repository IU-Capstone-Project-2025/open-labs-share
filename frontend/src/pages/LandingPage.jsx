import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import { 
  BookOpenIcon, 
  BeakerIcon, 
  UserGroupIcon, 
  AcademicCapIcon,
  ArrowRightIcon,
  SparklesIcon,
  RocketLaunchIcon,
  CheckIcon,
  ChartBarIcon
} from "@heroicons/react/24/outline";
import BackgroundCircles from "../components/BackgroundCircles";
import { statisticsAPI } from "../utils/api";

export default function LandingPage() {
  
  const [stats, setStats] = useState({
    totalLabs: 0,
    totalArticles: 0,
    totalUsers: 0,
    completedSubmissions: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStatistics = async () => {
      try {
        setLoading(true);
        const statistics = await statisticsAPI.getStatistics();
        setStats({
          totalLabs: statistics.labs || 0,
          totalArticles: statistics.articles || 0,
          totalUsers: statistics.users || 0,
          completedSubmissions: statistics.submissions || 0
        });
      } catch (error) {
        console.error('Error fetching statistics:', error);
        // Keep default values if API call fails
      } finally {
        setLoading(false);
      }
    };

    fetchStatistics();
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

  const benefits = [
    "Access to expert-designed learning materials",
    "Collaborative peer review system",
    "Real-world practical exercises",
    "Community-driven knowledge sharing",
    "Personalized learning recommendations",
    "Industry-relevant skill development"
  ];

  return (
    <div className="relative min-h-screen dark:bg-gray-900 bg-transparent">
      <BackgroundCircles />
      
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
                to="/signup" 
                className="px-8 py-4 bg-msc text-white rounded-lg font-semibold hover:bg-msc-hover transition-colors flex items-center group"
              >
                Get Started Now
                <ArrowRightIcon className="h-5 w-5 ml-2 group-hover:translate-x-1 transition-transform" />
              </Link>
              
              <Link 
                to="/signin" 
                className="px-8 py-4 bg-white dark:bg-gray-100 text-msc dark:text-gray-900 rounded-lg font-semibold hover:bg-gray-100 dark:hover:bg-white transition-colors border border-gray-200 dark:border-gray-300 flex items-center group"
              >
                Sign In
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
            <div className="text-center mb-12">
              <h2 className="text-3xl font-bold text-msc dark:text-white mb-4">
                What Makes Us Different
              </h2>
              <p className="text-lg text-msc/70 dark:text-gray-300 max-w-2xl mx-auto">
                Experience a new way of learning through collaborative, hands-on education
              </p>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
              {features.map((feature, index) => (
                <div key={index} className="text-center group">
                  <div className={`inline-flex p-4 rounded-full ${feature.color} mb-4 group-hover:scale-110 transition-transform duration-300`}>
                    <feature.icon className="h-8 w-8 text-white" />
                  </div>
                  <h3 className="text-xl font-semibold text-msc dark:text-white mb-2">
                    {feature.title}
                  </h3>
                  <p className="text-msc/70 dark:text-gray-300">
                    {feature.description}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Platform Statistics Section */}
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
                <div className="text-3xl font-bold text-msc dark:text-white">
                  {loading ? (
                    <div className="animate-pulse bg-gray-300 dark:bg-gray-600 h-8 w-16 mx-auto rounded"></div>
                  ) : (
                    stats.totalLabs
                  )}
                </div>
                <div className="text-light-blue dark:text-gray-400">Active Labs</div>
              </div>
              
              <div className="text-center">
                <div className="flex justify-center mb-3">
                  <BookOpenIcon className="h-8 w-8 text-green-500" />
                </div>
                <div className="text-3xl font-bold text-msc dark:text-white">
                  {loading ? (
                    <div className="animate-pulse bg-gray-300 dark:bg-gray-600 h-8 w-16 mx-auto rounded"></div>
                  ) : (
                    stats.totalArticles
                  )}
                </div>
                <div className="text-light-blue dark:text-gray-400">Research Articles</div>
              </div>
              
              <div className="text-center">
                <div className="flex justify-center mb-3">
                  <UserGroupIcon className="h-8 w-8 text-purple-500" />
                </div>
                <div className="text-3xl font-bold text-msc dark:text-white">
                  {loading ? (
                    <div className="animate-pulse bg-gray-300 dark:bg-gray-600 h-8 w-16 mx-auto rounded"></div>
                  ) : (
                    stats.totalUsers
                  )}
                </div>
                <div className="text-light-blue dark:text-gray-400">Community Members</div>
              </div>
              
              <div className="text-center">
                <div className="flex justify-center mb-3">
                  <ChartBarIcon className="h-8 w-8 text-orange-500" />
                </div>
                <div className="text-3xl font-bold text-msc dark:text-white">
                  {loading ? (
                    <div className="animate-pulse bg-gray-300 dark:bg-gray-600 h-8 w-16 mx-auto rounded"></div>
                  ) : (
                    stats.completedSubmissions
                  )}
                </div>
                <div className="text-light-blue dark:text-gray-400">Submissions</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-16 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
              <div>
                <h2 className="text-3xl font-bold text-msc dark:text-white mb-6">
                  Why Choose Open Labs Share?
                </h2>
                <div className="space-y-4">
                  {benefits.map((benefit, index) => (
                    <div key={index} className="flex items-start space-x-3">
                      <div className="flex-shrink-0 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center mt-0.5">
                        <CheckIcon className="h-4 w-4 text-white" />
                      </div>
                      <p className="text-msc/80 dark:text-gray-300">{benefit}</p>
                    </div>
                  ))}
                </div>
              </div>
              
              <div className="text-center">
                <div className="p-8 bg-gradient-to-br from-msc/10 to-blue-blue/10 rounded-xl">
                  <SparklesIcon className="h-20 w-20 text-msc mx-auto mb-6" />
                  <h3 className="text-2xl font-bold text-msc dark:text-white mb-4">
                    Ready to Start Learning?
                  </h3>
                  <p className="text-msc/70 dark:text-gray-300 mb-6">
                    Join thousands of learners already part of our community
                  </p>
                  <Link 
                    to="/signup"
                    className="inline-flex items-center px-6 py-3 bg-msc text-white rounded-lg font-semibold hover:bg-msc-hover transition-colors"
                  >
                    Create Free Account
                    <ArrowRightIcon className="h-5 w-5 ml-2" />
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 px-6">
        <div className="max-w-4xl mx-auto">
          <div className="bg-gradient-to-r from-msc to-blue-blue rounded-xl p-8 text-center text-white">
            <h2 className="text-3xl font-bold mb-4">
              Start Your Learning Journey Today
            </h2>
            <p className="text-xl mb-8 opacity-90">
              Join our community of learners, experts, and knowledge enthusiasts
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link 
                to="/signup"
                className="px-8 py-4 bg-white text-msc rounded-lg font-semibold hover:bg-gray-100 transition-colors"
              >
                Sign Up Free
              </Link>
              <Link 
                to="/signin"
                className="px-8 py-4 border-2 border-white text-white rounded-lg font-semibold hover:bg-white hover:text-msc transition-colors"
              >
                Already Have Account?
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
} 