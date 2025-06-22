export default function Home() {
  const loadingCards = Array(6).fill(null);

  return (
    <div className="relative min-h-screen bg-white dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-6 shadow-lg">
        <article className="text-wrap text-center text-msc">
          <h1 className="text-4xl font-bold dark:text-white">
            Welcome to Home Page
          </h1>
          <h3 className="text-2xl font-bold dark:text-white">
            will be later...
          </h3>
        </article>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          {loadingCards.map((_, index) => (
            <div
              key={index}
              className="h-32 bg-light-blue bg-opacity-70 dark:bg-gray-700 animate-pulse rounded-xl"
            />
          ))}
        </div>
      </div>
    </div>
  );
}
