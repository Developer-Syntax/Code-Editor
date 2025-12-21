#!/bin/bash

PROJECT_NAME="codeeditor-android"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_FILE="${PROJECT_NAME}_${TIMESTAMP}.zip"

echo "================================================"
echo "  Project Zipper - High Compression Mode"
echo "================================================"
echo ""
echo "Starting to zip project: $PROJECT_NAME"
echo "Output file: $OUTPUT_FILE"
echo ""

if command -v zip &> /dev/null; then
    echo "Using zip with maximum compression (-9)..."
    echo "Including ALL files and folders (including cache, build, etc.)..."
    
    zip -9 -r "$OUTPUT_FILE" .
    
    if [ $? -eq 0 ]; then
        FILE_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
        echo ""
        echo "================================================"
        echo "  ZIP COMPLETED SUCCESSFULLY!"
        echo "================================================"
        echo "File: $OUTPUT_FILE"
        echo "Size: $FILE_SIZE"
        echo ""
        ls -lh "$OUTPUT_FILE"
    else
        echo "ERROR: Failed to create zip file"
        exit 1
    fi
else
    echo "ERROR: zip command not found. Please install zip."
    exit 1
fi
