import ArticleCard from "../components/ArticleCard";
import LabCard from "../components/LabCard";
import { useState, useEffect } from "react";
import { getCurrentUser, isAuthenticated, getUserProfile, notifyUserDataUpdate, updateProfile } from "../utils/auth";
import { usersAPI, labsAPI, articlesAPI } from "../utils/api";
import { BeakerIcon, EyeIcon } from "@heroicons/react/24/outline";
import GemIcon from "../components/GemIcon";
import ActivityBalance from "../components/ActivityBalance";
import ToastNotification from "../components/ToastNotification";

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
  const [userStats, setUserStats] = useState({
    pointsBalance: 0,
    labsSolved: 0,
    labsReviewed: 0,
  });
  const [toast, setToast] = useState({ show: false, message: '', type: 'info' });
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        setLoading(true);
        
        if (isAuthenticated()) {
          
          try {
            const userData = await getUserProfile();
            setUser(userData);

            setUserStats({
              pointsBalance: userData.balance || 0,
              labsSolved: userData.labsSolved || 0,
              labsReviewed: userData.labsReviewed || 0,
            });
            
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

            const currentUser = getCurrentUser();
            setUser(currentUser);


            setUserStats({
              pointsBalance: currentUser.balance || 0,
              labsSolved: currentUser.labsSolved || 0,
              labsReviewed: currentUser.labsReviewed || 0,
            });
            
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
          

          try {
            const labsResponse = await labsAPI.getMyLabs();
            console.log('ProfilePage: My labs response:', labsResponse);
            
            const myLabs = labsResponse.labs || labsResponse || [];
            console.log('ProfilePage: My labs:', myLabs);
            

            const articlesResponse = await articlesAPI.getMyArticles();
            const myArticles = articlesResponse.articles || [];
            

            const labsWithType = myLabs.map(lab => ({ ...lab, type: "lab" }));
            const articlesWithType = myArticles.map(article => ({ ...article, type: "article" }));
            
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
      

      if (formData.password) {
        updateData.password = formData.password;
      }
      
      if (user && user.id) {
        const response = await updateProfile(updateData);
        

        try {
          const freshUserData = await getUserProfile();
          setUser(freshUserData);
          

          notifyUserDataUpdate();
          
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

          const updatedUser = response.user;
          setUser(updatedUser);
          

          localStorage.setItem('user', JSON.stringify(updatedUser));
          notifyUserDataUpdate();
          
          setOriginalData({ ...formData, password: "", confirmPassword: "" });
          setFormData(prev => ({ ...prev, password: "", confirmPassword: "" }));
        }
        
        setEditMode(false);
        
        if (response.usernameChanged) {
          setToast({ show: true, message: "Profile updated successfully! New authentication tokens have been issued due to username change.", type: 'success' });
        } else {
          setToast({ show: true, message: "Profile updated successfully! Your current login session remains active.", type: 'success' });
        }
      }
    } catch (err) {
      console.error("Error updating profile:", err);
      setToast({ show: true, message: "Failed to update profile: " + err.message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFormData(originalData);
    setErrors({});
    setEditMode(false);
  };

  const handleDeleteProfile = () => {
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = async () => {
    try {
      if (user && user.id) {
        await usersAPI.deleteUser(user.id);
        

        localStorage.removeItem('authToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        
        setToast({ show: true, message: "Profile deleted successfully", type: 'success' });
        setTimeout(() => {
          window.location.href = '/signin';
        }, 2000);
      }
    } catch (err) {
      console.error("Error deleting profile:", err);
      setToast({ show: true, message: "Failed to delete profile: " + err.message, type: 'error' });
    } finally {
      setShowDeleteModal(false);
    }
  };

  const handleCancelDelete = () => {
    setShowDeleteModal(false);
  };

  const ConfirmationModal = () => {
    if (!showDeleteModal) return null;

    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md w-full animate-fade-in">
          <p className="text-gray-800 dark:text-gray-200 mb-4">
            Are you sure you want to delete your profile? This action cannot be undone.
          </p>
          <div className="flex justify-end space-x-3">
            <button
              onClick={handleCancelDelete}
              className="px-4 py-2 text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmDelete}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    );
  };

  const handleUploadClick = () => {
    setShowUploadModal(true);
  };

  const handleFileUpload = async () => {
    if (!uploadFile || !user) return;

    try {
      setUploading(true);
      
      if (uploadFileType === 'lab') {

        setToast({ show: true, message: 'To upload a lab, please use the "Create Lab" feature. Redirecting you now...', type: 'info' });
        setTimeout(() => {
          window.location.href = '/create-lab';
        }, 2000);
      } else if (uploadFileType === 'article') {

        setToast({ show: true, message: 'Article upload functionality will be available when the Articles Service is fully integrated.', type: 'warning' });
      }
      

      setUploadFile(null);
      setUploadFileType('lab');
      setShowUploadModal(false);
    } catch (err) {
      console.error("Upload error:", err);
      setToast({ show: true, message: "Upload failed: " + err.message, type: 'error' });
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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {toast.show && (
        <ToastNotification
          message={toast.message}
          type={toast.type}
          onClose={() => setToast({ show: false, message: '', type: 'info' })}
        />
      )}
      
      <ConfirmationModal />
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <header className="mb-8">
          <h1 className="text-4xl font-extrabold text-gray-900 dark:text-white tracking-tight">
            Profile
          </h1>
        </header>
        
        <div className="space-y-8">
          {user && <ActivityBalance stats={userStats} />}

          {/* Edit Profile & Materials Section */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Left Column: Edit Profile */}
            <div className="lg:col-span-1">
              <div className="bg-white dark:bg-gray-800/50 rounded-2xl shadow-lg p-8">
                <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
                  Profile Information
                </h2>
                {!editMode ? (
                  <div>
                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-500 dark:text-gray-400">First Name</label>
                        <p className="mt-1 text-lg text-gray-900 dark:text-white">{formData.firstName || "Not set"}</p>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-500 dark:text-gray-400">Last Name</label>
                        <p className="mt-1 text-lg text-gray-900 dark:text-white">{formData.lastName || "Not set"}</p>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-500 dark:text-gray-400">Username</label>
                        <p className="mt-1 text-lg text-gray-900 dark:text-white">@{formData.username}</p>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-500 dark:text-gray-400">Email</label>
                        <p className="mt-1 text-lg text-gray-900 dark:text-white">{formData.email}</p>
                      </div>
                    </div>
                    <button
                      onClick={() => setEditMode(true)}
                      className="mt-6 w-full px-4 py-2 bg-msc text-white dark:bg-white dark:text-msc rounded-lg hover:bg-blue-700 transition-colors"
                    >
                      Edit Profile
                    </button>
                  </div>
                ) : (
                  <form onSubmit={handleSave} className="space-y-4">
                    {/* Form fields... */}
                    <div>
                      <label htmlFor="firstName" className="block text-sm font-medium text-gray-600 dark:text-gray-300">
                        First Name
                      </label>
                      <input
                        type="text"
                        name="firstName"
                        id="firstName"
                        value={formData.firstName}
                        onChange={handleChange}
                        className="mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-msc focus:border-msc"
                      />
                    </div>
                    <div>
                      <label htmlFor="lastName" className="block text-sm font-medium text-gray-600 dark:text-gray-300">
                        Last Name
                      </label>
                      <input
                        type="text"
                        name="lastName"
                        id="lastName"
                        value={formData.lastName}
                        onChange={handleChange}
                        className="mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-msc focus:border-msc"
                      />
                    </div>
                    <div>
                      <label htmlFor="username" className="block text-sm font-medium text-gray-600 dark:text-gray-300">
                        Username
                      </label>
                      <input
                        type="text"
                        name="username"
                        id="username"
                        value={formData.username}
                        onChange={handleChange}
                        className={`mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border ${errors.username ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'} rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-msc focus:border-msc`}
                      />
                      {errors.username && <p className="mt-1 text-sm text-red-500">{errors.username}</p>}
                    </div>
                    <div>
                      <label htmlFor="email" className="block text-sm font-medium text-gray-600 dark:text-gray-300">
                        Email
                      </label>
                      <input
                        type="email"
                        name="email"
                        id="email"
                        value={formData.email}
                        onChange={handleChange}
                        className="mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-msc focus:border-msc"
                      />
                    </div>
                    <div>
                      <label htmlFor="password" d className="block text-sm font-medium text-gray-600 dark:text-gray-300">
                        New Password (optional)
                      </label>
                      <input
                        type="password"
                        name="password"
                        id="password"
                        value={formData.password}
                        onChange={handleChange}
                        className={`mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border ${errors.password ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'} rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-msc focus:border-msc`}
                      />
                      {errors.password && <p className="mt-1 text-sm text-red-500">{errors.password}</p>}
                    </div>
                    <div>
                      <label htmlFor="confirmPassword" d className="block text-sm font-medium text-gray-600 dark:text-gray-300">
                        Confirm New Password
                      </label>
                      <input
                        type="password"
                        name="confirmPassword"
                        id="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        className={`mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border ${errors.confirmPassword ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'} rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-msc focus:border-msc`}
                      />
                      {errors.confirmPassword && <p className="mt-1 text-sm text-red-500">{errors.confirmPassword}</p>}
                    </div>
                    <div className="flex justify-end space-x-3 pt-4">
                      <button
                        type="button"
                        onClick={handleCancel}
                        className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        disabled={saving}
                        className="mt-6 w-full px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors"
                      >
                        {saving ? "Saving..." : "Save Changes"}
                      </button>
                    </div>
                  </form>
                )}
              </div>
              {/* <div className="mt-8">
                <button
                  onClick={handleDeleteProfile}
                  className="w-full text-left px-4 py-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-700 dark:text-red-300 hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors"
                >
                  <h3 className="font-semibold">Delete Account</h3>
                  <p className="text-sm mt-1">Permanently delete your account and all associated data. This action cannot be undone.</p>
                </button>
              </div> */}
            </div>

            {/* Right Column: My Materials */}
            <div className="lg:col-span-2">
              <div className="bg-white dark:bg-gray-800/50 rounded-2xl shadow-lg p-8">
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-2xl font-bold text-gray-800 dark:text-white">My Materials</h2>
                  <div className="flex space-x-2">
                    <button 
                      onClick={handleUploadClick}
                      className="px-4 py-2 bg-msc text-white dark:bg-white dark:text-msc rounded-lg hover:bg-msc-hover transition-colors"
                    >
                      Upload New
                    </button>
                    <div className="flex rounded-lg bg-gray-100 dark:bg-gray-700 p-1">
                      <button
                        onClick={() => setContentFilter("all")}
                        className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${
                          contentFilter === "all" ? "bg-white dark:bg-gray-600 text-gray-800 dark:text-white shadow" : "text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                        }`}
                      >
                        All
                      </button>
                      <button
                        onClick={() => setContentFilter("lab")}
                        className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${
                          contentFilter === "lab" ? "bg-white dark:bg-gray-600 text-gray-800 dark:text-white shadow" : "text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                        }`}
                      >
                        Labs
                      </button>
                      <button
                        onClick={() => setContentFilter("article")}
                        className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${
                          contentFilter === "article" ? "bg-white dark:bg-gray-600 text-gray-800 dark:text-white shadow" : "text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                        }`}
                      >
                        Articles
                      </button>
                    </div>
                  </div>
                </div>

                {loading ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {Array(4).fill(null).map((_, i) => (
                      <div key={i} className="h-40 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse" />
                    ))}
                  </div>
                ) : filteredMaterials.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {filteredMaterials.map((item) =>
                      item.type === "lab" ? (
                        <LabCard key={item.id} lab={item} />
                      ) : (
                        <ArticleCard key={item.id} article={item} />
                      )
                    )}
                  </div>
                ) : (
                  <div className="text-center py-12">
                    <p className="text-gray-500 dark:text-gray-400">
                      You haven't uploaded any materials yet.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
        
        {/* Upload Modal */}
        {showUploadModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl p-8 max-w-lg w-full">
              <h2 className="text-2xl font-bold mb-4 text-gray-900 dark:text-white">Upload New Material</h2>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  What are you uploading?
                </label>
                <select 
                  value={uploadFileType}
                  onChange={(e) => setUploadFileType(e.target.value)}
                  className="w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm"
                >
                  <option value="lab">Lab</option>
                  <option value="article">Article</option>
                </select>
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Select File
                </label>
                <input 
                  type="file"
                  onChange={(e) => setUploadFile(e.target.files[0])}
                  className="w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                />
              </div>
              <div className="flex justify-end space-x-3">
                <button
                  onClick={() => setShowUploadModal(false)}
                  className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Cancel
                </button>
                <button
                  onClick={handleFileUpload}
                  disabled={!uploadFile || uploading}
                  className="px-4 py-2 bg-msc text-white rounded-md disabled:opacity-50"
                >
                  {uploading ? 'Uploading...' : 'Upload'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
