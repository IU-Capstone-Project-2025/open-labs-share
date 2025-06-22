import { useState, useEffect, useRef, useCallback } from "react";
import { useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkGfm from "remark-gfm";
import "highlight.js/styles/github-dark.css";

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
  const [markdown, setMarkdown] = useState("");
  const [headings, setHeadings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeId, setActiveId] = useState("");
  const observer = useRef();
  const contentRef = useRef();
  const [file, setFile] = useState(null);
  const fileInputRef = useRef(null);
  const dropzoneRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

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

  const handleSubmit = () => {
    if (file) {
      //file upload logic
      console.log("Uploading file:", file.name);
      alert(`The file "${file.name}" uploaded successfully`);
      setFile(null);
    }
  };

  useEffect(() => {
    const fetchMarkdown = async () => {
      try {
        setLoading(true);
        const response = await fetch(`/labs_sample/${id}.md`);
        const text = await response.text();
        setMarkdown(text);
      } catch (err) {
        console.error("Markdown download error: ", err);
      } finally {
        setLoading(false);
      }
    };

    fetchMarkdown();
  }, [id]);

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
    code: ({ node, inline, className, ...props }) => {
      const isBash = className?.includes("language-bash");
      return inline ? (
        <code
          {...props}
          className="bg-gray-200 dark:bg-gray-700 px-1.5 py-0.5 rounded text-sm"
        />
      ) : (
        <div className="relative">
          {isBash && (
            <div className="absolute top-0 left-0 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-1 text-xs rounded-tl rounded-br">
              bash
            </div>
          )}
          <code
            {...props}
            className={`${className} block p-4 ${isBash ? "pt-8" : ""}`}
          />
        </div>
      );
    },
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen dark:bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-msc"></div>
      </div>
    );
  }

  return (
    <div className="flex dark:bg-gray-900 min-h-screen">
      <div
        ref={contentRef}
        className="flex-1 p-8 overflow-y-auto scroll-smooth"
      >
        <article className="prose dark:prose-invert max-w-none">
          <ReactMarkdown
            rehypePlugins={[rehypeHighlight]}
            remarkPlugins={[remarkGfm]}
            components={components}
          >
            {markdown}
          </ReactMarkdown>
        </article>
        <section id="submit-section" className="mt-12">
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
                : "bg-white dark:bg-gray-800"
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

          <div className="mt-4 flex justify-center">
            <button
              onClick={handleSubmit}
              disabled={!file}
              className={`px-16 py-3 rounded-md font-medium ${
                file
                  ? "bg-msc text-white hover:bg-msc-dark"
                  : "bg-light-blue-hover dark:bg-gray-600 text-gray-500 font-inter dark:text-gray-400 cursor-not-allowed"
              } transition-colors`}
            >
              Submit homework
            </button>
          </div>
        </section>
      </div>

      <aside className="w-64 p-4 border-l border-gray-200 dark:border-gray-700 overflow-y-auto sticky top-0 h-screen">
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
  );
}
