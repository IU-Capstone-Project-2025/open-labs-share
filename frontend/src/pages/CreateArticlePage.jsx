import { useNavigate } from "react-router-dom";
import ArticleUpload from "../components/ArticleUpload";

export default function CreateArticlePage() {
  const navigate = useNavigate();

  const handleArticleCreated = (result) => {
    console.log('Article created successfully:', result);
    if (result.id) {
      navigate('/my-articles');
    } else {
      navigate('/my-articles');
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="max-w-4xl mx-auto">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
          <ArticleUpload
            onSuccess={handleArticleCreated}
            onCancel={handleCancel}
            isModal={false}
          />
        </div>
      </div>
    </div>
  );
} 