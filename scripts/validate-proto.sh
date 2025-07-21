#!/bin/bash


RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

declare -a FAILED_SYNTAX=()
declare -a FAILED_NAMING=()
declare -a FAILED_SYNC=()

echo -e "${BLUE}=== Protocol Buffer Validation Script ===${NC}"
echo -e "${BLUE}Validating proto files across the project...${NC}"
echo ""

print_result() {
    local status=$1
    local message=$2
    if [ "$status" == "PASS" ]; then
        echo -e "  ${GREEN}✓${NC} $message"
        ((PASSED_CHECKS++))
    elif [ "$status" == "FAIL" ]; then
        echo -e "  ${RED}✗${NC} $message"
        ((FAILED_CHECKS++))
    elif [ "$status" == "WARN" ]; then
        echo -e "  ${YELLOW}⚠${NC} $message"
    else
        echo -e "  ${BLUE}ℹ${NC} $message"
    fi
    ((TOTAL_CHECKS++))
}

check_protoc() {
    if ! command -v protoc &> /dev/null; then
        echo -e "${RED}Error: protoc is not installed or not in PATH${NC}"
        echo "Please install Protocol Buffer Compiler (protoc)"
        echo "Ubuntu/Debian: sudo apt-get install protobuf-compiler"
        echo "macOS: brew install protobuf"
        echo "Windows: Download from https://github.com/protocolbuffers/protobuf/releases"
        exit 1
    fi
    echo -e "${GREEN}✓ protoc found: $(protoc --version)${NC}"
    echo ""
}

normalize_proto() {
    local file=$1
    sed -e 's|//.*$||g' \
        -e '/\/\*/,/\*\//d' \
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
        -e '/^[[:space:]]*option[[:space:]]\+cc_generic_services[[:space:]]*=/d' \
        -e 's/[[:space:]]\+/ /g' \
        -e 's/^[[:space:]]*//g' \
        -e 's/[[:space:]]*$//g' \
        -e '/^$/d' "$file" | \
    sort
}

validate_syntax() {
    local proto_file=$1
    local proto_dir=$(dirname "$proto_file")
    
    local temp_dir=$(mktemp -d)
    
    if protoc --proto_path="$proto_dir" --proto_path="." --descriptor_set_out="$temp_dir/temp.desc" "$proto_file" 2>/dev/null; then
        print_result "PASS" "Syntax validation: $(basename "$proto_file")"
        rm -rf "$temp_dir"
        return 0
    else
        print_result "FAIL" "Syntax validation: $(basename "$proto_file")"
        FAILED_SYNTAX+=("$proto_file")
        rm -rf "$temp_dir"
        return 1
    fi
}

