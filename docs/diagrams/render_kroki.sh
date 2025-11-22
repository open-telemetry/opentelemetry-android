#!/bin/bash

# Render all Mermaid diagrams using Kroki API
set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

OUTPUT_DIR="exported"
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}=== Rendering diagrams with Kroki ===${NC}\n"

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
        
        # Use Kroki API to render diagram
        response=$(curl -s -w "\n%{http_code}" -X POST \
            "https://kroki.io/mermaid/png" \
            -H "Content-Type: text/plain" \
            --data-binary "@$INPUT_FILE" \
            -o "$OUTPUT_FILE")
        
        http_code=$(echo "$response" | tail -n1)
        
        if [ "$http_code" = "200" ] && [ -f "$OUTPUT_FILE" ] && [ -s "$OUTPUT_FILE" ]; then
            echo -e "${GREEN}   ✓ Saved: ${OUTPUT_FILE}${NC}"
        else
            echo -e "${RED}   ✗ Failed (HTTP $http_code)${NC}"
            rm -f "$OUTPUT_FILE"
        fi
    fi
done

echo ""
echo -e "${GREEN}=== Complete! ===${NC}"
echo ""
ls -lh "${OUTPUT_DIR}/" 2>/dev/null || true

