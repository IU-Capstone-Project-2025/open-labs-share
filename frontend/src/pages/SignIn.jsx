import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Point from "../components/Point";

export default function SignIn() {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    navigate("/home");
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
              />
            </div>
            <button
              type="submit"
              className="w-full bg-msc hover:bg-msc-hover text-white py-3 px-4 rounded-lg shadow-md transition-colors duration-300 font-medium"
            >
              Sign In
            </button>
          </form>
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
