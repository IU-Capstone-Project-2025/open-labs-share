import { useState, useEffect, useRef, useCallback } from "react";
import { useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkMath from "remark-math";
import remarkGfm from "remark-gfm";
import rehypeKatex from "rehype-katex";
import rehypeHighlight from "rehype-highlight";
import 'katex/dist/katex.min.css';

// Custom CSS for dark mode support
const darkModeStyles = `
  /* LaTeX formulas - default light mode */
  .katex {
    color: #1e293b !important;
  }
  
  /* LaTeX formulas dark mode */
  .dark .katex {
    color: #e5e7eb !important;
  }
  
  /* Enhanced Code blocks dark mode - SCOPE TO .prose ONLY */
  .prose pre {
    background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%) !important;
    color: #e5e7eb !important;
    border: 1px solid #475569 !important;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3), 0 2px 4px -1px rgba(0, 0, 0, 0.2) !important;
    position: relative !important;
    overflow: hidden !important;
  }
  
  .prose pre::before {
    content: '' !important;
    position: absolute !important;
    top: 0 !important;
    left: 0 !important;
    right: 0 !important;
    height: 1px !important;
    background: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899) !important;
  }
  
  .prose pre code {
    background-color: transparent !important;
    color: #e5e7eb !important;
    font-family: 'Fira Code', 'Monaco', 'Consolas', 'Liberation Mono', 'Courier New', monospace !important;
    font-size: 14px !important;
    line-height: 1.6 !important;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3) !important;
  }
  
  /* Enhanced Syntax highlighting for dark mode */
  .prose pre .hljs-keyword { color: #fbbf24 !important; font-weight: 600 !important; }
  .prose pre .hljs-string { color: #10b981 !important; }
  .prose pre .hljs-comment { color: #6b7280 !important; font-style: italic !important; }
  .prose pre .hljs-number { color: #ef4444 !important; }
  .prose pre .hljs-function .hljs-title { color: #3b82f6 !important; }
  .prose pre .hljs-title.class_ { color: #8b5cf6 !important; font-weight: 600 !important; }
  .prose pre .hljs-built_in { color: #ec4899 !important; }
  .prose pre .hljs-variable { color: #e5e7eb !important; }
  .prose pre .hljs-operator { color: #f59e0b !important; }
  .prose pre .hljs-punctuation { color: #d1d5db !important; }
  .prose pre .hljs-property { color: #10b981 !important; }
  .prose pre .hljs-selector-tag { color: #fbbf24 !important; font-weight: 600 !important; }
  .prose pre .hljs-attr { color: #3b82f6 !important; }
  .prose pre .hljs-literal { color: #ef4444 !important; }
  .prose pre .hljs-regexp { color: #ec4899 !important; }
  .prose pre .hljs-type { color: #8b5cf6 !important; }
  .prose pre .hljs-meta { color: #6b7280 !important; }
  .prose pre .hljs-template-string { color: #10b981 !important; }
  .prose pre .hljs-subst { color: #e5e7eb !important; }
  .prose pre .hljs-preprocessor { color: #fbbf24 !important; }
  .prose pre .hljs-shebang { color: #6b7280 !important; }
  .prose pre .hljs-prompt { color: #10b981 !important; }
  .prose pre .hljs-output { color: #e5e7eb !important; }
  
  /* Additional syntax elements */
  .prose pre .hljs-name { color: #3b82f6 !important; }
  .prose pre .hljs-tag { color: #fbbf24 !important; }
  .prose pre .hljs-attribute { color: #3b82f6 !important; }
  .prose pre .hljs-value { color: #10b981 !important; }
  .prose pre .hljs-title.function_ { color: #3b82f6 !important; }
  .prose pre .hljs-title.function_.invoke__ { color: #3b82f6 !important; }
  .prose pre .hljs-params { color: #e5e7eb !important; }
  .prose pre .hljs-doctag { color: #fbbf24 !important; }
  .prose pre .hljs-section { color: #8b5cf6 !important; font-weight: 600 !important; }
  .prose pre .hljs-selector-id { color: #8b5cf6 !important; }
  .prose pre .hljs-selector-class { color: #8b5cf6 !important; }
  .prose pre .hljs-selector-attr { color: #3b82f6 !important; }
  .prose pre .hljs-selector-pseudo { color: #fbbf24 !important; }
  .prose pre .hljs-addition { color: #10b981 !important; background-color: rgba(16, 185, 129, 0.1) !important; }
  .prose pre .hljs-deletion { color: #ef4444 !important; background-color: rgba(239, 68, 68, 0.1) !important; }
  .prose pre .hljs-emphasis { font-style: italic !important; }
  .prose pre .hljs-strong { font-weight: 600 !important; color: #fbbf24 !important; }
  
  /* Enhanced Inline code dark mode */
  .prose :not(pre) > code {
    background: linear-gradient(135deg, #374151 0%, #1f2937 100%) !important;
    color: #e5e7eb !important;
    border: 1px solid #4b5563 !important;
    border-radius: 6px !important;
    padding: 2px 6px !important;
    font-family: 'Fira Code', 'Monaco', 'Consolas', 'Liberation Mono', 'Courier New', monospace !important;
    font-size: 0.875em !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2) !important;
  }
  
  /* Code block hover effects */
  .prose pre:hover {
    box-shadow: 0 8px 25px -5px rgba(0, 0, 0, 0.4), 0 4px 10px -2px rgba(0, 0, 0, 0.3) !important;
    transform: translateY(-1px) !important;
    transition: all 0.2s ease-in-out !important;
  }
  
  /* Code block scrollbar styling */
  .prose pre::-webkit-scrollbar {
    height: 8px !important;
  }
  
  .prose pre::-webkit-scrollbar-track {
    background: #1e293b !important;
    border-radius: 4px !important;
  }
  
  .prose pre::-webkit-scrollbar-thumb {
    background: #475569 !important;
    border-radius: 4px !important;
  }
  
  .prose pre::-webkit-scrollbar-thumb:hover {
    background: #64748b !important;
  }
  
  /* Light mode overrides */
  .light .prose pre {
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
    color: #1e293b !important;
    border: 1px solid #cbd5e1 !important;
    border-radius: 12px !important;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06) !important;
  }
  
  .light .prose pre::before {
    background: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899) !important;
  }
  
  .light .prose pre code {
    color: #1e293b !important;
    text-shadow: none !important;
  }
  
  /* Syntax highlighting for light mode */
  .light .prose pre .hljs-keyword { color: #d97706 !important; font-weight: 600 !important; }
  .light .prose pre .hljs-string { color: #059669 !important; }
  .light .prose pre .hljs-comment { color: #6b7280 !important; font-style: italic !important; }
  .light .prose pre .hljs-number { color: #dc2626 !important; }
  .light .prose pre .hljs-function .hljs-title { color: #2563eb !important; }
  .light .prose pre .hljs-title.class_ { color: #7c3aed !important; font-weight: 600 !important; }
  .light .prose pre .hljs-built_in { color: #db2777 !important; }
  .light .prose pre .hljs-variable { color: #1e293b !important; }
  .light .prose pre .hljs-operator { color: #d97706 !important; }
  .light .prose pre .hljs-punctuation { color: #374151 !important; }
  .light .prose pre .hljs-property { color: #059669 !important; }
  .light .prose pre .hljs-selector-tag { color: #d97706 !important; font-weight: 600 !important; }
  .light .prose pre .hljs-attr { color: #2563eb !important; }
  .light .prose pre .hljs-literal { color: #dc2626 !important; }
  .light .prose pre .hljs-regexp { color: #db2777 !important; }
  .light .prose pre .hljs-type { color: #7c3aed !important; }
  .light .prose pre .hljs-meta { color: #6b7280 !important; }
  .light .prose pre .hljs-template-string { color: #059669 !important; }
  .light .prose pre .hljs-subst { color: #1e293b !important; }
  .light .prose pre .hljs-preprocessor { color: #d97706 !important; }
  .light .prose pre .hljs-shebang { color: #6b7280 !important; }
  .light .prose pre .hljs-prompt { color: #059669 !important; }
  .light .prose pre .hljs-output { color: #1e293b !important; }
  
  /* Additional syntax elements for light mode */
  .light .prose pre .hljs-name { color: #2563eb !important; }
  .light .prose pre .hljs-tag { color: #d97706 !important; }
  .light .prose pre .hljs-attribute { color: #2563eb !important; }
  .light .prose pre .hljs-value { color: #059669 !important; }
  .light .prose pre .hljs-title.function_ { color: #2563eb !important; }
  .light .prose pre .hljs-title.function_.invoke__ { color: #2563eb !important; }
  .light .prose pre .hljs-params { color: #1e293b !important; }
  .light .prose pre .hljs-doctag { color: #d97706 !important; }
  .light .prose pre .hljs-section { color: #7c3aed !important; font-weight: 600 !important; }
  .light .prose pre .hljs-selector-id { color: #7c3aed !important; }
  .light .prose pre .hljs-selector-class { color: #7c3aed !important; }
  .light .prose pre .hljs-selector-attr { color: #2563eb !important; }
  .light .prose pre .hljs-selector-pseudo { color: #d97706 !important; }
  .light .prose pre .hljs-addition { color: #059669 !important; background-color: rgba(5, 150, 105, 0.1) !important; }
  .light .prose pre .hljs-deletion { color: #dc2626 !important; background-color: rgba(220, 38, 38, 0.1) !important; }
  .light .prose pre .hljs-emphasis { font-style: italic !important; }
  .light .prose pre .hljs-strong { font-weight: 600 !important; color: #d97706 !important; }
  
  .light .prose :not(pre) > code {
    background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%) !important;
    color: #1e293b !important;
    border: 1px solid #cbd5e1 !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
  }
`;

import GemIcon from "../components/GemIcon";
import CommentsSection from "../components/CommentsSection";
import ChatWindow from "../components/ChatWindow";
import ToastNotification from "../components/ToastNotification";
import { mlAPI } from "../utils/api.js";
import MarpRenderer from '../components/MarpRenderer';
import { getCurrentUser, isAuthenticated, notifyUserDataUpdate } from "../utils/auth";
import { labsAPI, submissionsAPI, marimoAPI } from "../utils/api";
import { useUser } from "../hooks/useUser";
import MarimoWasmComponent from "../components/MarimoWasmComponent";
import MarimoAssetList from "../components/MarimoAssetList";
import ResizablePanel from "../components/ResizablePanel";
import MarimoCell from "../components/MarimoCell";
import { MarimoSessionProvider } from "../contexts/MarimoSessionContext";

const flattenText = (children) => {
  if (typeof children === "string") return children;
  if (!Array.isArray(children)) return String(children);
  return children
    .map((child) => {
      if (typeof child === "string") return child;
      if (child.props?.children) return flattenText(child.props.children);
      return "";
    })
    .join("");
};

const generateId = (text) =>
  text
    .toLowerCase()
    .replace(/[^\wа-яё]+/gi, "-")
    .replace(/^-+|-+$/g, "");

export default function LabPage() {
  const { id } = useParams();
  const [lab, setLab] = useState(null);
  const [markdown, setMarkdown] = useState("");
  const [headings, setHeadings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeId, setActiveId] = useState("");
  const [marimoComponents, setMarimoComponents] = useState([]);
  const observer = useRef();
  const contentRef = useRef();
  const [files, setFiles] = useState([]);
  const [submissionText, setSubmissionText] = useState("");
  const fileInputRef = useRef(null);
  const dropzoneRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [chatMode, setChatMode] = useState('floating');
  const [toast, setToast] = useState({ show: false, message: "", type: "" });
  const [imageUrls, setImageUrls] = useState(new Map());
  
  const user = useUser();

  const scrollToSubmit = useCallback(() => {
    const submitSection = document.getElementById("submit-section");
    if (submitSection) {
      submitSection.scrollIntoView({ behavior: "smooth" });
    }
  }, []);

  const handleFileChange = (e) => {
    const selectedFiles = Array.from(e.target.files);
    if (selectedFiles.length > 0) {
      setFiles((prevFiles) => [...prevFiles, ...selectedFiles]);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFiles = Array.from(e.dataTransfer.files);
    if (droppedFiles.length > 0) {
      setFiles((prevFiles) => [...prevFiles, ...droppedFiles]);
    }
  };

  const removeFile = (fileToRemove) => {
    setFiles((prevFiles) => prevFiles.filter((file) => file !== fileToRemove));
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleSubmit = async () => {
    if ((files.length === 0 && !submissionText.trim()) || !user || user.balance < 1) return;

    try {
      setUploading(true);

      const submission = await submissionsAPI.submitLabSolution(id, submissionText, files);
      const submissionId = submission.submissionMetadata.submissionId;
      let autoGradeStarted = false;
      
      if (submissionId && user?.id && id) {
        try {
          const resp = await mlAPI.startAutoGrading({
            uuid: String(user.id),
            assignment_id: String(id),
            submission_id: String(submissionId)
          });
          if (resp.ok) {
            autoGradeStarted = true;
          } else {
            const text = await resp.text();
            setToast({ show: true, message: `Autograding failed to start: ${text}`, type: "error" });
          }
        } catch (err) {
          setToast({ show: true, message: `Autograding error: ${err.message}`, type: "error" });
        }
      }

      const updatedUser = { ...user, balance: user.balance - 1 };
      localStorage.setItem("user", JSON.stringify(updatedUser));
      notifyUserDataUpdate();

      setToast({ show: true, message: `Your solution was uploaded successfully!${autoGradeStarted ? ' Autograding started.' : ''}`, type: "success" });

      setFiles([]);
      setSubmissionText("");
    } catch (err) {
      console.error("Upload error:", err);
      setToast({ show: true, message: `Upload failed: ${err.message}`, type: "error" });
    } finally {
      setUploading(false);
    }
  };


  const isMarpPresentation = (markdown) => {

    
    const result = markdown.trim().startsWith('---\nmarp: true') || 
          markdown.includes('\nmarp: true\n') ||
          markdown.includes('marp: true');
    return result;
  };

  const getMinioFileUrl = (labId, filename) => {
    const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
    return `${minioEndpoint}/labs/${labId}/${filename}`;
  };

  const ImageRenderer = ({ src, alt, ...props }) => {
    const [imgUrl, setImgUrl] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
      const checkImage = async () => {
        if (src.startsWith('http://') || src.startsWith('https://')) {
          setImgUrl(src);
          setLoading(false);
          return;
        }

        try {
          const url = getMinioFileUrl(id, src);
          const response = await fetch(url, { method: 'HEAD' });
          
          if (response.ok) {
            setImgUrl(url);
          } else {
            setError(true);
          }
        } catch (err) {
          setError(true);
        } finally {
          setLoading(false);
        }
      };

      checkImage();
    }, [id, src]);

    if (loading) {
      return (
        <div className="max-w-full h-32 bg-gray-200 dark:bg-gray-700 rounded-lg flex items-center justify-center my-4">
          <span className="text-gray-500 dark:text-gray-400">Loading image...</span>
        </div>
      );
    }

    if (error) {
      return (
        <div className="max-w-full h-32 bg-gray-200 dark:bg-gray-700 rounded-lg flex items-center justify-center my-4">
          <span className="text-gray-500 dark:text-gray-400">Image not found: {src}</span>
        </div>
      );
    }

    return (
      <img 
        src={imgUrl}
        alt={alt}
        {...props}
        className="max-w-full h-auto rounded-lg shadow-md my-4"
        onError={() => setError(true)}
      />
    );
  };

  useEffect(() => {
    const fetchLabData = async () => {
      try {
        setLoading(true);
        const labResponse = await labsAPI.getLabById(id);
        setLab(labResponse);

        if (labResponse.assets?.length > 0) {
          const markdownAsset = labResponse.assets.find(a => 
            a.filename && a.filename.toLowerCase().endsWith('.md')
          );
          
          if (markdownAsset) {
            try {
              const url = getMinioFileUrl(id, markdownAsset.filename);
              const response = await fetch(url);
              
              if (response.ok) {
                const text = await response.text();
                setMarkdown(text);
              } else {
                throw new Error('Markdown file not found');
              }
            } catch (err) {
              console.error('Error loading markdown:', err);
              setMarkdown(getPlaceholderContent(labResponse));
            }
          } else {
            setMarkdown(getPlaceholderContent(labResponse));
          }
        } else {
          setMarkdown(getPlaceholderContent(labResponse));
        }

        try {
          await mlAPI.indexAssignment(id);
        } catch (err) {
          //setToast({ show: true, message: `Failed to index assignment: ${err.message}`, type: 'error' });
        }
        // Fetch Marimo components
        try {
          const marimoData = await marimoAPI.getComponentsByContent('lab', id);
          console.log("Fetched marimoData:", marimoData);
          console.log("marimoData type:", typeof marimoData);
          console.log("marimoData length:", marimoData ? marimoData.length : 0);
          
          if (marimoData && marimoData.length > 0) {
            console.log("First component:", marimoData[0]);
            console.log("First component code:", marimoData[0].code);
            
            // Fetch the actual code for each component
            const componentsWithCode = await Promise.all(
              marimoData.map(async (component) => {
                try {
                  let code = '';
                  if (component.notebookPath) {
                    try {
                      const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
                      const minioUrl = `${minioEndpoint}/marimo${component.notebookPath}`;
                      console.log("Trying to fetch from MinIO:", minioUrl);
                      
                      const minioResponse = await fetch(minioUrl);
                      if (minioResponse.ok) {
                        code = await minioResponse.text();
                        console.log("Successfully fetched code from MinIO:", code.substring(0, 100) + "...");
                      } else {
                        console.error("MinIO fetch failed:", minioResponse.status, minioResponse.statusText);
                      }
                    } catch (minioErr) {
                      console.error("MinIO fetch error:", minioErr);
                    }
                  }
                  
                  return {
                    ...component,
                    code: code || `# Component: ${component.name}\n# No code available yet\n# Path: ${component.notebookPath || 'Unknown'}`
                  };
                } catch (err) {
                  console.error(`Error fetching code for component ${component.id}:`, err);
                  return {
                    ...component,
                    code: `# Error loading component code\n# Component: ${component.name}\n# Error: ${err.message}`
                  };
                }
              })
            );
            
            console.log("Components with code:", componentsWithCode);
            setMarimoComponents(componentsWithCode);
          } else {
            setMarimoComponents([]);
          }
        } catch (err) {
          console.error("Error fetching marimo components:", err);
          // Do not set a page-level error for this, as it's optional content
        }

      } catch (err) {
        setError(`Failed to load lab: ${err.message}`);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchLabData();
    }

    return () => {
      imageUrls.forEach((url) => {
        URL.revokeObjectURL(url);
      });
    };
  }, [id]);

  const getPlaceholderContent = (labResponse) => {
    return `# ${labResponse.title || 'Lab Content'}

## About This Lab

${labResponse.shortDesc || labResponse.abstract || 'No description available.'}

## Lab Content

Lab content delivery is currently being developed. The markdown content for this lab will be available soon.

### What you can do now:
- Review the lab description above
- Submit your solution using the file upload section below
- Check back later for the full lab instructions

---

*Note: This is lab ID ${id}. Contact your instructor if you need the lab materials immediately.*`;
  };

  useEffect(() => {
    if (!contentRef.current) return;

    const callback = (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          setActiveId(entry.target.id);
        }
      });
    };

    observer.current = new IntersectionObserver(callback, {
      root: null,
      rootMargin: "-20% 0px -80% 0px",
      threshold: 0,
    });

    const timer = setTimeout(() => {
      const elements = contentRef.current.querySelectorAll("[data-heading]");
      if (elements.length > 0) {
        // Set the first heading as active by default
        setActiveId(elements[0].id);
        elements.forEach((el) => observer.current.observe(el));
      }
    }, 300);

    return () => {
      clearTimeout(timer);
      if (observer.current) observer.current.disconnect();
    };
  }, [markdown]);

  const scrollToHeading = (id) => {
    setActiveId(id);
    const el = document.getElementById(id);
    if (el) {
      const offset = 100; // Account for fixed header/navigation
      const elementPosition = el.getBoundingClientRect().top;
      const offsetPosition = elementPosition + window.pageYOffset - offset;
      
      window.scrollTo({
        top: offsetPosition,
        behavior: "smooth"
      });
    }
  };

  const HeadingRenderer =
    (level) =>
    ({ node, children }) => {
      const text = flattenText(children);
      const id = generateId(text);

      useEffect(() => {
        setHeadings((prev) => {
          if (!prev.find((h) => h.id === id)) {
            return [...prev, { id, title: text, level }];
          }
          return prev;
        });
      }, []);

      const Tag = `h${level}`;
      return (
        <Tag
          id={id}
          data-heading="true"
          className={`scroll-mt-20 text-gray-900 dark:text-white ${
            level === 1 ? "text-3xl font-bold mt-8 mb-4 pt-4 border-t border-gray-300 dark:border-gray-600" : ""
          } ${level === 2 ? "text-2xl font-bold mt-6 mb-3" : ""} ${
            level === 3 ? "text-xl font-semibold mt-4 mb-2" : ""
          } ${activeId === id ? "text-msc dark:text-msc-light" : ""}`}
        >
          {children}
        </Tag>
      );
    };

  if (error) {
    return (
      <div className="flex items-center justify-center h-screen dark:bg-gray-900">
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-lg max-w-md">
          <h2 className="text-xl font-bold text-red-500 mb-4">Error</h2>
          <p className="text-gray-700 dark:text-gray-300 mb-4">{error}</p>
          <button 
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-msc text-white rounded-lg hover:bg-msc-hover transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const leftContent = (
    <MarimoSessionProvider contentType="lab" contentId={id}>
      <div className="marimo-column space-y-4">
        {marimoComponents.length > 0 && (
          <div className="text-xs text-gray-500 dark:text-gray-400 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded px-3 py-2">
            <span className="font-medium">Shared session active</span> - variables persist between components. 
            Reload page to reset.
          </div>
        )}
        {marimoComponents.map((component) => (
          <div key={component.id} className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">{component.name}</h3>
            <MarimoCell component={component} />
          </div>
        ))}
      </div>
    </MarimoSessionProvider>
  );

  const rightContent = (
        <div
          ref={contentRef}
      className="flex-1 p-8 overflow-y-auto scroll-smooth"
        >
        {/* Lab Header Section */}
        {lab && (
          <section className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-8 mb-8">
            <div className="border-b border-gray-200 dark:border-gray-600 pb-6 mb-6">
              <h1 className="text-2xl sm:text-3xl md:text-4xl font-bold font-display text-gray-900 dark:text-white break-words">
                {lab.title}
              </h1>
              
              {lab.shortDesc && (
                <p className="mt-2 text-base sm:text-lg text-gray-500 dark:text-gray-400 break-words">
                  {lab.shortDesc}
                </p>
              )}
              
              <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-500 dark:text-gray-400">
                <span>By {lab.authorName} {lab.authorSurname}</span>
                <span>•</span>
                <span>{new Date(lab.createdAt).toLocaleDateString()}</span>
                <span>•</span>
                <span>{lab.views} views</span>
              </div>
              {lab.tags && lab.tags.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-2">
                  {lab.tags.map((tag, index) => (
                    <span
                      key={tag.id || tag}
                      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200 relative group"
                      title={typeof tag === 'object' ? tag.description : ''}
                    >
                      {typeof tag === 'object' ? tag.name : `Tag ${tag}`}
                      {typeof tag === 'object' && tag.description && (
                        <span className="absolute z-10 hidden group-hover:block w-64 px-2 py-1 mt-6 -ml-4 text-xs text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 rounded shadow-lg">
                          {tag.description}
                        </span>
                      )}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </section>
        )}

        {toast.show && (
          <ToastNotification 
            message={toast.message}
            type={toast.type}
            onClose={() => setToast({ show: false, message: "", type: "" })}
          />
        )}

        {/* Lab Content Section */}
        <section className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-8 mb-8">
          {isMarpPresentation(markdown) ? (
            <MarpRenderer content={markdown} labId={id} />
          ) : (
            <article className="max-w-none prose prose-gray dark:prose-invert prose-headings:text-gray-900 prose-headings:dark:text-white prose-p:text-gray-700 prose-p:dark:text-gray-300 prose-ul:text-gray-700 prose-ul:dark:text-gray-300 prose-ol:text-gray-700 prose-ol:dark:text-gray-300 prose-li:text-gray-700 prose-li:dark:text-gray-300 prose-strong:text-gray-900 prose-strong:dark:text-white prose-em:text-gray-700 prose-em:dark:text-gray-300 prose-blockquote:text-gray-700 prose-blockquote:dark:text-gray-300 prose-blockquote:border-gray-300 prose-blockquote:dark:border-gray-600 prose-code:text-gray-900 prose-code:dark:text-gray-100 prose-pre:bg-gray-100 prose-pre:dark:bg-gray-800 prose-pre:text-gray-900 prose-pre:dark:text-gray-100">
              <ReactMarkdown
                remarkPlugins={[
                  remarkGfm,
                  remarkMath
                ]}
                rehypePlugins={[
                  rehypeKatex,
                  rehypeHighlight
                ]}
                components={{
                  h1: HeadingRenderer(1),
                  h2: HeadingRenderer(2),
                  h3: HeadingRenderer(3),
                  img: ImageRenderer,
                  p: ({ node, ...props }) => (
                    <p {...props} className="my-4 leading-relaxed text-gray-700 dark:text-gray-300" />
                  ),
                  ul: ({ node, ...props }) => (
                    <ul {...props} className="list-disc pl-6 my-4 space-y-2 text-gray-700 dark:text-gray-300" />
                  ),
                  ol: ({ node, ...props }) => (
                    <ol {...props} className="list-decimal pl-6 my-4 space-y-2 text-gray-700 dark:text-gray-300" />
                  ),
                                      li: ({ node, ...props }) => <li {...props} className="pl-2 my-1 text-gray-700 dark:text-gray-300" />,


                  
                  table: ({ node, ...props }) => (
                    <div className="overflow-x-auto">
                      <table {...props} className="min-w-full divide-y divide-gray-300 dark:divide-gray-700 my-4 border border-gray-300 dark:border-gray-700" />
                    </div>
                  ),
                  th: ({ node, ...props }) => (
                    <th {...props} className="px-4 py-2 bg-gray-100 dark:bg-gray-800 text-left text-sm font-semibold text-gray-900 dark:text-white border-b border-gray-300 dark:border-gray-700" />
                  ),
                  td: ({ node, ...props }) => (
                    <td {...props} className="px-4 py-2 text-sm text-gray-900 dark:text-gray-100 border-b border-gray-300 dark:border-gray-700" />
                  ),
                }}
              >
                {markdown}
              </ReactMarkdown>
            </article>)}
        </section>

        {/* Homework Submission Section */}
        <section id="submit-section" className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-8 mb-8">
          <div className="flex items-center mb-6 pb-4 border-b border-gray-200 dark:border-gray-600">
            <div className="w-10 h-10 bg-msc rounded-full flex items-center justify-center mr-3">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5 text-white"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">Homework Submission</h2>
              <p className="text-sm text-gray-600 dark:text-gray-400">Upload your completed assignment files</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label htmlFor="solution_text" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Your Solution/Comment
              </label>
              <textarea
                id="solution_text"
                name="solution_text"
                rows="4"
                className="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-lg dark:bg-gray-700 dark:text-white focus:ring-2 focus:ring-msc focus:border-transparent transition-colors"
                value={submissionText}
                onChange={(e) => setSubmissionText(e.target.value)}
                placeholder="Enter any comments or text-based solution here..."
              ></textarea>
            </div>
          </div>
          
          <div className="mt-6">
            <p className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Upload Files
            </p>
            <div
              ref={dropzoneRef}
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onClick={handleUploadClick}
              className={`border-2 ${
                isDragging ? "border-msc bg-blue-50 dark:bg-blue-900/20" : "border-dashed border-gray-300 dark:border-gray-600"
              } rounded-lg p-8 text-center cursor-pointer transition-all duration-200 ${
                isDragging
                  ? "bg-blue-50 dark:bg-blue-900/20"
                  : "bg-gray-50 dark:bg-gray-700/50 hover:bg-gray-100 dark:hover:bg-gray-700"
              }`}
            >
              <input
                type="file"
                multiple
                ref={fileInputRef}
                onChange={handleFileChange}
                className="hidden"
              />
              <div className="w-12 h-12 mx-auto mb-4 flex items-center justify-center bg-msc dark:bg-msc rounded-full">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-6 w-6 text-white"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                  />
                </svg>
              </div>
              <div className="text-center">
                <p className="text-lg font-medium text-gray-900 dark:text-white">
                  {isDragging
                    ? "Drop files here"
                    : "Select files or drop them here"}
                </p>
                <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  You can upload multiple files
                </p>
              </div>
            </div>
          </div>

          {files.length > 0 && (
            <div className="mt-4">
              <h4 className="font-medium text-gray-900 dark:text-white">Selected files:</h4>
              <ul className="mt-2 space-y-2">
                {files.map((file, index) => (
                  <li key={index} className="flex items-center justify-between bg-gray-100 dark:bg-gray-700 p-3 rounded-lg border border-gray-200 dark:border-gray-600">
                    <span className="text-sm text-gray-800 dark:text-gray-300 truncate">{file.name}</span>
                    <button
                      onClick={() => removeFile(file)}
                      className="text-red-500 hover:text-red-700 dark:hover:text-red-400 transition-colors"
                      title="Remove file"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="mt-6 flex flex-col items-center">
            <button
              onClick={handleSubmit}
              disabled={(!submissionText.trim() && files.length === 0) || uploading || !user || (user && user.balance < 1)}
              className={`px-16 py-3 rounded-lg font-medium transition-all duration-200 ${
                (submissionText.trim() || files.length > 0) && !uploading && user && user.balance >= 1
                  ? "bg-msc text-white hover:bg-msc-dark shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                  : "bg-gray-300 dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed"
              }`}
            >
              {uploading ? "Uploading..." : "Submit homework"}
            </button>
            
            {/* Points requirement message */}
            {user && user.balance < 1 && (
              <div className="mt-3 text-sm text-red-600 dark:text-red-400 text-center">
                <div className="flex items-center justify-center">
                  <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                  <span className="flex items-center">
                    Insufficient balance. You need at least 1
                    <GemIcon className="h-4 w-4 mx-1" color="#dc2626" />
                    to submit homework.
                  </span>
                </div>
                <p className="text-xs mt-1 text-gray-500 dark:text-gray-400">
                  <span className="flex items-center justify-center">
                    Review other students' submissions to earn
                    <GemIcon className="h-3 w-3 mx-1" color="#6b7280" />.
                  </span>
                </p>
              </div>
            )}
            
            {/* General submission info */}
            {user && user.balance >= 1 && (
              <p className="mt-3 text-sm text-gray-600 dark:text-gray-400 text-center">
                <span className="flex items-center justify-center">
                  <svg className="w-4 h-4 mr-1 text-accent" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                  </svg>
                  This submission will cost 1
                  <GemIcon className="h-4 w-4 mx-1" color="#101e5a" />
                </span>
              </p>
            )}
          </div>
        </section>

        {/* Comments Section */}
        <section className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-8">
          <div className="flex items-center mb-6 pb-4 border-b border-gray-200 dark:border-gray-600">
            <div className="w-10 h-10 bg-green-600 rounded-full flex items-center justify-center mr-3">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5 text-white"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                />
              </svg>
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">Lab Discussion</h2>
              <p className="text-sm text-gray-600 dark:text-gray-400">Share your thoughts and ask questions about this lab</p>
            </div>
          </div>
          
          <CommentsSection 
            contentType="lab" 
            contentId={id} 
            userId={user?.id}
            userName={`${user?.firstName} ${user?.lastName}`}
          />
        </section>
    </div>
  );

  return (
    <>
      <style dangerouslySetInnerHTML={{ __html: darkModeStyles }} />
      <div className={`container mx-auto py-8 flex transition-all duration-300 bg-white dark:bg-gray-900 min-h-screen ${isChatOpen && chatMode === 'sidebar' ? 'lg:mr-[400px]' : ''}`}>
        <div className="flex-1 min-w-0 flex">
          {marimoComponents.length > 0 ? (
            <ResizablePanel
              leftComponent={leftContent}
              rightComponent={rightContent}
              initialLeftWidth={25}
            />
          ) : (
            rightContent
          )}
        </div>

        {/* Table of Contents */}
        <aside className="w-72 pl-8 sticky top-24 self-start hidden lg:block">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
            <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">
              Table of Contents
            </h3>
            <ul className="space-y-1">
              {headings.map((heading, index) => (
                <li
                  key={index}
                  style={{ paddingLeft: `${(heading.level - 1) * 12}px` }}
                  className={`transition-colors ${
                    activeId === heading.id
                      ? "text-msc dark:text-msc-light font-medium bg-blue-50 dark:bg-gray-700 rounded"
                      : "text-gray-600 dark:text-gray-400 hover:text-msc dark:hover:text-msc-light"
                  }`}
                >
                  <button
                    onClick={() => scrollToHeading(heading.id)}
                    className="text-left w-full py-1.5 px-2 text-sm truncate"
                    title={heading.title}
                  >
                    {heading.title}
                  </button>
                </li>
              ))}
            </ul>
            <button
              onClick={scrollToSubmit}
              className="mt-4 w-full py-3 px-4 bg-msc font-inter text-white rounded-md hover:bg-msc-dark transition-colors flex items-center justify-center"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5 mr-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                />
              </svg>
              Submit homework
            </button>
          </div>
        </aside>
      </div>

      <ChatWindow
        labId={id}
        isOpen={isChatOpen}
        onToggle={() => setIsChatOpen(!isChatOpen)}
        chatMode={chatMode}
        onSetChatMode={setChatMode}
      />
    </>
  );
}