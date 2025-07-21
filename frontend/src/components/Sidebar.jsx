import { NavLink, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { MoonIcon, SunIcon, ChevronDownIcon, ArrowRightOnRectangleIcon } from "@heroicons/react/24/outline";
import GemIcon from "./GemIcon";
import { signOut } from "../utils/auth";
import { useUser } from '../hooks/useUser';
import { DocumentTextIcon, NewspaperIcon, DocumentCheckIcon, PencilSquareIcon } from "@heroicons/react/24/outline";

export default function Sidebar({
  isOpen,
  toggleSidebar,
  currentTheme,
  toggleTheme,
  isMobile = false,
}) {
  const [activePath, setActivePath] = useState("");
  const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState(false);
  const [isCreateDropdownOpen, setIsCreateDropdownOpen] = useState(false);
  const [isHoveringSidebar, setIsHoveringSidebar] = useState(false);
  const navigate = useNavigate();
  const user = useUser();

  useEffect(() => {
    setActivePath(window.location.pathname);
  }, []);

  useEffect(() => {
    const handleWheel = (e) => {
      if (isHoveringSidebar) {
        // Prevent page scroll when hovering sidebar
        e.preventDefault();
        
        // Get the sidebar content element
        const sidebarContent = document.querySelector('.sidebar-content');
        if (sidebarContent) {
          const scrollAmount = e.deltaY;
          sidebarContent.scrollTop += scrollAmount;
        }
      }
    };

    // Add wheel event listener with passive: false to allow preventDefault
    document.addEventListener('wheel', handleWheel, { passive: false });

    return () => {
      document.removeEventListener('wheel', handleWheel);
    };
  }, [isHoveringSidebar]);

  const navItems = [
    { path: "/home", name: "Home" },
    { path: "/all-labs", name: "All Labs" },
    { path: "/all-articles", name: "All Articles" },
    { path: "/reviews", name: "Review Queue"},
    {
      name: "Create",
      dropdown: [
        { path: "/create-lab", name: "Create Lab" },
        { path: "/create-article", name: "Create Article" },
      ],
    },
  ];

  const profileDropdownItems = [    
    { path: "/profile", name: "Change Info" },
    { path: "/my-labs", name: "My Labs" },
    { path: "/my-articles", name: "My Articles" },
    { path: "/submissions/my", name: "My Submissions" },
    { path: "/feedback/my", name: "My Feedbacks" },
  ];

  const isProfileActive = profileDropdownItems.some(item => item.path === activePath);
  const isCreateActive = navItems.find(item => item.name === 'Create')?.dropdown?.some(subItem => subItem.path === activePath);

  const handleProfileClick = () => {
    setIsProfileDropdownOpen(!isProfileDropdownOpen);
  };

  const handleCreateClick = () => {
    setIsCreateDropdownOpen(!isCreateDropdownOpen);
  };

  const handleDropdownItemClick = (path) => {
    setActivePath(path);
    toggleSidebar();
  };

  const handleLogout = async () => {
    try {
      await signOut();
      
      toggleSidebar();
      navigate("/", { replace: true });
    } catch (error) {
      console.error('Logout error:', error);
      
      toggleSidebar();
      navigate("/", { replace: true });
    }
  };

  
  const getUserDisplayName = () => {
    if (!user) return "User";
    const fullName = `${user.firstName || ""} ${user.lastName || ""}`.trim();
    return fullName || user.username || "User";
  };
  
  
  const getUserInitials = () => {
    if (!user) return "?";
    const firstInitial = user.firstName?.charAt(0)?.toUpperCase() || "";
    const lastInitial = user.lastName?.charAt(0)?.toUpperCase() || "";
    return firstInitial && lastInitial ? `${firstInitial}${lastInitial}` : (user.username?.charAt(0)?.toUpperCase() || "?");
  };

  return (
    <div
      className={
        isMobile
          ? `fixed left-0 top-0 w-4/5 max-w-xs h-[95vh] my-5 bg-msc z-[9999] transition-transform duration-300 ease-in-out ${
              isOpen ? "translate-x-0" : "-translate-x-full"
            } overflow-y-auto rounded-r-xl`
          : `fixed inset-y-0 left-0 transform ${
              isOpen ? "translate-x-0" : "-translate-x-full"
            } w-64 bg-msc shadow-lg rounded-r-md transition-transform duration-300 ease-in-out z-50`
      }
      onMouseEnter={() => setIsHoveringSidebar(true)}
      onMouseLeave={() => setIsHoveringSidebar(false)}
    >
      {/* Close button for mobile */}
      {isMobile && (
        <button
          className="absolute top-4 right-4 z-[10000] p-2 rounded-full bg-white bg-opacity-20 hover:bg-opacity-40 text-white focus:outline-none"
          onClick={toggleSidebar}
          aria-label="Close sidebar"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      )}
      <div className={`flex flex-col h-full ${isMobile ? "p-4 overflow-y-auto" : "p-4 sidebar-content overflow-y-auto"}`}>
        <div className="flex justify-between items-center mb-8 p-4">
          <h1 className="text-2xl font-semibold text-white text-center font-inter font-light">
            Open Labs Share
          </h1>
        </div>

        {/* User Info Section */}
        {user && (
          <div className="mb-6 p-4 bg-white bg-opacity-10 rounded-lg">
            <div className="flex items-center space-x-3 mb-3">
              <div className="w-10 h-10 rounded-full bg-white bg-opacity-20 flex items-center justify-center text-white text-sm font-medium">
                {getUserInitials()}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-white font-medium text-sm truncate">
                  {getUserDisplayName()}
                </p>
                <p className="text-white text-opacity-70 text-xs truncate">
                  @{user.username || 'user'}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2 text-white text-opacity-90">
              <GemIcon className="h-4 w-4" color="rgba(255, 255, 255, 0.9)" />
              <span className="text-xs font-medium">{user.balance || 0} points</span>
            </div>
          </div>
        )}

        <nav className="flex-1">
          <ul className="space-y-2">
            {navItems.map((item) => (
              <li key={item.name}>
                {item.dropdown ? (
                  
                  <>
                    <button
                      onClick={handleCreateClick}
                      className={`w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors font-inter ${
                        isCreateActive
                          ? "bg-blue-blue bg-opacity-20 text-white backdrop-blur-lg font-medium"
                          : "text-white hover:bg-white hover:bg-opacity-10 font-light"
                      }`}
                    >
                      <span>{item.name}</span>
                      <ChevronDownIcon
                        className={`h-4 w-4 transition-transform duration-200 ${
                          isCreateDropdownOpen ? "rotate-180" : ""
                        }`}
                      />
                    </button>
                    {isCreateDropdownOpen && (
                      <ul className="mt-2 ml-4 space-y-1">
                        {item.dropdown.map((subItem) => (
                          <li key={subItem.path}>
                            <NavLink
                              to={subItem.path}
                              className={`block px-4 py-2 rounded-lg transition-colors font-inter text-sm ${
                                activePath === subItem.path
                                  ? "bg-blue-blue bg-opacity-30 text-white font-medium"
                                  : "text-white hover:bg-white hover:bg-opacity-10 font-light"
                              }`}
                              onClick={() => {
                                handleDropdownItemClick(subItem.path);
                                if (isMobile) toggleSidebar();
                              }}
                            >
                              {subItem.name}
                            </NavLink>
                          </li>
                        ))}
                      </ul>
                    )}
                  </>
                ) : (
                  
                  <NavLink
                    to={item.path}
                    className={`block px-4 py-3 rounded-lg transition-colors font-inter ${
                      activePath === item.path
                        ? "bg-blue-blue bg-opacity-20 text-white backdrop-blur-lg font-medium"
                        : "text-white hover:bg-white hover:bg-opacity-10 font-light"
                    }`}
                    onClick={() => {
                      setActivePath(item.path);
                      if (isMobile) toggleSidebar();
                      else toggleSidebar();
                    }}
                  >
                    {item.name}
                  </NavLink>
                )}
              </li>
            ))}
            
            {/* Profile Dropdown */}
            <li>
              <button
                onClick={handleProfileClick}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors font-inter ${
                  isProfileActive
                    ? "bg-blue-blue bg-opacity-20 text-white backdrop-blur-lg font-medium"
                    : "text-white hover:bg-white hover:bg-opacity-10 font-light"
                }`}
              >
                <span>Profile</span>
                <ChevronDownIcon 
                  className={`h-4 w-4 transition-transform duration-200 ${
                    isProfileDropdownOpen ? "rotate-180" : ""
                  }`} 
                />
              </button>
              
              {/* Dropdown Items */}
              {isProfileDropdownOpen && (
                <ul className="mt-2 ml-4 space-y-1">
                  {profileDropdownItems.map((item) => (
                    <li key={item.path}>
                      <NavLink
                        to={item.path}
                        className={`block px-4 py-2 rounded-lg transition-colors font-inter text-sm ${
                          activePath === item.path
                            ? "bg-blue-blue bg-opacity-30 text-white font-medium"
                            : "text-white hover:bg-white hover:bg-opacity-10 font-light"
                        }`}
                        onClick={() => handleDropdownItemClick(item.path)}
                      >
                        {item.name}
                      </NavLink>
                    </li>
                  ))}
                </ul>
              )}
            </li>
          </ul>
        </nav>

        <div className="mt-auto">
          {/* Logout Button outside Profile Dropdown */}
          <button
            onClick={handleLogout}
            className="w-full flex items-center space-x-2 px-4 py-3 rounded-lg transition-colors font-inter text-sm text-white hover:bg-red-500 hover:bg-opacity-20 font-light"
          >
            <ArrowRightOnRectangleIcon className="h-5 w-5" />
            <span>Sign Out</span>
          </button>
          
          <div className="p-2 flex justify-center">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-full bg-white dark:bg-gray-100 bg-opacity-10 dark:bg-opacity-100 hover:bg-opacity-20 dark:hover:bg-opacity-80 transition-colors"
            >
              {currentTheme === "dark" ? (
                <SunIcon className="h-6 w-6 text-gray-900" />
              ) : (
                <MoonIcon className="h-6 w-6 text-white" />
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
