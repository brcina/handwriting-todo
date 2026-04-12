#!/bin/bash

COMMAND=${1:-help}
MODEL="qwen2.5vl:7b"
ENV_FILE="$(dirname "$0")/vastai.env"
IMAGE="vastai/ollama:0.20.0"
DISK=16

# Load env
if [ -f "$ENV_FILE" ]; then
  set -a; source "$ENV_FILE"; set +a
fi

# Helpers

update_env() {
  local key=$1
  local value=$2
  if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$ENV_FILE"
  else
    echo "${key}=${value}" >> "$ENV_FILE"
  fi
}

require_instance() {
  if [ -z "$VAST_INSTANCE_ID" ]; then
    echo "Error: VAST_INSTANCE_ID not set in vastai.env. Run './ollama.vastai.sh create <OFFER_ID>' first."
    exit 1
  fi
}

get_ollama_url() {
  vastai show instance "$VAST_INSTANCE_ID" --raw \
    | jq -r '"http://\(.public_ipaddr):\(.ports["11434/tcp"][0].HostPort)"'
}

case "$COMMAND" in

  search)
    MIN_VRAM=${2:-8}
    echo "Searching for GPU offers with >=${MIN_VRAM}GB VRAM, sorted by price..."
    vastai search offers "gpu_ram>=${MIN_VRAM} num_gpus=1" -o 'dph' --raw \
      | jq '.[0:5] | .[] | {id, gpu_name, gpu_ram, dph_total}'
    ;;

  create)
    OFFER_ID=$2
    if [ -z "$OFFER_ID" ]; then
      echo "Usage: ./ollama.vastai.sh create <OFFER_ID>"
      echo "       Run './ollama.vastai.sh search' to find an offer ID."
      exit 1
    fi
    echo "Creating instance from offer $OFFER_ID with image $IMAGE and model $MODEL..."
    RESULT=$(vastai create instance "$OFFER_ID" \
      --image "$IMAGE" \
      --env "-p 11434:11434 -e OLLAMA_MODEL=\"$MODEL\"" \
      --onstart-cmd 'entrypoint.sh' \
      --disk "$DISK" --ssh --direct)
    echo "$RESULT"

    INSTANCE_ID=$(echo "$RESULT" | sed "s/.*'new_contract': \([0-9]*\).*/\1/")
    INSTANCE_API_KEY=$(echo "$RESULT" | sed "s/.*'instance_api_key': '\([^']*\)'.*/\1/")

    if [ -n "$INSTANCE_ID" ]; then
      update_env "VAST_INSTANCE_ID" "$INSTANCE_ID"
      echo "Saved VAST_INSTANCE_ID=$INSTANCE_ID to $ENV_FILE"
    fi
    if [ -n "$INSTANCE_API_KEY" ]; then
      update_env "VAST_API_TOKEN" "$INSTANCE_API_KEY"
      echo "Saved VAST_API_TOKEN to $ENV_FILE"
    fi
    echo ""
    echo "Instance created. Wait for it to start, then run './ollama.vastai.sh env' to update the Ollama URL."
    ;;

  env)
    require_instance
    echo "Fetching Ollama URL for instance $VAST_INSTANCE_ID..."
    OLLAMA_URL=$(get_ollama_url)
    if [ -z "$OLLAMA_URL" ] || [ "$OLLAMA_URL" = "null" ] || [[ "$OLLAMA_URL" == *"null"* ]]; then
      echo "Error: Could not resolve Ollama URL. Is the instance running?"
      vastai show instance "$VAST_INSTANCE_ID" --raw | jq '{status: .actual_status, ip: .public_ipaddr}'
      exit 1
    fi
    update_env "SPRING_AI_OLLAMA_BASE_URL" "$OLLAMA_URL"
    echo "Updated SPRING_AI_OLLAMA_BASE_URL=$OLLAMA_URL in $ENV_FILE"
    ;;

  start)
    require_instance
    echo "Starting instance $VAST_INSTANCE_ID..."
    vastai start instance "$VAST_INSTANCE_ID"
    ;;

  stop)
    require_instance
    echo "Stopping instance $VAST_INSTANCE_ID..."
    vastai stop instance "$VAST_INSTANCE_ID"
    ;;

  destroy)
    require_instance
    echo "Destroying instance $VAST_INSTANCE_ID..."
    read -rp "Are you sure? [y/N] " CONFIRM
    if [[ "$CONFIRM" =~ ^[Yy]$ ]]; then
      vastai destroy instance "$VAST_INSTANCE_ID"
      update_env "VAST_INSTANCE_ID" ""
      update_env "SPRING_AI_OLLAMA_BASE_URL" ""
      echo "Instance destroyed and env cleared."
    else
      echo "Aborted."
    fi
    ;;

  status)
    require_instance
    echo "Status of instance $VAST_INSTANCE_ID:"
    vastai show instance "$VAST_INSTANCE_ID" --raw \
      | jq '{status: .actual_status, ip: .public_ipaddr, gpu: .gpu_name, dph: .dph_total}'
    echo ""
    echo "Ollama reachability check at $SPRING_AI_OLLAMA_BASE_URL:"
    curl -s --max-time 5 "${SPRING_AI_OLLAMA_BASE_URL}/api/tags" | jq . 2>/dev/null \
      || echo "Ollama not reachable at $SPRING_AI_OLLAMA_BASE_URL"
    ;;

  logs)
    require_instance
    echo "Logs for instance $VAST_INSTANCE_ID:"
    vastai logs "$VAST_INSTANCE_ID"
    ;;

  watch)
    require_instance
    echo "Watching logs for instance $VAST_INSTANCE_ID (Ctrl+C to stop)..."
    watch -n 5 "vastai logs $VAST_INSTANCE_ID | tail -20"
    ;;

  pull)
    require_instance
    echo "Pulling model '$MODEL' on instance $VAST_INSTANCE_ID via ${SPRING_AI_OLLAMA_BASE_URL}..."
    curl -s "${SPRING_AI_OLLAMA_BASE_URL}/api/pull" -d "{\"name\": \"$MODEL\"}" | jq .
    ;;

  test)
    echo "Testing Ollama API at ${SPRING_AI_OLLAMA_BASE_URL}..."
    curl "${SPRING_AI_OLLAMA_BASE_URL}/api/chat" -d "{
      \"model\": \"$MODEL\",
      \"messages\": [{ \"role\": \"user\", \"content\": \"Say hello\" }],
      \"stream\": false
    }"
    ;;

  *)
    echo "Usage: ./ollama.vastai.sh <command> [args]"
    echo ""
    echo "  search [MIN_VRAM]   Search GPU offers (default: >=16GB VRAM), sorted by price"
    echo "  create <OFFER_ID>   Create instance, save ID + API key to vastai.env"
    echo "  env                 Resolve Ollama URL and update SPRING_AI_OLLAMA_BASE_URL in vastai.env"
    echo "  start               Start stopped instance"
    echo "  stop                Stop running instance"
    echo "  destroy             Destroy instance (asks for confirmation)"
    echo "  status              Show instance status + Ollama reachability"
    echo "  logs                Show instance logs"
    echo "  watch               Watch instance logs (refreshes every 5s)"
    echo "  pull                Pull model '$MODEL' on the remote instance"
    echo "  test                Send a test chat request to remote Ollama"
    echo ""
    echo "Instance: ${VAST_INSTANCE_ID:-<not set>}"
    echo "Ollama:   ${SPRING_AI_OLLAMA_BASE_URL:-<not set>}"
    ;;
esac