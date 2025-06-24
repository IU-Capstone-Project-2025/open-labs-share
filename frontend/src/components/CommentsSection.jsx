import { useState, useEffect } from "react";
import { commentsAPI } from "../utils/api";

export default function CommentsSection({ contentType, contentId, userId }) {
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [expandedReplies, setExpandedReplies] = useState(new Set());
  const [page, setPage] = useState(1);
  const [totalComments, setTotalComments] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  // Validate props
  if (!['lab', 'article'].includes(contentType)) {
    console.error('CommentsSection: contentType must be "lab" or "article"');
    return null;
  }

  // Fetch comments for the content
  const fetchComments = async (pageNum = 1, resetComments = false) => {
    try {
      setLoading(pageNum === 1);
      const data = await commentsAPI.getContentComments(contentType, contentId, pageNum, 20);
      const newComments = data.comments || [];
      
      if (resetComments || pageNum === 1) {
        setComments(newComments);
      } else {
        setComments(prev => [...prev, ...newComments]);
      }
      
      setTotalComments(data.total_count || 0);
      setHasMore(newComments.length === 20);
    } catch (error) {
      console.error("Error fetching comments:", error);
    } finally {
      setLoading(false);
    }
  };

  // Fetch replies for a specific comment
  const fetchReplies = async (commentId) => {
    try {
      const data = await commentsAPI.getCommentReplies(commentId, 1, 50);
      const replies = data.comments || [];
      
      // Update the comments state to include replies
      setComments(prev => prev.map(comment => 
        comment.id === commentId 
          ? { ...comment, replies: replies }
          : comment
      ));
      
      // Expand replies for this comment
      setExpandedReplies(prev => new Set([...prev, commentId]));
    } catch (error) {
      console.error("Error fetching replies:", error);
    }
  };

  // Submit a new comment
  const submitComment = async () => {
    if (!newComment.trim() || !userId) return;

    setSubmitting(true);
    try {
      const commentData = {
        user_id: parseInt(userId),
        content: newComment.trim(),
      };

      // Add the appropriate content ID field based on type
      if (contentType === 'lab') {
        commentData.lab_id = parseInt(contentId);
      } else if (contentType === 'article') {
        commentData.article_id = parseInt(contentId);
      }

      await commentsAPI.createComment(commentData);

      setNewComment("");
      fetchComments(1, true); // Refresh comments
    } catch (error) {
      console.error("Error submitting comment:", error);
    } finally {
      setSubmitting(false);
    }
  };

  // Submit a reply to a comment
  const submitReply = async (parentId) => {
    if (!replyText.trim() || !userId) return;

    setSubmitting(true);
    try {
      const commentData = {
        user_id: parseInt(userId),
        parent_id: parentId,
        content: replyText.trim(),
      };

      // Add the appropriate content ID field based on type
      if (contentType === 'lab') {
        commentData.lab_id = parseInt(contentId);
      } else if (contentType === 'article') {
        commentData.article_id = parseInt(contentId);
      }

      await commentsAPI.createComment(commentData);

      setReplyText("");
      setReplyingTo(null);
      fetchReplies(parentId); // Refresh replies for this comment
    } catch (error) {
      console.error("Error submitting reply:", error);
    } finally {
      setSubmitting(false);
    }
  };

  // Load more comments
  const loadMoreComments = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    fetchComments(nextPage, false);
  };

  // Toggle replies visibility
  const toggleReplies = (commentId) => {
    if (expandedReplies.has(commentId)) {
      setExpandedReplies(prev => {
        const newSet = new Set(prev);
        newSet.delete(commentId);
        return newSet;
      });
    } else {
      fetchReplies(commentId);
    }
  };

  // Format date
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString() + " " + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
  };

  // Get appropriate labels based on content type
  const getLabels = () => {
    if (contentType === 'lab') {
      return {
        title: 'Lab Comments',
        placeholder: 'Share your thoughts about this lab...',
        context: 'lab'
      };
    } else if (contentType === 'article') {
      return {
        title: 'Article Discussion',
        placeholder: 'Share your thoughts about this article...',
        context: 'article'
      };
    }
  };

  const labels = getLabels();

  useEffect(() => {
    if (contentId) {
      fetchComments();
    }
  }, [contentId, contentType]);

  if (!userId) {
    return (
      <div className="mt-12 p-6 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg">
        <p className="text-yellow-800 dark:text-yellow-200">
          Please sign in to view and post comments.
        </p>
      </div>
    );
  }

  return (
    <section>
      <div>
        {/* Comment Form */}
        <div className="mb-8">
          <textarea
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder={labels.placeholder}
            className="w-full p-4 border border-gray-300 dark:border-gray-600 rounded-lg resize-none bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:ring-2 focus:ring-msc focus:border-transparent"
            rows="4"
          />
          <div className="mt-3 flex justify-end">
            <button
              onClick={submitComment}
              disabled={!newComment.trim() || submitting}
              className={`px-6 py-2 rounded-md font-medium transition-colors ${
                newComment.trim() && !submitting
                  ? "bg-msc text-white hover:bg-msc-dark"
                  : "bg-gray-300 dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed"
              }`}
            >
              {submitting ? "Posting..." : "Post Comment"}
            </button>
          </div>
        </div>

        {/* Comments List */}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-msc"></div>
          </div>
        ) : (
          <div className="space-y-6">
            {comments.map((comment) => (
              <div key={comment.id} className="bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg p-6">
                {/* Comment Header */}
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-3">
                    <div className="w-8 h-8 bg-msc rounded-full flex items-center justify-center">
                      <span className="text-white text-sm font-medium">
                        {comment.user_id?.toString().charAt(0) || 'U'}
                      </span>
                    </div>
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white">
                        User {comment.user_id}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {formatDate(comment.created_at)}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Comment Content */}
                <div className="mb-4">
                  <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                    {comment.content}
                  </p>
                </div>

                {/* Comment Actions */}
                <div className="flex items-center space-x-4 text-sm">
                  <button
                    onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)}
                    className="text-msc hover:text-msc-dark font-medium"
                  >
                    Reply
                  </button>
                  <button
                    onClick={() => toggleReplies(comment.id)}
                    className="text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300"
                  >
                    {expandedReplies.has(comment.id) ? "Hide replies" : "Show replies"}
                  </button>
                </div>

                {/* Reply Form */}
                {replyingTo === comment.id && (
                  <div className="mt-4 pl-4 border-l-2 border-gray-200 dark:border-gray-600">
                    <textarea
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      placeholder="Write a reply..."
                      className="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-md resize-none bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:ring-2 focus:ring-msc focus:border-transparent"
                      rows="3"
                    />
                    <div className="mt-2 flex justify-end space-x-2">
                      <button
                        onClick={() => {
                          setReplyingTo(null);
                          setReplyText("");
                        }}
                        className="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => submitReply(comment.id)}
                        disabled={!replyText.trim() || submitting}
                        className={`px-4 py-2 text-sm rounded-md font-medium transition-colors ${
                          replyText.trim() && !submitting
                            ? "bg-msc text-white hover:bg-msc-dark"
                            : "bg-gray-300 dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed"
                        }`}
                      >
                        {submitting ? "Posting..." : "Reply"}
                      </button>
                    </div>
                  </div>
                )}

                {/* Replies */}
                {expandedReplies.has(comment.id) && comment.replies && (
                  <div className="mt-4 pl-4 border-l-2 border-gray-200 dark:border-gray-600 space-y-4">
                    {comment.replies.map((reply) => (
                      <div key={reply.id} className="bg-white dark:bg-gray-800 rounded-lg p-4">
                        <div className="flex items-center space-x-3 mb-2">
                          <div className="w-6 h-6 bg-msc rounded-full flex items-center justify-center">
                            <span className="text-white text-xs font-medium">
                              {reply.user_id?.toString().charAt(0) || 'U'}
                            </span>
                          </div>
                          <div>
                            <p className="text-sm font-medium text-gray-900 dark:text-white">
                              User {reply.user_id}
                            </p>
                            <p className="text-xs text-gray-500 dark:text-gray-400">
                              {formatDate(reply.created_at)}
                            </p>
                          </div>
                        </div>
                        <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                          {reply.content}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {/* Load More Button */}
            {hasMore && (
              <div className="flex justify-center pt-4">
                <button
                  onClick={loadMoreComments}
                  className="px-6 py-2 text-msc border border-msc rounded-md hover:bg-msc hover:text-white transition-colors"
                >
                  Load More Comments
                </button>
              </div>
            )}

            {comments.length === 0 && !loading && (
              <div className="text-center py-8">
                <p className="text-gray-500 dark:text-gray-400">
                  No comments yet. Be the first to share your thoughts about this {labels.context}!
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
} 