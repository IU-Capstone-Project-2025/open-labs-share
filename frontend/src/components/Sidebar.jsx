import { NavLink } from "react-router-dom";
import { useState, useEffect } from "react";
import { MoonIcon, SunIcon, ChevronDownIcon } from "@heroicons/react/24/outline";

export default function Sidebar({
  isOpen,
  toggleSidebar,
  currentTheme,
  toggleTheme,
}) {
  const [activePath, setActivePath] = useState("");
  const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState(false);

  useEffect(() => {
    setActivePath(window.location.pathname);
  }, []);

  const navItems = [
    { path: "/home", name: "Home" },
    { path: "/all-labs", name: "All Labs" },
    { path: "/all-articles", name: "All Articles" },
  ];

  const profileDropdownItems = [    
    { path: "/profile", name: "Change Info" },
    { path: "/my-labs", name: "My Labs" },
    { path: "/my-articles", name: "My Articles" },
  ];

  const isProfileActive = profileDropdownItems.some(item => item.path === activePath);

  const handleProfileClick = () => {
    setIsProfileDropdownOpen(!isProfileDropdownOpen);
  };

  const handleDropdownItemClick = (path) => {
    setActivePath(path);
    toggleSidebar();
  };

  return (
    <div
      className={`fixed inset-y-0 left-0 transform ${
        isOpen ? "translate-x-0" : "-translate-x-full"
      } 
      w-64 bg-msc shadow-lg rounded-r-md transition-transform duration-300 ease-in-out z-50`}
    >
      <div className="flex flex-col h-full p-4">
        <div className="flex justify-between items-center mb-8 p-4">
          <h1 className="text-2xl font-semibold text-white text-center font-inter font-light">
            Open Labs Share
          </h1>
        </div>

        <nav className="flex-1">
          <ul className="space-y-2">
            {navItems.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={`block px-4 py-3 rounded-lg transition-colors font-inter ${
                    activePath === item.path
                      ? "bg-blue-blue bg-opacity-20 text-white backdrop-blur-lg font-medium"
                      : "text-white hover:bg-white hover:bg-opacity-10 font-light"
                  }`}
                  onClick={() => {
                    setActivePath(item.path);
                    toggleSidebar();
                  }}
                >
                  {item.name}
                </NavLink>
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

        <div className="mt-auto p-4 flex justify-center">
          <button
            onClick={toggleTheme}
            className="p-2 rounded-full bg-white bg-opacity-10 hover:bg-opacity-20 transition-colors"
          >
            {currentTheme === "dark" ? (
              <SunIcon className="h-6 w-6 text-white" />
            ) : (
              <MoonIcon className="h-6 w-6 text-white" />
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
