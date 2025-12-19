#!/bin/zsh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/../../.." || exit 1

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "${YELLOW}Building Docker image...${NC}"
docker build -t messageformat-lexer-test -f syntax/pygments/test/Dockerfile . || {
    echo "${RED}Failed to build Docker image!${NC}"
    exit 1
}

echo ""
echo "${YELLOW}Running tests...${NC}"
echo ""

if [[ "$1" == "--interactive" || "$1" == "-i" ]]; then
    echo "${YELLOW}Starting interactive shell...${NC}"
    docker run --rm -it messageformat-lexer-test /bin/bash
else
    docker run --rm messageformat-lexer-test
    EXIT_CODE=$?

    echo ""
    if [ $EXIT_CODE -eq 0 ]; then
        echo "${GREEN}✓ All tests passed!${NC}"
    else
        echo "${RED}✗ Tests failed!${NC}"
    fi

    exit $EXIT_CODE
fi

