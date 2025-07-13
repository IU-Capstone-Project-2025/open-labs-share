import { useEffect, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import remarkMath from 'remark-math'
import rehypeKatex from 'rehype-katex'
import rehypeHighlight from 'rehype-highlight'

const getMinioFileUrl = (labId, filename) => {
  const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
  return `${minioEndpoint}/labs/${labId}/${filename}`;
};

export default function MarpRenderer({ content, labId }) {
  const [slides, setSlides] = useState([])
  
  const ImageRenderer = ({ src, alt, ...props }) => {
    const [imgUrl, setImgUrl] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
      const checkImage = async () => {
        if (src.startsWith('http://') || src.startsWith('https://')) {
          setImgUrl(src);
          setLoading(false);
          return;
        }

        try {
          const url = getMinioFileUrl(labId, src);
          const response = await fetch(url, { method: 'HEAD' });
          
          if (response.ok) {
            setImgUrl(url);
          } else {
            setError(true);
          }
        } catch (err) {
          setError(true);
        } finally {
          setLoading(false);
        }
      };

      checkImage();
    }, [labId, src]);

    if (loading) {
      return (
        <div className="max-w-full h-32 bg-gray-200 dark:bg-gray-700 rounded-lg flex items-center justify-center my-4">
          <span className="text-gray-500 dark:text-gray-400">Loading image...</span>
        </div>
      );
    }

    if (error) {
      return (
        <div className="max-w-full h-32 bg-gray-200 dark:bg-gray-700 rounded-lg flex items-center justify-center my-4">
          <span className="text-gray-500 dark:text-gray-400">Image not found: {src}</span>
        </div>
      );
    }

    return (
      <img 
        src={imgUrl}
        alt={alt}
        {...props}
        className="max-w-full h-auto rounded-lg shadow-md my-4"
        onError={() => setError(true)}
      />
    );
  };
  
  useEffect(() => {
    try {
      console.log('MarpRenderer received content:', content.substring(0, 200) + '...')
      const lines = content.split('\n')
      const slideContents = []
      let currentSlide = []
      let inSlide = false
      let inFrontmatter = false
      let frontmatterEnded = false
      
      if (lines[0]?.trim() === '---') {
        inFrontmatter = true
      } else {
        frontmatterEnded = true 
      }
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i]
        
        if (i === 0 && line.trim() === '---') {
          inFrontmatter = true
          continue
        }
        
        if (inFrontmatter && line.trim() === '---') {
          inFrontmatter = false
          frontmatterEnded = true
          continue
        }
        
        if (inFrontmatter) {
          continue
        }
        
        if (frontmatterEnded && line.trim() === '---') {
          if (currentSlide.length > 0) {
            slideContents.push(currentSlide.join('\n'))
            currentSlide = []
          }
          inSlide = true
          continue
        }
        
        if (frontmatterEnded) {
          currentSlide.push(line)
        }
      }
      
      if (currentSlide.length > 0) {
        slideContents.push(currentSlide.join('\n'))
      }
      
      if (slideContents.length === 0) {
        const contentWithoutFrontmatter = content.replace(/^---[\s\S]*?---\s*/, '')
        slideContents.push(contentWithoutFrontmatter)
      }
      
      console.log('MarpRenderer created slides:', slideContents.length, slideContents.map(s => s.substring(0, 50) + '...'))
      setSlides(slideContents)
    } catch (error) {
      console.error('Error parsing Marp content:', error)
      const contentWithoutFrontmatter = content.replace(/^---[\s\S]*?---\s*/, '')
      setSlides([contentWithoutFrontmatter])
    }
  }, [content])

  return (
    <div className="marp-container">
      <style>{`
        .marp-container {
          width: 100%;
          max-width: 1280px;
          margin: 0 auto;
        }
        .marp-slide {
          background: white;
          color: black;
          padding: 2rem;
          margin: 1rem 0;
          box-shadow: 0 4px 8px rgba(0,0,0,0.1);
          border-radius: 8px;
          min-height: 400px;
          display: flex;
          flex-direction: column;
          justify-content: center;
        }
        .marp-slide[data-dark-mode] {
          background: #1a1a1a;
          color: white;
        }
        .slide-content {
          width: 100%;
          height: 100%;
        }
        .slide-content h1 {
          font-size: 2.5rem;
          margin-bottom: 1rem;
        }
        .slide-content h2 {
          font-size: 2rem;
          margin-bottom: 0.8rem;
        }
        .slide-content h3 {
          font-size: 1.5rem;
          margin-bottom: 0.6rem;
        }
        .slide-content p {
          font-size: 1.1rem;
          line-height: 1.6;
          margin-bottom: 0.5rem;
        }
        .slide-content ul, .slide-content ol {
          font-size: 1.1rem;
          line-height: 1.6;
        }
        .slide-content li {
          margin-bottom: 0.3rem;
        }
      `}</style>
      
      {slides.map((slideContent, index) => (
        <div 
          key={`slide-${index}`}
          className="marp-slide"
        >
          <div className="slide-content">
            <ReactMarkdown
              remarkPlugins={[
                remarkGfm,
                remarkMath
              ]}
              rehypePlugins={[
                rehypeKatex,
                rehypeHighlight
              ]}
              components={{
                h1: ({ node, ...props }) => (
                  <h1 {...props} className="text-4xl font-bold mb-4" />
                ),
                h2: ({ node, ...props }) => (
                  <h2 {...props} className="text-3xl font-bold mb-3" />
                ),
                h3: ({ node, ...props }) => (
                  <h3 {...props} className="text-2xl font-bold mb-2" />
                ),
                p: ({ node, ...props }) => (
                  <p {...props} className="text-lg leading-relaxed mb-3" />
                ),
                ul: ({ node, ...props }) => (
                  <ul {...props} className="list-disc pl-6 mb-3 space-y-1" />
                ),
                ol: ({ node, ...props }) => (
                  <ol {...props} className="list-decimal pl-6 mb-3 space-y-1" />
                ),
                li: ({ node, ...props }) => (
                  <li {...props} className="text-lg" />
                ),
                img: ImageRenderer,
                pre: ({ node, ...props }) => (
                  <pre {...props} className="bg-gray-100 dark:bg-gray-800 rounded-lg p-4 overflow-x-auto my-4" />
                ),
                code: ({ node, className, children, ...props }) => {
                  const match = /language-(\w+)/.exec(className || "");
                  const isInline = !match;

                  return isInline ? (
                    <code
                      className="bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded text-sm"
                      {...props}
                    >
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
              {slideContent}
            </ReactMarkdown>
          </div>
        </div>
      ))}
    </div>
  )
}