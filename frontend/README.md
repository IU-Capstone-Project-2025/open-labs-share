# Open Labs Share - Frontend

The frontend is part of the Open Labs Share project.

## Project structure

- `public/` - Static files to be copied as-is (favicon, images)
- `src/`
  - `assets/` - Web application resources (styles, images, fonts)
  - `components/` - Reusable UI components (buttons, cards, etc.)
  - `pages/` - Web application pages (each page is a separate component)
  - `services/` - API clients and services for working with the backend (in the future)
  - `App.jsx` - The main component of the web application (routing)
  - `main.jsx` - JavaScript entry point that initializes the React application
  - `index.css` - Global styles
- `index.html` - The entry point of web application (static template)

## Run the frontend

1. Make sure that you are in the folder `frontend/`
2. Install the dependencies: `npm install`
3. Start the dev server: `npm run dev`

   The project will be available at `http://localhost:5173`

### Username and password to sign in to the account

- Username: user01
- Password: 12345678

## Configuration files

- `vite.config.js` - Vite Importer Settings
- `tailwind.config.js` - TailwindCSS Configuration
- `.gitignore` - Files that Git should ignore
