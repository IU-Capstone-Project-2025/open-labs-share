import { useState, useEffect, useRef, useCallback } from "react";
import { useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkGfm from "remark-gfm";
import "highlight.js/styles/github-dark.css";
import CommentsSection from "../components/CommentsSection";
import ChatWindow from "../components/ChatWindow";
import { getCurrentUser, isAuthenticated } from "../utils/auth";
import { labsAPI, submissionsAPI } from "../utils/api";

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
  const [file, setFile] = useState(null);
  const fileInputRef = useRef(null);
  const dropzoneRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [user, setUser] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [chatMode, setChatMode] = useState('floating'); // 'floating' or 'sidebar'

  // Initialize user state
  useEffect(() => {
    if (isAuthenticated()) {
      const currentUser = getCurrentUser();
      setUser(currentUser);
    }
  }, []);

  const scrollToSubmit = useCallback(() => {
    const submitSection = document.getElementById("submit-section");
    if (submitSection) {
      submitSection.scrollIntoView({ behavior: "smooth" });
    }
  }, []);

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      setFile(selectedFile);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      setFile(droppedFile);
    }
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
    if (!file || !user) return;

    try {
      setUploading(true);
      await submissionsAPI.submitLabFile(id, user.id, file);
      alert(`The file "${file.name}" uploaded successfully`);
      setFile(null);
    } catch (err) {
      console.error("Upload error:", err);
      alert("Upload failed: " + err.message);
    } finally {
      setUploading(false);
    }
  };

  useEffect(() => {
    const fetchLabData = async () => {
      try {
        setLoading(true);
        
        // Fetch lab details
        const labResponse = await labsAPI.getLabById(id);
        console.log('Lab response:', labResponse);
        setLab(labResponse);
        
        // Fetch lab assets to find the markdown file
        try {
          const assetsResponse = await labsAPI.getLabAssets(id);
          console.log('Assets response:', assetsResponse);
          console.log('Assets response type:', typeof assetsResponse);
          console.log('Assets response keys:', Object.keys(assetsResponse));
          
          // Check different possible property names for assets
          const assetsList = assetsResponse.assets || assetsResponse.assetsList || assetsResponse.data || assetsResponse;
          console.log('Parsed assets list:', assetsList);
          console.log('Assets list type:', typeof assetsList);
          console.log('Assets list is array:', Array.isArray(assetsList));
          
          if (assetsList && Array.isArray(assetsList) && assetsList.length > 0) {
            console.log('Assets found, processing...');
            console.log('First asset structure:', assetsList[0]);
            console.log('First asset keys:', Object.keys(assetsList[0]));
            
            // Find the markdown file (usually the first .md file)
            const markdownAsset = assetsList.find(asset => {
              console.log('Checking asset:', asset);
              const filename = asset.filename || asset.fileName || asset.name;
              console.log('Asset filename:', filename);
              return filename && filename.toLowerCase().endsWith('.md');
            });
            
            if (markdownAsset) {
              console.log('Found markdown asset:', markdownAsset);
              console.log('Markdown asset keys:', Object.keys(markdownAsset));
              
              // Try different property names for asset ID
              const assetId = markdownAsset.assetId || markdownAsset.asset_id || markdownAsset.id || markdownAsset.assetID;
              console.log('Using asset ID:', assetId);
              
              // Download the markdown content
              try {
                const blob = await labsAPI.downloadLabAsset(id, assetId);
                const text = await blob.text();
                console.log('Downloaded markdown content:', text.substring(0, 200) + '...');
                setMarkdown(text);
              } catch (downloadError) {
                console.error('Error downloading markdown:', downloadError);
                setMarkdown(getPlaceholderContent(labResponse));
              }
            } else {
              console.log('No markdown file found in assets');
              console.log('Available files:', assetsList.map(a => a.filename || a.fileName || a.name || 'unknown'));
              setMarkdown(getPlaceholderContent(labResponse));
            }
          } else {
            console.log('No assets found for this lab or assets is not an array');
            console.log('Assets data:', assetsList);
            setMarkdown(getPlaceholderContent(labResponse));
          }
        } catch (assetsError) {
          console.error('Error fetching lab assets:', assetsError);
          setMarkdown(getPlaceholderContent(labResponse));
        }
        
      } catch (err) {
        console.error("Error fetching lab data:", err);
        setError(`Failed to load lab: ${err.message}`);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchLabData();
    }
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

  const components = {
    h1: HeadingRenderer(1),
    h2: HeadingRenderer(2),
    h3: HeadingRenderer(3),
    ul: ({ node, ...props }) => (
      <ul
        {...props}
        className="list-disc pl-6 my-4 space-y-1 dark:text-gray-300"
      />
    ),
    ol: ({ node, ...props }) => (
      <ol
        {...props}
        className="list-decimal pl-6 my-4 space-y-1 dark:text-gray-300"
      />
    ),
    li: ({ node, ...props }) => <li {...props} className="pl-2" />,
    pre: ({ node, ...props }) => (
      <pre
        {...props}
        className="bg-gray-100 dark:bg-gray-800 rounded-lg p-4 overflow-x-auto my-4"
      />
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
    p: ({ node, ...props }) => (
      <p {...props} className="my-4 leading-relaxed dark:text-gray-300" />
    ),
    blockquote: ({ node, ...props }) => (
      <blockquote
        {...props}
        className="border-l-4 border-msc pl-4 my-4 italic dark:text-gray-300"
      />
    ),
    a: ({ node, ...props }) => (
      <a
        {...props}
        className="text-msc hover:text-msc-hover underline"
        target="_blank"
        rel="noopener noreferrer"
      />
    ),
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen dark:bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-msc"></div>
      </div>
    );
  }

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
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
                {lab.title}
              </h1>
              
              {lab.shortDesc && (
                <p className="text-lg text-gray-600 dark:text-gray-300 mb-6">
                  {lab.shortDesc}
                </p>
              )}
              
              <div className="flex items-center text-sm text-gray-500 dark:text-gray-400">
                <div className="flex items-center mr-6">
                  <div className="w-8 h-8 rounded-full bg-msc flex items-center justify-center text-white text-sm font-medium mr-3">
                    {lab.authorName?.[0] || 'U'}
                    {lab.authorSurname?.[0] || ''}
                  </div>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {lab.authorName && lab.authorSurname 
                      ? `${lab.authorName} ${lab.authorSurname}`
                      : 'Unknown Author'
                    }
                  </span>
                </div>
                
                {lab.createdAt && (
                  <div className="flex items-center mr-6">
                    <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd"/>
                    </svg>
                    Created: {new Date(lab.createdAt).toLocaleDateString()}
                  </div>
                )}
                
                <div className="flex items-center space-x-4">
                  {lab.views !== undefined && (
                    <span className="flex items-center">
                      <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                        <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd"/>
                      </svg>
                      {lab.views} views
                    </span>
                  )}
                  {lab.submissions !== undefined && (
                    <span className="flex items-center">
                      <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 4a1 1 0 011-1h12a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1V8z" clipRule="evenodd"/>
                      </svg>
                      {lab.submissions} submissions
                    </span>
                  )}
                </div>
              </div>
            </div>
          </section>
        )}

        {/* Lab Content Section */}
        <section className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-8 mb-8">
          <article className="prose dark:prose-invert max-w-none">
            <ReactMarkdown
              rehypePlugins={[rehypeHighlight]}
              remarkPlugins={[remarkGfm]}
              components={components}
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
            {file ? (
              <div className="text-center">
                <p className="font-medium text-msc dark:text-white">
                  The file is selected: {file.name}
                </p>
                <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                  Click to select another file
                </p>
              </div>
            ) : (
              <div className="text-center">
                <p className="text-lg font-medium text-msc dark:text-white">
                  {isDragging
                    ? "Put the file here"
                    : "Select the file or put it here"}
                </p>
                <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                  Supported file formats: PDF
                </p>
              </div>
            )}
          </div>

          <div className="mt-6 flex justify-center">
            <button
              onClick={handleSubmit}
              disabled={!file || uploading || !user}
              className={`px-16 py-3 rounded-md font-medium ${
                file && !uploading && user
                  ? "bg-msc text-white hover:bg-msc-dark"
                  : "bg-light-blue-hover dark:bg-gray-600 text-gray-500 font-inter dark:text-gray-400 cursor-not-allowed"
              } transition-colors`}
            >
              {uploading ? "Uploading..." : "Submit homework"}
            </button>
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
          
          <CommentsSection contentType="lab" contentId={id} userId={user?.id} />
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
