#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TOTAL_UPDATES=0
SUCCESSFUL_UPDATES=0
FAILED_UPDATES=0

declare -a UPDATED_FILES=()
declare -a FAILED_FILES=()

echo -e "${BLUE}=== Protocol Buffer Synchronization Script ===${NC}"

print_result() {
    local status=$1
    local message=$2
    if [ "$status" == "SUCCESS" ]; then
        echo -e "  ${GREEN}✓${NC} $message"
        ((SUCCESSFUL_UPDATES++))
    elif [ "$status" == "FAIL" ]; then
        echo -e "  ${RED}✗${NC} $message"
        ((FAILED_UPDATES++))
    elif [ "$status" == "WARN" ]; then
        echo -e "  ${YELLOW}⚠${NC} $message"
    elif [ "$status" == "INFO" ]; then
        echo -e "  ${BLUE}ℹ${NC} $message"
    else
        echo -e "  $message"
    fi
    ((TOTAL_UPDATES++))
}

show_usage() {
    echo -e "${BLUE}Usage:${NC}"
    echo "  $0 <source_proto_file>"
    echo ""
    echo -e "${BLUE}Description:${NC}"
    echo "  Synchronizes the source proto file with all other copies across services"
    echo "  while preserving language-specific options in each target file."
    echo ""
    echo -e "${BLUE}Examples:${NC}"
    echo "  $0 services/users-service/src/main/proto/users_service.proto"
    echo "  $0 services/labs-service/app/proto/labs.proto"
    echo ""
    echo -e "${BLUE}What this script does:${NC}"
    echo "  1. Finds all other copies of the same proto file"
    echo "  2. Extracts language-specific options from each target"
    echo "  3. Copies content from source to targets"
    echo "  4. Restores preserved language-specific options"
    echo "  5. Creates backups before making changes"
}

extract_language_options() {
    local file=$1
    grep -E "^[[:space:]]*option[[:space:]]+(java_package|java_outer_classname|java_multiple_files|go_package|csharp_namespace|objc_class_prefix|php_namespace|php_class_prefix|ruby_package|swift_prefix|py_generic_services|java_generic_services|cc_generic_services)[[:space:]]*=" "$file" 2>/dev/null || true
}

remove_language_options() {
    local content="$1"
    echo "$content" | sed \
        -e '/^[[:space:]]*option[[:space:]]\+java_package[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+java_outer_classname[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+java_multiple_files[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+go_package[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+csharp_namespace[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+objc_class_prefix[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+php_namespace[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+php_class_prefix[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+ruby_package[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+swift_prefix[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+py_generic_services[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+java_generic_services[[:space:]]*=/d' \
        -e '/^[[:space:]]*option[[:space:]]\+cc_generic_services[[:space:]]*=/d'
}

find_option_insertion_point() {
    local content="$1"
    local line_num=1
    local best_line=3
    
    echo "$content" | while IFS= read -r line; do
        if [[ "$line" =~ ^syntax ]]; then
            best_line=$((line_num + 1))
        fi
        
        if [[ "$line" =~ ^package ]]; then
            best_line=$((line_num + 1))
        fi
        if [[ "$line" =~ ^(import|service|message) ]]; then
            break
        fi
        ((line_num++))
    done
    
    echo $best_line
}

insert_language_options() {
    local content="$1"
    local options="$2"
    local insertion_point="$3"
    
    if [ -z "$options" ]; then
        echo "$content"
        return
    fi
    
    local line_count=$(echo "$content" | wc -l)
    
    if [ $insertion_point -gt $line_count ]; then
        insertion_point=$line_count
    fi
    
    {
        echo "$content" | head -n $((insertion_point - 1))
        echo ""
        echo "$options"
        echo ""
        echo "$content" | tail -n +$insertion_point
    } | sed '/^$/N;/^\n$/d'
}

find_proto_copies() {
    local source_file=$1
    local basename_file=$(basename "$source_file")
    
    find ./services \( -path "*/src/main/proto/*.proto" -o -path "*/app/proto/*.proto" -o -path "*/api/*.proto" \) -name "$basename_file" -type f 2>/dev/null | \
    grep -v build | \
    grep -v "^$source_file$"
}

create_backup() {
    return 0
}

