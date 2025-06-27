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

  // Redirect if already authenticated
  if (isAuthenticated()) {
    navigate("/home", { replace: true });
    return null;
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    // Clear error when user starts typing
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
    <div className="flex h-screen bg-white font-inter">
      <div className="relative w-1/2 bg-msc flex flex-col justify-center items-center text-white p-12 overflow-hidden">
        <div className="absolute bottom-1/5 left-2/3 w-48 h-48 rounded-full bg-blue-blue opacity-15 blur-sm"></div>
        <div className="absolute -bottom-48 -left-48 w-[600px] h-[600px] rounded-full bg-blue-blue opacity-15 blur-sm"></div>
        <div className="absolute bottom-3/4 left-80 w-96 h-96 rounded-full bg-blue-blue opacity-15 blur-sm"></div>

        <div className="relative z-10 text-center">
          <h1 className="text-6xl mb-6">Open Labs Share</h1>
          <p className="text-xl mx-auto max-w-md text-balance">
            A social knowledge network with peer review and personalized
            recommendations
          </p>
        </div>
      </div>

      <div className="w-1/2 flex items-center justify-center p-12">
        <div className="w-full max-w-lg">
          <h1 className="text-3xl font-bold mb-8 text-gray-800">
            Welcome back
          </h1>
          
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-md font-medium text-gray-700 mb-1">
                Email or Username
              </label>
              <input
                type="text"
                name="email"
                placeholder="Enter your email address or username"
                value={formData.email}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
                required
                disabled={loading}
              />
            </div>
            <div>
              <label className="block text-md font-medium text-gray-700 mb-1">
                Password
              </label>
              <input
                type="password"
                name="password"
                placeholder="Enter your password"
                value={formData.password}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
                required
                disabled={loading}
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-msc hover:bg-msc-hover text-white py-3 px-4 rounded-lg shadow-md transition-colors duration-300 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? "Signing In..." : "Sign In"}
            </button>
          </form>

          {/* Demo credentials section */}
          <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h3 className="text-sm font-semibold text-blue-800 mb-2">Demo Credentials:</h3>
            <div className="text-sm text-blue-700 space-y-1">
              <p><strong>Username:</strong> demouser | <strong>Password:</strong> password123</p>
              <p><strong>Email:</strong> demo@example.com | <strong>Password:</strong> password123</p>
              <p><strong>Or try:</strong> ryanGosling1980 | password123</p>
            </div>
          </div>

          <div className="mt-8">
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
            <div className="mt-4 text-center">
              <Link
                to="/signup"
                className="font-medium text-blue-blue hover:text-blue-hover transition-colors duration-300"
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
