#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/../../.." || exit 1

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

printf "${YELLOW}Building Docker image...${NC}\n"
docker build -t messageformat-lexer-test -f syntax/pygments/test/Dockerfile . || {
    printf "${RED}Failed to build Docker image!${NC}\n"
    exit 1
}

printf "\n"
printf "${YELLOW}Running tests...${NC}\n"
printf "\n"

if [ "$1" = "--interactive" ] || [ "$1" = "-i" ]; then
    printf "${YELLOW}Starting interactive shell...${NC}\n"
    docker run --rm -it messageformat-lexer-test /bin/bash
else
    docker run --rm messageformat-lexer-test
    EXIT_CODE=$?

    printf "\n"
    if [ $EXIT_CODE -eq 0 ]; then
        printf "${GREEN}✓ All tests passed!${NC}\n"
    else
        printf "${RED}✗ Tests failed!${NC}\n"
    fi

    exit $EXIT_CODE
fi

