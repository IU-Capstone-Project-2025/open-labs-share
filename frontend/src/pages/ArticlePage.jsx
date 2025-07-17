import { useState, useRef, useEffect } from "react";
import { useParams } from "react-router-dom";
import { Viewer, Worker } from '@react-pdf-viewer/core';
import { defaultLayoutPlugin } from '@react-pdf-viewer/default-layout';
import { toolbarPlugin } from '@react-pdf-viewer/toolbar';


import '@react-pdf-viewer/core/lib/styles/index.css';
import '@react-pdf-viewer/default-layout/lib/styles/index.css';
import '@react-pdf-viewer/toolbar/lib/styles/index.css';

import { articlesAPI } from "../utils/api";
import ToastNotification from "../components/ToastNotification";
import CommentSectionArticle from "../components/CommentSectionArticle";

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
  const [toast, setToast] = useState({ show: false, message: '', type: 'info' });

  
  const defaultLayoutPluginInstance = defaultLayoutPlugin();
  const toolbarPluginInstance = toolbarPlugin();


  useEffect(() => {
    const controller = new AbortController();

    const fetchArticleAndPdf = async () => {
      try {
        setLoading(true);
        setError(null);

        
        const articleData = await articlesAPI.getArticleById(id);
        setArticle(articleData);

        
        const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
        
        
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
      {toast.show && (
        <ToastNotification
          message={toast.message}
          type={toast.type}
          onClose={() => setToast({ show: false, message: '', type: 'info' })}
        />
      )}
      <div className="max-w-6xl mx-auto px-4 py-8">
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

        {/* PDF Viewer Section */}
        <section className="bg-white dark:bg-gray-800/50 rounded-2xl shadow-lg p-8 mb-8">
          <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
            Article PDF
          </h2>
          <div className="bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden" style={{ minHeight: '800px' }}>
            {pdfFile && (
              <div style={{ height: '800px', width: '100%' }}>
                <Worker workerUrl="https://unpkg.com/pdfjs-dist@2.16.105/build/pdf.worker.min.js">
                  <Viewer fileUrl={pdfFile} plugins={[defaultLayoutPluginInstance]} />
                </Worker>
              </div>
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
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">Article Discussion</h2>
              <p className="text-sm text-gray-600 dark:text-gray-400">Share your thoughts and ask questions about this article</p>
            </div>
          </div>
          <CommentSectionArticle 
            contentType="article" 
            contentId={id} 
            userId={article?.authorId}
            userName={`${article?.authorName || ''} ${article?.authorSurname || ''}`}
          />
        </section>
      </div>
    </div>
  );
}
