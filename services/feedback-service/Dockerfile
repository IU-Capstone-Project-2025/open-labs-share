# ----------- Build Stage -----------
FROM golang:1.24-alpine AS builder

WORKDIR /app

# Install build dependencies
RUN apk add --no-cache git protobuf protobuf-dev

# Copy go.mod and go.sum first for better caching
COPY go.mod go.sum ./
RUN go mod download

# Install protoc plugins
RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.34.1 && \
    go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.3.0

# Copy the entire project
COPY . .

# Generate protobuf code
RUN mkdir -p internal/grpc/proto && \
    protoc --go_out=. --go_opt=paths=source_relative \
           --go-grpc_out=. --go-grpc_opt=paths=source_relative \
           api/feedback_service.proto

# Build the binary
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o feedback-service ./cmd/

# ----------- Runtime Stage -----------
FROM gcr.io/distroless/static-debian11

WORKDIR /app

COPY --from=builder /app/feedback-service .
COPY --from=builder /app/migrations ./migrations

EXPOSE 9090

CMD ["./feedback-service"]
