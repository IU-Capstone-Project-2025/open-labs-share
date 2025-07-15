import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signUp, validateSignUpData, isAuthenticated } from "../utils/auth";
import Point from "../components/Point";

export default function SignUp() {
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState("");
  const navigate = useNavigate();

  if (isAuthenticated()) {
    navigate("/home", { replace: true });
    return null;
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
    
    if (serverError) {
      setServerError("");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setServerError("");

    const validation = validateSignUpData(formData);
    
    if (!validation.isValid) {
      setErrors(validation.errors);
      setLoading(false);
      return;
    }

    try {
      await signUp(formData);
      navigate("/home", { replace: true });
    } catch (err) {
      setServerError(err.message || "Sign up failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col md:flex-row min-h-screen bg-white font-inter">
      {/* Hero section - responsive layout */}
      <div className="relative w-full md:w-1/2 bg-msc flex flex-col justify-center items-center text-white p-6 sm:p-12 md:p-12 overflow-hidden mb-0">
        <div className="absolute bottom-1/5 left-2/3 w-48 h-48 rounded-full bg-blue-blue opacity-15 blur-sm hidden sm:block"></div>
        <div className="absolute -bottom-48 -left-48 w-[600px] h-[600px] rounded-full bg-blue-blue opacity-15 blur-sm hidden sm:block"></div>
        <div className="absolute bottom-3/4 left-80 w-96 h-96 rounded-full bg-blue-blue opacity-15 blur-sm hidden sm:block"></div>

        <div className="relative z-10 text-center bg-msc/90 p-4 sm:p-6 md:p-0 rounded-xl max-w-xs sm:max-w-md md:max-w-lg mx-auto md:bg-transparent md:p-0">
          <h1 className="text-3xl sm:text-6xl md:text-6xl mb-3 sm:mb-6 md:mb-6">Open Labs Share</h1>
          <p className="text-base sm:text-xl md:text-xl mx-auto max-w-xs sm:max-w-md md:max-w-lg text-balance">
            A social knowledge network with peer review and personalized
            recommendations
          </p>
        </div>
      </div>

      {/* Form section - responsive layout */}
      <div className="w-full md:w-1/2 flex items-center justify-center p-4 sm:p-8 md:p-12">
        <div className="w-full max-w-md md:max-w-lg">
          <h1 className="text-2xl sm:text-3xl md:text-3xl font-bold mb-6 md:mb-8 text-gray-800">Sign Up</h1>
          
          {serverError && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-600 text-sm">{serverError}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 sm:space-y-6">
            {/* Stack name fields vertically on mobile */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-6">
              <div>
                <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                  First Name
                </label>
                <input
                  type="text"
                  name="firstName"
                  placeholder="Ryan"
                  value={formData.firstName}
                  onChange={handleChange}
                  className={`w-full text-sm sm:text-base border ${
                    errors.firstName ? "border-red-500" : "border-gray-300"
                  } rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                  required
                  disabled={loading}
                />
                {errors.firstName && (
                  <p className="mt-1 text-xs sm:text-sm text-red-600">{errors.firstName}</p>
                )}
              </div>
              <div>
                <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                  Last Name
                </label>
                <input
                  type="text"
                  name="lastName"
                  placeholder="Gosling"
                  value={formData.lastName}
                  onChange={handleChange}
                  className={`w-full text-sm sm:text-base border ${
                    errors.lastName ? "border-red-500" : "border-gray-300"
                  } rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                  required
                  disabled={loading}
                />
                {errors.lastName && (
                  <p className="mt-1 text-xs sm:text-sm text-red-600">{errors.lastName}</p>
                )}
              </div>
            </div>
            
            <div>
              <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                Username
              </label>
              <input
                type="text"
                name="username"
                placeholder="ryanGosling1980"
                value={formData.username}
                onChange={handleChange}
                className={`w-full text-sm sm:text-base border ${
                  errors.username ? "border-red-500" : "border-gray-300"
                } rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                required
                disabled={loading}
              />
              {errors.username && (
                <p className="mt-1 text-xs sm:text-sm text-red-600">{errors.username}</p>
              )}
            </div>
            
            <div>
              <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                Email
              </label>
              <input
                type="email"
                name="email"
                placeholder="gosl1980@mail.com"
                value={formData.email}
                onChange={handleChange}
                className={`w-full text-sm sm:text-base border ${
                  errors.email ? "border-red-500" : "border-gray-300"
                } rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                required
                disabled={loading}
              />
              {errors.email && (
                <p className="mt-1 text-xs sm:text-sm text-red-600">{errors.email}</p>
              )}
            </div>

            {/* Stack password fields vertically on mobile */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-6">
              <div>
                <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                  Password
                </label>
                <input
                  type="password"
                  name="password"
                  placeholder="Enter 8 character or more"
                  value={formData.password}
                  onChange={handleChange}
                  className={`w-full text-sm sm:text-base border ${
                    errors.password ? "border-red-500" : "border-gray-300"
                  } rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                  required
                  disabled={loading}
                />
                {errors.password && (
                  <p className="mt-1 text-xs sm:text-sm text-red-600">{errors.password}</p>
                )}
              </div>
              <div>
                <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                  Confirm Password
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  placeholder="Enter 8 character or more"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className={`w-full text-sm sm:text-base border ${
                    errors.confirmPassword
                      ? "border-red-500"
                      : "border-gray-300"
                  } rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                  required
                  disabled={loading}
                />
                {errors.confirmPassword && (
                  <p className="mt-1 text-xs sm:text-sm text-red-600">
                    {errors.confirmPassword}
                  </p>
                )}
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full text-sm sm:text-base bg-msc hover:bg-msc-hover text-white py-2 sm:py-3 px-4 rounded-lg shadow-md transition-colors duration-300 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? "Creating Account..." : "Create Account"}
            </button>
          </form>

          {/* Demo accounts info - adjusted text size */}
          <div className="mt-4 sm:mt-6 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p className="text-xs sm:text-sm text-yellow-800">
              <strong>Note:</strong> Demo accounts already exist for testing:
              <br />• Username: demouser, ryanGosling1980, sarahjohnson
              <br />• Email: demo@example.com, gosl1980@mail.com
            </p>
          </div>

          <div className="mt-6 sm:mt-8">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white">
                  <Point></Point>
                </span>
              </div>
            </div>
            <div className="mt-3 sm:mt-4 text-center">
              <Link
                to="/signin"
                className="text-sm sm:text-base font-medium text-blue-blue hover:text-blue-hover transition-colors duration-300"
              >
                Account already exists
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}