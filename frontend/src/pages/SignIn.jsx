import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signIn, isAuthenticated } from "../utils/auth";
import Point from "../components/Point";

export default function SignIn() {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  if (isAuthenticated()) {
    navigate("/home", { replace: true });
    return null;
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    
    if (error) {
      setError("");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      await signIn(formData.email, formData.password);
      navigate("/home", { replace: true });
    } catch (err) {
      setError(err.message || "Sign in failed. Please try again.");
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
          <h1 className="text-2xl sm:text-3xl md:text-3xl font-bold mb-6 md:mb-8 text-gray-800">Welcome back</h1>
          
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 sm:space-y-6">
            <div>
              <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                Email or Username
              </label>
              <input
                type="text"
                name="email"
                placeholder="Enter your email or username"
                value={formData.email}
                onChange={handleChange}
                className="w-full text-sm sm:text-base border border-gray-300 rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
                required
                disabled={loading}
              />
            </div>
            <div>
              <label className="block text-sm sm:text-md font-medium text-gray-700 mb-1">
                Password
              </label>
              <input
                type="password"
                name="password"
                placeholder="Enter your password"
                value={formData.password}
                onChange={handleChange}
                className="w-full text-sm sm:text-base border border-gray-300 rounded-lg py-2 sm:py-3 px-3 sm:px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
                required
                disabled={loading}
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full text-sm sm:text-base bg-msc hover:bg-msc-hover text-white py-2 sm:py-3 px-4 rounded-lg shadow-md transition-colors duration-300 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? "Signing In..." : "Sign In"}
            </button>
          </form>

          {/* Demo credentials - adjusted text size */}
          <div className="mt-4 sm:mt-6 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <h3 className="text-xs sm:text-sm font-semibold text-blue-800 mb-1">Demo Credentials:</h3>
            <div className="text-xs sm:text-sm text-blue-700 space-y-1">
              <p><strong>Username:</strong> demouser | <strong>Password:</strong> 12345678</p>
              <p><strong>Email:</strong> demo@example.com | <strong>Password:</strong> password123</p>
              <p><strong>Or try:</strong> RyanGosling | 12345678</p>
            </div>
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
                to="/signup"
                className="text-sm sm:text-base font-medium text-blue-blue hover:text-blue-hover transition-colors duration-300"
              >
                Create a new account
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}