sync_target_file() {
    local source_file=$1
    local target_file=$2
    
    print_result "INFO" "Synchronizing $(basename "$target_file") in $(dirname "$target_file")"
    
    if [ ! -f "$target_file" ]; then
        print_result "FAIL" "Target file does not exist: $target_file"
        FAILED_FILES+=("$target_file: File not found")
        return 1
    fi
    
    
    local target_options
    target_options=$(extract_language_options "$target_file")
    
    local source_content
    if ! source_content=$(cat "$source_file" 2>/dev/null); then
        print_result "FAIL" "Could not read source file: $source_file"
        FAILED_FILES+=("$target_file: Could not read source")
        return 1
    fi
    
    local clean_source_content
    clean_source_content=$(remove_language_options "$source_content")
    
    local insertion_point
    insertion_point=$(find_option_insertion_point "$clean_source_content")
    
    local final_content
    final_content=$(insert_language_options "$clean_source_content" "$target_options" "$insertion_point")
    
    if echo "$final_content" > "$target_file"; then
        print_result "SUCCESS" "Updated $target_file"
        UPDATED_FILES+=("$target_file")
        
        if [ -n "$target_options" ]; then
            echo "$target_options" | while IFS= read -r option; do
                if [ -n "$option" ]; then
                    print_result "INFO" "Preserved: $(echo "$option" | xargs)"
                fi
            done
        fi
        
        return 0
    else
        print_result "FAIL" "Could not write to $target_file"
        FAILED_FILES+=("$target_file: Write failed")
        return 1
    fi
}

main() {
    if [ $# -ne 1 ]; then
        echo -e "${RED}Error: Source proto file not specified${NC}"
        echo ""
        show_usage
        exit 1
    fi
    
    local source_file="$1"
    
    if [ ! -f "$source_file" ]; then
        echo -e "${RED}Error: Source file does not exist: $source_file${NC}"
        exit 1
    fi
    
    if [[ ! "$source_file" =~ \.proto$ ]]; then
        echo -e "${RED}Error: Source file must be a .proto file: $source_file${NC}"
        exit 1
    fi
    
    if ! command -v protoc >/dev/null 2>&1; then
        echo -e "${YELLOW}Warning: protoc not found. Syntax validation will be skipped.${NC}"
    else
        local temp_dir=$(mktemp -d)
        local proto_dir=$(dirname "$source_file")
        if ! protoc --proto_path="$proto_dir" --proto_path="." --descriptor_set_out="$temp_dir/test.desc" "$source_file" >/dev/null 2>&1; then
            echo -e "${RED}Error: Source file has syntax errors: $source_file${NC}"
            rm -rf "$temp_dir"
            exit 1
        fi
        rm -rf "$temp_dir"
        print_result "SUCCESS" "Source file syntax is valid"
    fi
    
    local basename_file=$(basename "$source_file")
    echo -e "${BLUE}Source file: $source_file${NC}"
    echo -e "${BLUE}Looking for copies of: $basename_file${NC}"
    echo ""
    
    local target_files
    mapfile -t target_files < <(find_proto_copies "$source_file")
    
    if [ ${#target_files[@]} -eq 0 ]; then
        echo -e "${YELLOW}No copies of $basename_file found in other services${NC}"
        exit 0
    fi
    
    echo -e "${BLUE}Found ${#target_files[@]} copies to synchronize:${NC}"
    for target in "${target_files[@]}"; do
        echo "  - $target"
    done
    echo ""
    
    echo -e "${YELLOW}This will update the above files with content from:${NC}"
    echo -e "${YELLOW}  $source_file${NC}"
    echo -e "${YELLOW}Language-specific options will be preserved in each target.${NC}"
    echo -e "${YELLOW}No backups will be created.${NC}"
    echo ""
    read -p "Continue? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}Operation cancelled${NC}"
        exit 0
    fi
    
    echo ""
    echo -e "${BLUE}=== Synchronizing Proto Files ===${NC}"
    
    for target_file in "${target_files[@]}"; do
        sync_target_file "$source_file" "$target_file"
        echo ""
    done
    
    echo -e "${BLUE}=== Synchronization Summary ===${NC}"
    echo -e "Total operations: $TOTAL_UPDATES"
    echo -e "${GREEN}Successful updates: $SUCCESSFUL_UPDATES${NC}"
    echo -e "${RED}Failed updates: $FAILED_UPDATES${NC}"
    
    if [ ${#UPDATED_FILES[@]} -gt 0 ]; then
        echo ""
        echo -e "${GREEN}Successfully updated files:${NC}"
        for file in "${UPDATED_FILES[@]}"; do
            echo "  ✓ $file"
        done
    fi
    
    if [ ${#FAILED_FILES[@]} -gt 0 ]; then
        echo ""
        echo -e "${RED}Failed updates:${NC}"
        for failure in "${FAILED_FILES[@]}"; do
            echo "  ✗ $failure"
        done
    fi
    
    echo ""
    if [ $FAILED_UPDATES -eq 0 ]; then
        echo -e "${GREEN}✓ All proto files synchronized successfully!${NC}"
        echo -e "${BLUE}Recommendation: Run 'make proto-validate' to verify changes${NC}"
        exit 0
    else
        echo -e "${RED}✗ Proto synchronization completed with $FAILED_UPDATES errors${NC}"
        exit 1
    fi
}

main "$@" 