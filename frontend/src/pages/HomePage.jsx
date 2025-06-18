import LabCard from "../components/LabCard";

export default function Home() {
  const sampleLab = {
    id: 1,
    title: "Scheduling tasks",
    description:
      "A detailed study of the basic principles of quantum computing and their application in modern computer science.",
    author: {
      firstName: "Ivan",
      lastName: "Petrov",
      avatarUrl: "/path/to/avatar.jpg",
    },
  };
  return (
    <div className="flex dark:bg-gray-900">
      <h1 className="text-4xl font-bold dark:text-white">Home</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 p-4">
        <LabCard lab={sampleLab} />
      </div>
    </div>
  );
}
