/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        msc: "#101e5a",
        "msc-hover": "#1a2b7a",
        "blue-blue": "#5D80B6",
        "blue-hover": "#6d8fc7",
        "light-blue": "#ACBDD3",
        "my-white": "##f7f8f9",
      },
      fontFamily: {
        inter: ["Inter", "sans-serif"],
      },
    },
  },
  plugins: [],
};
