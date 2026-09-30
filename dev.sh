#!/bin/bash

# 🚀 StarShop Development Script
# Quản lý toàn bộ môi trường development: ngrok + Spring Boot + MoMo integration

set -e

PORT=8080
NGROK_URL_FILE=".ngrok-url"

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
    print_color $YELLOW "🛑 Shutting down all services..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f ngrok 2>/dev/null || true
    rm -f ngrok.log
    print_color $GREEN "✅ Cleanup completed"
    exit 0
}

trap cleanup SIGINT SIGTERM

# Function to show usage
show_usage() {
    echo ""
    print_color $CYAN "🚀 StarShop Development Script"
    echo ""
    echo "Usage: ./dev.sh [command]"
    echo ""
    echo "Commands:"
    echo "  ngrok       - Start ngrok only (recommended for IDEA development)"
    echo "  start       - Start ngrok + Spring Boot"
    echo "  app         - Start Spring Boot only"
    echo "  stop        - Stop all services"
    echo "  status      - Show services status"
    echo "  url         - Show current ngrok URL"
    echo "  help        - Show this help"
    echo ""
    echo "Recommended workflow:"
    echo "  ./dev.sh ngrok        # Start ngrok tunnel"
    echo "  # Then run Spring Boot from IntelliJ IDEA"
    echo ""
    echo "Other examples:"
    echo "  ./dev.sh start        # Start full environment"
    echo "  ./dev.sh stop         # Stop all services"
    echo ""
}

# Function to check if ngrok is installed
check_ngrok() {
    if ! command -v ngrok &> /dev/null; then
        print_color $RED "❌ ngrok is not installed!"
        print_color $YELLOW "💡 Install with: brew install ngrok"
        print_color $YELLOW "💡 Or download from: https://ngrok.com/download"
        exit 1
    fi
}

# Function to start ngrok
start_ngrok() {
    print_color $BLUE "🌐 Starting ngrok tunnel on port $PORT..."
    
    # Kill existing ngrok processes
    pkill -f ngrok 2>/dev/null || true
    sleep 1
    
    # Start ngrok in background
    ngrok http $PORT --log=stdout > ngrok.log 2>&1 &
    NGROK_PID=$!
    
    # Wait for ngrok to start
    print_color $YELLOW "⏳ Waiting for ngrok to initialize..."
    sleep 3
    
    # Get public URL from ngrok API
    MAX_RETRIES=10
    RETRY_COUNT=0
    NGROK_URL=""
    
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o '"public_url":"https://[^"]*' | head -1 | cut -d'"' -f4)
        
        if [ ! -z "$NGROK_URL" ]; then
            print_color $GREEN "✅ Ngrok tunnel started successfully!"
            print_color $CYAN "🌐 Public URL: $NGROK_URL"
            echo "$NGROK_URL" > $NGROK_URL_FILE
            echo ""
            print_color $PURPLE "📝 MoMo Integration URLs:"
            echo "   Return URL (local): http://localhost:$PORT/payment/momo/return"
            echo "   Notify URL (ngrok): $NGROK_URL/payment/momo/notify"
            echo "   SSE endpoint: http://localhost:$PORT/sse/orders/{orderId}"
            echo ""
            print_color $BLUE "💡 Ngrok dashboard: http://localhost:4040"
            print_color $BLUE "🔧 Ngrok PID: $NGROK_PID"
            echo ""
            print_color $GREEN "🎯 Next step: Run Spring Boot from IntelliJ IDEA"
            print_color $YELLOW "💡 IDEA will automatically use NGROK_URL environment variable"
            echo ""
            return 0
        fi
        
        RETRY_COUNT=$((RETRY_COUNT + 1))
        print_color $YELLOW "⏳ Retry $RETRY_COUNT/$MAX_RETRIES..."
        sleep 2
    done
    
    print_color $RED "❌ Failed to start ngrok after $MAX_RETRIES retries"
    return 1
}

