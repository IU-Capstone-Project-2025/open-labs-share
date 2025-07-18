import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { labsAPI, submissionsAPI, feedbackAPI } from '../utils/api';
import { mlAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { DocumentTextIcon, ClockIcon, UserIcon, PaperClipIcon, ArrowDownTrayIcon } from '@heroicons/react/24/outline';
import ToastNotification from '../components/ToastNotification';
import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import remarkGfm from 'remark-gfm';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';
import rehypeHighlight from 'rehype-highlight';

const formatDate = (dateString) => {
  if (!dateString) return 'Unknown date';
  
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }
    return date.toLocaleDateString();
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Invalid date';
  }
};

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

const FeedbackCard = ({ feedback }) => {
  const [downloadingFeedbackFiles, setDownloadingFeedbackFiles] = useState(new Set());
  const [feedbackToast, setFeedbackToast] = useState({ show: false, message: "", type: "" });

  const getFeedbackFileUrl = (feedbackId, filename) => {
    const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
    return `${minioEndpoint}/feedback/${feedbackId}/${filename}`;
  };

  const downloadFeedbackFile = async (filename) => {
    if (!feedback.id || !filename) return;

    try {
      setDownloadingFeedbackFiles(prev => new Set(prev).add(filename));
      
      const url = getFeedbackFileUrl(feedback.id, filename);
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
      
      setFeedbackToast({ show: true, message: `Downloaded ${filename} successfully!`, type: 'success' });
    } catch (error) {
      console.error(`Error downloading feedback file ${filename}:`, error);
      setFeedbackToast({ show: true, message: `Failed to download ${filename}: ${error.message}`, type: 'error' });
    } finally {
      setDownloadingFeedbackFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete(filename);
        return newSet;
      });
    }
  };

  const downloadAllFeedbackFiles = async () => {
    if (!feedback.attachments || feedback.attachments.length === 0) return;

    try {
      setDownloadingFeedbackFiles(prev => new Set([...prev, 'all']));
      
      if (feedback.attachments.length === 1) {
        await downloadFeedbackFile(feedback.attachments[0].filename);
        return;
      }

      for (const attachment of feedback.attachments) {
        await downloadFeedbackFile(attachment.filename);
        await new Promise(resolve => setTimeout(resolve, 500));
      }
      
      setFeedbackToast({ show: true, message: 'All feedback files downloaded successfully!', type: 'success' });
    } catch (error) {
      console.error('Error downloading all feedback files:', error);
      setFeedbackToast({ show: true, message: `Failed to download all feedback files: ${error.message}`, type: 'error' });
    } finally {
      setDownloadingFeedbackFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete('all');
        return newSet;
      });
    }
  };

  return (
    <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg mb-4">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center space-x-2">
          <UserIcon className="w-5 h-5 text-blue-500" />
          <span className="font-medium text-gray-900 dark:text-white">
            {feedback.reviewer.name} {feedback.reviewer.surname}
          </span>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            (@{feedback.reviewer.username})
          </span>
        </div>
        <div className="flex items-center space-x-1 text-sm text-gray-500 dark:text-gray-400">
          <ClockIcon className="w-4 h-4" />
          <span>{formatDate(feedback.createdAt)}</span>
        </div>
      </div>
      
      <div className="prose dark:prose-invert max-w-none">
        <article className="prose dark:prose-invert max-w-none">
          <ReactMarkdown
            remarkPlugins={[remarkGfm, remarkMath]}
            rehypePlugins={[rehypeKatex, rehypeHighlight]}
            components={{
              h1: ({node, ...props}) => <h1 className="mt-4 mb-2 font-bold text-gray-900 dark:text-white heading-level-1" {...props} />,
              h2: ({node, ...props}) => <h2 className="mt-4 mb-2 font-bold text-gray-900 dark:text-white heading-level-2" {...props} />,
              h3: ({node, ...props}) => <h3 className="mt-4 mb-2 font-bold text-gray-900 dark:text-white heading-level-3" {...props} />,
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
        <div className="mt-4">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-sm font-medium text-gray-900 dark:text-white">
              Attached Files ({feedback.attachments.length}):
            </h4>
            {feedback.attachments.length > 1 && (
              <button
                onClick={downloadAllFeedbackFiles}
                disabled={downloadingFeedbackFiles.has('all')}
                className="flex items-center px-2 py-1 bg-green-600 text-white text-xs rounded-md hover:bg-green-700 disabled:bg-green-400 transition-colors"
              >
                {downloadingFeedbackFiles.has('all') ? (
                  <>
                    <Spinner className="w-3 h-3 mr-1" />
                    Downloading...
                  </>
                ) : (
                  <>
                    <ArrowDownTrayIcon className="w-3 h-3 mr-1" />
                    Download All
                  </>
                )}
              </button>
            )}
          </div>
          <div className="space-y-2">
            {feedback.attachments.map((attachment, index) => (
              <div key={index} className="flex items-center justify-between p-2 bg-white dark:bg-gray-600 rounded-md">
                <div className="flex items-center">
                  <PaperClipIcon className="w-4 h-4 text-gray-500 mr-2" />
                  <div>
                    <div className="font-medium text-gray-900 dark:text-white text-sm">
                      {attachment.filename}
                    </div>
                    <div className="text-xs text-gray-500 dark:text-gray-400">
                      {Math.round(attachment.total_size / 1024)} KB
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => downloadFeedbackFile(attachment.filename)}
                  disabled={downloadingFeedbackFiles.has(attachment.filename)}
                  className="flex items-center px-2 py-1 bg-msc text-white text-xs rounded-md hover:bg-msc-hover disabled:bg-blue-400 transition-colors"
                >
                  {downloadingFeedbackFiles.has(attachment.filename) ? (
                    <>
                      <Spinner className="w-3 h-3 mr-1" />
                      Downloading...
                    </>
                  ) : (
                    <>
                      <ArrowDownTrayIcon className="w-3 h-3 mr-1" />
                      Download
                    </>
                  )}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {feedbackToast.show && (
        <ToastNotification 
          message={feedbackToast.message}
          type={feedbackToast.type}
          onClose={() => setFeedbackToast({ show: false, message: "", type: "" })}
        />
      )}
    </div>
  );
};

const SubmissionPage = () => {
  const { id } = useParams();
  const user = useUser();
  const [submission, setSubmission] = useState(null);
  const [lab, setLab] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading] = useState(true);
  const [feedbackLoading, setFeedbackLoading] = useState(true);
  const [error, setError] = useState(null);
  const [feedbackError, setFeedbackError] = useState(null);
  const [downloadingFiles, setDownloadingFiles] = useState(new Set());
  const [toast, setToast] = useState({ show: false, message: "", type: "" });
  const [gradingStatus, setGradingStatus] = useState(null);
  const [gradingResult, setGradingResult] = useState(null);
  const [gradingError, setGradingError] = useState(null);
  const [gradingLoading, setGradingLoading] = useState(false);


  const getSubmissionFileUrl = (submissionId, filename) => {
    const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
    return `${minioEndpoint}/submissions/${submissionId}/${filename}`;
  };

  const downloadFile = async (filename) => {
    if (!id || !filename) return;

    try {
      setDownloadingFiles(prev => new Set(prev).add(filename));
      
      const url = getSubmissionFileUrl(id, filename);
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
    if (!submission?.assets || submission.assets.length === 0) return;

    try {
      setDownloadingFiles(prev => new Set([...prev, 'all']));
      
      if (submission.assets.length === 1) {
        await downloadFile(submission.assets[0].filename);
        return;
      }

      for (const asset of submission.assets) {
        await downloadFile(asset.filename);
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
    const fetchSubmission = async () => {
      try {
        setLoading(true);
        const sub = await submissionsAPI.getSubmissionById(id);
        setSubmission(sub);

        if (sub?.labId) {
          const labData = await labsAPI.getLabById(sub.labId);
          setLab(labData);
        }
      } catch (err) {
        setError('Failed to fetch submission details.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchSubmission();
    }
  }, [id]);

  useEffect(() => {
    const fetchFeedback = async () => {
      if (!submission || !user) return;
      
      try {
        setFeedbackLoading(true);
        const response = await feedbackAPI.getMyFeedbackForSubmission(submission.submissionId);
        setFeedback(response);
      } catch (err) {
        if (err.message?.includes('404') || err.message?.includes('not found')) {
          setFeedback(null);
        } else {
          setFeedbackError('Failed to fetch feedback.');
          console.error('Error fetching feedback:', err);
        }
      } finally {
        setFeedbackLoading(false);
      }
    };

    fetchFeedback();
  }, [submission, user]);

  useEffect(() => {
    let polling = null;
    const fetchGrading = async () => {
      if (!submission || !user?.id || !submission.labId || (!submission.submissionId && !submission.id)) return;
      const submissionId = submission.submissionId || submission.id;
      const params = {
        uuid: String(user.id),
        assignment_id: String(submission.labId),
        submission_id: String(submissionId),
        webhook_url: 'http://localhost:8081',
      };
      setGradingLoading(true);
      setGradingError(null);
      try {
        const statusData = await mlAPI.getGradingStatus(params);
        setGradingStatus(statusData.status || statusData);
        console.log('Grading status:', statusData.status === 'SUCCESS');

        if (statusData.status === 'SUCCESS' || statusData.status === 'success') {
          try {
            const resultData = await mlAPI.getGradingResult(params);
            console.log('Grading res:', resultData);

            setGradingResult(resultData);
          } catch (err) {
            setGradingResult(null);
            setGradingError('Failed to fetch grading result');
          }
          setGradingLoading(false);
        } else if (statusData.status === 'FAILURE') {
          setGradingError('Autograding failed');
          setGradingLoading(false);
        } else {
          polling = setTimeout(fetchGrading, 5000);
          setGradingLoading(false);
        }
      } catch (err) {
        setGradingError('Error fetching grading status/result');
        setGradingLoading(false);
      }
    };
    if (submission && user && submission.labId) {
      fetchGrading();
    }
    return () => { if (polling) clearTimeout(polling); };
  }, [submission, user]);

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 mt-8">{error}</div>;
  }
  
  if (!submission) {
    return <div className="text-center text-gray-500 mt-8">Submission not found.</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-2">
        Submission for: "{lab?.title || `Lab #${submission.labId}`}"
      </h1>
      <p className="text-gray-600 dark:text-gray-400 mb-8">
        Submitted on: {formatDateTime(submission.createdAt)} by {submission.owner?.username || 'Unknown User'}
      </p>

      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow mb-8">
        <h2 className="text-2xl font-semibold mb-4">Submission Details</h2>
        <div className="prose dark:prose-invert max-w-none">
          <p><strong>Comment/Solution:</strong></p>
          <article className="prose dark:prose-invert max-w-none">
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkMath]}
              rehypePlugins={[rehypeKatex, rehypeHighlight]}
              components={{
                h1: ({node, ...props}) => <h1 className="mt-4 mb-2 font-bold text-gray-900 dark:text-white heading-level-1" {...props} />,
                h2: ({node, ...props}) => <h2 className="mt-4 mb-2 font-bold text-gray-900 dark:text-white heading-level-2" {...props} />,
                h3: ({node, ...props}) => <h3 className="mt-4 mb-2 font-bold text-gray-900 dark:text-white heading-level-3" {...props} />,
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
              {submission.text}
            </ReactMarkdown>
          </article>
          
          {submission.assets && submission.assets.length > 0 && (
            <div className="mt-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-xl font-semibold">Attached Files ({submission.assets.length}):</h3>
                {submission.assets.length > 1 && (
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
                {submission.assets.map(asset => (
                  <div key={asset.assetId} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
                    <div className="flex items-center">
                      <PaperClipIcon className="w-5 h-5 text-gray-500 mr-3" />
                      <div>
                        <div className="font-medium text-gray-900 dark:text-white">
                          {asset.filename}
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() => downloadFile(asset.filename)}
                      disabled={downloadingFiles.has(asset.filename)}
                      className="flex items-center px-3 py-1.5 bg-msc text-white text-sm rounded-md hover:bg-msc-hover disabled:bg-blue-400 transition-colors"
                    >
                      {downloadingFiles.has(asset.filename) ? (
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
        </div>
      </div>
      
      {/* Review section with feedback */}
      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
        <div className="flex items-center space-x-2 mb-4">
          <DocumentTextIcon className="w-6 h-6 text-blue-500" />
          <h2 className="text-2xl font-semibold">Reviews & Feedback</h2>
        </div>
        
        {feedbackLoading ? (
          <div className="flex justify-center items-center h-24">
            <Spinner />
          </div>
        ) : feedbackError ? (
          <div className="text-center text-red-500 py-4">
            {feedbackError}
          </div>
        ) : feedback ? (
          <FeedbackCard feedback={feedback} />
        ) : (
          <div className="text-center text-gray-500 py-8">
            <DocumentTextIcon className="w-12 h-12 mx-auto mb-2 text-gray-400" />
            <p>No feedback has been provided for this submission yet.</p>
          </div>
        )}
      </div>

      {/* Autograding Results Section */}
      <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow mb-8 mt-4">
        <h2 className="text-2xl font-semibold mb-4">Autograding Results</h2>
        {gradingLoading ? (
          <div className="flex items-center space-x-2 text-blue-500"><Spinner className="w-5 h-5" /> <span>Checking autograding status...</span></div>
        ) : gradingError ? (
          <div className="text-red-500">{gradingError}</div>
        ) : !gradingResult ? (
          <div className="text-gray-500">No autograding information available.</div>
        ) : null}
        {gradingResult && (
          <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-700 rounded space-y-4">
            <h3 className="text-lg font-semibold mb-2">Autograding Breakdown</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <span className="font-medium">Code Elegance:</span> {gradingResult.code_elegance_grade ?? '-'} / 10
              </div>
              <div>
                <span className="font-medium">Correctness:</span> {gradingResult.correctness_grade ?? '-'} / 10
              </div>
              <div>
                <span className="font-medium">Documentation:</span> {gradingResult.documentation_grade ?? '-'} / 10
              </div>
              <div>
                <span className="font-medium">Readability:</span> {gradingResult.readability_grade ?? '-'} / 10
              </div>
            </div>
            {gradingResult.code_elegance_feedback && (
              <div>
                <h4 className="font-semibold mt-4 mb-1">Code Elegance Feedback</h4>
                <p className="text-gray-800 dark:text-gray-200 whitespace-pre-line">{gradingResult.code_elegance_feedback}</p>
              </div>
            )}
            {gradingResult.correctness_feedback && (
              <div>
                <h4 className="font-semibold mt-4 mb-1">Correctness Feedback</h4>
                <p className="text-gray-800 dark:text-gray-200 whitespace-pre-line">{gradingResult.correctness_feedback}</p>
              </div>
            )}
            {gradingResult.documentation_feedback && (
              <div>
                <h4 className="font-semibold mt-4 mb-1">Documentation Feedback</h4>
                <p className="text-gray-800 dark:text-gray-200 whitespace-pre-line">{gradingResult.documentation_feedback}</p>
              </div>
            )}
            {gradingResult.readability_feedback && (
              <div>
                <h4 className="font-semibold mt-4 mb-1">Readability Feedback</h4>
                <p className="text-gray-800 dark:text-gray-200 whitespace-pre-line">{gradingResult.readability_feedback}</p>
              </div>
            )}
          </div>
        )}
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

export default SubmissionPage;