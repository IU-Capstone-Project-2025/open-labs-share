# Build stage
FROM golang:1.23-alpine AS builder

WORKDIR /app

# Install dependencies
RUN apk add --no-cache git protobuf protobuf-dev

# Copy go modules
COPY go.mod go.sum ./
RUN go mod download

# Install protoc plugins
RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
RUN go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

# Copy source code
COPY . .

# Generate protobuf files
RUN mkdir -p internal/grpc/proto
RUN protoc --go_out=. --go_opt=paths=source_relative \
    --go-grpc_out=. --go-grpc_opt=paths=source_relative \
    api/feedback.proto

# Build binary
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o feedback-service ./cmd/

# Runtime stage
FROM gcr.io/distroless/static-debian11

WORKDIR /app

# Copy binary from builder
COPY --from=builder /app/feedback-service .

# Copy migrations
COPY --from=builder /app/migrations ./migrations

# Expose gRPC port
EXPOSE 9090

# Run the service
CMD ["./feedback-service"]
