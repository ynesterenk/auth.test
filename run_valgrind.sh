#!/bin/bash

# Exit on any error
set -e

# Build OpenSCAD
mkdir -p build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Debug
make -j$(nproc)

# Define the OpenSCAD binary path
OPENSCAD_BINARY=$(pwd)/openscad

# Go back to the root directory
cd ..

# Create a temporary SCAD file to test
TEST_SCAD_FILE=$(mktemp --suffix=.scad)
echo "cube([1,1,1]);" > $TEST_SCAD_FILE

# Run Valgrind
valgrind --leak-check=full --suppressions=valgrind.supp --log-file=valgrind_report.log $OPENSCAD_BINARY -o /dev/null $TEST_SCAD_FILE

# Check the Valgrind report
if grep -q "ERROR SUMMARY: 0 errors" valgrind_report.log; then
  echo "No memory leaks detected."
else
  echo "Memory leaks detected. Check valgrind_report.log for details."
fi

# Clean up
rm $TEST_SCAD_FILE
