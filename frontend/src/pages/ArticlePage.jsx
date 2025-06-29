import { useState, useRef, useEffect } from "react";
import { useParams } from "react-router-dom";
import { Document, Page } from "react-pdf";
import "react-pdf/dist/esm/Page/AnnotationLayer.css";
import "react-pdf/dist/esm/Page/TextLayer.css";
import { pdfjs } from "react-pdf";
import { articlesAPI } from "../utils/api";
import ArticleReviewModal from "../components/ArticleReviewModal";
import CommentsSection from "../components/CommentsSection";
import { getCurrentUser, isAuthenticated } from "../utils/auth";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.js",
  import.meta.url
).toString();

export default function ArticlePage() {
  const { id } = useParams();
  const [numPages, setNumPages] = useState(null);
  const [article, setArticle] = useState(null);
  const [pdfFile, setPdfFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [user, setUser] = useState(null);

  useEffect(() => {
    if (isAuthenticated()) {
      const currentUser = getCurrentUser();
      setUser(currentUser);
    }
  }, []);

  const scrollToSubmit = () => {
    const submitSection = document.getElementById("submit-section");
    submitSection?.scrollIntoView({ behavior: "smooth" });
  };

  const handleReviewSubmit = (text, files) => {
    console.log("Submitting review:", { text, files });
    // This is where you would implement the API call to submit the review
    alert(`Review submitted!\nText: ${text}\nFiles: ${files.map(f => f.name).join(', ')}`);
  };

  const onDocumentLoadSuccess = ({ numPages }) => {
    setNumPages(numPages);
  };

  useEffect(() => {
    const controller = new AbortController();

    const fetchArticleAndPdf = async () => {
      try {
        setLoading(true);
        setError(null);

        // Fetch article metadata
        const articleData = await articlesAPI.getArticleById(id);
        setArticle(articleData);

        // TODO: Implement asset download from Minio via backend
        // For now, continue using the sample PDF
        const response = await fetch(`/articles_sample/${id}.pdf`, {
          signal: controller.signal,
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        if (
          !response.headers.get("content-type")?.includes("application/pdf")
        ) {
          throw new Error("Not a PDF file");
        }

        const blob = await response.blob();
        if (blob.size === 0) throw new Error("Empty PDF");

        setPdfFile(URL.createObjectURL(blob));
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
      <div className="flex items-center justify-center h-screen dark:bg-gray-900">
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-lg max-w-md">
          <h2 className="text-xl font-bold text-red-500 mb-4">Error</h2>
          <p className="text-gray-700 dark:text-gray-300 mb-4">{error}</p>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            File path: /articles_sample/{id}.pdf
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex dark:bg-gray-900 min-h-screen">
      <div className="max-w-6xl mx-auto flex w-full">
        <div className="flex-1 p-8 overflow-y-auto">
          {/* Article Content Section */}
          <section className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6 mb-8">
            <div className="flex items-center mb-6 pb-4 border-b border-gray-200 dark:border-gray-600">
              <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center mr-3">
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
                <h1 className="text-xl font-bold text-gray-900 dark:text-white">
                  {article ? article.title : "Research Article"}
                </h1>
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  {article ? `By ${article.authorName} ${article.authorSurname}` : "Read and review the academic paper"}
                </p>
              </div>
            </div>
            
            <div className="bg-gray-50 dark:bg-gray-750 rounded-lg p-4">
              {pdfFile && (
                <Document
                  file={pdfFile}
                  onLoadSuccess={onDocumentLoadSuccess}
                  onLoadError={(error) =>
                    setError(`Render error: ${error.message}`)
                  }
                  loading={<div className="text-center py-8">Loading PDF...</div>}
                >
                  {Array.from({ length: numPages }, (_, i) => (
                    <Page
                      key={`page_${i + 1}`}
                      pageNumber={i + 1}
                      width={800}
                      className="mb-4 border border-gray-200 dark:border-gray-700"
                      loading={
                        <div className="h-[800px] bg-gray-100 flex items-center justify-center">
                          Loading page {i + 1}...
                        </div>
                      }
                    />
                  ))}
                </Document>
              )}
            </div>
          </section>

          {/* Review Submission Section */}
          <section id="submit-section" className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-8 mb-8">
            <div className="flex items-center mb-6 pb-4 border-b border-gray-200 dark:border-gray-600">
              <div className="w-10 h-10 bg-purple-600 rounded-full flex items-center justify-center mr-3">
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
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                  />
                </svg>
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900 dark:text-white">Peer Review Submission</h2>
                <p className="text-sm text-gray-600 dark:text-gray-400">Submit your review and feedback on this article</p>
              </div>
            </div>

            <div className="mt-6 flex justify-center">
              <button
                onClick={() => setIsReviewModalOpen(true)}
                className="px-16 py-3 rounded-md font-medium bg-msc text-white hover:bg-msc-hover transition-colors"
              >
                Submit Review
              </button>
            </div>
          </section>
        </div>

        <aside className="w-64 p-4 border-l border-gray-200 dark:border-gray-700 overflow-y-auto sticky top-0 h-screen">
          <button
            onClick={scrollToSubmit}
            className="w-full py-2 px-4 bg-msc text-white rounded-md hover:bg-msc-dark transition-colors flex items-center justify-center"
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
                d="M19 14l-7 7m0 0l-7-7m7 7V3"
              />
            </svg>
            To Submit review
          </button>
        </aside>
      </div>
      <ArticleReviewModal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        onSubmit={handleReviewSubmit}
      />
    </div>
  );
}
