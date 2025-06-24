import { useState, useRef, useEffect } from "react";
import { useParams } from "react-router-dom";
import { Document, Page } from "react-pdf";
import "react-pdf/dist/esm/Page/AnnotationLayer.css";
import "react-pdf/dist/esm/Page/TextLayer.css";

import { pdfjs } from "react-pdf";
pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.js",
  import.meta.url
).toString();

export default function ArticlePage() {
  const { id } = useParams();
  const [numPages, setNumPages] = useState(null);
  const [pdfFile, setPdfFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [file, setFile] = useState(null);
  const fileInputRef = useRef(null);
  const dropzoneRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

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

  const handleSubmit = async () => {
    if (!file) return;

    try {
      console.log("Uploading file:", file.name);
      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch("/api/upload", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) throw new Error("Upload failed");

      alert(`File "${file.name}" uploaded successfully`);
      setFile(null);
    } catch (err) {
      console.error("Upload error:", err);
      alert("Upload failed: " + err.message);
    }
  };

  const onDocumentLoadSuccess = ({ numPages }) => {
    setNumPages(numPages);
  };

  useEffect(() => {
    const controller = new AbortController();

    const fetchPdf = async () => {
      try {
        setLoading(true);
        setError(null);

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
          console.error("PDF load error:", err);
          setError(`Failed to load PDF: ${err.message}`);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchPdf();

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
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-4 mb-8">
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

          <section id="submit-section" className="mt-8">
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
                  : "bg-white dark:bg-gray-800"
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
                  <p className="text-lg font-medium text-msc dark:text-white">
                    {isDragging ? "Drop file" : "Select or put file"}
                  </p>
                  <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                    PDF
                  </p>
                </div>
              )}
            </div>

            <div className="mt-6 flex justify-center">
              <button
                onClick={handleSubmit}
                disabled={!file}
                className={`px-16 py-3 rounded-md font-medium ${
                  file
                    ? "bg-msc text-white hover:bg-msc-hover"
                    : "bg-light-blue-hover dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed"
                } transition-colors`}
              >
                Submit review
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
    </div>
  );
}
