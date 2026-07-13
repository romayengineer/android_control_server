/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,svelte}",
  ],
  theme: {
    extend: {
      colors: {
        'status-green': '#10b981',
        'status-red': '#ef4444',
      },
    },
  },
  plugins: [],
}
