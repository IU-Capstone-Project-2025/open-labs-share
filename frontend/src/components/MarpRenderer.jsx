import { useEffect, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import remarkMath from 'remark-math'
import rehypeKatex from 'rehype-katex'
import rehypeHighlight from 'rehype-highlight'
import yaml from 'js-yaml';

const getMinioFileUrl = (labId, filename) => {
  const minioEndpoint = import.meta.env.VITE_MINIO_ENDPOINT || 'http://localhost:9000';
  return `${minioEndpoint}/labs/${labId}/${filename}`;
};

export default function MarpRenderer({ content, labId }) {
  const [slides, setSlides] = useState([])
  const [frontmatter, setFrontmatter] = useState({})
  
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
      let fm = {};
      let contentWithoutFrontmatter = content;
      if (content.trim().startsWith('---')) {
        const match = content.match(/^---[\s\S]*?---/);
        if (match) {
          const fmRaw = match[0].replace(/^---|---$/g, '');
          try {
            fm = yaml.load(fmRaw) || {};
          } catch (e) {
            fm = {};
          }
          contentWithoutFrontmatter = content.replace(/^---[\s\S]*?---\s*/, '');
        }
      }
      setFrontmatter(fm);

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
        slideContents.push(contentWithoutFrontmatter)
      }
      setSlides(slideContents)
    } catch (error) {
      setSlides([content.replace(/^---[\s\S]*?---\s*/, '')])
    }
  }, [content])

  const header = frontmatter.header || null;
  const footer = frontmatter.footer || null;
  const theme = frontmatter.theme || frontmatter.style || null;

  let themeClass = '';
  if (theme) {
    themeClass = `marp-theme-${theme}`;
  }

  function parseBackgroundImage(slideContent) {
    const bgRegex = /^!\[bg([^\]]*)\]\(([^)]+)\)\s*/;
    const match = slideContent.match(bgRegex);
    if (!match) return null;
    const params = match[1].trim();
    const src = match[2].trim();
    let style = {};
    if (params) {
      const parts = params.split(/\s+/);
      for (const part of parts) {
        if (part.startsWith('right:')) {
          style.right = part.replace('right:', '');
          style.backgroundPosition = `right ${style.right} top`;
        } else if (part.startsWith('left:')) {
          style.left = part.replace('left:', '');
          style.backgroundPosition = `left ${style.left} top`;
        } else if (part.startsWith('center:')) {
          style.center = part.replace('center:', '');
          style.backgroundPosition = `center ${style.center}`;
        } else if (part.startsWith('h:')) {
          style.height = part.replace('h:', '');
        } else if (part.startsWith('w:')) {
          style.width = part.replace('w:', '');
        }
      }
    }
    return { src, style, raw: match[0] };
  }

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
          padding: 2.5rem 3.5rem 2.5rem 3.5rem;
          margin: 2.5rem 0;
          box-shadow: 0 4px 8px rgba(0,0,0,0.1);
          border-radius: 12px;
          min-height: 440px;
          display: flex;
          flex-direction: column;
          justify-content: center;
          position: relative;
          overflow: hidden;
        }
        .marp-slide[data-dark-mode] {
          background: #1a1a1a;
          color: white;
        }
        .slide-content {
          width: 100%;
          height: 100%;
          position: relative;
          z-index: 2;
          margin: 0.5rem 0 0.5rem 0;
          padding: 0.5rem 0 0.5rem 0;
        }
        .slide-header {
          position: absolute;
          top: 1.5rem;
          right: 2rem;
          text-align: right;
          font-size: 1.1rem;
          color: #888;
          opacity: 0.85;
          z-index: 3;
        }
        .slide-footer {
          position: absolute;
          bottom: 1.5rem;
          left: 2rem;
          text-align: left;
          font-size: 1.1rem;
          color: #888;
          opacity: 0.85;
          z-index: 3;
        }
        .slide-page-number {
          position: absolute;
          bottom: 1.5rem;
          right: 2rem;
          font-size: 1.1rem;
          color: #888;
          opacity: 0.85;
          z-index: 3;
        }
        .slide-bg-image {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          object-fit: contain;
          z-index: 1;
          pointer-events: none;
          opacity: 0.25;
        }
        /* Example Marp themes (add more as needed) */
        .marp-theme-default {
          background: #fff;
          color: #222;
        }
        .marp-theme-dark {
          background: #222;
          color: #fff;
        }
        .marp-theme-gaia {
          background: #f4f4f4;
          color: #222;
        }
      `}</style>
      {slides.map((slideContent, index) => {
        const bg = parseBackgroundImage(slideContent);
        let slideBody = slideContent;
        let bgImgUrl = null;
        let bgImgStyle = {};
        if (bg) {
          slideBody = slideBody.replace(bg.raw, '');
          if (bg.src.startsWith('http://') || bg.src.startsWith('https://')) {
            bgImgUrl = bg.src;
          } else {
            bgImgUrl = getMinioFileUrl(labId, bg.src.replace(/^\.\//, ''));
          }
          bgImgStyle = {
            ...(bg.style.backgroundPosition ? { objectPosition: bg.style.backgroundPosition } : {}),
            ...(bg.style.height ? { height: bg.style.height.replace(/[^\d%px]/g, '') } : {}),
            ...(bg.style.width ? { width: bg.style.width.replace(/[^\d%px]/g, '') } : {}),
          };
        }
        return (
          <div 
            key={`slide-${index}`}
            className={`marp-slide ${themeClass}`}
          >
            {bgImgUrl && (
              <img
                src={bgImgUrl}
                alt="background"
                className="slide-bg-image"
                style={bgImgStyle}
                aria-hidden="true"
              />
            )}
            {header && (
              <div className="slide-header">
                <ReactMarkdown>{header}</ReactMarkdown>
              </div>
            )}
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
                {slideBody}
              </ReactMarkdown>
            </div>
            {footer && (
              <div className="slide-footer">
                <ReactMarkdown>{footer}</ReactMarkdown>
              </div>
            )}
            <div className="slide-page-number">
              {index + 1} / {slides.length}
            </div>
          </div>
        );
      })}
    </div>
  )
}