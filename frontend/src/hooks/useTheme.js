import { useState, useEffect } from 'react';

const useTheme = () => {
  const [theme, setTheme] = useState('light');

  useEffect(() => {
    const root = window.document.documentElement;
    const observer = new MutationObserver(() => {
      if (root.classList.contains('dark')) {
        setTheme('dark');
      } else {
        setTheme('light');
      }
    });

    observer.observe(root, { attributes: true, attributeFilter: ['class'] });

    // Initial check
    if (root.classList.contains('dark')) {
      setTheme('dark');
    }

    return () => observer.disconnect();
  }, []);

  return theme;
};

export default useTheme; 