import React from 'react';
import { Link } from 'react-router-dom';
import { BeakerIcon, EyeIcon } from "@heroicons/react/24/outline";
import GemIcon from "./GemIcon";

const StatCard = ({ icon, value, title, description, color }) => {
  const colorClasses = {
    blue: {
      bg: "bg-blue-50 dark:bg-blue-900/20",
      border: "border-blue-200 dark:border-blue-800",
      iconBg: "bg-blue-100 dark:bg-blue-800/50",
      iconText: "text-blue-600 dark:text-blue-300",
      valueText: "text-blue-800 dark:text-blue-200",
      titleText: "text-blue-700 dark:text-blue-300",
      descriptionText: "text-blue-600 dark:text-blue-400",
    },
    green: {
      bg: "bg-green-50 dark:bg-green-900/20",
      border: "border-green-200 dark:border-green-800",
      iconBg: "bg-green-100 dark:bg-green-800/50",
      iconText: "text-green-600 dark:text-green-300",
      valueText: "text-green-800 dark:text-green-200",
      titleText: "text-green-700 dark:text-green-300",
      descriptionText: "text-green-600 dark:text-green-400",
    },
    purple: {
      bg: "bg-purple-50 dark:bg-purple-900/20",
      border: "border-purple-200 dark:border-purple-800",
      iconBg: "bg-purple-100 dark:bg-purple-800/50",
      iconText: "text-purple-600 dark:text-purple-300",
      valueText: "text-purple-800 dark:text-purple-200",
      titleText: "text-purple-700 dark:text-purple-300",
      descriptionText: "text-purple-600 dark:text-purple-400",
    },
  };

  const classes = colorClasses[color] || colorClasses.blue;

  return (
    <div className={`p-6 rounded-2xl border ${classes.bg} ${classes.border} flex flex-col justify-between h-full`}>
      <div>
        <div className="flex items-start justify-between">
          <div className={`w-12 h-12 rounded-full flex items-center justify-center ${classes.iconBg}`}>
            {icon}
          </div>
          <div className="text-right">
            <p className={`text-4xl font-bold ${classes.valueText}`}>{value}</p>
            <p className={`text-lg font-medium ${classes.titleText}`}>{title}</p>
          </div>
        </div>
      </div>
      <p className={`mt-4 text-sm ${classes.descriptionText}`}>
        {description}
      </p>
    </div>
  );
};

export default function ActivityBalance({ stats }) {
  const { pointsBalance, labsSolved, labsReviewed } = stats;

  return (
    <div className="bg-white dark:bg-gray-800/50 rounded-2xl shadow-lg p-8">
      <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
        Your Activity & Balance
      </h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Link to="/all-labs" className="block hover:scale-105 transform transition-transform duration-300">
          <StatCard
            icon={<GemIcon className="w-6 h-6 text-blue-600 dark:text-blue-300" />}
            value={pointsBalance}
            title="Points Balance"
            description="Spend points to solve labs, earn points by reviewing others' solutions. Click here to browse labs."
            color="blue"
          />
        </Link>
        <Link to="/submissions/my" className="block hover:scale-105 transform transition-transform duration-300">
          <StatCard
            icon={<BeakerIcon className="w-6 h-6 text-green-600 dark:text-green-300" />}
            value={labsSolved}
            title="Labs Solved"
            description="Total number of labs you've successfully completed. Click here to view your submissions."
            color="green"
          />
        </Link>
        <Link to="/reviews" className="block hover:scale-105 transform transition-transform duration-300">
          <StatCard
            icon={<EyeIcon className="w-6 h-6 text-purple-600 dark:text-purple-300" />}
            value={labsReviewed}
            title="Labs Reviewed"
            description="Total number of lab solutions you've reviewed for others. Click here to review more."
            color="purple"
          />
        </Link>
      </div>
    </div>
  );
} 