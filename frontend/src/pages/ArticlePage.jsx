import { useState, useRef, useEffect } from "react";
import { useParams } from "react-router-dom";
import { Viewer, Worker } from '@react-pdf-viewer/core';
import { defaultLayoutPlugin } from '@react-pdf-viewer/default-layout';
import { toolbarPlugin } from '@react-pdf-viewer/toolbar';

// Import the styles
import '@react-pdf-viewer/core/lib/styles/index.css';
import '@react-pdf-viewer/default-layout/lib/styles/index.css';
import '@react-pdf-viewer/toolbar/lib/styles/index.css';

import { articlesAPI } from "../utils/api";

export default function ArticlePage() {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [pdfFile, setPdfFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [file, setFile] = useState(null);
  const fileInputRef = useRef(null);
  const dropzoneRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  // Create plugins for react-pdf-viewer
  const defaultLayoutPluginInstance = defaultLayoutPlugin();
  const toolbarPluginInstance = toolbarPlugin();

  const scrollToSubmit = () => {
    const submitSection = document.getElementById("submit-section");
    submitSection?.scrollIntoView({ behavior: "smooth" });
  };

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    setFile(selectedFile || null);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    handleFileChange({ target: { files: e.dataTransfer.files } });
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!file) {
      alert('Please select a PDF file for your review.');
      return;
    }

    const formData = new FormData();
    formData.append('review_file', file);

    try {
      // TODO: Replace with actual review submission API
      console.log('Submitting review...', file.name);
      alert('Review submitted successfully! (This is a placeholder)');
      setFile(null);
      fileInputRef.current.value = '';
    } catch (error) {
      console.error('Error submitting review:', error);
      alert('Failed to submit review. Please try again.');
    }
  };

  useEffect(() => {
    const controller = new AbortController();

    const fetchArticleAndPdf = async () => {
      try {
        setLoading(true);
        setError(null);

        // Step 1: Fetch article metadata (remains the same)
        const articleData = await articlesAPI.getArticleById(id);
        setArticle(articleData);

        // Step 2: Construct direct URL to Minio and fetch the PDF.
        // This requires the VITE_MINIO_ENDPOINT to be set in the frontend's .env file.
        const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
        
        // The filename 'article.pdf' is assumed based on backend documentation.
        const pdfUrl = `${minioEndpoint}/articles/${id}/article.pdf`;

        const pdfResponse = await fetch(pdfUrl, {
          signal: controller.signal,
        });

        if (!pdfResponse.ok) {
          throw new Error(`Failed to fetch PDF from Minio: HTTP status ${pdfResponse.status}`);
        }

        const contentType = pdfResponse.headers.get("content-type");
        if (!contentType || (!contentType.includes("application/pdf") && !contentType.includes("application/octet-stream"))) {
          throw new Error(`Invalid content type for PDF: ${contentType}`);
        }

        const blob = await pdfResponse.blob();
        if (blob.size === 0) {
          throw new Error("Empty PDF file received from Minio.");
        }

        const blobUrl = URL.createObjectURL(blob);
        setPdfFile(blobUrl);
      } catch (err) {
        if (err.name !== "AbortError") {
          console.error("Article or PDF load error:", err);
          setError(`Failed to load article or PDF: ${err.message}`);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchArticleAndPdf();

    return () => {
      controller.abort();
      if (pdfFile) URL.revokeObjectURL(pdfFile);
    };
  }, [id]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen dark:bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-msc"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8 dark:bg-gray-900 min-h-screen">
        <div className="max-w-4xl mx-auto">
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-6">
            <h2 className="text-lg font-semibold text-red-800 dark:text-red-200 mb-2">
              Error Loading Article
            </h2>
            <p className="text-red-600 dark:text-red-300">{error}</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-md transition-colors"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!article) {
    return (
      <div className="container mx-auto px-4 py-8 dark:bg-gray-900 min-h-screen">
        <div className="max-w-4xl mx-auto">
          <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-6">
            <h2 className="text-lg font-semibold text-yellow-800 dark:text-yellow-200 mb-2">
              Article Not Found
            </h2>
            <p className="text-yellow-600 dark:text-yellow-300">
              The requested article could not be found.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Article Header */}
        <div className="max-w-4xl mx-auto mb-12 text-center">
          <h1 className="text-4xl font-bold font-display text-gray-900 dark:text-white">
            {article.title}
          </h1>
          <p className="mt-2 text-lg text-gray-500 dark:text-gray-400">
            {article.shortDesc}
          </p>
          <div className="mt-6 flex justify-center items-center flex-wrap gap-x-6 gap-y-2 text-sm text-gray-500 dark:text-gray-400">
            <span>By {article.authorName} {article.authorSurname}</span>
            <span>•</span>
            <span>{new Date(article.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}</span>
            <span>•</span>
            <span>{article.views} views</span>
          </div>
        </div>

        {/* Main Content */}
        <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-4 gap-12">
          {/* PDF Viewer Section */}
          <div className="lg:col-span-3">
            <section className="bg-white dark:bg-gray-800/50 rounded-2xl shadow-lg p-8">
              <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
                Article PDF
              </h2>
              <div className="bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden">
                {pdfFile && (
                  <div style={{ height: '750px' }}>
                    <Worker workerUrl="https://unpkg.com/pdfjs-dist@2.16.105/build/pdf.worker.min.js">
                      <Viewer fileUrl={pdfFile} plugins={[defaultLayoutPluginInstance]} />
                    </Worker>
                  </div>
                )}
              </div>
            </section>
          </div>

          {/* Review Submission Section */}
          <div className="lg:col-span-1">
            <section id="submit-section" className="sticky top-24 bg-white dark:bg-gray-800/50 rounded-2xl shadow-lg p-8">
              <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
                Submit Your Review
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6 text-sm">
                Upload your peer review document for this article. Please ensure your review is in PDF format.
              </p>

              <form onSubmit={handleSubmit} className="space-y-6">
                <div
                  ref={dropzoneRef}
                  onDrop={handleDrop}
                  onDragOver={handleDragOver}
                  onDragLeave={() => setIsDragging(false)}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 ${
                    isDragging ? "border-msc" : "border-dashed border-gray-400"
                  } rounded-lg p-8 text-center cursor-pointer transition-colors ${
                    isDragging
                      ? "bg-blue-50 dark:bg-gray-700"
                      : "bg-gray-50 dark:bg-gray-750"
                  }`}
                >
                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleFileChange}
                    className="hidden"
                    accept=".pdf"
                  />
                  <div className="w-12 h-12 mx-auto mb-4 flex items-center justify-center bg-gray-100 dark:bg-gray-700 rounded-full">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="h-6 w-6 text-light-blue dark:text-gray-400"
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
                        Selected: {file.name}
                      </p>
                      <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                        Click to change
                      </p>
                    </div>
                  ) : (
                    <div className="text-center">
                      <p className="font-medium text-gray-700 dark:text-gray-300">
                        Drag and drop your review PDF here
                      </p>
                      <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                        or click to browse files
                      </p>
                    </div>
                  )}
                </div>

                <div className="flex justify-end space-x-4">
                  <button
                    type="button"
                    onClick={() => {
                      setFile(null);
                      fileInputRef.current.value = '';
                    }}
                    className="px-6 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700 transition-colors"
                    disabled={!file}
                  >
                    Clear
                  </button>
                  <button
                    type="submit"
                    className="px-6 py-2 bg-msc hover:bg-msc-hover text-white rounded-md disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    disabled={!file}
                  >
                    Submit Review
                  </button>
                </div>
              </form>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}