validate_naming() {
    local proto_file=$1
    local filename=$(basename "$proto_file")
    local failed=0
    
    if [[ ! "$filename" =~ ^[a-z][a-z0-9_]*_service\.proto$ ]]; then
        print_result "FAIL" "File naming: $filename (should be snake_case_service.proto)"
        FAILED_NAMING+=("$proto_file: Invalid filename format - must end with '_service.proto'")
        ((failed++))
    fi
    
    local temp_file=$(mktemp)
    sed -e 's|//.*$||g' -e 's|/\*.*\*/||g' -e '/\/\*/,/\*\//d' "$proto_file" > "$temp_file"
    
    local line_num
    
    while IFS=: read -r line_num line; do
        if [[ "$line" =~ ^[[:space:]]*service[[:space:]]+([A-Za-z][A-Za-z0-9_]*)[[:space:]]*\{[[:space:]]*$ ]]; then
            local service_name="${BASH_REMATCH[1]}"
            if [[ ! "$service_name" =~ ^[A-Z][A-Za-z0-9]*$ ]]; then
                print_result "FAIL" "Service naming: $service_name at line $line_num (should be PascalCase)"
                FAILED_NAMING+=("$proto_file:$line_num: Invalid service name '$service_name'")
                ((failed++))
            fi
        fi
    done < <(grep -n "^[[:space:]]*service[[:space:]]" "$temp_file" 2>/dev/null || true)
    
    while IFS=: read -r line_num line; do
        if [[ "$line" =~ ^[[:space:]]*message[[:space:]]+([A-Za-z][A-Za-z0-9_]*)[[:space:]]*\{[[:space:]]*$ ]]; then
            local message_name="${BASH_REMATCH[1]}"
            if [[ ! "$message_name" =~ ^[A-Z][A-Za-z0-9]*$ ]]; then
                print_result "FAIL" "Message naming: $message_name at line $line_num (should be PascalCase)"
                FAILED_NAMING+=("$proto_file:$line_num: Invalid message name '$message_name'")
                ((failed++))
            fi
        fi
    done < <(grep -n "^[[:space:]]*message[[:space:]]" "$temp_file" 2>/dev/null || true)
    
    while IFS=: read -r line_num line; do
        if [[ "$line" =~ ^[[:space:]]*enum[[:space:]]+([A-Za-z][A-Za-z0-9_]*)[[:space:]]*\{[[:space:]]*$ ]]; then
            local enum_name="${BASH_REMATCH[1]}"
            if [[ ! "$enum_name" =~ ^[A-Z][A-Za-z0-9]*$ ]]; then
                print_result "FAIL" "Enum naming: $enum_name at line $line_num (should be PascalCase)"
                FAILED_NAMING+=("$proto_file:$line_num: Invalid enum name '$enum_name'")
                ((failed++))
            fi
        fi
    done < <(grep -n "^[[:space:]]*enum[[:space:]]" "$temp_file" 2>/dev/null || true)
    
    while IFS=: read -r line_num line; do
        if [[ "$line" =~ ^[[:space:]]*rpc[[:space:]]+([A-Za-z][A-Za-z0-9_]*)[[:space:]]*\([[:space:]]*[A-Za-z] ]]; then
            local rpc_name="${BASH_REMATCH[1]}"
            if [[ ! "$rpc_name" =~ ^[A-Z][A-Za-z0-9]*$ ]]; then
                print_result "FAIL" "RPC naming: $rpc_name at line $line_num (should be PascalCase)"
                FAILED_NAMING+=("$proto_file:$line_num: Invalid RPC name '$rpc_name'")
                ((failed++))
            fi
        fi
    done < <(grep -n "^[[:space:]]*rpc[[:space:]]" "$temp_file" 2>/dev/null || true)
    
    while IFS=: read -r line_num line; do
        if [[ "$line" =~ ^[[:space:]]*([a-zA-Z_][a-zA-Z0-9_]*|repeated[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*|optional[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*=[[:space:]]*[0-9]+[[:space:]]*\;[[:space:]]*$ ]]; then
            local field_name="${BASH_REMATCH[2]}"
            if [[ ! "$field_name" =~ ^[a-z][a-z0-9_]*$ ]]; then
                print_result "FAIL" "Field naming: $field_name at line $line_num (should be snake_case)"
                FAILED_NAMING+=("$proto_file:$line_num: Invalid field name '$field_name'")
                ((failed++))
            fi
        fi
    done < <(grep -n "^[[:space:]]*\(repeated\|optional\|[a-zA-Z_]\)[a-zA-Z0-9_ ]*=" "$temp_file" 2>/dev/null || true)
    
    rm -f "$temp_file"
    
    if [ $failed -eq 0 ]; then
        print_result "PASS" "Naming conventions: $(basename "$proto_file")"
        return 0
    else
        return 1
    fi
}

find_proto_duplicates() {
    declare -A proto_files
    
    while IFS= read -r -d '' file; do
        local basename=$(basename "$file")
        if [[ -n "${proto_files[$basename]}" ]]; then
            proto_files[$basename]="${proto_files[$basename]}|$file"
        else
            proto_files[$basename]="$file"
        fi
    done < <(find ./services \( -path "*/src/main/proto/*.proto" -o -path "*/app/proto/*.proto" -o -path "*/api/*.proto" \) -type f -print0 2>/dev/null | grep -zv build)
    
    for basename in "${!proto_files[@]}"; do
        local files=(${proto_files[$basename]//|/ })
        if [ ${#files[@]} -gt 1 ]; then
            echo -e "${YELLOW}Found duplicate proto files for '$basename':${NC}"
            for file in "${files[@]}"; do
                echo "  - $file"
            done
            
            compare_proto_files "${files[@]}"
            echo ""
        fi
    done
}

compare_proto_files() {
    local files=("$@")
    local temp_dir=$(mktemp -d)
    local base_name=$(basename "${files[0]}")
    
    echo -e "${BLUE}Comparing semantic equivalence for $base_name:${NC}"
    
    local normalized_files=()
    for i in "${!files[@]}"; do
        local normalized_file="$temp_dir/normalized_$i.proto"
        normalize_proto "${files[$i]}" > "$normalized_file"
        normalized_files+=("$normalized_file")
    done
    
    local all_match=true
    for i in $(seq 1 $((${#normalized_files[@]} - 1))); do
        if ! diff -q "${normalized_files[0]}" "${normalized_files[$i]}" > /dev/null; then
            all_match=false
            print_result "FAIL" "Files differ: ${files[0]} vs ${files[$i]}"
            FAILED_SYNC+=("${files[0]} and ${files[$i]} are not semantically equivalent")
            
            echo -e "${RED}Differences found:${NC}"
            diff -u "${normalized_files[0]}" "${normalized_files[$i]}" | head -20 || true
            echo ""
        fi
    done
    
    if [ "$all_match" = true ]; then
        print_result "PASS" "All copies of $base_name are semantically equivalent"
    fi
    
    rm -rf "$temp_dir"
}

main() {
    check_protoc
    
    mapfile -d $'\0' proto_files < <(find ./services \( -path "*/src/main/proto/*.proto" -o -path "*/app/proto/*.proto" -o -path "*/api/*.proto" \) -type f -print0 2>/dev/null | grep -zv build)
    
    if [ ${#proto_files[@]} -eq 0 ]; then
        echo -e "${YELLOW}No proto files found in the services directory${NC}"
        exit 0
    fi
    
    echo -e "${BLUE}Found ${#proto_files[@]} proto files${NC}"
    echo ""
    
    echo -e "${BLUE}=== Step 1: Syntax Validation ===${NC}"
    for proto_file in "${proto_files[@]}"; do
        validate_syntax "$proto_file"
    done
    echo ""
    
    echo -e "${BLUE}=== Step 2: Naming Convention Validation ===${NC}"
    for proto_file in "${proto_files[@]}"; do
        validate_naming "$proto_file"
    done
    echo ""
    
    echo -e "${BLUE}=== Step 3: Semantic Equivalence Validation ===${NC}"
    find_proto_duplicates
    
    echo -e "${BLUE}=== Validation Summary ===${NC}"
    echo -e "Total checks: $TOTAL_CHECKS"
    echo -e "${GREEN}Passed: $PASSED_CHECKS${NC}"
    echo -e "${RED}Failed: $FAILED_CHECKS${NC}"
    
    if [ ${#FAILED_SYNTAX[@]} -gt 0 ]; then
        echo ""
        echo -e "${RED}Syntax validation failures:${NC}"
        for failure in "${FAILED_SYNTAX[@]}"; do
            echo "  - $failure"
        done
    fi
    
    if [ ${#FAILED_NAMING[@]} -gt 0 ]; then
        echo ""
        echo -e "${RED}Naming convention failures:${NC}"
        for failure in "${FAILED_NAMING[@]}"; do
            echo "  - $failure"
        done
    fi
    
    if [ ${#FAILED_SYNC[@]} -gt 0 ]; then
        echo ""
        echo -e "${RED}Semantic equivalence failures:${NC}"
        for failure in "${FAILED_SYNC[@]}"; do
            echo "  - $failure"
        done
    fi
    
    echo ""
    if [ $FAILED_CHECKS -eq 0 ]; then
        echo -e "${GREEN}✓ All proto validation checks passed!${NC}"
        exit 0
    else
        echo -e "${RED}✗ Proto validation failed with $FAILED_CHECKS errors${NC}"
        exit 1
    fi
}

main "$@" 