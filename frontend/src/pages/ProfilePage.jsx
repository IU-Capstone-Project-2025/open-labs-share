import ArticleCard from "../components/ArticleCard";
import LabCard from "../components/LabCard";
import { useState, useEffect } from "react";
import { getCurrentUser, isAuthenticated, getUserProfile } from "../utils/auth";
import { usersAPI, labsAPI } from "../utils/api";

export default function ProfilePage() {
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [originalData, setOriginalData] = useState({});
  const [errors, setErrors] = useState({});
  const [contentFilter, setContentFilter] = useState("all");
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadFileType, setUploadFileType] = useState('lab');
  const [uploadFile, setUploadFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [user, setUser] = useState(null);
  const [userContent, setUserContent] = useState([]);

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        setLoading(true);
        
        if (isAuthenticated()) {
          // Fetch fresh profile data from auth service for consistency
          try {
            const userData = await getUserProfile();
            setUser(userData);
            
            const profileData = {
              firstName: userData.firstName || "",
              lastName: userData.lastName || "",
              username: userData.username || "",
              email: userData.email || "",
              password: "",
              confirmPassword: "",
            };
            
            setFormData(profileData);
            setOriginalData(profileData);
          } catch (err) {
            console.error("Could not fetch user profile:", err);
            // Fallback to current user data from localStorage
            const currentUser = getCurrentUser();
            setUser(currentUser);
            
            const profileData = {
              firstName: currentUser.firstName || "",
              lastName: currentUser.lastName || "",
              username: currentUser.username || "",
              email: currentUser.email || "",
              password: "",
              confirmPassword: "",
            };
            
            setFormData(profileData);
            setOriginalData(profileData);
          }
          
          // Fetch user's content (labs)
          try {
            const labsResponse = await labsAPI.getMyLabs();
            console.log('ProfilePage: My labs response:', labsResponse);
            
            const myLabs = labsResponse.labs || labsResponse || [];
            console.log('ProfilePage: My labs:', myLabs);
            
            // For articles - try to fetch real articles if available, otherwise use empty array
            // TODO: Replace with real articles API when articles service is implemented
            const mockArticles = [];
            
            // Add type field to distinguish between labs and articles
            const labsWithType = myLabs.map(lab => ({ ...lab, type: "lab" }));
            const articlesWithType = mockArticles.map(article => ({ ...article, type: "article" }));
            
            console.log('ProfilePage: Labs with type:', labsWithType);
            console.log('ProfilePage: Articles with type:', articlesWithType);
            
            setUserContent([...labsWithType, ...articlesWithType]);
          } catch (err) {
            console.error("Error fetching user content:", err);
            setUserContent([]);
          }
        }
      } catch (err) {
        console.error("Error fetching user data:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, []);

  const filteredMaterials =
    contentFilter === "all"
      ? userContent
      : userContent.filter((item) => item?.type === contentFilter);

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

  const handleSave = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    try {
      setSaving(true);
      
      const updateData = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        username: formData.username,
        email: formData.email,
      };
      
      // Only include password if it's provided
      if (formData.password) {
        updateData.password = formData.password;
      }
      
      if (user && user.id) {
        const response = await usersAPI.updateUser(user.id, updateData);
        
        // After successful update, fetch fresh data from auth service
        try {
          const freshUserData = await getUserProfile();
          setUser(freshUserData);
          
          const refreshedProfileData = {
            firstName: freshUserData.firstName || "",
            lastName: freshUserData.lastName || "",
            username: freshUserData.username || "",
            email: freshUserData.email || "",
            password: "",
            confirmPassword: "",
          };
          
          setFormData(refreshedProfileData);
          setOriginalData(refreshedProfileData);
        } catch (err) {
          console.warn("Could not fetch fresh profile data after update:", err);
          // Fallback to response data
          const updatedUser = response.userInfo || response;
          setUser(updatedUser);
          setOriginalData({ ...formData, password: "", confirmPassword: "" });
          setFormData(prev => ({ ...prev, password: "", confirmPassword: "" }));
        }
        
        setEditMode(false);
        
        if (response.usernameChanged) {
          alert("Profile updated successfully! New authentication tokens have been issued due to username change.");
        } else {
          alert("Profile updated successfully! Your current login session remains active.");
        }
      }
    } catch (err) {
      console.error("Error updating profile:", err);
      alert("Failed to update profile: " + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFormData(originalData);
    setErrors({});
    setEditMode(false);
  };

  const handleDeleteProfile = async () => {
    if (window.confirm("Are you sure you want to delete your profile? This action cannot be undone.")) {
      try {
        if (user && user.id) {
          await usersAPI.deleteUser(user.id);
          
          // Clear local storage and redirect to signin
          localStorage.removeItem('authToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          
          alert("Profile deleted successfully");
          window.location.href = '/signin';
        }
      } catch (err) {
        console.error("Error deleting profile:", err);
        alert("Failed to delete profile: " + err.message);
      }
    }
  };

  const handleUploadClick = () => {
    setShowUploadModal(true);
  };

  const handleFileUpload = async () => {
    if (!uploadFile || !user) return;

    try {
      setUploading(true);
      
      if (uploadFileType === 'lab') {
        // Redirect to lab creation page with the file
        alert('To upload a lab, please use the "Create Lab" feature. Redirecting you now...');
        window.location.href = '/create-lab';
      } else if (uploadFileType === 'article') {
        // For articles, show message that articles service needs to be implemented
        alert('Article upload functionality will be available when the Articles Service is fully integrated.');
      }
      
      // Reset form
      setUploadFile(null);
      setUploadFileType('lab');
      setShowUploadModal(false);
    } catch (err) {
      console.error("Upload error:", err);
      alert("Upload failed: " + err.message);
    } finally {
      setUploading(false);
    }
  };

  if (!isAuthenticated()) {
    return (
      <div className="relative min-h-screen font-inter dark:bg-gray-900 py-10 px-6 bg-transparent">
        <div className="max-w-6xl mx-auto">
          <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
              Profile
            </h1>
            <div className="text-center py-8">
              <p className="text-gray-600 dark:text-gray-400">Please sign in to view your profile.</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="relative min-h-screen font-inter dark:bg-gray-900 py-10 px-6 bg-transparent">
        <div className="max-w-6xl mx-auto">
          <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
            <h1 className="text-3xl font-bold text-msc dark:text-white mb-6">
              Profile
            </h1>
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-msc mx-auto"></div>
              <p className="text-gray-600 dark:text-gray-400 mt-4">Loading profile...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

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
          {filteredMaterials.length === 0 ? (
            <div className="col-span-full text-center py-12">
              <div className="mb-4">
                <svg className="mx-auto h-16 w-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 48 48">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7h6m0 0v12m0-12l-6 6m6-6l6 6" />
                </svg>
              </div>
              <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">
                No {contentFilter === 'all' ? 'materials' : contentFilter + 's'} uploaded yet
              </h3>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                {contentFilter === 'lab' || contentFilter === 'all' 
                  ? 'Start sharing your knowledge by creating your first lab!' 
                  : 'Upload your first ' + contentFilter + ' to get started.'}
              </p>
              <button
                onClick={() => window.location.href = '/create-lab'}
                className="px-6 py-3 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors flex items-center mx-auto space-x-2"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                <span>Create Your First Lab</span>
              </button>
            </div>
          ) : (
            filteredMaterials.map((item, index) => {
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
            })
          )}
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
                <select
                  value={uploadFileType}
                  onChange={(e) => setUploadFileType(e.target.value)}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg py-2 px-3 dark:bg-gray-700 dark:text-white"
                >
                  <option value="lab">Lab</option>
                  <option value="article">Article</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Choose File
                </label>
                <input
                  type="file"
                  onChange={(e) => setUploadFile(e.target.files[0])}
                  className="w-full"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  onClick={() => setShowUploadModal(false)}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  onClick={handleFileUpload}
                  className="px-4 py-2 bg-msc hover:bg-msc-hover text-white rounded-lg"
                  disabled={!uploadFile || uploading}
                >
                  {uploading ? "Uploading..." : "Upload"}
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
