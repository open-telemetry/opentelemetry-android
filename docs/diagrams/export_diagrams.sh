#!/bin/bash

# Export Mermaid diagrams to PNG images
# Requires: @mermaid-js/mermaid-cli
# Install: npm install -g @mermaid-js/mermaid-cli

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create output directory
OUTPUT_DIR="exported"
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}=== Mermaid Diagram Exporter ===${NC}\n"

# Check if mmdc is installed
if ! command -v mmdc &> /dev/null; then
    echo "❌ mermaid-cli (mmdc) is not installed"
    echo ""
    echo "Install it with:"
    echo "  npm install -g @mermaid-js/mermaid-cli"
    echo ""
    echo "Or use mermaid.live to export manually:"
    echo "  https://mermaid.live/"
    exit 1
fi

echo "✅ mermaid-cli found"
echo ""

# Export each diagram
DIAGRAMS=(
    "PR1_concurrency_before"
    "PR1_concurrency_after"
    "PR2_component_architecture"
    "PR2_metric_export_flow"
    "PR3_session_propagation"
    "PR3_before_after"
    "PR3_instrumentation_lifecycle"
)

for diagram in "${DIAGRAMS[@]}"; do
    INPUT_FILE="${diagram}.mmd"
    OUTPUT_FILE="${OUTPUT_DIR}/${diagram}.png"
    
    if [ -f "$INPUT_FILE" ]; then
        echo "📊 Exporting: ${diagram}..."
        mmdc -i "$INPUT_FILE" -o "$OUTPUT_FILE" -b transparent -w 1200
        echo -e "${GREEN}   ✓ Saved: ${OUTPUT_FILE}${NC}"
    else
        echo "⚠️  Skipping: ${INPUT_FILE} (not found)"
    fi
done

echo ""
echo -e "${GREEN}=== Export Complete! ===${NC}"
echo ""
echo "Images saved in: ${OUTPUT_DIR}/"
echo ""
echo "To use in PR descriptions:"
echo "  1. Upload images to GitHub (drag & drop into PR/issue)"
echo "  2. Copy the uploaded URL"
echo "  3. Use: ![Diagram Name](url)"
echo ""
echo "Or commit them to the repo:"
echo "  git add ${OUTPUT_DIR}/"
echo "  git commit -m 'Add diagram exports'"

