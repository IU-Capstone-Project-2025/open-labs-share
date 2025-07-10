import { useNavigate } from "react-router-dom";
import LabUpload from "../components/LabUpload";

export default function CreateLabPage() {
  const navigate = useNavigate();

  const handleLabCreated = (result) => {
    console.log('Lab created successfully:', result);
    if (result.lab && result.lab.id) {
      navigate(`/labs/${result.lab.id}`);
    } else {
      navigate('/my-labs');
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <div className="relative min-h-screen dark:bg-gray-900 py-10 px-6 bg-transparent">
      <div className="max-w-4xl mx-auto">
        <div className="relative z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl p-8 shadow-lg">
          <LabUpload 
            onSuccess={handleLabCreated}
            onCancel={handleCancel}
            isModal={false}
          />
        </div>
      </div>
    </div>
  );
}