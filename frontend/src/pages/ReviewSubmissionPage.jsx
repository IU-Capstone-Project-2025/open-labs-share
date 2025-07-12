import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { submissionsAPI, feedbackAPI, labsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { PaperClipIcon, ExclamationCircleIcon, ArrowDownTrayIcon } from '@heroicons/react/24/outline';
import ToastNotification from "../components/ToastNotification";


const ReviewSubmissionPage = () => {
    const { submissionId } = useParams();
    const navigate = useNavigate();
    const user = useUser();

    const [submission, setSubmission] = useState(null);
    const [lab, setLab] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [toast, setToast] = useState({ show: false, message: "", type: "" });
    const [downloadingFiles, setDownloadingFiles] = useState(new Set());

    const [feedbackContent, setFeedbackContent] = useState('');
    const [files, setFiles] = useState([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const fileInputRef = useRef(null);

    const getSubmissionFileUrl = (submissionId, filename) => {
        const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
        return `${minioEndpoint}/submissions/${submissionId}/${filename}`;
    };

    const downloadFile = async (filename) => {
        if (!submissionId || !filename) return;

        try {
            setDownloadingFiles(prev => new Set(prev).add(filename));
            
            const url = getSubmissionFileUrl(submissionId, filename);
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
                // If only one file, download it directly
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
                const sub = await submissionsAPI.getSubmissionById(submissionId);
                setSubmission(sub.data || sub);

                if (sub.data?.labId || sub.labId) {
                    const labId = sub.data?.labId || sub.labId;
                    try {
                        const labData = await labsAPI.getLabById(labId);
                        setLab(labData);
                    } catch (labErr) {
                        console.error('Failed to fetch lab data:', labErr);
                    }
                }
            } catch (err) {
                setError('Failed to fetch submission details.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchSubmission();
    }, [submissionId]);

    const handleFileChange = (e) => {
        setFiles(Array.from(e.target.files));
    };

    const handleSubmitFeedback = async (e) => {
        e.preventDefault();
        if (!feedbackContent.trim() || !submission) {
            setToast({ show: true, message: 'Feedback content cannot be empty.', type: 'error' });
            return;
        }

        setIsSubmitting(true);
        setError(null);

        try {
            const formData = new FormData();
            formData.append('submissionId', submission.submissionId);
            formData.append('studentId', submission.owner.id);
            formData.append('content', feedbackContent);
            
            if (files.length > 0) {
                files.forEach(file => {
                    formData.append('files', file);
                });
            }

            await feedbackAPI.createFeedback(formData);
            
            setToast({ show: true, message: 'Feedback submitted successfully!', type: 'success' });
            setTimeout(() => navigate('/reviews'), 2000);
        } catch (err) {
            console.error('Full error:', err);
            const errorMsg = err.response?.data?.message || 
                            err.message || 
                            'Failed to submit feedback. Please try again.';
            setToast({ show: true, message: errorMsg, type: 'error' });
        } finally {
            setIsSubmitting(false);
        }
    };

    if (loading) return <div className="flex justify-center items-center h-64"><Spinner /></div>;
    if (error) return <div className="text-center text-red-500 mt-8">{error}</div>;
    if (!submission) return <div className="text-center text-gray-500 mt-8">Submission not found.</div>;
    const getLabTitle = () => {
        if (submission.lab?.title) return submission.lab.title;
        if (submission.labTitle) return submission.labTitle;
        if (submission.labId) return `Lab #${submission.labId}`;
        return 'Unknown Lab';
    };

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-3xl font-bold mb-4">Reviewing Submission for: {lab?.title || `Lab #${submission?.labId}` || 'Unknown Lab'}</h1>
            <p className="text-lg mb-6">Submitted by: {submission.owner.name} {submission.owner.surname} ({submission.owner.username})</p>
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-8">
                <h2 className="text-2xl font-semibold mb-4">Submission Details</h2>
                <div className="prose dark:prose-invert max-w-none">
                    <p>{submission.text}</p>
                </div>
                {submission.assets && submission.assets.length > 0 && (
                    <div className="mt-6">
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-xl font-semibold">Attachments ({submission.assets.length}):</h3>
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
            {toast.show && (
                <ToastNotification 
                    message={toast.message}
                    type={toast.type}
                    onClose={() => setToast({ show: false, message: "", type: "" })}
                />
            )}

            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
                <h2 className="text-2xl font-semibold mb-4">Your Feedback</h2>
                
                {error && (
                    <div className="mb-4 p-3 bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-100 rounded-md">
                    {error}
                    </div>
                )}

                <div className="mb-4">
                    <textarea
                    value={feedbackContent}
                    onChange={(e) => setFeedbackContent(e.target.value)}
                    className="w-full h-48 p-3 border rounded-md dark:bg-gray-700 dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-msc"
                    placeholder="Provide your detailed feedback here..."
                    />
                </div>

                <div className="mb-6">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Attach Files (optional)
                    </label>
                    <div className="flex items-center">
                    <input
                        type="file"
                        multiple
                        ref={fileInputRef}
                        onChange={handleFileChange}
                        className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 dark:file:bg-gray-600 dark:file:text-gray-200"
                    />
                    </div>
                </div>

                <div className="flex justify-end">
                    <button
                    onClick={handleSubmitFeedback}
                    disabled={isSubmitting}
                    className="px-4 py-2 bg-msc text-white rounded-md hover:bg-msc-hover disabled:bg-blue-300 dark:disabled:bg-blue-900 transition-colors flex items-center"
                    >
                    {isSubmitting ? (
                        <>
                        <Spinner className="mr-2" />
                        Submitting...
                        </>
                    ) : (
                        'Submit Feedback'
                    )}
                    </button>
                </div>
                </div>
        </div>
    );
};

export default ReviewSubmissionPage;