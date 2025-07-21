import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { feedbackAPI, submissionsAPI, labsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { PaperClipIcon, ArrowDownTrayIcon } from '@heroicons/react/24/outline';
import ToastNotification from '../components/ToastNotification';
import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import remarkGfm from 'remark-gfm';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';
import rehypeHighlight from 'rehype-highlight';

const formatDateTime = (dateString) => {
  if (!dateString) return 'Unknown date';
  
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }
    return date.toLocaleString();
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Invalid date';
  }
};

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
const HeadingRenderer = (level) => ({ node, children }) => {
  const text = flattenText(children);
  const id = generateId(text);
  const Tag = `h${level}`;
  return (
    <Tag
      id={id}
      data-heading="true"
      className={`scroll-mt-20 ${
        level === 1 ? "text-3xl font-bold mt-8 mb-4 pt-4 border-t" : ""
      } ${level === 2 ? "text-2xl font-bold mt-6 mb-3" : ""} ${
        level === 3 ? "text-xl font-semibold mt-4 mb-2" : ""
      }`}
    >
      {children}
    </Tag>
  );
};

const FeedbackViewPage = () => {
  const { feedbackId } = useParams();
  const navigate = useNavigate();
  const [feedback, setFeedback] = useState(null);
  const [labTitle, setLabTitle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [downloadingFiles, setDownloadingFiles] = useState(new Set());
  const [toast, setToast] = useState({ show: false, message: "", type: "" });
  const user = useUser();

  const getFeedbackFileUrl = (feedbackId, filename) => {
    const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
    return `${minioEndpoint}/feedback/${feedbackId}/${filename}`;
  };

  const downloadFile = async (filename) => {
    if (!feedbackId || !filename) return;

    try {
      setDownloadingFiles(prev => new Set(prev).add(filename));
      
      const url = getFeedbackFileUrl(feedbackId, filename);
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error(`Failed to download file: HTTP ${response.status}`);
      }
      
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      window.URL.revokeObjectURL(downloadUrl);
      
      setToast({ show: true, message: `Downloaded ${filename} successfully!`, type: 'success' });
    } catch (error) {
      console.error(`Error downloading file ${filename}:`, error);
      setToast({ show: true, message: `Failed to download ${filename}: ${error.message}`, type: 'error' });
    } finally {
      setDownloadingFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete(filename);
        return newSet;
      });
    }
  };

  const downloadAllFiles = async () => {
    if (!feedback?.attachments || feedback.attachments.length === 0) return;

    try {
      setDownloadingFiles(prev => new Set([...prev, 'all']));
      
      if (feedback.attachments.length === 1) {
        await downloadFile(feedback.attachments[0].filename);
        return;
      }

      for (const attachment of feedback.attachments) {
        await downloadFile(attachment.filename);
        await new Promise(resolve => setTimeout(resolve, 500));
      }
      
      setToast({ show: true, message: 'All files downloaded successfully!', type: 'success' });
    } catch (error) {
      console.error('Error downloading all files:', error);
      setToast({ show: true, message: `Failed to download all files: ${error.message}`, type: 'error' });
    } finally {
      setDownloadingFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete('all');
        return newSet;
      });
    }
  };

  useEffect(() => {
    const fetchFeedback = async () => {
      try {
        setLoading(true);
        
        if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(feedbackId)) {
          throw new Error('Invalid feedback ID format');
        }
        
        const response = await feedbackAPI.getFeedbackById(feedbackId);
        
        if (response.reviewer.id !== user?.id) {
          throw new Error('You are not logged in to view this feedback.');
        }
        
        setFeedback(response);
        
        try {
          const submission = await submissionsAPI.getSubmissionById(response.submissionId);
          const labId = submission.labId || submission.data?.labId;
          
          if (labId) {
            const lab = await labsAPI.getLabById(labId);
            setLabTitle(lab?.title);
          }
        } catch (labErr) {
          console.error('Failed to fetch lab data:', labErr);
        }
      } catch (err) {
        setError(err.message || 'Feedback details could not be uploaded.');
        console.error(err);
        setTimeout(() => navigate('/feedback/my', { replace: true }), 2000);
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchFeedback();
    }
  }, [feedbackId, user, navigate]);

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return (
      <div className="text-center text-red-500 mt-8">
        <p>{error}</p>
        <p className="text-sm mt-2">Redirection to the feedback page...</p>
      </div>
    );
  }

  if (!feedback) {
    return <div className="text-center text-gray-500 mt-8">Feedback not found.</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <Link to="/feedback/my" className="text-blue-600 hover:underline">
          ←  Back to my feedbacks
        </Link>
      </div>

      <h1 className="text-3xl font-bold mb-4 dark:text-white">
        Feedback for submission to "{labTitle || `Lab #${feedback.submissionId}`}"
      </h1>

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-8">
        <h2 className="text-xl font-semibold mb-2 dark:text-white">Feedback details</h2>
        <div className="mb-4 dark:text-white">
          <strong>Student:</strong> {feedback.student.name} {feedback.student.surname} ({feedback.student.username})
        </div>
        <div className="mb-4 dark:text-white">
          <strong>Date of creation:</strong> {formatDateTime(feedback.createdAt)}
        </div>
        
        <h2 className="text-xl font-semibold mb-2 mt-6 dark:text-white">Your feedback</h2>
        <div className="prose dark:prose-invert max-w-none bg-gray-50 dark:bg-gray-700 p-4 rounded-md">
          <article className="prose dark:prose-invert max-w-none">
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkMath]}
              rehypePlugins={[rehypeKatex, rehypeHighlight]}
              components={{
                h1: HeadingRenderer(1),
                h2: HeadingRenderer(2),
                h3: HeadingRenderer(3),
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
                  <pre {...props} className="bg-gray-800 rounded-lg p-4 overflow-x-auto my-6" />
                ),
                code: ({ node, className, children, ...props }) => {
                  const match = /language-(\w+)/.exec(className || "");
                  const isInline = !match;
                  return isInline ? (
                    <code className="bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded text-sm" {...props}>
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
              {feedback.content}
            </ReactMarkdown>
          </article>
        </div>

        {feedback.attachments && feedback.attachments.length > 0 && (
          <div className="mt-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold dark:text-white">Attached Files ({feedback.attachments.length}):</h3>
              {feedback.attachments.length > 1 && (
                <button
                  onClick={downloadAllFiles}
                  disabled={downloadingFiles.has('all')}
                  className="flex items-center px-3 py-1.5 bg-green-600 text-white text-sm rounded-md hover:bg-green-700 disabled:bg-green-400 transition-colors"
                >
                  {downloadingFiles.has('all') ? (
                    <>
                      <Spinner className="w-4 h-4 mr-2" />
                      Downloading...
                    </>
                  ) : (
                    <>
                      <ArrowDownTrayIcon className="w-4 h-4 mr-2" />
                      Download All
                    </>
                  )}
                </button>
              )}
            </div>
            <div className="space-y-2">
              {feedback.attachments.map((attachment, index) => (
                <div key={index} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
                  <div className="flex items-center">
                    <PaperClipIcon className="w-5 h-5 text-gray-500 mr-3" />
                    <div>
                      <div className="font-medium text-gray-900 dark:text-white">
                        {attachment.filename}
                      </div>
                      <div className="text-sm text-gray-500 dark:text-gray-400">
                        {Math.round(attachment.total_size / 1024)} KB
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={() => downloadFile(attachment.filename)}
                    disabled={downloadingFiles.has(attachment.filename)}
                    className="flex items-center px-3 py-1.5 bg-msc text-white text-sm rounded-md hover:bg-msc-hover disabled:bg-blue-400 transition-colors"
                  >
                    {downloadingFiles.has(attachment.filename) ? (
                      <>
                        <Spinner className="w-4 h-4 mr-2" />
                        Downloading...
                      </>
                    ) : (
                      <>
                        <ArrowDownTrayIcon className="w-4 h-4 mr-2" />
                        Download
                      </>
                    )}
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
        
        <div className="mt-6 pt-4 border-t border-gray-200 dark:border-gray-600">
          <div className="text-sm text-gray-500 dark:text-gray-400">
            <p>Last update: {formatDateTime(feedback.updatedAt)}</p>
          </div>
        </div>
      </div>

      {toast.show && (
        <ToastNotification 
          message={toast.message}
          type={toast.type}
          onClose={() => setToast({ show: false, message: "", type: "" })}
        />
      )}
    </div>
  );
};

export default FeedbackViewPage;