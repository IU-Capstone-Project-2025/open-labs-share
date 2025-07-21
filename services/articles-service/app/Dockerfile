FROM python:3.12-slim

WORKDIR /app

# Install dependencies
COPY ./requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create __init__.py files to make directories Python packages
RUN touch proto/__init__.py utils/__init__.py

# Generate protobuf files
RUN python -m grpc_tools.protoc -I./proto --python_out=./proto --grpc_python_out=./proto proto/articles_service.proto

# Create files directory for tmp files
RUN mkdir -p /app/files && chmod 777 /app/files

# Expose gRPC port
EXPOSE 50051

# Run the gRPC service
CMD ["python", "-u", "main.py"] 