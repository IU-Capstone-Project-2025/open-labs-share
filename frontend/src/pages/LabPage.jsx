import { useState, useEffect, useRef, useCallback } from "react";
import { useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkMath from "remark-math";
import remarkGfm from "remark-gfm";
import rehypeKatex from "rehype-katex";
import 'katex/dist/katex.min.css';
import 'highlight.js/styles/github-dark.css';
import GemIcon from "../components/GemIcon";
import CommentsSection from "../components/CommentsSection";
import ChatWindow from "../components/ChatWindow";
import ToastNotification from "../components/ToastNotification";
import { getCurrentUser, isAuthenticated, notifyUserDataUpdate } from "../utils/auth";
import { labsAPI, submissionsAPI } from "../utils/api";
import { useUser } from "../hooks/useUser";

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
      await submissionsAPI.submitLabSolution(id, submissionText, files);

      // Update user's balance locally after successful submission
      const updatedUser = { ...user, balance: user.balance - 1 };

      // Store updated user data in localStorage
      localStorage.setItem("user", JSON.stringify(updatedUser));

      // Notify all components about the user data update (including this component)
      notifyUserDataUpdate();

      setToast({ show: true, message: "Your solution was uploaded successfully!", type: "success" });
      setFiles([]);
      setSubmissionText("");
    } catch (err) {
      console.error("Upload error:", err);
      setToast({ show: true, message: `Upload failed: ${err.message}`, type: "error" });
    } finally {
      setUploading(false);
    }
  };


  // Функция для загрузки файла напрямую из MinIO
  const fetchAssetFromMinio = async (filename) => {
    try {
      const url = getMinioFileUrl(id, filename);
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error(`Failed to fetch asset: HTTP ${response.status}`);
      }
      
      return await response.blob();
    } catch (error) {
      console.error(`Error fetching asset ${filename}:`, error);
      throw error;
    }
  };

  // Функция для предзагрузки изображений
  const preloadImages = async () => {
    if (!lab?.assets) return new Map();

    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.svg', '.webp'];
    const imageAssets = lab.assets.filter(asset => 
      imageExtensions.some(ext => asset.filename.toLowerCase().endsWith(ext))
    );

    const urlMap = new Map();
    
    for (const asset of imageAssets) {
      try {
        const blob = await fetchAssetFromMinio(asset.filename);
        const blobUrl = URL.createObjectURL(blob);
        urlMap.set(asset.filename, blobUrl);
        console.log(`Preloaded image: ${asset.filename}`);
      } catch (error) {
        console.error(`Failed to preload image ${asset.filename}:`, error);
      }
    }
    
    setImageUrls(urlMap);
    return urlMap;
  };

  // Функция для получения URL из MinIO
  const getMinioFileUrl = (labId, filename) => {
    const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
    return `${minioEndpoint}/labs/${labId}/${filename}`;
  };

  // Компонент для рендеринга изображений
  const ImageRenderer = ({ src, alt, ...props }) => {
    const [imgUrl, setImgUrl] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
      const checkImage = async () => {
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
      } catch (err) {
        setError(`Failed to load lab: ${err.message}`);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchLabData();
    }

    // Cleanup function для освобождения blob URLs
    return () => {
      imageUrls.forEach((url) => {
        URL.revokeObjectURL(url);
      });
    };
  }, [id]);

  // Helper function to generate placeholder content
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
      root: contentRef.current,
      rootMargin: "0px 0px -50% 0px",
      threshold: 0.1,
    });

    const timer = setTimeout(() => {
      const elements = contentRef.current.querySelectorAll("[data-heading]");
      elements.forEach((el) => observer.current.observe(el));
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
      el.scrollIntoView({ behavior: "smooth", block: "start" });
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
          className={`scroll-mt-20 ${
            level === 1 ? "text-3xl font-bold mt-8 mb-4 pt-4 border-t" : ""
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

  return (
    <>
      <div className={`container mx-auto px-4 py-8 flex transition-all duration-300 ${isChatOpen && chatMode === 'sidebar' ? 'lg:mr-[400px]' : ''}`}>
        <div
          ref={contentRef}
          className="flex-1 p-8 overflow-y-auto scroll-smooth"
        >
        {/* Lab Header Section */}
        {lab && (
          <section className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-8 mb-8">
            <div className="border-b border-gray-200 dark:border-gray-600 pb-6 mb-6">
              <h1 className="text-4xl font-bold font-display text-gray-900 dark:text-white">
                {lab.title}
              </h1>
              
              {lab.shortDesc && (
                <p className="mt-2 text-lg text-gray-500 dark:text-gray-400">
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
          <article className="prose dark:prose-invert max-w-none">
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
                // Добавляем кастомный рендерер для изображений
                img: ImageRenderer,
                p: ({ node, ...props }) => (
                  <p {...props} className="my-4 leading-relaxed dark:text-gray-300" />
                ),
                ul: ({ node, ...props }) => (
                  <ul {...props} className="list-disc pl-6 my-4 space-y-2 dark:text-gray-300" />
                ),
                ol: ({ node, ...props }) => (
                  <ol {...props} className="list-decimal pl-6 my-4 space-y-2 dark:text-gray-300" />
                ),
                li: ({ node, ...props }) => <li {...props} className="pl-2 my-1" />,
                pre: ({ node, ...props }) => (
                  <pre {...props} className="bg-gray-100 dark:bg-gray-800 rounded-lg p-4 overflow-x-auto my-6" />
                ),
                code: ({ node, className, children, ...props }) => {
                  const match = /language-(\w+)/.exec(className || "");
                  const isInline = !match;

                  return isInline ? (
                    <code
                      className="bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded text-sm"
                      {...props}
                    >
                      {children}
                    </code>
                  ) : (
                    <code className={className} {...props}>
                      {children}
                    </code>
                  );
                },
                
                table: ({ node, ...props }) => (
                  <div className="overflow-x-auto">
                    <table {...props} className="min-w-full divide-y divide-gray-700 my-4 border border-gray-700" />
                  </div>
                ),
                th: ({ node, ...props }) => (
                  <th {...props} className="px-4 py-2 bg-gray-800 text-left text-sm font-semibold text-white border-b border-gray-700" />
                ),
                td: ({ node, ...props }) => (
                  <td {...props} className="px-4 py-2 text-sm text-black border-b border-gray-700" />
                ),
              }}
            >
              {markdown}
            </ReactMarkdown>
          </article>
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
                className="w-full p-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-white"
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
                isDragging ? "border-msc" : "border-dashed border-blue-blue"
              } rounded-lg p-8 text-center cursor-pointer transition-colors ${
                isDragging
                  ? "bg-blue-50 dark:bg-gray-800"
                  : "bg-gray-50 dark:bg-gray-750"
              }`}
            >
              <input
                type="file"
                multiple
                ref={fileInputRef}
                onChange={handleFileChange}
                className="hidden"
              />
              <div className="w-12 h-12 mx-auto mb-4 flex items-center justify-center bg-light-blue-hover dark:bg-gray-700 rounded-full">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-6 w-6 text-light-blue"
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
                <p className="text-lg font-medium text-msc dark:text-white">
                  {isDragging
                    ? "Drop files here"
                    : "Select files or drop them here"}
                </p>
                <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                  You can upload multiple files
                </p>
              </div>
            </div>
          </div>

          {files.length > 0 && (
            <div className="mt-4">
              <h4 className="font-medium dark:text-white">Selected files:</h4>
              <ul className="mt-2 space-y-2">
                {files.map((file, index) => (
                  <li key={index} className="flex items-center justify-between bg-gray-100 dark:bg-gray-700 p-2 rounded-md">
                    <span className="text-sm text-gray-800 dark:text-gray-300 truncate">{file.name}</span>
                    <button
                      onClick={() => removeFile(file)}
                      className="text-red-500 hover:text-red-700"
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
              className={`px-16 py-3 rounded-md font-medium ${
                (submissionText.trim() || files.length > 0) && !uploading && user && user.balance >= 1
                  ? "bg-msc text-white hover:bg-msc-dark"
                  : "bg-light-blue-hover dark:bg-gray-600 text-gray-500 font-inter dark:text-gray-400 cursor-not-allowed"
              } transition-colors`}
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

        {/* Table of Contents */}
        <aside className="w-64 pr-8 sticky top-24 self-start hidden lg:block">
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