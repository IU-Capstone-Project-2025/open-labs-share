import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { submissionsAPI, feedbackAPI } from '../utils/api';
import { useUser } from '../hooks/useUser';
import Spinner from '../components/Spinner';

const ReviewSubmissionPage = () => {
    const { submissionId } = useParams();
    const navigate = useNavigate();
    const user = useUser();

    const [submission, setSubmission] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

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
            alert('Feedback content cannot be empty.');
            return;
        }

        setIsSubmitting(true);
        setError(null);

        const formData = new FormData();
        formData.append('submissionId', submission.submissionId);
        formData.append('studentId', submission.owner.id);
        formData.append('content', feedbackContent);
        files.forEach(file => {
            formData.append('files', file);
        });

        try {
            await feedbackAPI.createFeedback(formData);
            alert('Feedback submitted successfully!');
            navigate('/reviews');
        } catch (err) {
            setError('Failed to submit feedback. Please try again.');
            console.error(err);
        } finally {
            setIsSubmitting(false);
        }
    };

    if (loading) return <div className="flex justify-center items-center h-64"><Spinner /></div>;
    if (error) return <div className="text-center text-red-500 mt-8">{error}</div>;
    if (!submission) return <div className="text-center text-gray-500 mt-8">Submission not found.</div>;

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-3xl font-bold mb-4">Reviewing Submission for: {submission.lab?.title || 'Unknown Lab'}</h1>
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

            <form onSubmit={handleSubmitFeedback} className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
                <h2 className="text-2xl font-semibold mb-4">Your Feedback</h2>
                <textarea
                    value={feedbackContent}
                    onChange={(e) => setFeedbackContent(e.target.value)}
                    className="w-full h-48 p-3 border rounded-md dark:bg-gray-700 dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Provide your detailed feedback here..."
                    required
                />
                <div className="mt-4">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Attach Files (optional)</label>
                    <input
                        type="file"
                        multiple
                        ref={fileInputRef}
                        onChange={handleFileChange}
                        className="mt-1 block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                    />
                </div>
                <div className="mt-6">
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full py-3 px-4 bg-blue-600 text-white font-bold rounded-md hover:bg-blue-700 disabled:bg-blue-300 flex justify-center items-center"
                    >
                        {isSubmitting ? <Spinner /> : 'Submit Feedback'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default ReviewSubmissionPage; 