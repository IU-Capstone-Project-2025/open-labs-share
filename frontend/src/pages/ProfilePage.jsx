import ArticleCard from "../components/ArticleCard";
import LabCard from "../components/LabCard";
import { useState } from "react";

export default function MyArticles() {
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState({
    firstName: "Ryan",
    lastName: "Gosling",
    username: "ryanGosling1980",
    email: "gosl1980@mail.com",
    password: "",
    confirmPassword: "",
  });
  const [errors, setErrors] = useState({});
  const [contentFilter, setContentFilter] = useState("all");
  const [showUploadModal, setShowUploadModal] = useState(false);

  const sampleArticle = {
    id: 1,
    title: "Article 4: Scheduling",
    description:
      "Everyday practice shows that the beginning of daily work on the formation",
    author: {
      firstName: "Ryan",
      lastName: "Gosling",
    },
    type: "article",
  };
  const sampleLab = {
    id: 1,
    title: "Lab 6: Scheduling task",
    description:
      "Everyday practice shows that the beginning of daily work on the formation",
    author: {
      firstName: "Ryan",
      lastName: "Gosling",
    },
    type: "lab",
  };

  const allMaterials = [sampleArticle, sampleLab, ...Array(4).fill(null)];
  const filteredMaterials =
    contentFilter === "all"
      ? allMaterials
      : allMaterials.filter((item) => item?.type === contentFilter);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    let isValid = true;

    if (!formData.username.trim()) {
      newErrors.username = "Username is required";
      isValid = false;
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.username)) {
      newErrors.username = "Only letters, numbers and _ are allowed";
      isValid = false;
    }

    if (formData.password && formData.password.length < 8) {
      newErrors.password = "Password must be at least 8 characters";
      isValid = false;
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = "Passwords do not match";
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  const handleSave = (e) => {
    e.preventDefault();
    if (validateForm()) {
      setEditMode(false);
      //save to backend
    }
  };

  const handleDeleteProfile = () => {
    //delete profile logic
    console.log("Profile deletion requested");
  };

  const handleUploadClick = () => {
    setShowUploadModal(true);
  };

  return (
    <div className="relative min-h-screen font-inter dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="max-w-6xl mx-auto">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg mb-6">
          <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
            Profile
          </h1>
        </div>

        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-xl font-bold text-msc dark:text-white">
            Edit user information
          </h1>
        </div>

        {editMode ? (
          <form className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-md font-medium text-gray-700 dark:text-gray-300 mb-1">
                  First Name
                </label>
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent dark:bg-gray-800 dark:text-white"
                  required
                />
              </div>
              <div>
                <label className="block text-md font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Last Name
                </label>
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent dark:bg-gray-800 dark:text-white"
                  required
                />
              </div>
            </div>

            <div className="mt-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Username
              </label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                className={`w-full border ${
                  errors.username
                    ? "border-red-500"
                    : "border-gray-300 dark:border-gray-600"
                } rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent dark:bg-gray-800 dark:text-white`}
                required
                pattern="[a-zA-Z0-9_]+"
                title="Only letters, numbers and underscore are allowed"
              />
              {errors.username && (
                <p className="mt-1 text-sm text-red-600">{errors.username}</p>
              )}
            </div>

            <div className="mt-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Email
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="w-full border border-gray-300 dark:border-gray-600 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent dark:bg-gray-800 dark:text-white"
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-md font-medium text-gray-700 dark:text-gray-300 mb-1">
                  New Password (leave blank to keep current)
                </label>
                <input
                  type="password"
                  name="password"
                  placeholder="Enter 8 characters or more"
                  value={formData.password}
                  onChange={handleChange}
                  className={`w-full border ${
                    errors.password
                      ? "border-red-500"
                      : "border-gray-300 dark:border-gray-600"
                  } rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent dark:bg-gray-800 dark:text-white`}
                />
                {errors.password && (
                  <p className="mt-1 text-sm text-red-600">{errors.password}</p>
                )}
              </div>
              <div>
                <label className="block text-md font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Confirm Password
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  placeholder="Confirm your password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className={`w-full border ${
                    errors.confirmPassword
                      ? "border-red-500"
                      : "border-gray-300 dark:border-gray-600"
                  } rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent dark:bg-gray-800 dark:text-white`}
                />
                {errors.confirmPassword && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.confirmPassword}
                  </p>
                )}
              </div>
            </div>

            <div className="flex justify-end space-x-4 pt-4">
              <button
                type="button"
                onClick={handleDeleteProfile}
                className="px-6 py-2 border border-red-700 text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20 font-medium rounded-lg transition-colors duration-300"
                style={{ color: "#A40D0D" }}
              >
                Delete Profile
              </button>
              <button
                type="submit"
                onClick={handleSave}
                className="px-6 py-2 bg-msc hover:bg-msc-hover text-white font-medium rounded-lg shadow-md transition-colors duration-300"
              >
                Save Changes
              </button>
            </div>
          </form>
        ) : (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                  First Name
                </p>
                <p className="text-lg text-msc   font-semibold dark:text-white">
                  {formData.firstName}
                </p>
              </div>
              <div className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                  Last Name
                </p>
                <p className="text-lg text-msc font-semibold dark:text-white">
                  {formData.lastName}
                </p>
              </div>
            </div>
            <div className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                Username
              </p>
              <p className="text-lg text-msc font-semibold dark:text-white">
                @{formData.username}
              </p>
            </div>
            <div className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">
                Email
              </p>
              <p className="text-lg text-msc font-semibold dark:text-white">
                {formData.email}
              </p>
            </div>
          </div>
        )}
        <div className="flex justify-end items-center mt-4">
          {!editMode && (
            <button
              onClick={() => setEditMode(true)}
              className="px-6 py-2 bg-msc hover:bg-msc-hover text-white font-medium rounded-lg shadow-md transition-colors duration-300"
            >
              Edit Profile
            </button>
          )}
        </div>
      </div>

        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
          <h1 className="text-2xl font-bold text-msc dark:text-white">
            My uploaded materials
          </h1>
          <div className="flex items-center gap-4 w-full sm:w-auto">
            <div className="relative flex-1 sm:flex-none">
              <select
                value={contentFilter}
                onChange={(e) => setContentFilter(e.target.value)}
                className="appearance-none bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg py-2 pl-4 pr-16 text-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
              >
                <option value="all">All</option>
                <option value="article">Articles</option>
                <option value="lab">Labs</option>
              </select>
              <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-700 dark:text-gray-300">
                <svg
                  className="fill-current h-4 w-4"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 20 20"
                >
                  <path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z" />
                </svg>
              </div>
            </div>
            <button
              onClick={handleUploadClick}
              className="px-8 py-2 bg-msc hover:bg-msc-hover font-inter text-white font-medium rounded-lg shadow-md transition-colors duration-300 whitespace-nowrap flex items-center gap-2"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z"
                  clipRule="evenodd"
                />
              </svg>
              Upload the new file
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          {filteredMaterials.map((item, index) => {
            if (!item) {
              return (
                <div
                  key={index}
                  className="h-32 bg-light-blue bg-opacity-40 dark:bg-gray-700 animate-pulse rounded-xl"
                />
              );
            }
            return item.type === "article" ? (
              <ArticleCard key={item.id} article={item} />
            ) : (
              <LabCard key={item.id} lab={item} />
            );
          })}
        </div>
      </div>

      {showUploadModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4 dark:text-white">
              Upload File
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Select File Type
                </label>
                <select className="w-full border border-gray-300 dark:border-gray-600 rounded-lg py-2 px-3 dark:bg-gray-700 dark:text-white">
                  <option value="article">Article</option>
                  <option value="lab">Lab</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Choose File
                </label>
                <input type="file" className="w-full" />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  onClick={() => setShowUploadModal(false)}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg"
                >
                  Cancel
                </button>
                <button className="px-4 py-2 bg-msc hover:bg-msc-hover text-white rounded-lg">
                  Upload
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
      </div>
    </div>
  );
}
