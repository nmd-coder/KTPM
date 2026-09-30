#!/bin/bash

# 🚀 StarShop Development Script (VS Code Port Forwarding)
# Quản lý Spring Boot application với VS Code port forwarding

set -e

PORT=8080
VSCODE_URL_FILE=".vscode-forward-url"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    printf "${1}${2}${NC}\n"
}

# Function to cleanup on exit
cleanup() {
    echo ""
    print_color $YELLOW "🛑 Shutting down Spring Boot application..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    print_color $GREEN "✅ Cleanup completed"
    exit 0
}

trap cleanup SIGINT SIGTERM

# Function to show usage
show_usage() {
    echo ""
    print_color $CYAN "🚀 StarShop Development Script (VS Code Port Forwarding)"
    echo ""
    echo "Usage: ./dev-vscode.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start       - Start Spring Boot application"
    echo "  stop        - Stop Spring Boot application"
    echo "  status      - Show application status"
    echo "  set-url     - Set VS Code forwarding URL"
    echo "  help        - Show this help message"
    echo ""
    print_color $YELLOW "📝 Note: Make sure to set your VS Code forwarding URL first using 'set-url' command"
    echo ""
}

# Function to set VS Code forwarding URL
set_vscode_url() {
    echo ""
    print_color $CYAN "🔗 Setting VS Code Port Forwarding URL"
    echo ""
    echo "Please enter your VS Code forwarding URL (e.g., https://abc123-8080.app.github.dev):"
    read -r vscode_url
    
    if [[ -z "$vscode_url" ]]; then
        print_color $RED "❌ URL cannot be empty"
        exit 1
    fi
    
    # Validate URL format
    if [[ ! "$vscode_url" =~ ^https?:// ]]; then
        print_color $RED "❌ URL must start with http:// or https://"
        exit 1
    fi
    
    echo "$vscode_url" > "$VSCODE_URL_FILE"
    print_color $GREEN "✅ VS Code forwarding URL saved: $vscode_url"
    
    # Set environment variable for current session
    export VSCODE_FORWARD_URL="$vscode_url"
    print_color $BLUE "🔧 Environment variable VSCODE_FORWARD_URL set for this session"
}

# Function to get VS Code URL
get_vscode_url() {
    if [[ -f "$VSCODE_URL_FILE" ]]; then
        cat "$VSCODE_URL_FILE"
    else
        echo ""
    fi
}

# Function to start Spring Boot
start_spring_boot() {
    print_color $BLUE "🚀 Starting Spring Boot application..."
    
    # Check if VS Code URL is set
    vscode_url=$(get_vscode_url)
    if [[ -z "$vscode_url" ]]; then
        print_color $YELLOW "⚠️  VS Code forwarding URL not set. Use 'set-url' command first or the app will use localhost URLs."
        echo ""
    else
        print_color $GREEN "🔗 Using VS Code forwarding URL: $vscode_url"
        export VSCODE_FORWARD_URL="$vscode_url"
    fi
    
    # Check if port is available
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
        print_color $RED "❌ Port $PORT is already in use"
        print_color $YELLOW "💡 Stop the existing process or use a different port"
        exit 1
    fi
    
    print_color $BLUE "📦 Building and starting Spring Boot..."
    mvn spring-boot:run &
    SPRING_PID=$!
    
    # Wait for Spring Boot to start
    print_color $YELLOW "⏳ Waiting for Spring Boot to start..."
    sleep 10
    
    # Check if Spring Boot started successfully
    if ps -p $SPRING_PID > /dev/null; then
        print_color $GREEN "✅ Spring Boot started successfully!"
        print_color $CYAN "🌐 Application URLs:"
        echo "   Local: http://localhost:$PORT"
        if [[ -n "$vscode_url" ]]; then
            echo "   VS Code Forward: $vscode_url"
        fi
        echo ""
        print_color $YELLOW "📝 MoMo callback will use: ${VSCODE_FORWARD_URL:-http://localhost:$PORT}/payment/momo/notify"
        echo ""
        print_color $BLUE "🎯 Press Ctrl+C to stop all services"
        wait $SPRING_PID
    else
        print_color $RED "❌ Failed to start Spring Boot"
        exit 1
    fi
}

# Function to stop services
stop_services() {
    print_color $YELLOW "🛑 Stopping Spring Boot application..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    print_color $GREEN "✅ Spring Boot stopped"
}

# Function to show status
show_status() {
    print_color $CYAN "📊 Service Status"
    echo ""
    
    # Check Spring Boot
    if pgrep -f "spring-boot:run" > /dev/null; then
        print_color $GREEN "✅ Spring Boot: Running"
    else
        print_color $RED "❌ Spring Boot: Not running"
    fi
    
    # Check VS Code URL
    vscode_url=$(get_vscode_url)
    if [[ -n "$vscode_url" ]]; then
        print_color $GREEN "✅ VS Code URL: $vscode_url"
    else
        print_color $YELLOW "⚠️  VS Code URL: Not set"
    fi
    
    echo ""
}

# Main script logic
case "${1:-help}" in
    "start")
        start_spring_boot
        ;;
    "stop")
        stop_services
        ;;
    "status")
        show_status
        ;;
    "set-url")
        set_vscode_url
        ;;
    "help"|*)
        show_usage
        ;;
esac
