import { useState, useRef } from "react";

export default function ArticleReviewModal({ isOpen, onClose, onSubmit }) {
    const [reviewText, setReviewText] = useState("");
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
        onSubmit(reviewText, files);
        setReviewText("");
        setFiles([]);
        onClose();
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex justify-center items-center">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl p-8 max-w-2xl w-full">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">Submit Your Review</h2>

                <div className="space-y-6">
                    <div>
                        <label htmlFor="reviewText" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                            Your Review Text
                        </label>
                        <textarea
                            id="reviewText"
                            rows="4"
                            className="w-full p-2 border border-gray-300 rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                            value={reviewText}
                            onChange={(e) => setReviewText(e.target.value)}
                            placeholder="Enter your review here..."
                        ></textarea>
                    </div>

                    <div
                        onDrop={handleDrop}
                        onDragOver={handleDragOver}
                        onDragLeave={handleDragLeave}
                        onClick={() => fileInputRef.current?.click()}
                        className={`border-2 ${isDragging ? "border-msc" : "border-dashed border-gray-400"} rounded-lg p-8 text-center cursor-pointer transition-colors`}
                    >
                        <input
                            type="file"
                            multiple
                            ref={fileInputRef}
                            onChange={handleFileChange}
                            className="hidden"
                        />
                        <p className="text-lg font-medium text-msc dark:text-white">
                            {isDragging ? "Drop files here" : "Select or drop supporting files"}
                        </p>
                        <p className="text-sm text-light-blue dark:text-gray-400 mt-1">
                            You can upload multiple files (e.g., annotated PDFs, documents).
                        </p>
                    </div>

                    {files.length > 0 && (
                        <div>
                            <h4 className="text-md font-semibold text-gray-800 dark:text-gray-200 mb-2">Selected files:</h4>
                            <ul className="space-y-2">
                                {files.map((file, index) => (
                                    <li key={index} className="flex justify-between items-center bg-gray-100 dark:bg-gray-700 p-2 rounded-md">
                                        <span className="text-sm text-gray-700 dark:text-gray-300">{file.name}</span>
                                        <button onClick={() => removeFile(file)} className="text-red-500 hover:text-red-700 text-sm font-bold">X</button>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>

                <div className="mt-8 flex justify-end space-x-4">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 rounded-md text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-600 hover:bg-gray-300 dark:hover:bg-gray-500 transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        className="px-6 py-2 rounded-md text-white bg-msc hover:bg-msc-hover transition-colors"
                    >
                        Submit Review
                    </button>
                </div>
            </div>
        </div>
    );
} 