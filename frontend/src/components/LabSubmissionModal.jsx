import { useState, useRef } from "react";

export default function LabSubmissionModal({ isOpen, onClose, onSubmit, status }) {
    const [submissionText, setSubmissionText] = useState("");
    const [files, setFiles] = useState([]);
    const [isDragging, setIsDragging] = useState(false);
    const fileInputRef = useRef(null);

    if (!isOpen) return null;

    const handleFileChange = (e) => {
        if (e.target.files) {
            setFiles(prevFiles => [...prevFiles, ...Array.from(e.target.files)]);
        }
    };

    const handleDrop = (e) => {
        e.preventDefault();
        setIsDragging(false);
        if (e.dataTransfer.files) {
            setFiles(prevFiles => [...prevFiles, ...Array.from(e.dataTransfer.files)]);
        }
    };
    
    const handleDragOver = (e) => {
        e.preventDefault();
        setIsDragging(true);
    };

    const handleDragLeave = (e) => {
        e.preventDefault();
        setIsDragging(false);
    };

    const removeFile = (fileToRemove) => {
        setFiles(files.filter(file => file !== fileToRemove));
    };

    const handleSubmit = () => {
        onSubmit(submissionText, files);
        // Don't close immediately, wait for parent to handle status
        // setSubmissionText("");
        // setFiles([]);
        // onClose();
    };

    const isSubmitting = status.state === 'pending';

    const handleClose = () => {
        if (isSubmitting) return; // Don't close while submitting
        setSubmissionText("");
        setFiles([]);
        onClose(); // Parent component will reset status
    }

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex justify-center items-center p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl p-8 max-w-2xl w-full">
                
                {status.state === 'idle' || status.state === 'pending' ? (
                    <>
                        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">Submit Your Work</h2>

                        <div className="space-y-6">
                            <div>
                                <label htmlFor="submissionText" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                    Your Submission Text
                                </label>
                                <textarea
                                    id="submissionText"
                                    rows="4"
                                    className="w-full p-2 border border-gray-300 rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-white disabled:opacity-50"
                                    value={submissionText}
                                    onChange={(e) => setSubmissionText(e.target.value)}
                                    placeholder="Enter any text for your submission here..."
                                    disabled={isSubmitting}
                                ></textarea>
                            </div>

                            <div
                                onDrop={handleDrop}
                                onDragOver={handleDragOver}
                                onDragLeave={handleDragLeave}
                                onClick={() => !isSubmitting && fileInputRef.current?.click()}
                                className={`border-2 ${isDragging ? "border-msc" : "border-dashed border-gray-400"} rounded-lg p-8 text-center ${!isSubmitting ? 'cursor-pointer' : 'cursor-not-allowed'} transition-colors`}
                            >
                                <input
                                    type="file"
                                    multiple
                                    ref={fileInputRef}
                                    onChange={handleFileChange}
                                    className="hidden"
                                    disabled={isSubmitting}
                                />
                                <p className="text-lg font-medium text-msc dark:text-white">
                                    {isSubmitting ? "Uploading..." : isDragging ? "Drop files here" : "Select or drop files"}
                                </p>
                                <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                                    You can upload multiple files.
                                </p>
                            </div>

                            {files.length > 0 && (
                                <div>
                                    <h4 className="text-md font-semibold text-gray-800 dark:text-gray-200 mb-2">Selected files:</h4>
                                    <ul className="space-y-2 max-h-32 overflow-y-auto">
                                        {files.map((file, index) => (
                                            <li key={index} className="flex justify-between items-center bg-gray-100 dark:bg-gray-700 p-2 rounded-md">
                                                <span className="text-sm text-gray-700 dark:text-gray-300 truncate pr-2">{file.name}</span>
                                                <button 
                                                    onClick={() => !isSubmitting && removeFile(file)} 
                                                    className="text-red-500 hover:text-red-700 text-sm font-bold disabled:opacity-50"
                                                    disabled={isSubmitting}
                                                >
                                                    X
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </div>

                        <div className="mt-8 flex justify-end space-x-4">
                            <button
                                onClick={handleClose}
                                disabled={isSubmitting}
                                className="px-6 py-2 rounded-md text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-600 hover:bg-gray-300 dark:hover:bg-gray-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleSubmit}
                                disabled={isSubmitting || files.length === 0}
                                className="px-6 py-2 rounded-md text-white bg-msc hover:bg-msc-hover transition-colors flex items-center disabled:bg-msc-dark disabled:opacity-70 disabled:cursor-not-allowed"
                            >
                                {isSubmitting && (
                                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                )}
                                {isSubmitting ? 'Submitting...' : 'Submit'}
                            </button>
                        </div>
                    </>
                ) : (
                    <div className="text-center">
                        <h2 className={`text-2xl font-bold mb-4 ${status.state === 'success' ? 'text-green-500' : 'text-red-500'}`}>
                            {status.state === 'success' ? 'Submission Successful' : 'Submission Failed'}
                        </h2>
                        <p className="text-gray-700 dark:text-gray-300 mb-6">{status.message}</p>
                        <button
                            onClick={handleClose}
                            className="px-8 py-3 rounded-md text-white bg-msc hover:bg-msc-hover transition-colors"
                        >
                            Close
                        </button>
                    </div>
                )}


            </div>
        </div>
    );
} 