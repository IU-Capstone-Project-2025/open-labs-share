#!/bin/bash

# validate-all-builds.sh - Comprehensive Build Validation Script
# This script validates that all backend services can build successfully
# preventing cross-service breaking changes from reaching dev/main branches

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Script configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
readonly BUILD_TEMP_DIR="${PROJECT_ROOT}/build-validation-temp"
readonly LOG_DIR="${BUILD_TEMP_DIR}/logs"

# Global counters
TOTAL_SERVICES=0
SUCCESSFUL_BUILDS=0
FAILED_BUILDS=0
SERVICES_WITH_WARNINGS=0

# Arrays to track results
declare -a SUCCESSFUL_SERVICES=()
declare -a FAILED_SERVICES=()
declare -a WARNING_SERVICES=()

# Cleanup function
cleanup() {
    echo -e "\n${CYAN}Cleaning up temporary build directory...${NC}"
    
    # Remove main temp directory
    if [[ -n "${BUILD_TEMP_DIR:-}" && -d "$BUILD_TEMP_DIR" ]]; then
        rm -rf "$BUILD_TEMP_DIR"
    fi
    
    # Clean up any leftover virtual environments in service directories
    if [[ -n "${PROJECT_ROOT:-}" && -d "$PROJECT_ROOT" ]]; then
        find "$PROJECT_ROOT/services" -name "*venv*" -type d 2>/dev/null | while read -r venv_dir; do
            # Only remove clearly temporary venv directories
            if [[ "$(basename "$venv_dir")" == *"venv-"* || "$(basename "$venv_dir")" == "test-venv" ]]; then
                echo -e "${CYAN}ℹ Cleaning up leftover virtual environment: $(basename "$venv_dir")${NC}"
                rm -rf "$venv_dir"
            fi
        done
        
        # Clean up any temporary proto files that might have been generated in service directories
        find "$PROJECT_ROOT/services" -name "*_pb2.py" -o -name "*_pb2_grpc.py" -o -name "*.pb.go" 2>/dev/null | while read -r proto_file; do
            # Only remove if it's clearly a generated temporary file
            local dir_name=$(dirname "$proto_file")
            local file_name=$(basename "$proto_file")
            
            # Clean up Python proto files from proto directories
            if [[ "$dir_name" == *"/proto" ]] && [[ "$file_name" == *"_pb2"* ]] && [[ ! -f "${dir_name}/.keep_generated" ]]; then
                echo -e "${CYAN}ℹ Cleaning up temporary proto file: $file_name${NC}"
                rm -f "$proto_file"
            # Clean up Go proto files from api directories  
            elif [[ "$dir_name" == *"/api" ]] && [[ "$file_name" == *".pb.go" ]] && [[ ! -f "${dir_name}/.keep_generated" ]]; then
                echo -e "${CYAN}ℹ Cleaning up temporary proto file: $file_name${NC}"
                rm -f "$proto_file"
            fi
        done
    fi
    
    # Ensure we're back in the project root
    if [[ -n "${PROJECT_ROOT:-}" && -d "$PROJECT_ROOT" ]]; then
        cd "$PROJECT_ROOT" 2>/dev/null || true
    fi
}

# Set up trap for cleanup - ensure cleanup happens even if script fails or is interrupted
trap cleanup EXIT INT TERM SIGINT SIGTERM

