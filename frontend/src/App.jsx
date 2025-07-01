import { useState, useEffect, useRef } from "react";
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
import { getCurrentUser, isAuthenticated, startTokenRefresh, stopTokenRefresh } from "./utils/auth";
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
import BackgroundCircles from "./components/BackgroundCircles";

// Component to protect routes that require authentication
function ProtectedRoute({ children }) {
  const authenticated = isAuthenticated();
  
  if (!authenticated) {
    console.log('User not authenticated, redirecting to signin');
    return <Navigate to="/signin" replace />;
  }
  
  return children;
}

// Component for routes that should only be accessible to unauthenticated users
function PublicOnlyRoute({ children }) {
  const authenticated = isAuthenticated();
  
  if (authenticated) {
    console.log('User already authenticated, redirecting to home');
    return <Navigate to="/home" replace />;
  }
  
  return children;
}

// Component for the landing page (accessible to everyone)
function LandingRoute({ children }) {
  // This route is accessible to both authenticated and unauthenticated users
  return children;
}

function AppContent() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [theme, setTheme] = useState("light");
  const [user, setUser] = useState(null);
  const sidebarRef = useRef();
  const location = useLocation();

  const showSidebar = !["/signup", "/signin"].includes(location.pathname) && 
                   !(location.pathname === "/" && !isAuthenticated());

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
    // Update user state when location changes (e.g., after login/logout)
    const currentUser = getCurrentUser();
    setUser(currentUser);
  }, [location]);

  // Listen for user data updates from other components
  useEffect(() => {
    const handleUserDataUpdate = () => {
      const updatedUser = getCurrentUser();
      setUser(updatedUser);
    };

    window.addEventListener('userDataUpdated', handleUserDataUpdate);
    
    return () => {
      window.removeEventListener('userDataUpdated', handleUserDataUpdate);
    };
  }, []);

  // Start automatic token refresh when app loads
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

  // Get user initials for profile avatar
  const getUserInitials = () => {
    if (!user) return "?";
    const firstInitial = user.firstName?.charAt(0)?.toUpperCase() || "";
    const lastInitial = user.lastName?.charAt(0)?.toUpperCase() || "";
    return firstInitial + lastInitial || user.username?.charAt(0)?.toUpperCase() || "?";
  };

  return (
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
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg
                    className="w-4 h-4 text-msc"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                    />
                  </svg>
                </div>
                <input
                  type="text"
                  placeholder="Search..."
                  className="block w-full pl-10 pr-3 py-2 border border-gray-200 rounded-md text-sm 
                              placeholder:text-light-blue
                              text-msc
                              bg-light-blue bg-opacity-55
                              focus:outline-none focus:ring-1 focus:ring-msc"
                />
              </div>
            </div>

            <div className="flex items-center space-x-3">
              {user && (
                <div className="text-right hidden sm:block">
                  <div className="text-sm text-gray-600 dark:text-gray-300">
                    Welcome, {user.firstName || user.username}!
                  </div>
                  <div className="flex items-center justify-end space-x-1 text-xs text-msc dark:text-gray-400">
                    <GemIcon className="h-4 w-4" color="#101e5a" />
                    <span>{user.balance || 0} points</span>
                  </div>
                </div>
              )}
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

      {showSidebar && (
        <Sidebar
          ref={sidebarRef}
          isOpen={isSidebarOpen}
          toggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)}
          currentTheme={theme}
          toggleTheme={toggleTheme}
          user={user}
          onUserUpdate={setUser}
        />
      )}
      <main
        className={`${showSidebar ? "p-4" : ""} transition-all duration-300 ${
          isSidebarOpen && showSidebar ? "ml-64" : "ml-0"
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
          {/* Catch-all route for undefined paths */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}
