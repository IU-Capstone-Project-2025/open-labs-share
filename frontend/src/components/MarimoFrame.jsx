import React from 'react';
import PropTypes from 'prop-types';

const MarimoFrame = ({ src }) => {
  if (!src) {
    return <div>Loading Marimo notebook...</div>;
  }

  return (
    <iframe
      src={src}
      width="100%"
      height="800px"
      style={{ border: '1px solid #ddd', borderRadius: '4px', minHeight: '600px' }}
      title="Marimo Notebook"
      allow="clipboard-write"
    ></iframe>
  );
};

MarimoFrame.propTypes = {
  src: PropTypes.string,
};

export default MarimoFrame;
