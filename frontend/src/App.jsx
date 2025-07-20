import { useState, useEffect, useRef, useCallback } from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  useLocation,
  Link,
  Navigate,
} from "react-router-dom";
import { Bars3Icon } from "@heroicons/react/24/outline";
import GemIcon from "./components/GemIcon";
import { getCurrentUser, isAuthenticated, getUserProfile, startTokenRefresh, stopTokenRefresh } from "./utils/auth";
import Sidebar from "./components/Sidebar";
import Home from "./pages/HomePage";
import LandingPage from "./pages/LandingPage";
import SignIn from "./pages/SignIn";
import SignUp from "./pages/SignUp";
import Profile from "./pages/ProfilePage";
import MyLabs from "./pages/MyLabsPage";
import MyArticles from "./pages/MyArticlesPage";
import AllLabs from "./pages/AllLabsPage";
import AllArticles from "./pages/AllArticlesPage";
import LabPage from "./pages/LabPage";
import ArticlePage from "./pages/ArticlePage";
import CreateLabPage from "./pages/CreateLabPage";
import CreateArticlePage from "./pages/CreateArticlePage";
import MySubmissionsPage from "./pages/MySubmissionsPage";
import SubmissionPage from "./pages/SubmissionPage";
import BackgroundCircles from "./components/BackgroundCircles";
import Search from "./components/Search";
import { UserContext } from './hooks/useUser';
import ReviewQueuePage from './pages/ReviewQueuePage';
import ReviewSubmissionPage from './pages/ReviewSubmissionPage';
import MyFeedbackPage from './pages/MyFeedbackPage';
import FeedbackViewPage from './pages/FeedbackViewPage';

import SearchResultsPage from './pages/SearchResultsPage';
import useMediaQuery from "./hooks/useMediaQuery";


function ProtectedRoute({ children }) {
  const authenticated = isAuthenticated();
  
  if (!authenticated) {
    console.log('User not authenticated, redirecting to signin');
    return <Navigate to="/signin" replace />;
  }
  
  return children;
}

function PublicOnlyRoute({ children }) {
  const authenticated = isAuthenticated();
  
  if (authenticated) {
    console.log('User already authenticated, redirecting to home');
    return <Navigate to="/home" replace />;
  }
  
  return children;
}

function LandingRoute({ children }) {
  return children;
}

