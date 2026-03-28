#!/bin/bash

COMMAND=${1:-help}
MODEL="llama3.2-vision:11b"

case "$COMMAND" in
  run)
    echo "Starting Ollama (debug)..."
    OLLAMA_DEBUG=1 ollama serve
    ;;
  stop)
    echo "Stopping Ollama service..."
    systemctl stop ollama
    ;;
  pull)
    echo "Pulling model: $MODEL"
    ollama pull "$MODEL"
    ;;
  test)
    echo "Testing Ollama API..."
    curl http://localhost:11434/api/chat -d "{
      \"model\": \"$MODEL\",
      \"messages\": [{ \"role\": \"user\", \"content\": \"Say hello\" }],
      \"stream\": false
    }"
    ;;
  status)
    curl -s http://localhost:11434/api/tags | jq . 2>/dev/null || echo "Ollama not reachable at localhost:11434"
    ;;
  *)
    echo "Usage: ./ollama.local.sh [run|stop|pull|test|status]"
    echo ""
    echo "  run     Start Ollama with debug output"
    echo "  stop    Stop Ollama via systemctl"
    echo "  pull    Pull model: $MODEL"
    echo "  test    Send a test chat request"
    echo "  status  Show running models / check reachability"
    ;;
esac