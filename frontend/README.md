# Open Labs Share - Frontend

A React-based frontend application for the Open Labs Share platform, built with Vite, Tailwind CSS, and React Router.

## Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── components/         # Reusable UI components
│   ├── pages/             # Page components and routing
│   ├── hooks/             # Custom React hooks
│   ├── utils/             # Utility functions and API clients
│   ├── App.jsx            # Main application component
│   ├── main.jsx           # Application entry point
│   └── index.css          # Global styles
├── package.json           # Dependencies and scripts
├── vite.config.js         # Vite configuration
├── tailwind.config.js     # Tailwind CSS configuration
├── postcss.config.js      # PostCSS configuration
├── nginx.conf             # Nginx configuration for production
└── index.html             # HTML template
```

## Getting Started

### Prerequisites

- Node.js (v16 or higher)
- npm or yarn

### Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

4. Open your browser and navigate to `http://localhost:5173`



## Architecture

### Technology Stack

- **React 18** - UI library
- **Vite** - Build tool and dev server
- **React Router** - Client-side routing
- **Tailwind CSS** - Utility-first CSS framework
- **Heroicons** - Icon library

### Key Features

- **Authentication System** - JWT-based auth with automatic token refresh
- **Dark Mode Support** - Built-in theme switching
- **Real-time Updates** - Event-driven user data synchronization
- **File Upload** - Support for lab and article submissions
- **Search & Filtering** - Advanced search capabilities
- **Review System** - Submission review and feedback

## Documentation

### [Components](./docs/components.md)
Reusable UI components used throughout the application.

### [Pages](./docs/pages.md)
Page components and routing structure.


## Configuration

### Configuration files

- `vite.config.js` - Vite Importer Settings
- `tailwind.config.js` - TailwindCSS Configuration
- `.gitignore` - Files that Git should ignore
