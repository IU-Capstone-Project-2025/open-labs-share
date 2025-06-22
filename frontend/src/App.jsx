import { useState, useEffect, useRef } from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  useLocation,
  Link,
} from "react-router-dom";
import { Bars3Icon } from "@heroicons/react/24/outline";
import Sidebar from "./components/Sidebar";
import Home from "./pages/HomePage";
import SignIn from "./pages/SignIn";
import SignUp from "./pages/SignUp";
import Profile from "./pages/ProfilePage";
import MyLabs from "./pages/MyLabsPage";
import MyArticles from "./pages/MyArticlesPage";
import LabPage from "./pages/LabPage";
import ArticlePage from "./pages/ArticlePage";
import BackgroundCircles from "./components/BackgroundCircles";

function AppContent() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [theme, setTheme] = useState("light");
  const sidebarRef = useRef();
  const location = useLocation();

  const showSidebar = !["/", "/signin"].includes(location.pathname);

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

  const toggleTheme = () => {
    const newTheme = theme === "light" ? "dark" : "light";
    setTheme(newTheme);
    localStorage.setItem("theme", newTheme);
    document.documentElement.classList.toggle("dark", newTheme === "dark");
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

            <div className="flex items-center">
              <Link to="/profile" className="flex items-center">
                <div className="w-10 h-10 rounded-full bg-msc flex items-center justify-center text-white text-lg cursor-pointer hover:bg-msc-hover">
                  <span>R</span>
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
        />
      )}
      <main
        className={`${showSidebar ? "p-4" : ""} transition-all duration-300 ${
          isSidebarOpen && showSidebar ? "ml-64" : "ml-0"
        }`}
      >
        <Routes>
          <Route path="/" element={<SignUp />} />
          <Route path="/signin" element={<SignIn />} />
          <Route path="/home" element={<Home />} />
          <Route path="/my-labs" element={<MyLabs />} />
          <Route path="/lab/:id" element={<LabPage />} />
          <Route path="/my-articles" element={<MyArticles />} />
          <Route path="/article/:id" element={<ArticlePage />} />
          <Route path="/profile" element={<Profile />} />
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