# Utility functions
print_header() {
    echo -e "\n${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_step() {
    echo -e "\n${PURPLE}>>> $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

# Check if required tools are installed
check_prerequisites() {
    print_step "Checking prerequisites..."
    
    local missing_tools=()
    
    # Check Java and Gradle
    if ! command -v java &> /dev/null; then
        missing_tools+=("java")
    fi
    
    if ! command -v gradle &> /dev/null; then
        missing_tools+=("gradle")
    fi
    
    # Check Python
    if ! command -v python3 &> /dev/null && ! command -v python &> /dev/null; then
        missing_tools+=("python3")
    fi
    
    # Check Go
    if ! command -v go &> /dev/null; then
        missing_tools+=("go")
    fi
    
    # Check protoc
    if ! command -v protoc &> /dev/null; then
        missing_tools+=("protoc")
    fi
    
    # Check Docker (optional but recommended)
    if ! command -v docker &> /dev/null; then
        print_warning "Docker not found - container builds will be skipped"
    fi
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        echo -e "\nPlease install the missing tools:"
        echo -e "  Java: https://adoptium.net/"
        echo -e "  Gradle: https://gradle.org/install/"
        echo -e "  Python: https://python.org/downloads/"
        echo -e "  Go: https://golang.org/dl/"
        echo -e "  Protoc: https://grpc.io/docs/protoc-installation/"
        exit 1
    fi
    
    print_success "All prerequisites are available"
}

# Setup build environment
setup_build_environment() {
    print_step "Setting up build environment..."
    
    # Create temporary directories
    mkdir -p "$BUILD_TEMP_DIR"
    mkdir -p "$LOG_DIR"
    
    # Change to project root
    cd "$PROJECT_ROOT"
    
    print_success "Build environment ready"
}

# Validate proto files first
validate_proto_files() {
    print_step "Validating Protocol Buffer files..."
    
    if [[ -f "$SCRIPT_DIR/validate-proto.sh" ]]; then
        if bash "$SCRIPT_DIR/validate-proto.sh" > "$LOG_DIR/proto-validation.log" 2>&1; then
            print_success "Proto file validation passed"
        else
            print_error "Proto file validation failed"
            echo -e "Check log: $LOG_DIR/proto-validation.log"
            cat "$LOG_DIR/proto-validation.log"
            return 1
        fi
    else
        print_warning "Proto validation script not found, skipping"
    fi
}

# Build Java services
build_java_service() {
    local service_name="$1"
    local service_path="$2"
    
    print_step "Building Java service: $service_name"
    
    if [[ ! -d "$service_path" ]]; then
        print_error "Service directory not found: $service_path"
        return 1
    fi
    
    local log_file="$LOG_DIR/$service_name-build.log"
    
    cd "$service_path"
    
    # Clean previous builds
    if gradle clean > "$log_file" 2>&1; then
        print_info "Cleaned previous build for $service_name"
    else
        print_warning "Clean failed for $service_name, continuing anyway"
    fi
    
    # Generate proto files
    if gradle generateProto >> "$log_file" 2>&1; then
        print_info "Generated proto files for $service_name"
    else
        print_error "Proto generation failed for $service_name"
        echo -e "Check log: $log_file"
        cd "$PROJECT_ROOT"
        return 1
    fi
    
    # Download dependencies
    if gradle downloadDependencies --no-daemon >> "$log_file" 2>&1; then
        print_info "Downloaded dependencies for $service_name"
    else
        print_warning "Dependency download had issues for $service_name"
    fi
    
    # Build the project
    if gradle build --no-daemon -x test >> "$log_file" 2>&1; then
        print_success "Build successful for $service_name"
        cd "$PROJECT_ROOT"
        return 0
    else
        print_error "Build failed for $service_name"
        echo -e "Check log: $log_file"
        echo -e "\n${RED}Recent build errors:${NC}"
        tail -20 "$log_file" | grep -E "(error|Error|ERROR|FAILED|Failed)" || echo "No specific errors found in log tail"
        cd "$PROJECT_ROOT"
        return 1
    fi
}

# Build Python services
build_python_service() {
    local service_name="$1"
    local service_path="$2"
    
    print_step "Building Python service: $service_name"
    
    if [[ ! -d "$service_path" ]]; then
        print_error "Service directory not found: $service_path"
        return 1
    fi
    
    local log_file="$LOG_DIR/$service_name-build.log"
    local app_path="$service_path"
    
    # Check for requirements.txt in root first, then app subdirectory
    if [[ -f "$service_path/requirements.txt" ]]; then
        # Use root directory
        app_path="$service_path"
    elif [[ -f "$service_path/app/requirements.txt" ]]; then
        # Use app subdirectory
        app_path="$service_path/app"
    else
        print_error "requirements.txt not found in $service_path or $service_path/app"
        cd "$PROJECT_ROOT"
        return 1
    fi
    
    cd "$app_path"
    
    # Create virtual environment
    local venv_path="$BUILD_TEMP_DIR/venv-$service_name"
    local python_cmd="python3"
    
    # Use python if python3 doesn't work properly (common on Windows)
    if ! python3 --version &> /dev/null && command -v python &> /dev/null; then
        python_cmd="python"
    fi
    
    if $python_cmd -m venv "$venv_path" > "$log_file" 2>&1; then
        print_info "Created virtual environment for $service_name"
    else
        print_error "Failed to create virtual environment for $service_name"
        cd "$PROJECT_ROOT"
        return 1
    fi
    
    # Activate virtual environment and install dependencies
    # Windows uses Scripts, Unix uses bin
    if [[ -f "$venv_path/bin/activate" ]]; then
        source "$venv_path/bin/activate"
    elif [[ -f "$venv_path/Scripts/activate" ]]; then
        source "$venv_path/Scripts/activate"
    else
        print_error "Virtual environment activation script not found for $service_name"
        cd "$PROJECT_ROOT"
        return 1
    fi
    
    if pip install --no-cache-dir -r requirements.txt >> "$log_file" 2>&1; then
        print_info "Installed dependencies for $service_name"
    else
        print_error "Failed to install dependencies for $service_name"
        echo -e "Check log: $log_file"
        deactivate
        cd "$PROJECT_ROOT"
        return 1
    fi
    
    # Generate proto files if proto directory exists
    if [[ -d "proto" ]]; then
        local proto_files=(proto/*.proto)
        if [[ -f "${proto_files[0]}" ]]; then
            for proto_file in "${proto_files[@]}"; do
                local proto_name=$(basename "$proto_file")
                if python -m grpc_tools.protoc -I./proto --python_out=./proto --grpc_python_out=./proto "proto/$proto_name" >> "$log_file" 2>&1; then
                    print_info "Generated proto files from $proto_name"
                else
                    print_error "Failed to generate proto files from $proto_name"
                    deactivate
                    cd "$PROJECT_ROOT"
                    return 1
                fi
            done
        fi
    fi
    
    # Check Python syntax
    if find . -name "*.py" -not -path "./venv/*" -not -path "./$venv_path/*" -exec python -m py_compile {} \; >> "$log_file" 2>&1; then
        print_success "Python syntax check passed for $service_name"
        deactivate
        cd "$PROJECT_ROOT"
        return 0
    else
        print_error "Python syntax check failed for $service_name"
        echo -e "Check log: $log_file"
        deactivate
        cd "$PROJECT_ROOT"
        return 1
    fi
}

# Build Go services
build_go_service() {
    local service_name="$1"
    local service_path="$2"
    
    print_step "Building Go service: $service_name"
    
    if [[ ! -d "$service_path" ]]; then
        print_error "Service directory not found: $service_path"
        return 1
    fi
    
    local log_file="$LOG_DIR/$service_name-build.log"
    
    cd "$service_path"
    
    # Check for go.mod
    if [[ ! -f "go.mod" ]]; then
        print_error "go.mod not found in $service_path"
        cd "$PROJECT_ROOT"
        return 1
    fi
    
    # Download dependencies
    if go mod download > "$log_file" 2>&1; then
        print_info "Downloaded Go dependencies for $service_name"
    else
        print_error "Failed to download Go dependencies for $service_name"
        echo -e "Check log: $log_file"
        cd "$PROJECT_ROOT"
        return 1
    fi
    
    # Generate proto files if needed
    if [[ -d "api" ]] && ls api/*.proto &> /dev/null; then
        # Install protoc plugins if not available
        if ! go list -m google.golang.org/protobuf/cmd/protoc-gen-go &> /dev/null; then
            go install google.golang.org/protobuf/cmd/protoc-gen-go@latest >> "$log_file" 2>&1
        fi
        if ! go list -m google.golang.org/grpc/cmd/protoc-gen-go-grpc &> /dev/null; then
            go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest >> "$log_file" 2>&1
        fi
        
        # Generate proto files
        mkdir -p internal/grpc/proto
        for proto_file in api/*.proto; do
            if protoc --go_out=. --go_opt=paths=source_relative \
                --go-grpc_out=. --go-grpc_opt=paths=source_relative \
                "$proto_file" >> "$log_file" 2>&1; then
                print_info "Generated proto files from $(basename "$proto_file")"
            else
                print_error "Failed to generate proto files from $(basename "$proto_file")"
                cd "$PROJECT_ROOT"
                return 1
            fi
        done
    fi
    
    # Build the service
    if go build -v ./... >> "$log_file" 2>&1; then
        print_success "Build successful for $service_name"
        cd "$PROJECT_ROOT"
        return 0
    else
        print_error "Build failed for $service_name"
        echo -e "Check log: $log_file"
        echo -e "\n${RED}Recent build errors:${NC}"
        tail -20 "$log_file" | grep -E "(error|Error|ERROR)" || echo "No specific errors found in log tail"
        cd "$PROJECT_ROOT"
        return 1
    fi
}

# Build all services
build_all_services() {
    print_header "Building All Backend Services"
    
    # Java services
    local java_services=(
        "auth-service:services/auth-service"
        "users-service:services/users-service"
        "api-gateway:services/api-gateway"
    )
    
    # Python services
    local python_services=(
        "articles-service:services/articles-service"
        "labs-service:services/labs-service"
    )
    
    # Go services
    local go_services=(
        "feedback-service:services/feedback-service"
    )
    
    # Build Java services
    for service_info in "${java_services[@]}"; do
        IFS=':' read -r service_name service_path <<< "$service_info"
        TOTAL_SERVICES=$((TOTAL_SERVICES + 1))
        
        if build_java_service "$service_name" "$service_path"; then
            SUCCESSFUL_BUILDS=$((SUCCESSFUL_BUILDS + 1))
            SUCCESSFUL_SERVICES+=("$service_name")
        else
            FAILED_BUILDS=$((FAILED_BUILDS + 1))
            FAILED_SERVICES+=("$service_name")
        fi
    done
    
    # Build Python services
    for service_info in "${python_services[@]}"; do
        IFS=':' read -r service_name service_path <<< "$service_info"
        TOTAL_SERVICES=$((TOTAL_SERVICES + 1))
        
        if build_python_service "$service_name" "$service_path"; then
            SUCCESSFUL_BUILDS=$((SUCCESSFUL_BUILDS + 1))
            SUCCESSFUL_SERVICES+=("$service_name")
        else
            FAILED_BUILDS=$((FAILED_BUILDS + 1))
            FAILED_SERVICES+=("$service_name")
        fi
    done
    
    # Build Go services
    for service_info in "${go_services[@]}"; do
        IFS=':' read -r service_name service_path <<< "$service_info"
        TOTAL_SERVICES=$((TOTAL_SERVICES + 1))
        
        if build_go_service "$service_name" "$service_path"; then
            SUCCESSFUL_BUILDS=$((SUCCESSFUL_BUILDS + 1))
            SUCCESSFUL_SERVICES+=("$service_name")
        else
            FAILED_BUILDS=$((FAILED_BUILDS + 1))
            FAILED_SERVICES+=("$service_name")
        fi
    done
}

# Print final report
print_final_report() {
    print_header "Build Validation Report"
    
    echo -e "\n${CYAN}Summary:${NC}"
    echo -e "  Total services: $TOTAL_SERVICES"
    echo -e "  Successful builds: ${GREEN}$SUCCESSFUL_BUILDS${NC}"
    echo -e "  Failed builds: ${RED}$FAILED_BUILDS${NC}"
    echo -e "  Services with warnings: ${YELLOW}$SERVICES_WITH_WARNINGS${NC}"
    
    if [[ ${#SUCCESSFUL_SERVICES[@]} -gt 0 ]]; then
        echo -e "\n${GREEN}✓ Successful builds:${NC}"
        for service in "${SUCCESSFUL_SERVICES[@]}"; do
            echo -e "  ${GREEN}✓${NC} $service"
        done
    fi
    
    if [[ ${#FAILED_SERVICES[@]} -gt 0 ]]; then
        echo -e "\n${RED}✗ Failed builds:${NC}"
        for service in "${FAILED_SERVICES[@]}"; do
            echo -e "  ${RED}✗${NC} $service"
        done
        echo -e "\n${YELLOW}Check individual service logs in: $LOG_DIR${NC}"
    fi
    
    if [[ ${#WARNING_SERVICES[@]} -gt 0 ]]; then
        echo -e "\n${YELLOW}⚠ Services with warnings:${NC}"
        for service in "${WARNING_SERVICES[@]}"; do
            echo -e "  ${YELLOW}⚠${NC} $service"
        done
    fi
    
    if [[ $FAILED_BUILDS -eq 0 ]]; then
        echo -e "\n${GREEN}🎉 All services built successfully!${NC}"
        echo -e "${GREEN}✅ Safe to merge/deploy${NC}"
        return 0
    else
        echo -e "\n${RED}❌ Some services failed to build${NC}"
        echo -e "${RED}🚫 DO NOT merge/deploy until issues are resolved${NC}"
        return 1
    fi
}

# Show usage information
show_usage() {
    echo -e "${BLUE}Usage:${NC}"
    echo -e "  $0 [OPTIONS]"
    echo -e ""
    echo -e "${BLUE}Description:${NC}"
    echo -e "  Validates that all backend services (Java, Python, Go) can build successfully."
    echo -e "  This helps prevent cross-service breaking changes from reaching dev/main branches."
    echo -e ""
    echo -e "${BLUE}Options:${NC}"
    echo -e "  -h, --help     Show this help message"
    echo -e "  -v, --verbose  Enable verbose output"
    echo -e ""
    echo -e "${BLUE}Services validated:${NC}"
    echo -e "  Java services: auth-service, users-service, api-gateway"
    echo -e "  Python services: articles-service, labs-service"
    echo -e "  Go services: feedback-service"
    echo -e ""
    echo -e "${BLUE}What this script does:${NC}"
    echo -e "  1. Validates Protocol Buffer files"
    echo -e "  2. Builds each service in isolation"
    echo -e "  3. Generates proto files for each service"
    echo -e "  4. Reports any build failures or warnings"
    echo -e "  5. Provides detailed logs for debugging"
}

# Main function
main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -v|--verbose)
                set -x
                shift
                ;;
            *)
                echo -e "${RED}Unknown option: $1${NC}"
                show_usage
                exit 1
                ;;
        esac
    done
    
    print_header "Backend Services Build Validation"
    print_info "Starting comprehensive build validation for all backend services..."
    
    check_prerequisites
    setup_build_environment
    validate_proto_files
    build_all_services
    
    if print_final_report; then
        exit 0
    else
        exit 1
    fi
}

# Run main function with all arguments
main "$@" 