function AppContent() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [theme, setTheme] = useState("light");
  const [user, setUser] = useState(null);
  const [userLoading, setUserLoading] = useState(true);
  const sidebarRef = useRef();
  const location = useLocation();
  const updateTimeoutRef = useRef(null);
  const isMobile = useMediaQuery("(max-width: 640px)");

  const showSidebar = !["/signup", "/signin"].includes(location.pathname) && 
                   !(location.pathname === "/" && !isAuthenticated());
  
  const updateUserState = useCallback(async () => {
    console.log('updateUserState called at:', new Date().toISOString());
    if (isAuthenticated()) {
      try {
        setUserLoading(true);
        const freshUserData = await getUserProfile();
        setUser(freshUserData);
      } catch (error) {
        console.error('Failed to fetch user profile, using cached data:', error);
        const cachedUser = getCurrentUser();
        setUser(cachedUser);
      } finally {
        setUserLoading(false);
      }
    } else {
      setUser(null);
      setUserLoading(false);
    }
  }, []);

  const debouncedUpdateUserState = useCallback(() => {
    if (updateTimeoutRef.current) {
      clearTimeout(updateTimeoutRef.current);
    }
    updateTimeoutRef.current = setTimeout(() => {
      updateUserState();
    }, 100);
  }, [updateUserState]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        isSidebarOpen &&
        sidebarRef.current &&
        !sidebarRef.current.contains(event.target) &&
        !event.target.closest('button[aria-label="Toggle sidebar"]') &&
        !event.target.closest(".sidebar-toggle-button")
      ) {
        setIsSidebarOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isSidebarOpen]);

  useEffect(() => {
    const savedTheme = localStorage.getItem("theme") || "light";
    setTheme(savedTheme);
    document.documentElement.classList.toggle("dark", savedTheme === "dark");
  }, []);

  useEffect(() => {
    updateUserState();

    window.addEventListener('userDataUpdated', debouncedUpdateUserState);

    return () => {
      window.removeEventListener('userDataUpdated', debouncedUpdateUserState);
      if (updateTimeoutRef.current) {
        clearTimeout(updateTimeoutRef.current);
      }
    };
  }, [updateUserState, debouncedUpdateUserState]);

  useEffect(() => {
    if (isAuthenticated()) {
      startTokenRefresh();
    }
    return () => {
      stopTokenRefresh();
    };
  }, []);

  const toggleTheme = () => {
    const newTheme = theme === "light" ? "dark" : "light";
    setTheme(newTheme);
    localStorage.setItem("theme", newTheme);
    document.documentElement.classList.toggle("dark", newTheme === "dark");
  };

  const getUserInitials = () => {
    if (!user) return "?";
    const firstInitial = user.firstName?.charAt(0)?.toUpperCase() || "";
    const lastInitial = user.lastName?.charAt(0)?.toUpperCase() || "";
    return firstInitial && lastInitial ? `${firstInitial}${lastInitial}` : (user.username?.charAt(0)?.toUpperCase() || "?");
  };

  return (
    <UserContext.Provider value={user}>
      <div className="min-h-screen bg-white dark:bg-gray-900">
        {showSidebar && (
          <header className="sticky top-0 z-40 bg-white dark:bg-gray-800 shadow-sm">
            <div className="flex items-center justify-between h-16 px-4">
              <div
                className={`flex items-center space-x-4 transition-all duration-300 ${
                  isSidebarOpen ? "ml-64" : "ml-0"
                }`}
              >
                <button
                  onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                  className="p-1.5 rounded-md text-msc dark:text-gray-300 hover:bg-light-blue hover:bg-opacity-55 dark:hover:bg-gray-700"
                  aria-label="Toggle sidebar"
                >
                  <Bars3Icon className="h-6 w-6" />
                </button>

                <div className="relative w-64">
                  <Search />
                </div>
              </div>

              <div className="flex items-center space-x-3">
                {user && !userLoading ? (
                  <div className="text-right hidden sm:block">
                    <div className="text-sm text-gray-600 dark:text-gray-300">
                      Welcome, {user.firstName} {user.lastName}!
                    </div>
                    <div className="flex items-center justify-end space-x-1 text-xs text-msc dark:text-gray-400">
                      <GemIcon className="h-4 w-4" color="#101e5a" />
                      <span>{user.balance || 0} points</span>
                    </div>
                  </div>
                ) : userLoading ? (
                  <div className="text-right hidden sm:block">
                    <div className="text-sm text-gray-600 dark:text-gray-300">
                      Loading...
                    </div>
                    <div className="flex items-center justify-end space-x-1 text-xs text-msc dark:text-gray-400">
                      <GemIcon className="h-4 w-4" color="#101e5a" />
                      <span>...</span>
                    </div>
                  </div>
                ) : null}
                <Link to="/profile" className="flex items-center">
                  <div className="w-10 h-10 rounded-full bg-msc flex items-center justify-center text-white text-sm cursor-pointer hover:bg-msc-hover transition-colors">
                    <span>{getUserInitials()}</span>
                  </div>
                </Link>
              </div>
            </div>
          </header>
        )}

        {showSidebar && <BackgroundCircles />}

        {/* Overlay for mobile sidebar */}
        {showSidebar && isSidebarOpen && isMobile && (
          <div
            className="fixed inset-0 bg-black bg-opacity-40 backdrop-blur-sm z-[9998]"
            onClick={() => setIsSidebarOpen(false)}
            aria-label="Sidebar overlay"
          />
        )}

        {showSidebar && (
          <Sidebar
            ref={sidebarRef}
            isOpen={isSidebarOpen}
            toggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)}
            currentTheme={theme}
            toggleTheme={toggleTheme}
            isMobile={isMobile}
          />
        )}
        <main
          className={`${showSidebar ? "p-4" : ""} transition-all duration-300 ${
            isSidebarOpen && showSidebar && !isMobile ? "ml-64" : "ml-0"
          }`}
        >
          <Routes>
            <Route path="/" element={<LandingRoute><LandingPage /></LandingRoute>} />
            <Route path="/signup" element={<PublicOnlyRoute><SignUp /></PublicOnlyRoute>} />
            <Route path="/signin" element={<PublicOnlyRoute><SignIn /></PublicOnlyRoute>} />
            <Route path="/home" element={<ProtectedRoute><Home /></ProtectedRoute>} />
            <Route path="/my-labs" element={<ProtectedRoute><MyLabs /></ProtectedRoute>} />
            <Route path="/all-labs" element={<ProtectedRoute><AllLabs /></ProtectedRoute>} />
            <Route path="/create-lab" element={<ProtectedRoute><CreateLabPage /></ProtectedRoute>} />
            <Route path="/lab/:id" element={<ProtectedRoute><LabPage /></ProtectedRoute>} />
            <Route path="/my-articles" element={<ProtectedRoute><MyArticles /></ProtectedRoute>} />
            <Route path="/all-articles" element={<ProtectedRoute><AllArticles /></ProtectedRoute>} />
            <Route path="/article/:id" element={<ProtectedRoute><ArticlePage /></ProtectedRoute>} />
            <Route path="/create-article" element={<ProtectedRoute><CreateArticlePage /></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
            <Route path="/submissions/my" element={<ProtectedRoute><MySubmissionsPage /></ProtectedRoute>} />
            <Route path="/submissions/:id" element={<ProtectedRoute><SubmissionPage /></ProtectedRoute>} />
            <Route path="/reviews" element={<ReviewQueuePage />} />
            <Route path="/feedback/:submissionId" element={<ReviewSubmissionPage />} />
            <Route path="/feedback/my" element={<ProtectedRoute><MyFeedbackPage /></ProtectedRoute>} />
            <Route path="/feedback/view/:feedbackId" element={<ProtectedRoute><FeedbackViewPage /></ProtectedRoute>} />
            <Route path="/search" element={<ProtectedRoute><SearchResultsPage /></ProtectedRoute>} />

            {/* Catch-all route for undefined paths */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </UserContext.Provider>
  );
}

export default function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}
