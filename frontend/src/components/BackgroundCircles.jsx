export default function BackgroundCircles() {
  return (
    <div className="fixed inset-0 overflow-hidden pointer-events-none z-0">
      <div className="absolute -right-32 -top-32 w-80 h-80 rounded-full bg-light-blue dark:bg-gray-700 opacity-30 blur-md"></div>
      <div className="absolute -right-40 -bottom-40 w-80 h-80 rounded-full bg-light-blue dark:bg-gray-700 opacity-30 blur-md"></div>
      <div className="absolute -left-40 -top-40 w-80 h-80 rounded-full bg-light-blue dark:bg-gray-700 opacity-30 blur-md"></div>
      <div className="absolute -left-32 -bottom-32 w-64 h-64 rounded-full bg-light-blue dark:bg-gray-700 opacity-30 blur-md"></div>
    </div>
  );
}
