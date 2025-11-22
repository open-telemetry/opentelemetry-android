#!/bin/bash

# Render all Mermaid diagrams with WHITE background for better visibility
set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

OUTPUT_DIR="exported"
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}=== Re-rendering diagrams with WHITE backgrounds ===${NC}\n"

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
        echo "📊 Rendering: ${diagram}..."
        cat "$INPUT_FILE" | curl -s -X POST -H "Content-Type: text/plain" \
            --data-binary @- \
            "https://kroki.io/mermaid/png?background=white" \
            -o "$OUTPUT_FILE"
        
        if [ -f "$OUTPUT_FILE" ] && [ -s "$OUTPUT_FILE" ]; then
            echo -e "${GREEN}   ✓ Saved: ${OUTPUT_FILE}${NC}"
        else
            echo "   ⚠ Failed"
        fi
    fi
done

echo ""
echo -e "${GREEN}=== Complete! All diagrams now have white backgrounds ===${NC}"
echo ""
ls -lh "${OUTPUT_DIR}/"

