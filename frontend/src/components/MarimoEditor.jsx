import React from 'react';
import Editor from 'react-simple-code-editor';
import { highlight, languages } from 'prismjs/components/prism-core';
import 'prismjs/components/prism-python';
import useTheme from '../hooks/useTheme'; // Import the custom hook

// Import both themes
import 'prism-themes/themes/prism-vsc-dark-plus.css'; // Dark theme
import 'prismjs/themes/prism.css'; // Light theme

const MarimoEditor = ({ code, setCode }) => {
  const theme = useTheme();

  return (
    <div 
      className="border rounded-md overflow-hidden"
      style={{ backgroundColor: theme === 'dark' ? '#1e1e1e' : '#f5f5f5' }}
    >
      <Editor
        value={code}
        onValueChange={setCode}
        highlight={code => highlight(code, languages.python, 'python')}
        padding={10}
        style={{
          fontFamily: '"Fira code", "Fira Mono", monospace',
          fontSize: 14,
          outline: 'none',
          border: 'none',
        }}
        className="min-h-[200px]"
      />
    </div>
  );
};

export default MarimoEditor; 