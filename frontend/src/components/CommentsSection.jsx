import { useState, useEffect } from "react";
import { commentsAPI } from '../utils/api';

export default function CommentsSection({ contentId, user }) {
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

  const MOCK_COMMENTS = [
    {
      id: 1,
      user_id: 101,
      username: "Alice",
      content: "This is a great starting point for the lab! I had a question about the first step.",
      created_at: new Date(Date.now() - 86400000 * 2).toISOString(),
      replies: [
        {
          id: 3,
          user_id: 102,
          username: "Bob",
          content: "I agree! For the first step, did you try consulting the documentation? It was helpful for me.",
          created_at: new Date(Date.now() - 86400000).toISOString(),
          replies: []
        },
        {
          id: 4,
          user_id: 101,
          username: "Alice",
          content: "Good idea, I'll check that out. Thanks!",
          created_at: new Date(Date.now() - 3600000).toISOString(),
          replies: []
        }
      ]
    },
    {
      id: 2,
      user_id: 103,
      username: "Charlie",
      content: "Has anyone managed to get the final part of the experiment working? I'm a bit stuck.",
      created_at: new Date(Date.now() - 3600000 * 5).toISOString(),
      replies: []
    }
  ];

  const fetchComments = async (pageNum = 1, resetComments = false) => {
    try {
      setLoading(true);
      const data = await commentsAPI.getLabComments(contentId, pageNum);
      
      const newComments = data.comments || [];

      if (resetComments) {
        setComments([...MOCK_COMMENTS, ...newComments]);
      } else {
        setComments(prev => [...prev, ...newComments]);
      }
      
      setTotalComments(data.totalComments);
      setHasMore(comments.length < data.totalComments);
    } catch (error) {
      console.error("Error fetching comments:", error);
      // Fallback to only mock comments on error
      if (comments.length === 0) {
        setComments(MOCK_COMMENTS);
      }
    } finally {
      setLoading(false);
    }
  };

  // Fetch replies for a specific comment
  const fetchReplies = async (commentId) => {
    try {
      const data = await commentsAPI.getCommentReplies(commentId);
      const replies = data.comments || [];
      
      const addReplies = (commentList, parentId, newReplies) => {
        return commentList.map(comment => {
          if (comment.id === parentId) {
            return { ...comment, replies: newReplies };
          }
          if (comment.replies && comment.replies.length > 0) {
            return { ...comment, replies: addReplies(comment.replies, parentId, newReplies) };
          }
          return comment;
        });
      };
      
      setComments(prev => addReplies(prev, commentId, replies));
      setExpandedReplies(prev => new Set([...prev, commentId]));
    } catch (error) {
      console.error("Error fetching replies:", error);
    }
  };

  // Submit a new comment
  const submitComment = async () => {
    if (!newComment.trim() || !user) return;

    setSubmitting(true);
    try {
      const commentData = {
        content: newComment.trim(),
        lab_id: parseInt(contentId),
      };
      
      const createdComment = await commentsAPI.createComment(commentData);
      setComments(prev => [...prev, createdComment]);
      setNewComment("");
    } catch (error) {
      console.error("Error submitting comment:", error);
    } finally {
      setSubmitting(false);
    }
  };

  // Submit a reply to a comment
  const submitReply = async (parentId) => {
    if (!replyText.trim() || !user) return;

    setSubmitting(true);
    try {
      const replyData = {
        content: replyText.trim(),
        parent_id: parentId,
        lab_id: parseInt(contentId),
      };
      
      await commentsAPI.createComment(replyData);
      
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

  useEffect(() => {
    if (contentId) {
      fetchComments();
    }
  }, [contentId]);

  if (!user) {
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
            placeholder='Share your thoughts about this lab...'
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
              <Comment
                key={comment.id}
                comment={comment}
                onReply={setReplyingTo}
                isReplying={replyingTo === comment.id}
                onCancelReply={() => setReplyingTo(null)}
                onSubmitReply={submitReply}
                replyText={replyText}
                onReplyTextChange={setReplyText}
                submittingReply={submitting}
                formatDate={formatDate}
              />
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
                  No comments yet. Be the first to share your thoughts about this lab!
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

function Comment({
  comment,
  onReply,
  isReplying,
  onCancelReply,
  onSubmitReply,
  replyText,
  onReplyTextChange,
  submittingReply,
  formatDate,
}) {
  const [showReplies, setShowReplies] = useState(true);

  return (
    <div className="flex space-x-4">
      <div className="flex-shrink-0">
        <div className="w-10 h-10 bg-msc rounded-full flex items-center justify-center">
          <span className="text-white text-sm font-medium">
            {comment.username?.charAt(0).toUpperCase() || 'U'}
          </span>
        </div>
      </div>
      <div className="flex-1">
        <div className="bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center space-x-3">
              <p className="font-semibold text-gray-900 dark:text-white">
                {comment.username || `User ${comment.user_id}`}
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {formatDate(comment.created_at)}
              </p>
            </div>
          </div>
          <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap mb-3">
            {comment.content}
          </p>
          <button
            onClick={() => onReply(comment.id)}
            className="text-sm font-medium text-msc hover:underline"
          >
            Reply
          </button>
        </div>

        {isReplying && (
          <div className="mt-4 ml-8">
            <textarea
              value={replyText}
              onChange={(e) => onReplyTextChange(e.target.value)}
              placeholder={`Replying to ${comment.username}...`}
              className="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-lg resize-none bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:ring-2 focus:ring-msc"
              rows="3"
            />
            <div className="mt-2 flex justify-end space-x-3">
              <button
                onClick={onCancelReply}
                className="px-4 py-1.5 text-sm rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-600"
              >
                Cancel
              </button>
              <button
                onClick={() => onSubmitReply(comment.id)}
                disabled={!replyText.trim() || submittingReply}
                className="px-4 py-1.5 text-sm rounded-md font-medium text-white bg-msc hover:bg-msc-dark disabled:bg-gray-400"
              >
                {submittingReply ? "Replying..." : "Reply"}
              </button>
            </div>
          </div>
        )}

        {comment.replies && comment.replies.length > 0 && (
          <div className="mt-4 pl-8 border-l-2 border-gray-200 dark:border-gray-600">
            <button
              onClick={() => setShowReplies(!showReplies)}
              className="text-sm font-medium text-msc hover:underline mb-2"
            >
              {showReplies ? "Hide Replies" : `View ${comment.replies.length} Replies`}
            </button>
            {showReplies && (
              <div className="space-y-4">
                {comment.replies.map(reply => (
                  <Comment
                    key={reply.id}
                    comment={reply}
                    onReply={onReply}
                    isReplying={isReplying}
                    onCancelReply={onCancelReply}
                    onSubmitReply={onSubmitReply}
                    replyText={replyText}
                    onReplyTextChange={onReplyTextChange}
                    submittingReply={submittingReply}
                    formatDate={formatDate}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
} 