# Function to start Spring Boot
start_app() {
    print_color $GREEN "🍃 Starting Spring Boot application..."
    print_color $BLUE "📍 Local: http://localhost:$PORT"
    
    if [ -f "$NGROK_URL_FILE" ]; then
        NGROK_URL=$(cat $NGROK_URL_FILE)
        print_color $CYAN "🌍 Public: $NGROK_URL"
        export NGROK_URL=$NGROK_URL
    fi
    
    echo ""
    print_color $YELLOW "Press Ctrl+C to stop all services"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # Run Spring Boot with Maven
    ./mvnw spring-boot:run
}

# Function to stop all services
stop_services() {
    print_color $YELLOW "🛑 Stopping all services..."
    
    # Stop Spring Boot
    pkill -f "spring-boot:run" 2>/dev/null && print_color $GREEN "✅ Spring Boot stopped" || print_color $BLUE "ℹ️  Spring Boot not running"
    
    # Stop ngrok
    pkill -f ngrok 2>/dev/null && print_color $GREEN "✅ Ngrok stopped" || print_color $BLUE "ℹ️  Ngrok not running"
    
    # Clean up files
    rm -f .ngrok-url ngrok.log
    
    print_color $GREEN "✅ All services stopped"
}

# Function to show status
show_status() {
    print_color $CYAN "📊 Services Status:"
    echo ""
    
    # Check Spring Boot
    if pgrep -f "spring-boot:run" > /dev/null; then
        print_color $GREEN "✅ Spring Boot: Running"
        print_color $BLUE "   Local: http://localhost:$PORT"
    else
        print_color $RED "❌ Spring Boot: Not running"
    fi
    
    # Check ngrok
    if pgrep -f ngrok > /dev/null; then
        print_color $GREEN "✅ Ngrok: Running"
        if [ -f "$NGROK_URL_FILE" ]; then
            NGROK_URL=$(cat $NGROK_URL_FILE)
            print_color $CYAN "   Public: $NGROK_URL"
            print_color $BLUE "   Dashboard: http://localhost:4040"
        fi
    else
        print_color $RED "❌ Ngrok: Not running"
    fi
    
    echo ""
}

# Function to show current ngrok URL
show_url() {
    if [ -f "$NGROK_URL_FILE" ]; then
        NGROK_URL=$(cat $NGROK_URL_FILE)
        print_color $CYAN "🌐 Current ngrok URL: $NGROK_URL"
        echo ""
        print_color $PURPLE "📝 MoMo Integration URLs:"
        echo "   Return URL: http://localhost:$PORT/payment/momo/return"
        echo "   Notify URL: $NGROK_URL/payment/momo/notify"
        echo "   SSE Test: http://localhost:$PORT/payment/momo/sse-test"
    else
        print_color $RED "❌ No ngrok URL found. Start ngrok first."
    fi
}

# Main script logic  
case "${1:-ngrok}" in
    "start")
        print_color $CYAN "🚀 Starting full development environment..."
        check_ngrok
        if start_ngrok; then
            start_app
        else
            exit 1
        fi
        ;;
    "ngrok")
        print_color $CYAN "🌐 Starting ngrok tunnel for IDEA development..."
        check_ngrok
        if start_ngrok; then
            print_color $GREEN "🎉 Ngrok ready! Now run Spring Boot from IntelliJ IDEA"
            print_color $YELLOW "💡 Set environment variable in IDEA: NGROK_URL=$(cat $NGROK_URL_FILE 2>/dev/null || echo 'not-found')"
            echo ""
            print_color $BLUE "🔄 Keep this terminal open to maintain ngrok tunnel"
            print_color $BLUE "📊 Monitor ngrok traffic at: http://localhost:4040"
            echo ""
            # Keep ngrok running
            wait
        else
            exit 1
        fi
        ;;
    "app")
        print_color $CYAN "🍃 Starting Spring Boot only..."
        start_app
        ;;
    "stop")
        stop_services
        ;;
    "status")
        show_status
        ;;
    "url")
        show_url
        ;;
    "help"|"-h"|"--help")
        show_usage
        ;;
    *)
        print_color $RED "❌ Unknown command: $1"
        show_usage
        exit 1
        ;;
esac
