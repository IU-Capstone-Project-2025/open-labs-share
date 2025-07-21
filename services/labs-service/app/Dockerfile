FROM python:3.12-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    postgresql-client \
    dos2unix \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements file
COPY ./requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . /app/

# Create __init__.py files to make directories Python packages
RUN touch proto/__init__.py utils/__init__.py

# Generate protobuf files for all proto files
RUN python -m grpc_tools.protoc -I./proto --python_out=./proto --grpc_python_out=./proto proto/*.proto

# Create files directory for temporary storage
RUN mkdir -p /app/files

# Expose the gRPC port
EXPOSE 9091

# Run the application
ENTRYPOINT ["python", "-u", "main.py"]