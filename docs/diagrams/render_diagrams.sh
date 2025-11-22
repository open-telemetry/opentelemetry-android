#!/bin/bash

# Render Mermaid diagrams to PNG using mermaid.ink API
# No installation required - uses web service

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Create output directory
OUTPUT_DIR="exported"
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}=== Mermaid Diagram Renderer (via mermaid.ink) ===${NC}\n"

# Function to encode diagram and download PNG
render_diagram() {
    local input_file=$1
    local output_file=$2
    local diagram_name=$(basename "$input_file" .mmd)
    
    echo "📊 Rendering: ${diagram_name}..."
    
    # Read the mermaid file
    local diagram_content=$(cat "$input_file")
    
    # Base64 encode (URL-safe)
    local encoded=$(echo "$diagram_content" | base64)
    
    # Download from mermaid.ink
    local url="https://mermaid.ink/img/${encoded}?type=png"
    
    curl -s -o "$output_file" "$url"
    
    if [ -f "$output_file" ] && [ -s "$output_file" ]; then
        echo -e "${GREEN}   ✓ Saved: ${output_file}${NC}"
        return 0
    else
        echo -e "${YELLOW}   ⚠ Failed to download${NC}"
        return 1
    fi
}

# List of diagrams to render
DIAGRAMS=(
    "PR1_concurrency_before"
    "PR1_concurrency_after"
    "PR2_component_architecture"
    "PR2_metric_export_flow"
    "PR3_session_propagation"
    "PR3_before_after"
    "PR3_instrumentation_lifecycle"
)

# Render each diagram
SUCCESS=0
FAILED=0

for diagram in "${DIAGRAMS[@]}"; do
    INPUT_FILE="${diagram}.mmd"
    OUTPUT_FILE="${OUTPUT_DIR}/${diagram}.png"
    
    if [ -f "$INPUT_FILE" ]; then
        if render_diagram "$INPUT_FILE" "$OUTPUT_FILE"; then
            ((SUCCESS++))
        else
            ((FAILED++))
        fi
    else
        echo "⚠️  Skipping: ${INPUT_FILE} (not found)"
        ((FAILED++))
    fi
    
    # Small delay to avoid rate limiting
    sleep 1
done

echo ""
echo -e "${GREEN}=== Rendering Complete! ===${NC}"
echo "✓ Success: $SUCCESS"
if [ $FAILED -gt 0 ]; then
    echo "⚠ Failed: $FAILED"
fi
echo ""
echo "Images saved in: ${OUTPUT_DIR}/"
echo ""
echo "Next steps:"
echo "  1. Check images in ${OUTPUT_DIR}/"
echo "  2. Upload to your PR (drag & drop)"
echo "  3. Use the snippets in PR_DIAGRAM_SNIPPETS.md"

