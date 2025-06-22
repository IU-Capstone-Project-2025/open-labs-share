import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
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
  const navigate = useNavigate();

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

    if (formData.password.length < 8) {
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

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validateForm()) {
      navigate("/home");
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
          <h1 className="text-3xl font-bold mb-8 text-gray-800">Sign Up</h1>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-2 gap-6">
              <div>
                <label className="block text-md font-medium text-gray-700 mb-1">
                  First Name
                </label>
                <input
                  type="text"
                  name="firstName"
                  placeholder="Ryan"
                  value={formData.firstName}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
                  required
                />
              </div>
              <div>
                <label className="block text-md font-medium text-gray-700 mb-1">
                  Last Name
                </label>
                <input
                  type="text"
                  name="lastName"
                  placeholder="Gosling"
                  value={formData.lastName}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
                  required
                />
              </div>
            </div>
            <div>
              <label className="block text-md font-medium text-gray-700 mb-1">
                Username
              </label>
              <input
                type="text"
                name="username"
                placeholder="ryanGosling1980"
                value={formData.username}
                onChange={handleChange}
                className={`w-full border ${
                  errors.username ? "border-red-500" : "border-gray-300"
                } rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                required
                pattern="[a-zA-Z0-9_]+"
                title="Only letters, numbers and underscore are allowed"
              />
              {errors.username && (
                <p className="mt-1 text-sm text-red-600">{errors.username}</p>
              )}
            </div>
            <div>
              <label className="block text-md font-medium text-gray-700 mb-1">
                Email
              </label>
              <input
                type="email"
                name="email"
                placeholder="gosl1980@mail.com"
                value={formData.email}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div>
                <label className="block text-md font-medium text-gray-700 mb-1">
                  Password
                </label>
                <input
                  type="password"
                  name="password"
                  placeholder="Enter 8 character or more"
                  value={formData.password}
                  onChange={handleChange}
                  className={`w-full border ${
                    errors.password ? "border-red-500" : "border-gray-300"
                  } rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                  required
                />
                {errors.password && (
                  <p className="mt-1 text-sm text-red-600">{errors.password}</p>
                )}
              </div>
              <div>
                <label className="block text-md font-medium text-gray-700 mb-1">
                  Confirm Password
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  placeholder="Enter 8 character or more"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className={`w-full border ${
                    errors.confirmPassword
                      ? "border-red-500"
                      : "border-gray-300"
                  } rounded-lg py-3 px-4 focus:outline-none focus:ring-2 focus:ring-msc focus:border-transparent`}
                  required
                />
                {errors.confirmPassword && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.confirmPassword}
                  </p>
                )}
              </div>
            </div>

            <button
              type="submit"
              className="w-full bg-msc hover:bg-msc-hover text-white py-3 px-4 rounded-lg shadow-md transition-colors duration-300 font-medium"
            >
              Create Account
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
                to="/signin"
                className="font-medium text-blue-blue hover:text-blue-hover transition-colors duration-300"
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
