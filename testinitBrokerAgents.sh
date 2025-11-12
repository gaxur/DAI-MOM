#!/bin/bash
# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # Sin color

# Función para imprimir con color
print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

# Verificar compilación
print_step "Checking compiled files ..."
if [ ! -f "MessageBrokerImpl.class" ]; then
    print_warning "Compiling files ..."
    javac *.java
    if [ $? -eq 0 ]; then
        print_success "Compilation successful"
    else
        print_error "Compilation failed"
        exit 1
    fi
else
    print_success "Files already compiled"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "OPTION 1: Launch agents and MOM tests"
echo "OPTION 2: View documentation (README.md)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
read -p "Select an option (1-2): " opcion

case $opcion in
    1)
        print_step "Starting complete demonstration ..."
        echo ""
        
        # Verificar si rmiregistry está corriendo
        print_step "Checking RMI registry ..."
        if ! pgrep -f rmiregistry > /dev/null; then
            print_warning "Starting rmiregistry ..."
            rmiregistry &
            sleep 2
            print_success "RMI registry started"
        else
            print_success "RMI registry is already running"
        fi
        
        echo ""
        print_step "Starting MessageBroker with agents system ..."
        echo ""
        java MessageBrokerImpl rmi://localhost/MessageBroker &
        BROKER_PID=$!
        sleep 3
        
        echo ""
        print_step "Waiting for broker to be ready ..."
        sleep 2
        
        echo ""
        print_step "Running agents system tests ..."
        echo ""
        java TestAgentsProducer rmi://localhost/MessageBroker
        
        echo ""
        print_warning "Press Enter to stop the broker ..."
        read
        
        print_step "Stopping broker ..."
        kill $BROKER_PID 2>/dev/null
        print_success "Demonstration completed"
        ;;
    2)
        print_step "Displaying documentation ..."
        echo ""
        
        if [ ! -f "README.md" ]; then
            print_error "README.md file not found"
            exit 1
        fi
        
        if command -v glow &> /dev/null; then
            glow README.md -p -s dark -w 100
        else
            sleep 2
            cat README.md | less -R
        fi
        ;;
    *)
        print_error "Invalid option. Please select 1 or 2."
        exit 1
        ;;
esac