import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { submissionsAPI, feedbackAPI, labsAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';
import { PaperClipIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline';
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

    const [feedbackContent, setFeedbackContent] = useState('');
    const [files, setFiles] = useState([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const fileInputRef = useRef(null);

    useEffect(() => {
        const fetchSubmission = async () => {
            try {
                setLoading(true);
                const sub = await submissionsAPI.getSubmissionById(submissionId);
                setSubmission(sub.data || sub);

                // Fetch lab data separately if we have labId
                if (sub.data?.labId || sub.labId) {
                    const labId = sub.data?.labId || sub.labId;
                    try {
                        const labData = await labsAPI.getLabById(labId);
                        setLab(labData);
                    } catch (labErr) {
                        console.error('Failed to fetch lab data:', labErr);
                        // Continue without lab data
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
                        <h3 className="text-xl font-semibold">Attachments:</h3>
                        <ul className="list-disc list-inside mt-2">
                            {submission.assets.map(asset => (
                                <li key={asset.assetId}>
                                    <a href="#" className="text-blue-600 hover:underline">{asset.filename} ({Math.round(asset.totalSize / 1024)} KB)</a>
                                </li>
                            ))}
                        </ul>
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
                
                {/* Отображение ошибок */}
                {error && (
                    <div className="mb-4 p-3 bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-100 rounded-md">
                    {error}
                    </div>
                )}

                {/* Поле для текста фидбэка */}
                <div className="mb-4">
                    <textarea
                    value={feedbackContent}
                    onChange={(e) => setFeedbackContent(e.target.value)}
                    className="w-full h-48 p-3 border rounded-md dark:bg-gray-700 dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Provide your detailed feedback here..."
                    />
                </div>

                {/* Поле для загрузки файлов */}
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

                {/* Кнопка отправки */}
                <div className="flex justify-end">
                    <button
                    onClick={handleSubmitFeedback}
                    disabled={isSubmitting}
                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-blue-300 dark:disabled:bg-blue-900 transition-colors flex items-center"
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