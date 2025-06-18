import { NavLink } from "react-router-dom";
import { useState, useEffect } from "react";
import { MoonIcon, SunIcon, XMarkIcon } from "@heroicons/react/24/outline";

export default function Sidebar({
  isOpen,
  toggleSidebar,
  currentTheme,
  toggleTheme,
}) {
  const [activePath, setActivePath] = useState("");

  useEffect(() => {
    setActivePath(window.location.pathname);
  }, []);

  const navItems = [
    { path: "/home", name: "Home" },
    { path: "/my-labs", name: "My Labs" },
    { path: "/my-articles", name: "My Articles" },
    { path: "/profile", name: "Profile" },
  ];

  return (
    <div
      className={`fixed inset-y-0 left-0 transform ${
        isOpen ? "translate-x-0" : "-translate-x-full"
      } 
      w-64 bg-white dark:bg-gray-800 shadow-lg rounded-r-xl transition-transform duration-300 ease-in-out z-50`}
    >
      <div className="flex flex-col h-full p-4">
        <div className="flex justify-between items-center mb-8 p-4">
          <h1 className="text-xl font-bold text-gray-800 dark:text-white">
            Open Labs Share
          </h1>
        </div>

        <nav className="flex-1">
          <ul className="space-y-2">
            {navItems.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={`block px-4 py-3 rounded-lg transition-colors ${
                    activePath === item.path
                      ? "bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-200"
                      : "text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700"
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
          </ul>
        </nav>

        <div className="mt-auto p-4">
          <button
            onClick={toggleTheme}
            className="flex items-center justify-between w-full px-4 py-3 rounded-lg bg-gray-100 dark:bg-gray-700"
          >
            <span className="text-gray-700 dark:text-gray-300">
              {currentTheme === "dark" ? "Light Theme" : "Dark Theme"}
            </span>
            {currentTheme === "dark" ? (
              <SunIcon className="h-5 w-5 text-yellow-400" />
            ) : (
              <MoonIcon className="h-5 w-5 text-gray-600" />
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
